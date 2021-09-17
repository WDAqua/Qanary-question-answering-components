package eu.wdaqa.qanary.watson;

public class NamedEntity {
    private String uri;
    private int begin;
    private int end;
    private double confidence;

    public NamedEntity(String uri, int begin, int end, double confidence) {
        this.begin = begin;
        this.end = end;
        this.uri = uri;
        this.confidence = confidence;
    }

    public NamedEntity(String uri, int begin, int end) {
        this.begin = begin;
        this.end = end;
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public double getConfidence() {
        return confidence;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return (uri + " at location: (" + this.getBegin() + ", " + this.getEnd() + ") with probability: " + this.getConfidence());
    }
}
