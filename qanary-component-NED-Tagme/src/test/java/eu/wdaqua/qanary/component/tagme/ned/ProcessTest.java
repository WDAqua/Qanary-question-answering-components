package eu.wdaqua.qanary.component.tagme.ned;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TagmeNED#process}: process() loads the question from the
 * triplestore and links entities via the external TagMe service. With an empty
 * triplestore the question cannot be resolved, and process() surfaces that failure
 * rather than silently swallowing it. The triplestore is mocked so the test runs offline.
 */
class ProcessTest {

    @Test
    void processSurfacesUnresolvableQuestion() throws Exception {
        TagmeNED component = spy(new TagmeNED("test.application", false, "/tmp/tagme-cache", "https://tagme.example/", 0.0f));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryUtils utils = mock(QanaryUtils.class);
        QanaryTripleStoreConnector connector = mock(QanaryTripleStoreConnector.class);
        ResultSet emptyResultSet = mock(ResultSet.class);

        lenient().when(emptyResultSet.hasNext()).thenReturn(false);
        when(utils.getQanaryTripleStoreConnector()).thenReturn(connector);
        lenient().when(connector.select(anyString())).thenReturn(emptyResultSet);
        doReturn(utils).when(component).getUtils(message);

        assertThrows(Exception.class, () -> component.process(message));
    }
}
