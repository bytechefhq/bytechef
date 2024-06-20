package com.bytechef.component.data.mapper.util.mapping;

public abstract class Mapping<F,T> {
    private F from;
    private T to;

    public Mapping() {
    }

    public Mapping(F from, T to) {
        this.from = from;
        this.to = to;
    }

    public F getFrom() {
        return from;
    }

    public void setFrom(F from) {
        this.from = from;
    }

    public T getTo() {
        return to;
    }

    public void setTo(T to) {
        this.to = to;
    }
}
