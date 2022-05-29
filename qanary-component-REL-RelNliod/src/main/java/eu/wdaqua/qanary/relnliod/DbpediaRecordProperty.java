package eu.wdaqua.qanary.relnliod;

import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DbpediaRecordProperty {

    private TreeMap<String,String> map = new TreeMap<String,String>() ;

    public DbpediaRecordProperty(@Value("${rel-nliod.ttl.file}") final String ttlFile) {
        createDbpediaRecordProperty(ttlFile);
    }

    public void put(String property, String dbpediaLink) {
        map.put(property.trim(), dbpediaLink.trim());
    }

    public TreeMap<String,String> get(){
        return map;
    }

    public void print() {
        int count = 1;
        for(String s : map.keySet()) {
            System.out.println(count++ +" "+ map.get(s) + " : " + s.toString());
        }

    }
    public void createDbpediaRecordProperty(final String ttlFile) {

        System.out.println("Starting createDbpediaRecordProperty()");
        try {
            File filename = new File(ttlFile);
            System.out.println(filename.getAbsolutePath());
            PipedRDFIterator<Triple> iter = new PipedRDFIterator<>();
            final PipedRDFStream<Triple> inputStream = new PipedTriplesStream(iter);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Runnable parser;
            parser = new Runnable() {
                @Override
                public void run() {
                    RDFDataMgr.parse(inputStream, filename.getAbsolutePath());

                }
            };

            executor.submit(parser);
            //	executor.shutdown();
            int count = 0;
            while (iter.hasNext()) {
                org.apache.jena.graph.Triple next = iter.next();
                this.put(next.getObject().toString().replaceAll("\"", "").toLowerCase(),
                        next.getSubject().toString());
                count++;
            }
            System.out.println(count);
            this.print();
        }catch (Exception e) {
            System.out.println("Except: {}"+e);
            // TODO Auto-generated catch block
        }

    }

}
