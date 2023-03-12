package eu.wdaqua.qanary.component.lucenelinker.nerd;

import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import eu.wdaqua.qanary.component.QanaryComponent;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.util.AttributeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * represents a component that tries to map sequences of words in the question to the rdfs:labels
 * given in the ontology
 *
 * @author Dennis Diefenbach
 */

@Component
public class LuceneLinker extends QanaryComponent {
    private static final Logger logger = LoggerFactory.getLogger(LuceneLinker.class);

    private final String applicationName;

    private String FILENAME_INSERT_ANNOTATION = "/queries/insert_one_annotation.rq";

    public LuceneLinker(@Value("${spring.application.name}") final String applicationName) {
        this.applicationName = applicationName;

        // check if files exists and are not empty
        QanaryTripleStoreConnector.guardNonEmptyFileFromResources(FILENAME_INSERT_ANNOTATION);
    }

    /**
     * default processor of a QanaryMessage
     */
    public QanaryMessage process(QanaryMessage myQanaryMessage) {
        long startTime = System.currentTimeMillis();
        logger.info("Qanary Message: {}", myQanaryMessage);

        try {
            // STEP1: Retrieve the named graph and the endpoint
            QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
            QanaryQuestion<String> myQanaryQuestion = new QanaryQuestion(myQanaryMessage, myQanaryUtils.getQanaryTripleStoreConnector());
            String myQuestion = myQanaryQuestion.getTextualRepresentation();
            logger.info("Question {}", myQuestion);

            // STEP2: Retrieve information that are needed for the computations

            //String question="Who was influenced by Socrates?";
            //String question="Give me all Swedish oceanographers.";
            //String question="Which German cities have more than 250000 inhabitants?";
            //String question="What is the birth name of Angela Merkel?";
            // STEP3: Pass the information to the component and execute it
            //Tokenize the question using the lucene tokenizer
            Index index = new Index("/Users/Dennis/Desktop/dump.ttl");
            ArrayList<String> stopWords = new ArrayList<String>(Arrays.asList(new String[]{"give", "me", "is", "are", "was", "were", "has", "have", "had", "do", "does", "did", "of", "the", "a", "in", "by", "to", "me", "all", "with", "from", "for", "and", "who"}));
            Analyzer analyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
            //Analyzer analyzer = index.analyzer;
            TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(myQuestion));
            tokenStream.reset();
            List<AttributeSource> tokens = new ArrayList<AttributeSource>();
            while (tokenStream.incrementToken()) {
                tokens.add(tokenStream.cloneAttributes());
            }
            tokenStream.close();
            tokenStream.end();
            //analyzer.close();


            //Try to match each sequence of words in a question to an uri searching at rdfs:label
            List<Annotation> annotations = new ArrayList<Annotation>();
            for (int i = 0; i < tokens.size(); i++) {
                boolean found = true;
                String search = tokens.get(i).getAttribute(CharTermAttribute.class).toString();
                int k = 1;
                List<String> candidates = new ArrayList<String>();
                while (found) {
                    logger.info("Search string {} ", search);
                    List<String> tmp = index.query("\"" + search + "\"");

                    //If no matches found add the previews ones if they exist
                    if (tmp.size() == 0) {
                        found = false;
                        for (String uri : candidates) {
                            if (!uri.equals("http://dbpedia.org/")) {
                                int begin = tokens.get(i).getAttribute(OffsetAttribute.class).startOffset();
                                int end = tokens.get(i + k - 2).getAttribute(OffsetAttribute.class).endOffset();
                                logger.info("Added uri {} ", uri);
                                annotations.add(new Annotation(begin, end, uri));
                            }
                        }
                    } else {
                        if (candidates.size() > 0) {
                            for (String uri : candidates) {
                                if (!uri.equals("http://dbpedia.org/")) {
                                    int begin = tokens.get(i).getAttribute(OffsetAttribute.class).startOffset();
                                    int end = tokens.get(i + k - 2).getAttribute(OffsetAttribute.class).endOffset();
                                    logger.info("Added uri {} ", uri);
                                    annotations.add(new Annotation(begin, end, uri));
                                }
                            }
                        }
                        //candidates=new ArrayList<String>(Arrays.asList(tmp));
                        candidates = tmp;
                        if (i + k < tokens.size()) {
                            search += " " + tokens.get(i + k).getAttribute(CharTermAttribute.class).toString();
                            k++;
                        } else {
                            found = false;
                            for (String uri : candidates) {
                                if (!uri.equals("http://dbpedia.org/")) {
                                    int begin = tokens.get(i).getAttribute(OffsetAttribute.class).startOffset();
                                    int end = tokens.get(i + k - 1).getAttribute(OffsetAttribute.class).endOffset();
                                    logger.info("Added uri {} ", uri);
                                    annotations.add(new Annotation(begin, end, uri));
                                }
                            }
                        }
                    }
                }
            }

            Iterator<Annotation> it = annotations.iterator();
            while (it.hasNext()) {
                Annotation a = it.next();
                if (stopWords.contains(myQuestion.substring(a.begin, a.end).toLowerCase())) {
                    it.remove();
                }
            }

            //Remove duplicates
            it = annotations.iterator();
            while (it.hasNext()) {
                Annotation a1 = it.next();
                int count = 0;
                for (Annotation a2 : annotations) {
                    if ((a1.uri).equals(a2.uri)) {
                        count++;
                    }
                }
                if (count > 1) {
                    it.remove();
                }
            }

            // STEP4: Push the result of the component to the triplestore
            logger.info("Apply commons alignment on outgraph");

            for (Annotation a : annotations) {

                QuerySolutionMap bindingsForInsert = new QuerySolutionMap();
                bindingsForInsert.add("graph", ResourceFactory.createResource(myQanaryQuestion.getOutGraph().toASCIIString()));
                bindingsForInsert.add("targetQuestion", ResourceFactory.createResource(myQanaryQuestion.getUri().toASCIIString()));
                bindingsForInsert.add("start", ResourceFactory.createTypedLiteral(String.valueOf(a.begin), XSDDatatype.XSDnonNegativeInteger));
                bindingsForInsert.add("end", ResourceFactory.createTypedLiteral(String.valueOf(a.end), XSDDatatype.XSDnonNegativeInteger));
                bindingsForInsert.add("answer", ResourceFactory.createResource(a.uri));
                bindingsForInsert.add("application", ResourceFactory.createResource("urn:qanary:" + this.applicationName));

                // get the template of the INSERT query
                String sparql = this.loadQueryFromFile(FILENAME_INSERT_ANNOTATION, bindingsForInsert);
                logger.info("SPARQL query: {}", sparql);
                myQanaryUtils.getQanaryTripleStoreConnector().update(sparql);

            }
        } catch (IOException e) {
            logger.error("Error: {}", e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            logger.error("Error: {}", e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidTokenOffsetsException e) {
            logger.error("Error: {}", e);
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("Error: {}", e);
            e.printStackTrace();
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        logger.info("Time: {}", estimatedTime);

        return myQanaryMessage;
    }

    private String loadQueryFromFile(String filenameWithRelativePath, QuerySolutionMap bindings) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(filenameWithRelativePath, bindings);
    }

    @Deprecated(since = "3.1.5", forRemoval = true)
    private void loadTripleStore(String sparqlQuery, String endpoint) {
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateProcessor proc = UpdateExecutionFactory.createRemote(request, endpoint);
        proc.execute();
    }

    @Deprecated(since = "3.1.5", forRemoval = true)
    private ResultSet selectTripleStore(String sparqlQuery, String endpoint) {
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qExe = QueryExecutionFactory.sparqlService(endpoint, query);
        return qExe.execSelect();
    }


}
