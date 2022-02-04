package eu.wdaqua.qanary.sqgQB;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.wdaqua.qanary.commons.QanaryExceptionNoOrMultipleQuestions;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;


@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class SQG extends QanaryComponent {

	private static final Logger logger = LoggerFactory.getLogger(SQG.class);
	private final String SQGendpoint = "http://porque.cs.upb.de:5055/qg/api/v1.0/query";

	public SQG() throws IOException, InterruptedException {


	}
		
	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * 
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		QanaryUtils qanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> qanaryQuestion = new QanaryQuestion<>(myQanaryMessage);
		String myQuestion = qanaryQuestion.getTextualRepresentation();
		logger.info("myQuestion: {}", myQuestion);

		ArrayList entities = fetchEntities(qanaryQuestion, qanaryUtils);
		ArrayList<Entity> relations = fetchRelations(qanaryQuestion,qanaryUtils);
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(SQGendpoint);
		httppost.addHeader("Content-Type", "application/json");

		JSONObject json = new JSONObject()
				.put("question", myQuestion);
		JSONArray entitiesJson = new JSONArray();
		for (Object e: entities)
		{
			Entity ent = (Entity) e;
			entitiesJson.put(new JSONObject().put("surface", myQuestion.substring(ent.begin,ent.end)).put("uris",
					new JSONArray().put(new JSONObject().put("confidence",1).put("uri",ent.uri))
			));
		}
		JSONArray relationsJson = new JSONArray();
		for (Object e: relations)
		{
			Entity ent = (Entity) e;
			relationsJson.put(new JSONObject().put("surface", myQuestion.substring(ent.begin,ent.end)).put("uris",
					new JSONArray().put(new JSONObject().put("confidence",1).put("uri",ent.uri))
			));
		}

		json.put("entities",entitiesJson);
		json.put("relations",relationsJson);
		json.put("kb","dbpedia");
		HttpResponse response = null;

		StringEntity entitytemp = new StringEntity(json.toString());
		httppost.setEntity(entitytemp);
		try {
			response = httpclient.execute(httppost);
		}
		catch (Exception e)
		{
			logger.info("Except: {}", e);
		}
		HttpEntity entity = response.getEntity();
		InputStream instream = entity.getContent();
		String text2 = IOUtils.toString(instream, StandardCharsets.UTF_8.name());
		JSONObject responsejson = new JSONObject(text2);
		JSONArray queriesJson = (JSONArray) responsejson.get("queries");
		ArrayList<Query> queryArrayList = new ArrayList<>();
 		for ( int i =0 ; i < queriesJson.length();i++) {
			JSONObject ques = (JSONObject) queriesJson.get(i);
			queryArrayList.add(new Query(ques.get("query").toString(),(Double) ques.get("confidence")));

		}
		final String endpoint = myQanaryMessage.getEndpoint().toString();
		final String questionUri = getQuestionURI(qanaryUtils, myQanaryMessage.getInGraph().toString(), endpoint);
		final String updateQuery = createUpdateQueryFromQueryTemplate(queryArrayList, qanaryUtils, questionUri);

		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		logger.info("apply vocabulary alignment on outgraph");
		qanaryUtils.updateTripleStore(updateQuery, endpoint);

		return myQanaryMessage;
	}

	private ArrayList<Entity> fetchEntities(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed {
		final String sparql = "" // 
							+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
							+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
							+ "SELECT ?start ?end ?uri " //
							+ "FROM <" + qanaryQuestion.getInGraph() + "> " //
							+ "WHERE { " //
							+ "    ?a a qa:AnnotationOfInstance . " // 
							+ "    ?a oa:hasTarget [ " //
							+ "		     a               oa:SpecificResource; " //
							+ "		     oa:hasSource    ?q; " //
							+ "	         oa:hasSelector  [ " //
							+ "			         a        oa:TextPositionSelector ; " //
							+ "			         oa:start ?start ; " //
							+ "			         oa:end   ?end " //
							+ "		     ] " //
							+ "    ] . " //
							+ " ?a oa:hasBody ?uri ; " //
							+ "} ";

		logger.info("fetchEntities for given question with query {}", sparql);
		ArrayList entities = new ArrayList();

		final ResultSet entitiesResultSet = qanaryUtils.selectFromTripleStore(sparql);
		final StringBuilder argument = new StringBuilder();
		while (entitiesResultSet.hasNext()) {
			QuerySolution s = entitiesResultSet.next();

			final Entity entity = new Entity(s.getResource("uri").getURI(), s.getLiteral("start").getInt(), s.getLiteral("end").getInt());
			entities.add(entity);

			logger.info("uri:{} start:{} end:{}", entity.uri, entity.begin, entity.end);
		}
		return entities;
	}

	private ArrayList<Entity> fetchRelations(final QanaryQuestion<String> qanaryQuestion, final QanaryUtils qanaryUtils) throws SparqlQueryFailed, QanaryExceptionNoOrMultipleQuestions, URISyntaxException {
		final String sparql = "" // 
							+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
							+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " // 
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
							+ "SELECT ?relationurl ?start ?end " //
							+ "FROM <" + qanaryQuestion.getInGraph() + "> " //
							+ "WHERE { " //
							+ "  ?a a qa:AnnotationOfRelation . " //  
							+ "  ?a oa:hasTarget [ " //  
							+ "           a    oa:SpecificResource; " // 
							+ "           oa:hasSource    <" + qanaryQuestion.getUri() + ">; "
							+ "           oa:start ?start;                                   "
							+ "           oa:end ?end;                                   "
							+ "  ] ; " //   
							+ "     oa:hasBody ?relationurl ;" // 
							+ "	    oa:annotatedAt ?time  " // 
							+ "} " //
							+ "ORDER BY ?start ";

		logger.info("fetchRelations for given question with query {}", sparql);

		final ResultSet relationResultSet = qanaryUtils.selectFromTripleStore(sparql);
		final StringBuilder argument = new StringBuilder();
		ArrayList entities = new ArrayList();
		while (relationResultSet.hasNext()) {
			QuerySolution s = relationResultSet.next();

			final Entity entity = new Entity(s.getResource("relationurl").getURI(),s.getLiteral("start").getInt(), s.getLiteral("end").getInt());
			entities.add(entity);

			logger.info("uri info {}", entity.uri);
		}
		return entities;
	}


	private String createUpdateQueryFromQueryTemplate(ArrayList<Query> queryTemplates, final QanaryUtils qanaryUtils, String questionUri) {
		String sparqlPart1 = "";
		String sparqlPart2 = "";
		int i = 0;
		for (Query q : queryTemplates) {
			sparqlPart1 += "" // 
					+ "?a" + i + " a qa:AnnotationOfAnswerSPARQL . " // 
					+ "?a" + i + " oa:hasTarget <"+questionUri+"> . " // 
					+ "?a" + i + " oa:hasBody \"" + q.query.replace("\n", " ") + "\" ;" //
					+ "     oa:annotatedBy <urn:qanary:QB#" + SQG.class.getName()+"> ; " //
					+ "         oa:annotatedAt ?time ; " //
					+ "         qa:hasScore " + q.score + " . \n";
			sparqlPart2 += "BIND (IRI(str(RAND())) AS ?a" + i + ") . \n";
			i++;
		}

		final String sparql = "" // 
							+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " // 
							+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
							+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
							+ "INSERT { " //  
							+ "  GRAPH <" + qanaryUtils.getInGraph() + "> { " + sparqlPart1 + "}" // 
							+ "} " //
							+ "WHERE { " //
							+ "  " + sparqlPart2 // 
							+ "  BIND (IRI(str(RAND())) AS ?b) ." //
							+ "  BIND (now() as ?time) . " //
							+ "}";
		return sparql;
	}
	
	private String getQuestionURI(QanaryUtils myQanaryUtils, String namedGraph, String endpoint) throws SparqlQueryFailed {
			String sparql = "" // 
					+ "PREFIX qa:<http://www.wdaqua.eu/qa#> " //
					+ "SELECT ?questionuri " //
					+ "FROM <" + namedGraph + "> " //
					+ "WHERE {?questionuri a qa:Question}";

			ResultSet result = myQanaryUtils.selectFromTripleStore(sparql, endpoint);
			return result.next().getResource("questionuri").toString();
	}
	
	protected class Entity {
		public int begin;
		public int end;
		public final String uri;

		Entity(String uri, int begin, int end){
			this.uri = uri;
			this.begin = begin;
			this.end = end;
		}
	}
	protected class Query {
		public double score;
		public String query;
		Query(String query, Double score){
			this.query = query;
			this.score = score;
		}

	}


}
