package org.motechproject.scheduletracking.it;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.commons.date.model.Time;
import org.motechproject.scheduletracking.domain.Enrollment;
import org.motechproject.scheduletracking.domain.Schedule;
import org.motechproject.scheduletracking.domain.ScheduleFactory;
import org.motechproject.scheduletracking.domain.json.ScheduleRecord;
import org.motechproject.scheduletracking.repository.AllEnrollments;
import org.motechproject.scheduletracking.repository.AllSchedules;
import org.motechproject.scheduletracking.repository.TrackedSchedulesJsonReaderImpl;
import org.motechproject.scheduletracking.service.EnrollmentRecord;
import org.motechproject.scheduletracking.service.EnrollmentRequest;
import org.motechproject.scheduletracking.service.EnrollmentsQuery;
import org.motechproject.scheduletracking.service.ScheduleTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.motechproject.commons.date.util.DateUtil.newDateTime;
import static org.motechproject.commons.date.util.DateUtil.now;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/motech/*.xml")
public class ScheduleTrackingServiceIT {

    @Autowired
    private AllSchedules allSchedules;
    @Autowired
    private ScheduleTrackingService scheduleTrackingService;
    @Autowired
    private AllEnrollments allEnrollments;
    @Autowired
    private ScheduleFactory scheduleFactory;

    @Before
    public void setUp(){
        List<ScheduleRecord> scheduleRecords = new TrackedSchedulesJsonReaderImpl().getAllSchedules("/schedules");
        for (ScheduleRecord scheduleRecord : scheduleRecords) {
            Schedule schedule = scheduleFactory.build(scheduleRecord, Locale.ENGLISH);
            allSchedules.add(schedule);
        }
    }

    @After
    public void tearDown() {
        allEnrollments.removeAll();
        allSchedules.removeAll();
    }

    @Test
    public void shouldUpdateEnrollmentIfAnActiveEnrollmentAlreadyExists() {
        Enrollment activeEnrollment = allEnrollments.getActiveEnrollment("externalId", "IPTI Schedule");
        assertNull("Active enrollment present", activeEnrollment);

        Time originalPreferredAlertTime = new Time(8, 10);
        DateTime now = now();
        String enrollmentId = scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId("externalId").setScheduleName("IPTI Schedule").setPreferredAlertTime(originalPreferredAlertTime).setReferenceDate(now.toLocalDate()).setReferenceTime(null).setEnrollmentDate(null).setEnrollmentTime(null).setStartingMilestoneName(null).setMetadata(null));
        assertNotNull("EnrollmentId is null", enrollmentId);

        activeEnrollment = allEnrollments.get(enrollmentId);
        assertNotNull("No active enrollment present", activeEnrollment);
        assertEquals(originalPreferredAlertTime, activeEnrollment.getPreferredAlertTime());
        assertEquals(newDateTime(now.toLocalDate(), new Time(0, 0)), activeEnrollment.getStartOfSchedule());

        Time updatedPreferredAlertTime = new Time(2, 5);
        DateTime updatedReferenceDate = now.minusDays(1);
        String updatedEnrollmentId = scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId("externalId").setScheduleName("IPTI Schedule").setPreferredAlertTime(updatedPreferredAlertTime).setReferenceDate(updatedReferenceDate.toLocalDate()).setReferenceTime(null).setEnrollmentDate(null).setEnrollmentTime(null).setStartingMilestoneName(null));
        assertEquals(enrollmentId, updatedEnrollmentId);

        activeEnrollment = allEnrollments.get(updatedEnrollmentId);
        assertNotNull("No active enrollment present", activeEnrollment);
        assertEquals(updatedPreferredAlertTime, activeEnrollment.getPreferredAlertTime());
        assertEquals(newDateTime(updatedReferenceDate.toLocalDate(), new Time(0, 0)), activeEnrollment.getStartOfSchedule());
    }

    @Test
    public void fulfillMilestoneShouldBeIdempotent() {
        scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId("entity_1").setScheduleName("IPTI Schedule").setPreferredAlertTime(null).setReferenceDate(LocalDate.now()).setReferenceTime(null).setEnrollmentDate(LocalDate.now()).setEnrollmentTime(null).setStartingMilestoneName(null).setMetadata(null));
        scheduleTrackingService.fulfillCurrentMilestone("entity_1", "IPTI Schedule", LocalDate.now(), new Time(8, 20));
        scheduleTrackingService.fulfillCurrentMilestone("entity_1", "IPTI Schedule", LocalDate.now(), new Time(8, 20));

        List<EnrollmentRecord> enrollment = scheduleTrackingService.search(new EnrollmentsQuery().havingExternalId("entity_1").havingSchedule("IPTI Schedule"));
        assertEquals("IPTI 2", enrollment.get(0).getCurrentMilestoneName());
    }

    @Test
    public void shouldReturnScheduleFromDb() {
        Schedule schedule = scheduleTrackingService.getScheduleByName("IPTI Schedule");

        assertNotNull(schedule);
        assertEquals("IPTI 1", schedule.getFirstMilestone().getName());
        assertEquals(2, schedule.getMilestones().size());
        assertNotNull(schedule.getMilestone("IPTI 1"));
        assertEquals("IPTI Schedule", schedule.getName());
        assertEquals("IPTI 2", schedule.getNextMilestoneName("IPTI 1"));
        assertNull(schedule.getNextMilestoneName("IPTI 2"));
    }

    @Test
    public void shouldNotReturnScheduleThatDoesNotExist() {
        Schedule schedule = scheduleTrackingService.getScheduleByName("Fake Schedule");
        assertNull(schedule);
    }

    @Test
    public void shouldReturnAllSchedules() {
        List<Schedule> schedules = scheduleTrackingService.getAllSchedules();
        assertEquals(10, schedules.size());
    }
}
