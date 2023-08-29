package eu.wdaqua.qanary.component.pojos;

public class AnnotationOfInstancePojo {

    private String targetQuestion;
    private int start;
    private int end;
    private double score;
    private String originResource;
    private String newResource;

    public AnnotationOfInstancePojo(String originResource, String targetQuestion, int start, int end, double score) {
        this.originResource = originResource;
        this.targetQuestion = targetQuestion;
        this.start = start;
        this.end = end;
        this.score = score;
    }

    public int getEnd() {
        return end;
    }

    public int getStart() {
        return start;
    }

    public double getScore() {
        return score;
    }

    public String getTargetQuestion() {
        return targetQuestion;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void setTargetQuestion(String targetQuestion) {
        this.targetQuestion = targetQuestion;
    }

    public String getNewResource() {
        return newResource;
    }

    public String getOriginResource() {
        return originResource;
    }

    public void setNewResource(String newResource) {
        this.newResource = newResource;
    }

    public void setOriginResource(String originResource) {
        this.originResource = originResource;
    }

    @Override
    public String toString() {
        return "AnnotationOfInstancePojo{" +
                "targetQuestion='" + targetQuestion + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", score=" + score +
                ", originResource='" + originResource + '\'' +
                ", newResource='" + newResource + '\'' +
                '}';
    }
}
