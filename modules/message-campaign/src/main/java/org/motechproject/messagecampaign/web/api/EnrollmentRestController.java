package org.motechproject.messagecampaign.web.api;

import org.apache.commons.lang.StringUtils;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.motechproject.messagecampaign.domain.CampaignNotFoundException;
import org.motechproject.messagecampaign.domain.campaign.CampaignEnrollment;
import org.motechproject.messagecampaign.domain.campaign.CampaignEnrollmentStatus;
import org.motechproject.messagecampaign.service.CampaignEnrollmentService;
import org.motechproject.messagecampaign.service.CampaignEnrollmentsQuery;
import org.motechproject.messagecampaign.service.MessageCampaignService;
import org.motechproject.messagecampaign.web.ex.EnrollmentNotFoundException;
import org.motechproject.messagecampaign.web.model.EnrollmentDto;
import org.motechproject.messagecampaign.web.model.EnrollmentList;
import org.motechproject.messagecampaign.web.model.EnrollmentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Locale;

/**
 *  Controller for handling message campaign enrollment requests.
 */

@Controller
@RequestMapping(value = "web-api/enrollments")
public class EnrollmentRestController {

    private static final String HAS_MANAGE_ENROLLMENTS_ROLE = "hasRole('manageEnrollments')";

    @Autowired
    private MessageCampaignService messageCampaignService;

    @Autowired
    private CampaignEnrollmentService enrollmentService;

    @RequestMapping(value = "/{campaignName}/users/{userId}", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize(HAS_MANAGE_ENROLLMENTS_ROLE)
    public void enrollOrUpdateUser(@PathVariable String campaignName, @PathVariable String userId,
                                   @RequestBody EnrollmentRequest enrollmentRequest) {

        CampaignRequest campaignRequest = new CampaignRequest(userId, campaignName,
                enrollmentRequest.getReferenceDate(), enrollmentRequest.getStartTime());

        String enrollmentId = enrollmentRequest.getEnrollmentId();

        if (StringUtils.isNotBlank(enrollmentId)) {
            messageCampaignService.updateEnrollment(campaignRequest, enrollmentId);
        } else {
            messageCampaignService.enroll(campaignRequest);
        }
    }

    @RequestMapping(value = "/{campaignName}/users/{userId}", method = RequestMethod.GET)
    @PreAuthorize(HAS_MANAGE_ENROLLMENTS_ROLE)
    @ResponseBody
    public EnrollmentDto getEnrollment(@PathVariable String campaignName, @PathVariable String userId) {
        CampaignEnrollmentsQuery query = new CampaignEnrollmentsQuery()
                .withCampaignName(campaignName).withExternalId(userId);

        List<CampaignEnrollment> enrollments = enrollmentService.search(query);

        if (enrollments.isEmpty()) {
            throw enrollmentsNotFoundException(userId);
        } else {
            return new EnrollmentDto(enrollments.get(0));
        }
    }

    @RequestMapping(value = "/{campaignName}/users/{userId}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize(HAS_MANAGE_ENROLLMENTS_ROLE)
    public void updateEnrollment(@PathVariable String campaignName, @PathVariable String userId,
                                 @RequestBody EnrollmentRequest enrollmentRequest) {
        CampaignRequest campaignRequest = new CampaignRequest(userId, campaignName,
                enrollmentRequest.getReferenceDate(), enrollmentRequest.getStartTime());

        messageCampaignService.unenroll(userId, campaignName);
        messageCampaignService.enroll(campaignRequest);
    }

    @RequestMapping(value = "/{campaignName}/users/{externalId}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize(HAS_MANAGE_ENROLLMENTS_ROLE)
    public void removeEnrollment(@PathVariable String campaignName, @PathVariable String externalId) {
        CampaignEnrollmentsQuery query = new CampaignEnrollmentsQuery()
                .withCampaignName(campaignName).withExternalId(externalId);

        List<CampaignEnrollment> enrollments = enrollmentService.search(query);

        if (enrollments.isEmpty()) {
            throw enrollmentsNotFoundException(externalId);
        } else {
            CampaignRequest campaignRequest = new CampaignRequest();
            campaignRequest.setCampaignName(campaignName);
            campaignRequest.setExternalId(externalId);

            messageCampaignService.unenroll(externalId, campaignName);
        }
    }

    @RequestMapping(value = "/{campaignName}/users", method = RequestMethod.GET)
    @PreAuthorize(HAS_MANAGE_ENROLLMENTS_ROLE)
    @ResponseBody
    public EnrollmentList getEnrollmentsForCampaign(@PathVariable String campaignName) {
        CampaignEnrollmentsQuery query = new CampaignEnrollmentsQuery().withCampaignName(campaignName);

        List<CampaignEnrollment> enrollments = enrollmentService.search(query);

        EnrollmentList enrollmentList = new EnrollmentList(enrollments);
        enrollmentList.setCommonCampaignName(campaignName);

        return enrollmentList;
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.GET)
    @PreAuthorize(HAS_MANAGE_ENROLLMENTS_ROLE)
    @ResponseBody
    public EnrollmentList getEnrollmentsForUser(@PathVariable String userId) {
        CampaignEnrollmentsQuery query = new CampaignEnrollmentsQuery().withExternalId(userId);

        List<CampaignEnrollment> enrollments = enrollmentService.search(query);

        if (enrollments.isEmpty()) {
            throw enrollmentsNotFoundException(userId);
        } else {
            EnrollmentList enrollmentList = new EnrollmentList(enrollments);
            enrollmentList.setCommonExternalId(userId);

            return enrollmentList;
        }
    }

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    @PreAuthorize(HAS_MANAGE_ENROLLMENTS_ROLE)
    @ResponseBody
    public EnrollmentList getAllEnrollments(@RequestParam(required = false) String enrollmentStatus,
                                            @RequestParam(required = false) String externalId,
                                            @RequestParam(required = false) String campaignName) {
        CampaignEnrollmentsQuery query = new CampaignEnrollmentsQuery();

        if (enrollmentStatus != null) {
            query.havingState(CampaignEnrollmentStatus.valueOf(enrollmentStatus.toUpperCase(Locale.ENGLISH)));
        }
        if (campaignName != null) {
            query.withCampaignName(campaignName);
        }
        if (externalId != null) {
            query.withExternalId(externalId);
        }

        List<CampaignEnrollment> enrollments = enrollmentService.search(query);

        return new EnrollmentList(enrollments);
    }

    @ExceptionHandler({EnrollmentNotFoundException.class, CampaignNotFoundException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleException(Exception e) {
        return e.getMessage();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleIllegalArgException(Exception e) {
        return e.getMessage();
    }

    private EnrollmentNotFoundException enrollmentsNotFoundException(String userId) {
        return new EnrollmentNotFoundException("No enrollments found for user " + userId);
    }
}
