/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.data.mapper.model;

/**
 * @author Marko Kriskovic
 */
public abstract class Mapping<F, T> {

    private F from;
    private T to;

    protected Mapping() {
    }

    protected Mapping(F from, T to) {
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
