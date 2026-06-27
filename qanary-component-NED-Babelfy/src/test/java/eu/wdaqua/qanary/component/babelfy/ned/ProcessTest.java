package eu.wdaqua.qanary.component.babelfy.ned;

import com.google.gson.JsonArray;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link BabelfyNED#process}: with the Babelfy service returning no
 * links, process() writes nothing to the triplestore and returns the message. The
 * external Babelfy API and the triplestore are mocked so the test runs offline.
 */
class ProcessTest {

    @Test
    void processReturnsMessageWhenNoEntitiesFound() throws Exception {
        BabelfyServiceFetcher fetcher = mock(BabelfyServiceFetcher.class);
        when(fetcher.sendRequestToApi(anyString())).thenReturn(new JsonArray());
        when(fetcher.getLinksForQuestion(any())).thenReturn(new ArrayList<>());

        BabelfyNED component = spy(new BabelfyNED("test.application", fetcher));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryQuestion<String> question = mock(QanaryQuestion.class);
        QanaryUtils utils = mock(QanaryUtils.class);

        when(question.getTextualRepresentation()).thenReturn("Who is Batman?");
        doReturn(utils).when(component).getUtils(message);
        doReturn(question).when(component).getQanaryQuestion(message);

        QanaryMessage result = component.process(message);

        assertSame(message, result);
        verifyNoInteractions(utils); // no links => no triplestore update
    }
}
