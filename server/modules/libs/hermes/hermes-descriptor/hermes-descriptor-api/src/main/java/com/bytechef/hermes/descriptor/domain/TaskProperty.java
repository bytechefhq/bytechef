/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.descriptor.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public abstract sealed class TaskProperty<T extends TaskProperty<T>>
        permits TaskProperty.OptionTaskProperty, TaskProperty.TypeTaskProperty {

    public enum Type {
        ARRAY,
        BOOLEAN,
        DATE_TIME,
        INTEGER,
        NULL,
        NUMBER,
        OBJECT,
        STRING,
    }

    protected String description;
    protected DisplayOption displayOption;
    protected String displayName;
    protected String name;
    protected String placeholder;

    private TaskProperty(String name) {
        this.name = name;
    }

    @SuppressWarnings("unchecked")
    public T description(String description) {
        this.description = description;

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T displayName(String displayName) {
        this.displayName = displayName;

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T displayOption(DisplayOption.DisplayOptionEntry... displayOptionEntries) {
        this.displayOption = DisplayOption.build(List.of(displayOptionEntries));

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T placeholder(String placeholder) {
        this.placeholder = placeholder;

        return (T) this;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DisplayOption getDisplayOption() {
        return displayOption;
    }

    public String getName() {
        return name;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public static final class OptionTaskProperty extends TaskProperty<OptionTaskProperty> {

        private Boolean multipleValues;
        private List<TaskProperty<?>> options;

        OptionTaskProperty() {
            super(null);
        }

        public OptionTaskProperty multipleValues(Boolean multipleValues) {
            this.multipleValues = multipleValues;

            return this;
        }

        public OptionTaskProperty options(TaskProperty<?>... options) {
            this.options = Stream.of(options).filter(Objects::nonNull).toList();

            return this;
        }

        public Boolean getMultipleValues() {
            return multipleValues;
        }

        public List<TaskProperty<?>> getOptions() {
            return options;
        }
    }

    public static sealed class TypeTaskProperty<T extends TypeTaskProperty<T>> extends TaskProperty<T>
            permits TaskProperty.AnyTaskProperty, TaskProperty.ValueTaskProperty {

        protected Boolean required;

        TypeTaskProperty(String name) {
            super(name);
        }

        @SuppressWarnings("unchecked")
        public T required(Boolean required) {
            this.required = required;

            return (T) this;
        }

        public Boolean getRequired() {
            return required;
        }
    }

    public static final class AnyTaskProperty extends TypeTaskProperty<AnyTaskProperty> {

        private List<? extends TypeTaskProperty<?>> types;

        AnyTaskProperty(String name) {
            super(name);
        }

        public AnyTaskProperty types(TypeTaskProperty<?>... properties) {
            this.types = List.of(properties);

            return this;
        }

        public List<? extends TypeTaskProperty<?>> getTypes() {
            return types;
        }
    }

    public static sealed class ValueTaskProperty<T, V extends ValueTaskProperty<T, V>> extends TypeTaskProperty<V>
            permits TaskProperty.ArrayTaskProperty,
                    TaskProperty.BooleanTaskProperty,
                    TaskProperty.DateTimeTaskProperty,
                    TaskProperty.IntegerTaskProperty,
                    TaskProperty.NullTaskProperty,
                    TaskProperty.NumberTaskProperty,
                    TaskProperty.ObjectTaskProperty,
                    TaskProperty.StringTaskProperty {

        protected List<TaskPropertyOption> options;
        protected T defaultValue;
        protected T exampleValue;
        protected List<String> loadOptionsDependsOn;
        protected String loadOptionsMethod;
        protected Type type;

        public ValueTaskProperty(String name) {
            super(name);
        }

        @SuppressWarnings("unchecked")
        public V loadOptionsDependsOn(String... propertyNames) {
            this.loadOptionsDependsOn = List.of(propertyNames);

            return (V) this;
        }

        @SuppressWarnings("unchecked")
        public V loadOptionsMethod(String loadOptionsMethod) {
            this.loadOptionsMethod = loadOptionsMethod;

            return (V) this;
        }

        @SuppressWarnings("unchecked")
        public V options(TaskPropertyOption... options) {
            this.options = List.of(options);

            return (V) this;
        }

        public T getDefaultValue() {
            return defaultValue;
        }

        public T getExampleValue() {
            return exampleValue;
        }

        public List<String> getLoadOptionsDependsOn() {
            return loadOptionsDependsOn;
        }

        public String getLoadOptionsMethod() {
            return loadOptionsMethod;
        }

        public List<TaskPropertyOption> getOptions() {
            return options;
        }

        public Type getType() {
            return type;
        }
    }

    public static final class ArrayTaskProperty extends ValueTaskProperty<Object[], ArrayTaskProperty> {

        private List<? extends TypeTaskProperty<?>> items;

        ArrayTaskProperty(String name) {
            super(name);
            this.type = Type.ARRAY;
        }

        public ArrayTaskProperty exampleValue(Boolean... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayTaskProperty exampleValue(Integer... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayTaskProperty exampleValue(Long... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayTaskProperty exampleValue(Float... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayTaskProperty exampleValue(Double... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayTaskProperty exampleValue(String... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayTaskProperty exampleValue(Map<String, ?>... exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ArrayTaskProperty defaultValue(Boolean... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayTaskProperty defaultValue(Integer... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayTaskProperty defaultValue(Long... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayTaskProperty defaultValue(Float... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayTaskProperty defaultValue(Double... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayTaskProperty defaultValue(String... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayTaskProperty defaultValue(Map<String, ?>... defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ArrayTaskProperty items(TypeTaskProperty<?>... items) {
            this.items = List.of(items);

            return this;
        }

        public List<? extends TypeTaskProperty<?>> getItems() {
            return items;
        }
    }

    public static final class ObjectTaskProperty extends ValueTaskProperty<Object, ObjectTaskProperty> {

        private Boolean additionalProperties;
        private List<? extends TypeTaskProperty<?>> properties;

        ObjectTaskProperty(String name) {
            super(name);
            this.type = Type.OBJECT;
        }

        public ObjectTaskProperty additionalProperties(boolean additionalProperties) {
            this.additionalProperties = additionalProperties;

            return this;
        }

        public ObjectTaskProperty defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public ObjectTaskProperty exampleValue(Object exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }

        public ObjectTaskProperty properties(TypeTaskProperty<?>... properties) {
            this.properties = List.of(properties);

            return this;
        }

        public Boolean getAdditionalProperties() {
            return additionalProperties;
        }

        public List<? extends TypeTaskProperty<?>> getProperties() {
            return properties;
        }
    }

    public static final class BooleanTaskProperty extends ValueTaskProperty<Boolean, BooleanTaskProperty> {

        BooleanTaskProperty(String name) {
            super(name);
            this.type = Type.BOOLEAN;
        }

        public BooleanTaskProperty defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public BooleanTaskProperty exampleValue(boolean exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }
    }

    public static final class DateTimeTaskProperty extends ValueTaskProperty<LocalDateTime, DateTimeTaskProperty> {

        DateTimeTaskProperty(String name) {
            super(name);
            this.type = Type.DATE_TIME;
        }

        public DateTimeTaskProperty defaultValue(LocalDateTime defaultValue) {
            this.defaultValue = defaultValue;

            return this;
        }

        public DateTimeTaskProperty exampleValue(LocalDateTime exampleValue) {
            this.exampleValue = exampleValue;

            return this;
        }
    }

    public static final class NullTaskProperty extends ValueTaskProperty<Void, NullTaskProperty> {

        NullTaskProperty(String name) {
            super(name);
            this.type = Type.NULL;
        }
    }

    public static final class NumberTaskProperty extends ValueTaskProperty<Number, NumberTaskProperty> {

        private Integer maxValue;
        private Integer minValue;
        private Integer numberPrecision;

        NumberTaskProperty(String name) {
            super(name);
            this.type = Type.NUMBER;
        }

        public NumberTaskProperty defaultValue(int value) {
            this.defaultValue = value;

            return this;
        }

        public NumberTaskProperty defaultValue(long value) {
            this.defaultValue = value;

            return this;
        }

        public NumberTaskProperty defaultValue(float value) {
            this.defaultValue = value;

            return this;
        }

        public NumberTaskProperty defaultValue(double value) {
            this.defaultValue = value;

            return this;
        }

        public NumberTaskProperty numberPrecision(Integer numberPrecision) {
            this.numberPrecision = numberPrecision;

            return this;
        }

        public NumberTaskProperty maxValue(int maxValue) {
            this.maxValue = maxValue;

            return this;
        }

        public NumberTaskProperty minValue(int minValue) {
            this.minValue = minValue;

            return this;
        }

        public Integer getMaxValue() {
            return maxValue;
        }

        public Integer getMinValue() {
            return minValue;
        }

        public Integer getNumberPrecision() {
            return numberPrecision;
        }
    }

    public static final class IntegerTaskProperty extends ValueTaskProperty<Integer, IntegerTaskProperty> {

        private Integer maxValue;
        private Integer minValue;

        IntegerTaskProperty(String name) {
            super(name);
            this.type = Type.INTEGER;
        }

        public IntegerTaskProperty defaultValue(Integer value) {
            this.defaultValue = value;

            return this;
        }

        public IntegerTaskProperty maxValue(int maxValue) {
            this.maxValue = maxValue;

            return this;
        }

        public IntegerTaskProperty minValue(int minValue) {
            this.minValue = minValue;

            return this;
        }

        public Integer getMaxValue() {
            return maxValue;
        }

        public Integer getMinValue() {
            return minValue;
        }
    }

    public static final class StringTaskProperty extends ValueTaskProperty<String, StringTaskProperty> {

        StringTaskProperty(String name) {
            super(name);
            this.type = Type.STRING;
        }

        public StringTaskProperty defaultValue(String value) {
            this.defaultValue = value;

            return this;
        }
    }
}
