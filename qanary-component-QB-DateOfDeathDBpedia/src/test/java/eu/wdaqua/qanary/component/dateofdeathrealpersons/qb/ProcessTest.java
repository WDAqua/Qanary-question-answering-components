package eu.wdaqua.qanary.component.dateofdeathrealpersons.qb;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link QueryBuilderDateOfDeathDBpedia#process}: with no matching
 * annotations in the triplestore, process() builds no queries and returns the
 * message. The triplestore is mocked (empty result set) so the test runs offline.
 */
class ProcessTest {

    @Test
    void processReturnsMessageWhenNoAnnotations() throws Exception {
        QueryBuilderDateOfDeathDBpedia component = spy(new QueryBuilderDateOfDeathDBpedia("test.application"));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryQuestion<String> question = mock(QanaryQuestion.class);
        QanaryUtils utils = mock(QanaryUtils.class);
        QanaryTripleStoreConnector connector = mock(QanaryTripleStoreConnector.class);
        ResultSet emptyResultSet = mock(ResultSet.class);

        when(emptyResultSet.hasNext()).thenReturn(false);
        when(question.getTextualRepresentation()).thenReturn("When did Albert Einstein die?");
        lenient().when(question.getUri()).thenReturn(new URI("urn:qanary:question"));
        lenient().when(question.getOutGraph()).thenReturn(new URI("urn:qanary:outgraph"));
        when(utils.getQanaryTripleStoreConnector()).thenReturn(connector);
        when(connector.select(anyString())).thenReturn(emptyResultSet);
        lenient().doNothing().when(connector).update(anyString());
        doReturn(utils).when(component).getUtils();
        doReturn(question).when(component).getQanaryQuestion(message);

        QanaryMessage result = component.process(message);

        assertSame(message, result);
    }
}
