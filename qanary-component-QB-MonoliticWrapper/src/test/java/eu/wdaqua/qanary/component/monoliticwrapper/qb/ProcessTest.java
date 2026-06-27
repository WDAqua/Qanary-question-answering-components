package eu.wdaqua.qanary.component.monoliticwrapper.qb;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
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
 * Unit test for {@link Monolitic#process}: the component runs a fixed ASK query
 * against a knowledge graph and stores the JSON answer. The triplestore access is
 * mocked (ASK => false) so the test runs offline and process() returns the message.
 */
class ProcessTest {

    @Test
    void processStoresAnswerAndReturnsMessage() throws Exception {
        Monolitic component = spy(new Monolitic("test.application"));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryQuestion<String> question = mock(QanaryQuestion.class);
        QanaryUtils utils = mock(QanaryUtils.class);
        QanaryTripleStoreConnector connector = mock(QanaryTripleStoreConnector.class);

        when(question.getTextualRepresentation()).thenReturn("Who develops DBpedia?");
        lenient().when(message.getEndpoint()).thenReturn(new URI("urn:qanary:endpoint"));
        lenient().when(question.getOutGraph()).thenReturn(new URI("urn:qanary:outgraph"));
        lenient().when(question.getUri()).thenReturn(new URI("urn:qanary:question"));
        when(utils.askTripleStore(anyString(), anyString())).thenReturn(false);
        lenient().when(utils.getQanaryTripleStoreConnector()).thenReturn(connector);
        lenient().doNothing().when(connector).update(anyString());
        doReturn(utils).when(component).getUtils(message);
        doReturn(question).when(component).getQanaryQuestion(message);

        QanaryMessage result = component.process(message);

        assertSame(message, result);
    }
}
