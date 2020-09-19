package eu.wdaqua.qanary.tagme;

class NamedEntity {
    private Double linkProbability;
    private int begin;
    private int end;
    private String link;
    
    public NamedEntity(String link, int begin, int end, double linkProbability) {
    	this.link = link;
    	this.begin = begin;
    	this.end = end;
    	this.linkProbability = linkProbability;
    }

    public NamedEntity(String link, int begin, int end) {
    	this.link = link;
    	this.begin = begin;
    	this.end = end;
    }
    
    public Double getLinkProbability() {
    	return linkProbability;
    }
    
    public int getBegin() {
    	return begin;
    }
    
    public int getEnd() {
    	return end;
    }
    
    public String getLink() {
    	return link;
    }
    
    @Override
    public String toString() {
    	return link+" at ("+begin+","+end+") with probability "+linkProbability;
    }
}