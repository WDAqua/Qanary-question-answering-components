package eu.wdaqua.qanary.sparqlexecuter;

import eu.wdaqua.qanary.sparqlexecuter.exception.SparqlQueryFailed;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class TripleStoreConnector {
    private static final Logger logger = LoggerFactory.getLogger(TripleStoreConnector.class);
    private final URI endpoint;
    private final String username;
    private final String password;

    private RDFConnection conn;

    public TripleStoreConnector(URI endpoint) {
        this.endpoint = endpoint;
        this.username = null;
        this.password = null;

        logger.debug("SPARQL Connection initialized: endpoint:{}", endpoint);
        this.connect();
        logger.info("SPARQL Connection created on endpoint {}", endpoint);

    }

    public TripleStoreConnector(URI endpoint, String username, String password) {
        this.endpoint = endpoint;
        this.username = username;
        this.password = password;

        logger.debug("SPARQL Connection initialized: endpoint:{}, username:{}, password:{}", endpoint, username, password);
        this.connect();
        logger.info("SPARQL Connection created on endpoint {}", endpoint);
    }

    /**
     * get current time in milliseconds
     */
    protected static long getTime() {
        return System.currentTimeMillis();
    }

    private void connect() {
        if (this.username == null || this.password == null) {
            this.conn = RDFConnection.connect(this.endpoint.toString());
        } else {
            this.conn = RDFConnection.connectPW(this.endpoint.toString(), this.username, this.password);
        }
    }

    /**
     * @param description
     * @param duration
     */
    protected void logTime(long duration, String description) {
        logger.info("runtime measurement: {} ms for {}", duration, description);
    }

    public ResultSet select(String sparql) throws SparqlQueryFailed {
        long start = getTime();

        try {
            QueryExecution qExec = conn.query(sparql);

            this.logTime(getTime() - start, "SELECT on " + this.endpoint.toString() + ": " + sparql);
            return qExec.execSelect();
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.endpoint.toString(), e);
        }
    }

    public boolean ask(String sparql) throws SparqlQueryFailed {
        long start = getTime();

        try {
            QueryExecution qExec = conn.query(sparql);

            this.logTime(getTime() - start, "ASK on " + this.endpoint.toString() + ": " + sparql);
            return qExec.execAsk();
        } catch (Exception e) {
            throw new SparqlQueryFailed(sparql, this.endpoint.toString(), e);
        }
    }
}
