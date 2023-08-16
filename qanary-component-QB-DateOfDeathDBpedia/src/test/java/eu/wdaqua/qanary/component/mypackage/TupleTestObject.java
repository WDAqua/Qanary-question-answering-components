package eu.wdaqua.qanary.component.mypackage;

public class TupleTestObject {

    private int start;
    private int end;
    private String dbpediaResource;

    public TupleTestObject(int start, int end, String dbpediaResource) {
        this.start = start;
        this.end = end;
        this.dbpediaResource = dbpediaResource;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getdbpediaResource() {
        return dbpediaResource;
    }

    public void setdbpediaResource(String dbpediaResource) {
        this.dbpediaResource = dbpediaResource;
    }

    @Override
    public String toString() {
        return "TupleTestObject{" +
                "start=" + start +
                ", end=" + end +
                ", dbpediaResource='" + dbpediaResource + '\'' +
                '}';
    }
}
