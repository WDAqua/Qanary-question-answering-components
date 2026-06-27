package eu.wdaqua.qanary.component.lucenelinker.nerd;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Smoke test for {@link LuceneLinker#process}: process() wraps its work in a
 * try/catch, so an unresolvable question (empty mocked triplestore) is handled
 * and the incoming message is returned. Runs offline.
 */
class ProcessTest {

    @Test
    void processReturnsMessageWhenQuestionUnresolvable() throws Exception {
        LuceneLinker component = spy(new LuceneLinker("test.application"));

        QanaryMessage message = mock(QanaryMessage.class);
        QanaryUtils utils = mock(QanaryUtils.class);
        QanaryTripleStoreConnector connector = mock(QanaryTripleStoreConnector.class);
        ResultSet emptyResultSet = mock(ResultSet.class);

        when(emptyResultSet.hasNext()).thenReturn(false);
        when(utils.getQanaryTripleStoreConnector()).thenReturn(connector);
        when(connector.select(anyString())).thenReturn(emptyResultSet);
        doReturn(utils).when(component).getUtils(message);

        assertSame(message, component.process(message));
    }
}
