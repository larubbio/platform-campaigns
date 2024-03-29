package org.motechproject.messagecampaign.scheduler;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.commons.date.model.Time;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.messagecampaign.builder.CampaignBuilder;
import org.motechproject.messagecampaign.builder.EnrollRequestBuilder;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.dao.AllMessageCampaigns;
import org.motechproject.messagecampaign.domain.campaign.Campaign;
import org.motechproject.messagecampaign.domain.campaign.CampaignEnrollment;
import org.motechproject.messagecampaign.domain.campaign.OffsetCampaign;
import org.motechproject.messagecampaign.domain.message.OffsetCampaignMessage;
import org.motechproject.messagecampaign.scheduler.exception.CampaignEnrollmentException;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.scheduler.contract.RunOnceSchedulableJob;

import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.joda.time.Period.days;
import static org.joda.time.Period.minutes;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.commons.date.util.DateUtil.newDateTime;
import static org.motechproject.commons.date.util.DateUtil.today;
import static org.motechproject.testing.utils.TimeFaker.fakeNow;
import static org.motechproject.testing.utils.TimeFaker.stopFakingTime;

public class OffsetCampaignSchedulerServiceTest {

    OffsetCampaignSchedulerService offsetCampaignSchedulerService;

    @Mock
    private MotechSchedulerService schedulerService;
    @Mock
    private AllMessageCampaigns allMessageCampaigns;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        initMocks(this);
        offsetCampaignSchedulerService = new OffsetCampaignSchedulerService(schedulerService, allMessageCampaigns);
    }

    @Test
    public void shouldScheduleJobForOffsetCampaignMessage() {
        try {
            fakeNow(newDateTime(2010, 10, 1));

            Campaign campaign = new OffsetCampaign();
            campaign.setName("camp");
            when(allMessageCampaigns.getCampaign("camp")).thenReturn(campaign);

            offsetCampaignSchedulerService.scheduleMessageJob(
                    new CampaignEnrollment("entity1", "camp").setReferenceDate(new LocalDate(2010, 10, 3)),
                    new OffsetCampaignMessage(days(5)).messageKey("foo").setStartTime(5, 30));

            ArgumentCaptor<RunOnceSchedulableJob> jobCaptor = ArgumentCaptor.forClass(RunOnceSchedulableJob.class);
            verify(schedulerService).scheduleRunOnceJob(jobCaptor.capture());
            RunOnceSchedulableJob job = jobCaptor.getValue();

            assertEquals(newDateTime(2010, 10, 8, 5, 30, 0).toDate(), job.getStartDate());
        } finally {
            stopFakingTime();
        }
    }

    @Test
    public void shouldHandleTheCaseWhenOffsetIsLessThanADayAndEnrollWithAppropriateTime() {
        try {
            fakeNow(newDateTime(2010, 10, 1));

            Campaign campaign = new OffsetCampaign();
            campaign.setName("camp");
            when(allMessageCampaigns.getCampaign("camp")).thenReturn(campaign);

            offsetCampaignSchedulerService.scheduleMessageJob(
                    new CampaignEnrollment("entity1", "camp").setReferenceDate(new LocalDate(2010, 10, 3)).setDeliverTime(8, 20),
                    new OffsetCampaignMessage(minutes(5)).messageKey("foo").setStartTime(5, 30));

            ArgumentCaptor<RunOnceSchedulableJob> jobCaptor = ArgumentCaptor.forClass(RunOnceSchedulableJob.class);
            verify(schedulerService).scheduleRunOnceJob(jobCaptor.capture());
            RunOnceSchedulableJob job = jobCaptor.getValue();

            assertEquals(newDateTime(2010, 10, 3, 8, 25, 0).toDate(), job.getStartDate());
        } finally {
            stopFakingTime();
        }
    }

    @Test
    public void userPreferredDeliverTimeShouldOverrideMessageDeliverTime() {
        try {
            fakeNow(newDateTime(2010, 10, 1));

            Campaign campaign = new OffsetCampaign();
            campaign.setName("camp");
            when(allMessageCampaigns.getCampaign("camp")).thenReturn(campaign);

            offsetCampaignSchedulerService.scheduleMessageJob(
                    new CampaignEnrollment("entity1", "camp").setReferenceDate(new LocalDate(2010, 10, 3)).setDeliverTime(8, 20),
                    new OffsetCampaignMessage(days(3)).messageKey("foo").setStartTime(5, 30));

            ArgumentCaptor<RunOnceSchedulableJob> jobCaptor = ArgumentCaptor.forClass(RunOnceSchedulableJob.class);
            verify(schedulerService).scheduleRunOnceJob(jobCaptor.capture());
            RunOnceSchedulableJob job = jobCaptor.getValue();

            assertEquals(newDateTime(2010, 10, 6, 8, 20, 0).toDate(), job.getStartDate());
        } finally {
            stopFakingTime();
        }
    }

    @Test
    public void shouldScheduleJobsAfterGivenTimeOffsetIntervalFromReferenceDate_WhenCampaignStartOffsetIsZero() {
        CampaignRequest request = new EnrollRequestBuilder().withDefaults().withReferenceDate(today()).build();
        OffsetCampaign campaign = new CampaignBuilder().defaultOffsetCampaign();

        OffsetCampaignSchedulerService offsetCampaignScheduler = new OffsetCampaignSchedulerService(schedulerService, allMessageCampaigns);

        when(allMessageCampaigns.getCampaign("testCampaign")).thenReturn(campaign);

        CampaignEnrollment enrollment = new CampaignEnrollment("12345", "testCampaign").setReferenceDate(today()).setDeliverTime(new Time(9, 30));
        offsetCampaignScheduler.start(enrollment);
        ArgumentCaptor<RunOnceSchedulableJob> capture = ArgumentCaptor.forClass(RunOnceSchedulableJob.class);
        verify(schedulerService, times(2)).scheduleRunOnceJob(capture.capture());

        List<RunOnceSchedulableJob> allJobs = capture.getAllValues();

        Date startDate1 = DateUtil.newDateTime(DateUtil.today().plusDays(7), request.deliverTime().getHour(), request.deliverTime().getMinute(), 0).toDate();
        Assert.assertEquals(startDate1.toString(), allJobs.get(0).getStartDate().toString());
        Assert.assertEquals("org.motechproject.messagecampaign.fired-campaign-message", allJobs.get(0).getMotechEvent().getSubject());
        assertMotechEvent(allJobs.get(0), "MessageJob.testCampaign.12345.child-info-week-1", "child-info-week-1");

        Date startDate2 = DateUtil.newDateTime(DateUtil.today().plusDays(14), request.deliverTime().getHour(), request.deliverTime().getMinute(), 0).toDate();
        Assert.assertEquals(startDate2.toString(), allJobs.get(1).getStartDate().toString());
        Assert.assertEquals("org.motechproject.messagecampaign.fired-campaign-message", allJobs.get(1).getMotechEvent().getSubject());
        assertMotechEvent(allJobs.get(1), "MessageJob.testCampaign.12345.child-info-week-1a", "child-info-week-1a");
    }

    @Test
    public void shouldCheckIfStartTimeIsProvidedWithCampaignMessageOrEnrollmentForSchedulingACampaign() {
        int timeOffsetGreaterThanADay = (24 * 60 * 60) + 1;
        String externalId = "externalId";
        String campaignName = "campaignName";
        CampaignEnrollment enrollment = new CampaignEnrollment(externalId, campaignName).setDeliverTime(null);
        OffsetCampaignMessage campaignMessage = new OffsetCampaignMessage();
        campaignMessage.name(campaignName);
        campaignMessage.setStartTime(null);
        campaignMessage.timeOffset(new Period(timeOffsetGreaterThanADay * 1000));

        expectedException.expect(CampaignEnrollmentException.class);
        expectedException.expectMessage(String.format("Cannot enroll %s for message campaign %s - Start time not defined for campaign. Define it in campaign-message.json or at enrollment time", externalId, campaignName));

        offsetCampaignSchedulerService.scheduleMessageJob(enrollment, campaignMessage);

        verifyZeroInteractions(schedulerService);
    }

    private void assertMotechEvent(RunOnceSchedulableJob runOnceSchedulableJob, String expectedJobId, String messageKey) {
        Assert.assertEquals(expectedJobId, runOnceSchedulableJob.getMotechEvent().getParameters().get("JobID"));
        Assert.assertEquals("testCampaign", runOnceSchedulableJob.getMotechEvent().getParameters().get("CampaignName"));
        Assert.assertEquals("12345", runOnceSchedulableJob.getMotechEvent().getParameters().get("ExternalID"));
        Assert.assertEquals(messageKey, runOnceSchedulableJob.getMotechEvent().getParameters().get("MessageKey"));
    }
}
