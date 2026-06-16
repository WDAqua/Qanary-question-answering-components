package eu.wdaqua.qanary.component.copyvaluesofpriorgraph;

import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.endpointKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.inGraphKey;
import static eu.wdaqua.qanary.commons.config.QanaryConfiguration.outGraphKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import eu.wdaqua.qanary.commons.QanaryMessage;

/**
 * Unit tests for the CopyValuesOfPriorGraph component. These intentionally avoid
 * booting the full Spring context so they do not depend on a reachable Qanary
 * pipeline / triplestore and run fast and deterministically.
 */
class CopyValuesOfPriorGraphTest {

    /** the Qanary message (endpoint/inGraph/outGraph) round-trips through URIs */
    @Test
    void qanaryMessageRoundTrips() throws URISyntaxException {
        QanaryMessage message = new QanaryMessage(new URI(endpointKey), new URI(inGraphKey), new URI(outGraphKey));
        assertNotNull(message.getEndpoint());
        assertEquals(endpointKey, message.getEndpoint().toString());
        assertEquals(inGraphKey, message.getInGraph().toString());
        assertEquals(outGraphKey, message.getOutGraph().toString());
    }

    /** the SPARQL query templates the component relies on are present and non-empty */
    @Test
    void requiredSparqlQueryFilesArePresentAndNonEmpty() throws Exception {
        for (String resource : new String[] {
                "/queries/fetchRequiredAnnotations.rq",
                "/queries/addDataToGraph.rq",
                "/queries/storeComputedAnnotations.rq" }) {
            try (InputStream in = getClass().getResourceAsStream(resource)) {
                assertNotNull(in, "missing resource " + resource);
                assertTrue(new String(in.readAllBytes()).strip().length() > 0, "empty resource " + resource);
            }
        }
    }
}
