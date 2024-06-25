package com.bytechef.component.data.mapper.util.mapping;

public class ObjectTypeMapping extends Mapping<Object, Object>{
    private int type;

    public ObjectTypeMapping() {
    }

    public ObjectTypeMapping(String from, String to, int type) {
        super(from, to);
        this.type = type;
    }

    public int isRequired() {
        return type;
    }

    public void setRequired(int type) {
        this.type = type;
    }
}
