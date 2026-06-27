package eu.wdaqua.qanary.component.shuyo.ld;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link LanguageDetection#process}: the component detects the
 * language of the question and writes the language tag back to the triplestore.
 * The triplestore access is mocked so the test runs offline.
 */
class ProcessTest {

    @Test
    void processDetectsLanguageAndReturnsTheMessage() throws Exception {
        LanguageDetection component = spy(new LanguageDetection("test.application"));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryQuestion<String> question = mock(QanaryQuestion.class);
        QanaryUtils utils = mock(QanaryUtils.class);
        QanaryTripleStoreConnector connector = mock(QanaryTripleStoreConnector.class);

        when(question.getTextualRepresentation()).thenReturn("This is an English sentence.");
        when(question.getOutGraph()).thenReturn(new URI("urn:qanary:outgraph"));
        when(question.getUri()).thenReturn(new URI("urn:qanary:question"));
        when(utils.getQanaryTripleStoreConnector()).thenReturn(connector);
        doNothing().when(connector).update(anyString());
        doReturn(question).when(component).getQanaryQuestion();
        doReturn(utils).when(component).getUtils();

        QanaryMessage result = component.process(message);

        assertSame(message, result);
    }
}
