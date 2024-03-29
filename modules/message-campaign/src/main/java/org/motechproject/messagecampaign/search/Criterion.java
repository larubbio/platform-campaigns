package org.motechproject.messagecampaign.search;

import org.motechproject.messagecampaign.dao.AllCampaignEnrollments;
import org.motechproject.messagecampaign.domain.campaign.CampaignEnrollment;

import java.util.List;

public interface Criterion {
    List<CampaignEnrollment> fetch(AllCampaignEnrollments allCampaignEnrollments);

    List<CampaignEnrollment> filter(List<CampaignEnrollment> campaignEnrollments);
}


