package eu.wdaqua.qanary.component.ambiverse.ned;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AmbiverseNed#process}: the component reads the question and
 * calls the external Ambiverse entity-linking service. process() does not swallow a
 * failure to obtain the question, so the error is surfaced rather than silently lost.
 */
class ProcessTest {

    @Test
    void processSurfacesQuestionRetrievalFailure() throws Exception {
        AmbiverseNed component = spy(new AmbiverseNed("test.application"));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryQuestion<String> question = mock(QanaryQuestion.class);
        QanaryUtils utils = mock(QanaryUtils.class);

        when(question.getTextualRepresentation()).thenThrow(new RuntimeException("question unavailable"));
        doReturn(utils).when(component).getUtils(message);
        doReturn(question).when(component).getQanaryQuestion(message);

        assertThrows(Exception.class, () -> component.process(message));
    }
}
