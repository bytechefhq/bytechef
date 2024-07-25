package com.bytechef.component.openai.util.records;

public class ImageMessageRecord {
    private String content;
    private Float weight;

    public ImageMessageRecord(){}

    public ImageMessageRecord(String content, Float weight){
        this.content=content;
        this.weight=weight;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }
}
