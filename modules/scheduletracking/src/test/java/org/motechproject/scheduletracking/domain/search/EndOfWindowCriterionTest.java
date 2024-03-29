package org.motechproject.scheduletracking.domain.search;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.scheduletracking.domain.Enrollment;
import org.motechproject.scheduletracking.domain.WindowName;
import org.motechproject.scheduletracking.repository.AllEnrollments;
import org.motechproject.scheduletracking.service.impl.EnrollmentServiceImpl;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.commons.date.util.DateUtil.newDateTime;
import static org.powermock.api.mockito.PowerMockito.when;

public class EndOfWindowCriterionTest {

    @Mock
    EnrollmentServiceImpl enrollmentService;
    @Mock
    AllEnrollments allEnrollments;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void shouldFindEnrollmentsWhoseGivenWindowEndsDuringGivenTimeRange() {
        List<Enrollment> enrollments = new ArrayList<Enrollment>();
        Enrollment enrollment1 = mock(Enrollment.class);
        Enrollment enrollment2 = mock(Enrollment.class);
        Enrollment enrollment3 = mock(Enrollment.class);
        Enrollment enrollment4 = mock(Enrollment.class);
        enrollments.addAll(asList(enrollment1, enrollment2, enrollment3, enrollment4));

        when(allEnrollments.getAll()).thenReturn(enrollments);

        when(enrollment1.getEndOfWindowForCurrentMilestone(WindowName.due)).thenReturn(newDateTime(2012, 2, 3, 5, 10, 0));
        when(enrollment2.getEndOfWindowForCurrentMilestone(WindowName.due)).thenReturn(newDateTime(2012, 2, 3, 0, 0, 0));
        when(enrollment3.getEndOfWindowForCurrentMilestone(WindowName.due)).thenReturn(newDateTime(2012, 2, 5, 0, 0, 0));
        when(enrollment4.getEndOfWindowForCurrentMilestone(WindowName.due)).thenReturn(newDateTime(2012, 2, 6, 0, 0, 0));

        DateTime start = newDateTime(2012, 2, 3, 0, 0, 0);
        DateTime end = newDateTime(2012, 2, 5, 23, 59, 59);
        List<Enrollment> fetchedEnrollments = new EndOfWindowCriterion(WindowName.due, start, end).fetch(allEnrollments);
        List<Enrollment> filteredEnrollments = new EndOfWindowCriterion(WindowName.due, start, end).filter(enrollments);

        assertEquals(asList(enrollment1, enrollment2, enrollment3), fetchedEnrollments);
        assertEquals(asList(enrollment1, enrollment2, enrollment3), filteredEnrollments);
    }
}
