package eu.wdaqua.qanary.component.tebaqawrapper.qb;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Smoke test for {@link TeBaQAQueryBuilder#process}: process() loads the question (from the
 * triplestore / via the framework) and calls its downstream service. With the
 * triplestore mocked and the question unresolvable, process() surfaces the failure
 * rather than silently swallowing it; the test exercises process() entirely offline.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class ProcessTest {

    @Test
    void processSurfacesUnresolvableQuestion() throws Exception {
        eu.wdaqua.qanary.component.tebaqawrapper.qb.TeBaQAQueryBuilder component = spy(new eu.wdaqua.qanary.component.tebaqawrapper.qb.TeBaQAQueryBuilder(0.0f, "en", java.util.List.of("en"), java.net.URI.create("urn:qanary:test"), "en", mock(org.springframework.web.client.RestTemplate.class), mock(eu.wdaqua.qanary.communications.CacheOfRestTemplateResponse.class)));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryUtils utils = mock(QanaryUtils.class);
        QanaryQuestion question = mock(QanaryQuestion.class);
        QanaryTripleStoreConnector connector = mock(QanaryTripleStoreConnector.class);
        ResultSet emptyResultSet = mock(ResultSet.class);

        when(emptyResultSet.hasNext()).thenReturn(false);
        when(utils.getQanaryTripleStoreConnector()).thenReturn(connector);
        when(connector.select(anyString())).thenReturn(emptyResultSet);
        when(question.getTextualRepresentation()).thenThrow(new RuntimeException("question unavailable"));
        when(question.getTextualRepresentation(anyString())).thenThrow(new RuntimeException("question unavailable"));
        doReturn(utils).when(component).getUtils(message);
        doReturn(utils).when(component).getUtils();
        doReturn(question).when(component).getQanaryQuestion(message);
        doReturn(question).when(component).getQanaryQuestion();

        assertThrows(Exception.class, () -> component.process(message));
    }
}
