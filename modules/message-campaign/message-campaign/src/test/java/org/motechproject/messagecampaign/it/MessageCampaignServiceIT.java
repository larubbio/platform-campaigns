package org.motechproject.messagecampaign.it;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.domain.campaign.CampaignType;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.motechproject.messagecampaign.userspecified.CampaignMessageRecord;
import org.motechproject.messagecampaign.userspecified.CampaignRecord;
import org.motechproject.scheduler.factory.MotechSchedulerFactoryBean;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.quartz.TriggerKey.triggerKey;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:testMessageCampaignApplicationContext.xml")
public class MessageCampaignServiceIT {

    @Autowired
    MessageCampaignService messageCampaignService;

    @Autowired
    MotechSchedulerFactoryBean motechSchedulerFactoryBean;

    Scheduler scheduler;

    @Before
    public void setup() {
        scheduler = motechSchedulerFactoryBean.getQuartzScheduler();
    }

    @Test
    public void shouldUnscheduleMessageJobsWhenCampaignIsStopped() throws SchedulerException {
        final String campaignName = "Malnutrition";
        try {
            messageCampaignService.saveCampaign(createCampaignRecord(campaignName));
            CampaignRequest campaignRequest = new CampaignRequest("entity_1", campaignName, new LocalDate(2020, 7, 10), null, null);

            TriggerKey triggerKey = triggerKey("org.motechproject.messagecampaign.fired-campaign-message-MessageJob.Malnutrition.entity_1.key-runonce", "default");

            messageCampaignService.startFor(campaignRequest);
            assertTrue(scheduler.checkExists(triggerKey));

            messageCampaignService.stopAll(campaignRequest);
            assertFalse(scheduler.checkExists(triggerKey));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            messageCampaignService.deleteCampaign(campaignName);
        }
    }

    private CampaignRecord createCampaignRecord(String campaignName) {
        CampaignRecord campaign = new CampaignRecord();
        campaign.setCampaignType(CampaignType.ABSOLUTE);
        campaign.setMaxDuration("10");
        campaign.setName(campaignName);

        CampaignMessageRecord message = new CampaignMessageRecord();
        message.setDate(LocalDate.now());
        message.setStartTime("20:44");
        message.setMessageKey("key");
        message.setLanguages(asList("lang1", "lang2", "lang3"));

        campaign.setMessages(asList(message));

        return campaign;
    }

}
