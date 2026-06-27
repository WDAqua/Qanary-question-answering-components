package eu.wdaqua.qanary.component.comiccharacternamesimple.ner;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ComicCharacterNameSimpleNamedEntityRecognizer#process}:
 * when the textual representation of the question cannot be retrieved, process()
 * logs the error and returns the message unchanged.
 */
class ProcessTest {

    @Test
    void processReturnsMessageWhenQuestionTextUnavailable() throws Exception {
        ComicCharacterNameSimpleNamedEntityRecognizer component =
                spy(new ComicCharacterNameSimpleNamedEntityRecognizer("test.application"));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryQuestion<String> question = mock(QanaryQuestion.class);
        QanaryUtils utils = mock(QanaryUtils.class);

        when(question.getEndpoint()).thenReturn(new URI("urn:qanary:triplestore"));
        when(question.getTextualRepresentation()).thenThrow(new RuntimeException("no question text"));
        doReturn(utils).when(component).getUtils(message);
        doReturn(question).when(component).getQanaryQuestion(message);

        QanaryMessage result = component.process(message);

        assertSame(message, result);
    }
}
