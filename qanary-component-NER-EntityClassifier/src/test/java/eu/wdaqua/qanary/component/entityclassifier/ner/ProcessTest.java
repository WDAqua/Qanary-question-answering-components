package eu.wdaqua.qanary.component.entityclassifier.ner;

import eu.wdaqua.qanary.commons.QanaryMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Smoke test for {@link EntityClassifier#process}: this component's process()
 * performs no work and returns the incoming message unchanged.
 */
class ProcessTest {

    @Test
    void processReturnsTheMessageUnchanged() throws Exception {
        EntityClassifier component = new EntityClassifier();
        QanaryMessage message = mock(QanaryMessage.class);

        assertSame(message, component.process(message));
    }
}
