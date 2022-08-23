package eu.wdaqa.qanary.component.watson.ned;

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

    public boolean equals(NamedEntity anotherNamedEntity) {
        // do not compare the confidence score
        if (this.getUri().compareTo(anotherNamedEntity.getUri()) != 0) {
            return false;
        } else if (this.getBegin() != anotherNamedEntity.getBegin()) {
            return false;
        } else if (this.getEnd() != anotherNamedEntity.getEnd()) {
            return false;
        } else {
            return true;
        }
    }
}
