package com.bytechef.component.openai.util.records;

public class MessageRecord {
    private String content;
    private String role;

    public MessageRecord(){}

    public MessageRecord(String content, String role){
        this.content=content;
        this.role=role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
