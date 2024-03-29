package org.motechproject.messagecampaign.scheduler;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.motechproject.commons.date.model.Time;
import org.motechproject.event.MotechEvent;
import org.motechproject.messagecampaign.EventKeys;
import org.motechproject.messagecampaign.dao.AllMessageCampaigns;
import org.motechproject.messagecampaign.domain.campaign.CampaignEnrollment;
import org.motechproject.messagecampaign.domain.campaign.OffsetCampaign;
import org.motechproject.messagecampaign.domain.message.CampaignMessage;
import org.motechproject.messagecampaign.domain.message.OffsetCampaignMessage;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.scheduler.contract.RunOnceSchedulableJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.motechproject.commons.date.util.DateUtil.newDateTime;
import static org.motechproject.commons.date.util.DateUtil.now;

/**
 *  SchedulerService responsible for (un)scheduling offset campaign enrollment
 */

@Component
public class OffsetCampaignSchedulerService extends CampaignSchedulerService<OffsetCampaignMessage, OffsetCampaign> {

    private Logger logger = Logger.getLogger(this.getClass());

    @Autowired
    public OffsetCampaignSchedulerService(MotechSchedulerService schedulerService, AllMessageCampaigns allMessageCampaigns) {
        super(schedulerService, allMessageCampaigns);
    }

    @Override
    protected void scheduleMessageJob(CampaignEnrollment enrollment, CampaignMessage message) {
        OffsetCampaignMessage offsetMessage = (OffsetCampaignMessage) message;

        Time deliverTime = deliverTimeFor(enrollment, message);
        DateTime jobTime = (newDateTime(enrollment.getReferenceDate(), deliverTime)).toLocalDateTime()
                .plusSeconds(offsetMessage.timeOffset().toStandardSeconds().getSeconds()).toDateTime();
        if (jobTime.isAfter(now())) {
            MotechEvent motechEvent = new MotechEvent(EventKeys.SEND_MESSAGE, jobParams(message.messageKey(), enrollment));
            RunOnceSchedulableJob runOnceSchedulableJob = new RunOnceSchedulableJob(motechEvent, jobTime.toDate());

            try {
                getSchedulerService().scheduleRunOnceJob(runOnceSchedulableJob);
            } catch (IllegalArgumentException e) {
                logger.info("Unable to schedule offset campaign message " + message.messageKey() + " for ID: " + enrollment.getExternalId() + " enrolled in campaign: " + enrollment.getCampaignName() + " - Message date is in the past");
            }
        }
    }

    @Override
    public void stop(CampaignEnrollment enrollment) {
        OffsetCampaign campaign = (OffsetCampaign) getAllMessageCampaigns().getCampaign(enrollment.getCampaignName());
        for (OffsetCampaignMessage message : campaign.getMessages()) {
            getSchedulerService().safeUnscheduleRunOnceJob(EventKeys.SEND_MESSAGE, messageJobIdFor(message.messageKey(), enrollment.getExternalId(), enrollment.getCampaignName()));
        }
    }
}
