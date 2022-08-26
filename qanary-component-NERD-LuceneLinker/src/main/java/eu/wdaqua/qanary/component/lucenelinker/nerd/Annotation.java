package eu.wdaqua.qanary.component.lucenelinker.nerd;

class Annotation {
    public int begin;
    public int end;
    public String uri;

    Annotation(int begin, int end, String uri) {
        this.begin = begin;
        this.end = end;
        this.uri = uri;
    }
}
