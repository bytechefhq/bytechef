package com.bytechef.component.data.mapper.util.mapping;

public class RequiredStringMapping extends Mapping<String, String>{
    private boolean required;

    public RequiredStringMapping() {
    }

    public RequiredStringMapping(String from, String to, boolean required) {
        super(from, to);
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
