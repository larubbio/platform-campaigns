package org.motechproject.pillreminder.dao;

import org.ektorp.CouchDbConnector;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.motechproject.pillreminder.domain.DailyScheduleDetails;
import org.motechproject.pillreminder.domain.PillRegimen;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class AllPillRegimensTest {

    @Mock
    private CouchDbConnector couchDbConnector;

    private AllPillRegimens allPillRegimens;

    @Before
    public void setUp() {
        initMocks(this);
        allPillRegimens = new AllPillRegimens(couchDbConnector);
    }

    @Test
    public void shouldAddPillRegimen() {
        PillRegimen pillRegimen = new PillRegimen("123", null, new DailyScheduleDetails(10, 5, 5));
        allPillRegimens.add(pillRegimen);
        verify(couchDbConnector).create(pillRegimen);
    }
}
