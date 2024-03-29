package org.motechproject.pillreminder.service;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.scheduler.factory.MotechSchedulerFactoryBean;
import org.motechproject.scheduler.service.impl.MotechSchedulerServiceImpl;
import org.motechproject.pillreminder.contract.DailyPillRegimenRequest;
import org.motechproject.pillreminder.contract.DosageRequest;
import org.motechproject.pillreminder.contract.MedicineRequest;
import org.motechproject.pillreminder.dao.AllPillRegimens;
import org.motechproject.pillreminder.domain.PillRegimen;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:testApplicationPillReminder.xml"})
public class PillReminderServiceIT {
    @Autowired
    private org.motechproject.pillreminder.service.PillReminderService pillReminderService;
    @Autowired
    private MotechSchedulerFactoryBean motechSchedulerFactoryBean;
    
    @Autowired
    private AllPillRegimens allPillRegimens;

    private Scheduler scheduler;
    private LocalDate startDate;
    private LocalDate endDate;

    @Before
    public void setUp() {
        scheduler = motechSchedulerFactoryBean.getQuartzScheduler();
        startDate = DateUtil.newDate(2020, 1, 20);
        endDate = DateUtil.newDate(2021, 1, 20);
    }

    @Test
    public void shouldSaveTheDailyPillRegimenAndScheduleJob() throws SchedulerException {
        String externalId = "1234";
        allPillRegimens.removeAll("externalId", externalId);
        int scheduledJobsNum = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(MotechSchedulerServiceImpl.JOB_GROUP_NAME)).size();

        ArrayList<MedicineRequest> medicineRequests = new ArrayList<MedicineRequest>();
        MedicineRequest medicineRequest1 = new MedicineRequest("m1", startDate, endDate);
        medicineRequests.add(medicineRequest1);
        MedicineRequest medicineRequest2 = new MedicineRequest("m2", startDate, startDate.plusDays(5));
        medicineRequests.add(medicineRequest2);

        ArrayList<DosageRequest> dosageContracts = new ArrayList<DosageRequest>();
        dosageContracts.add(new DosageRequest(9, 5, medicineRequests));

        pillReminderService.createNew(new DailyPillRegimenRequest(externalId, 2, 15, 5, dosageContracts));
        Assert.assertEquals(scheduledJobsNum + 1, scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(MotechSchedulerServiceImpl.JOB_GROUP_NAME)).size());
    }

    @Test
    public void shouldRenewThePillRegimenAndScheduleJob() throws SchedulerException {

        int scheduledJobsNum = scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(MotechSchedulerServiceImpl.JOB_GROUP_NAME)).size();

        ArrayList<MedicineRequest> medicineRequests = new ArrayList<MedicineRequest>();
        MedicineRequest medicineRequest1 = new MedicineRequest("m1", startDate, endDate);
        medicineRequests.add(medicineRequest1);
        MedicineRequest medicineRequest2 = new MedicineRequest("m2", startDate, startDate.plusDays(5));
        medicineRequests.add(medicineRequest2);

        ArrayList<DosageRequest> dosageContracts = new ArrayList<DosageRequest>();
        dosageContracts.add(new DosageRequest(9, 5, medicineRequests));

        String externalId = "123456789";
        pillReminderService.createNew(new DailyPillRegimenRequest(externalId, 2, 15, 5, dosageContracts));

        ArrayList<DosageRequest> newDosageContracts = new ArrayList<DosageRequest>();
        newDosageContracts.add(new DosageRequest(9, 5, Arrays.asList(new MedicineRequest("m1", DateUtil.today(), DateUtil.today().plusDays(100)))));
        newDosageContracts.add(new DosageRequest(4, 5, Arrays.asList(new MedicineRequest("m2", DateUtil.today(), DateUtil.today().plusDays(100)))));
        pillReminderService.renew(new DailyPillRegimenRequest(externalId, 2, 15, 5, newDosageContracts));
        Assert.assertEquals(scheduledJobsNum + 2, scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals(MotechSchedulerServiceImpl.JOB_GROUP_NAME)).size());
        PillRegimen regimen = allPillRegimens.findByExternalId(externalId);
        allPillRegimens.remove(regimen);
    }
}
