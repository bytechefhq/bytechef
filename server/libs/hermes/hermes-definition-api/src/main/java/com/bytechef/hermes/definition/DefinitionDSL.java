
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

package com.bytechef.hermes.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DefinitionDSL {

    public static ModifiableProperty.ModifiableArrayProperty array() {
        return new ModifiableProperty.ModifiableArrayProperty(null);
    }

    public static ModifiableProperty.ModifiableArrayProperty array(String name) {
        return new ModifiableProperty.ModifiableArrayProperty(name);
    }

    public static ModifiableProperty.ModifiableBooleanProperty bool() {
        return new ModifiableProperty.ModifiableBooleanProperty(null);
    }

    public static ModifiableProperty.ModifiableBooleanProperty bool(String name) {
        return new ModifiableProperty.ModifiableBooleanProperty(name);
    }

    public static ModifiableProperty.ModifiableDateProperty date() {
        return new ModifiableProperty.ModifiableDateProperty(null);
    }

    public static ModifiableProperty.ModifiableDateProperty date(String name) {
        return new ModifiableProperty.ModifiableDateProperty(name);
    }

    public static ModifiableProperty.ModifiableDateTimeProperty dateTime() {
        return new ModifiableProperty.ModifiableDateTimeProperty(null);
    }

    public static ModifiableProperty.ModifiableDateTimeProperty dateTime(String name) {
        return new ModifiableProperty.ModifiableDateTimeProperty(name);
    }

    public static ModifiableDisplay display(String label) {
        return new ModifiableDisplay(label);
    }

    public static ModifiableDisplayOption.HideDisplayOptionCondition hide(String propertyName, Object value) {
        return new ModifiableDisplayOption.HideDisplayOptionCondition(Map.of(propertyName, List.of(value)));
    }

    public static ModifiableDisplayOption.HideDisplayOptionCondition hide(String propertyName, List<Object> values) {
        return new ModifiableDisplayOption.HideDisplayOptionCondition(Map.of(propertyName, values));
    }

    public static ModifiableDisplayOption.HideDisplayOptionCondition hide(
        String propertyName1, List<Object> values1, String propertyName2, List<Object> values2) {
        return new ModifiableDisplayOption.HideDisplayOptionCondition(
            Map.of(propertyName1, values1, propertyName2, values2));
    }

    public static ModifiableDisplayOption.HideDisplayOptionCondition hide(
        String propertyName1,
        List<Object> values1,
        String propertyName2,
        List<Object> values2,
        String propertyName3,
        List<Object> values3) {
        return new ModifiableDisplayOption.HideDisplayOptionCondition(
            Map.of(propertyName1, values1, propertyName2, values2, propertyName3, values3));
    }

    public static ModifiableDisplayOption.HideDisplayOptionCondition hide(
        String propertyName1,
        List<Object> values1,
        String propertyName2,
        List<Object> values2,
        String propertyName3,
        List<Object> values3,
        String propertyName4,
        List<Object> values4) {
        return new ModifiableDisplayOption.HideDisplayOptionCondition(
            Map.of(propertyName1, values1, propertyName2, values2, propertyName3, values3, propertyName4, values4));
    }

    public static ModifiableDisplayOption.HideDisplayOptionCondition hide(
        String propertyName1,
        List<Object> values1,
        String propertyName2,
        List<Object> values2,
        String propertyName3,
        List<Object> values3,
        String propertyName4,
        List<Object> values4,
        String propertyName5,
        List<Object> values5) {
        return new ModifiableDisplayOption.HideDisplayOptionCondition(Map.of(
            propertyName1,
            values1,
            propertyName2,
            values2,
            propertyName3,
            values3,
            propertyName4,
            values4,
            propertyName5,
            values5));
    }

    public static ModifiableProperty.ModifiableIntegerProperty integer() {
        return new ModifiableProperty.ModifiableIntegerProperty(null);
    }

    public static ModifiableProperty.ModifiableIntegerProperty integer(String name) {
        return new ModifiableProperty.ModifiableIntegerProperty(name);
    }

    public static Property.NullProperty nullable() {
        return new ModifiableProperty.ModifiableNullProperty(null);
    }

    public static Property.NullProperty nullable(String name) {
        return new ModifiableProperty.ModifiableNullProperty(name);
    }

    public static ModifiableProperty.ModifiableNumberProperty number() {
        return new ModifiableProperty.ModifiableNumberProperty(null);
    }

    public static ModifiableProperty.ModifiableNumberProperty number(String name) {
        return new ModifiableProperty.ModifiableNumberProperty(name);
    }

    public static ModifiableProperty.ModifiableObjectProperty object() {
        return new ModifiableProperty.ModifiableObjectProperty(null);
    }

    public static ModifiableProperty.ModifiableObjectProperty object(String name) {
        return new ModifiableProperty.ModifiableObjectProperty(name);
    }

    public static ModifiableProperty.ModifiableOneOfProperty oneOf() {
        return oneOf(null);
    }

    public static ModifiableProperty.ModifiableOneOfProperty oneOf(String name) {
        return new ModifiableProperty.ModifiableOneOfProperty(name);
    }

    public static ModifiablePropertyOption option(String name, boolean value) {
        return new ModifiablePropertyOption(name, value);
    }

    public static ModifiablePropertyOption option(String name, boolean value, String description) {
        return new ModifiablePropertyOption(name, value, description);
    }

    public static ModifiablePropertyOption option(String name, double value) {
        return new ModifiablePropertyOption(name, value);
    }

    public static ModifiablePropertyOption option(String name, double value, String description) {
        return new ModifiablePropertyOption(name, value, description);
    }

    public static ModifiablePropertyOption option(String name, int value) {
        return new ModifiablePropertyOption(name, value);
    }

    public static ModifiablePropertyOption option(String name, int value, String description) {
        return new ModifiablePropertyOption(name, value, description);
    }

    public static ModifiablePropertyOption option(String name, LocalDate value) {
        return new ModifiablePropertyOption(name, value);
    }

    public static ModifiablePropertyOption option(String name, LocalDate value, String description) {
        return new ModifiablePropertyOption(name, value, description);
    }

    public static ModifiablePropertyOption option(String name, LocalDateTime value) {
        return new ModifiablePropertyOption(name, value);
    }

    public static ModifiablePropertyOption option(String name, LocalDateTime value, String description) {
        return new ModifiablePropertyOption(name, value, description);
    }

    public static ModifiablePropertyOption option(String name, Object value) {
        return new ModifiablePropertyOption(name, value);
    }

    public static ModifiablePropertyOption option(String name, Object value, String description) {
        return new ModifiablePropertyOption(name, value, description);
    }

    public static ModifiablePropertyOption option(String name, String value) {
        return new ModifiablePropertyOption(name, value);
    }

    public static ModifiablePropertyOption option(String name, String value, String description) {
        return new ModifiablePropertyOption(name, value, description);
    }

    public static ModifiableResources resources() {
        return new ModifiableResources();
    }

    public static ModifiableDisplayOption.ShowDisplayOptionCondition show(String propertyName, Object value) {
        return new ModifiableDisplayOption.ShowDisplayOptionCondition(Map.of(propertyName, List.of(value)));
    }

    public static ModifiableDisplayOption.ShowDisplayOptionCondition show(String propertyName, List<Object> values) {
        return new ModifiableDisplayOption.ShowDisplayOptionCondition(Map.of(propertyName, values));
    }

    public static ModifiableDisplayOption.ShowDisplayOptionCondition show(
        String propertyName1, List<Object> values1, String propertyName2, List<Object> values2) {
        return new ModifiableDisplayOption.ShowDisplayOptionCondition(
            Map.of(propertyName1, values1, propertyName2, values2));
    }

    public static ModifiableDisplayOption.ShowDisplayOptionCondition show(
        String propertyName1,
        List<Object> values1,
        String propertyName2,
        List<Object> values2,
        String propertyName3,
        List<Object> values3) {
        return new ModifiableDisplayOption.ShowDisplayOptionCondition(
            Map.of(propertyName1, values1, propertyName2, values2, propertyName3, values3));
    }

    public static ModifiableDisplayOption.ShowDisplayOptionCondition show(
        String propertyName1,
        List<Object> values1,
        String propertyName2,
        List<Object> values2,
        String propertyName3,
        List<Object> values3,
        String propertyName4,
        List<Object> values4) {
        return new ModifiableDisplayOption.ShowDisplayOptionCondition(
            Map.of(propertyName1, values1, propertyName2, values2, propertyName3, values3, propertyName4, values4));
    }

    public static ModifiableDisplayOption.ShowDisplayOptionCondition show(
        String propertyName1,
        List<Object> values1,
        String propertyName2,
        List<Object> values2,
        String propertyName3,
        List<Object> values3,
        String propertyName4,
        List<Object> values4,
        String propertyName5,
        List<Object> values5) {
        return new ModifiableDisplayOption.ShowDisplayOptionCondition(Map.of(
            propertyName1,
            values1,
            propertyName2,
            values2,
            propertyName3,
            values3,
            propertyName4,
            values4,
            propertyName5,
            values5));
    }

    public static ModifiableProperty.ModifiableStringProperty string() {
        return new ModifiableProperty.ModifiableStringProperty(null);
    }

    public static ModifiableProperty.ModifiableStringProperty string(String name) {
        return new ModifiableProperty.ModifiableStringProperty(name);
    }

    protected static ModifiableProperty.ModifiableObjectProperty buildObject(
        String name, String description, String objectType) {
        return new ModifiableProperty.ModifiableObjectProperty(name)
            .description(description)
            .objectType(objectType);
    }

    protected static ModifiableProperty.ModifiableObjectProperty buildObject(
        String name, String description, String objectType, Property<?>... properties) {
        return new ModifiableProperty.ModifiableObjectProperty(name)
            .description(description)
            .objectType(objectType)
            .properties(properties);
    }

    public static final class ModifiableDisplay implements Display {

        private String category;
        private String description;
        private String icon;
        private String label;
        private String subtitle;
        private String[] tags;

        private ModifiableDisplay(String label) {
            this.label = label;
        }

        public ModifiableDisplay category(String category) {
            this.category = category;

            return this;
        }

        public ModifiableDisplay description(String description) {
            this.description = description;

            return this;
        }

        public ModifiableDisplay icon(String icon) {
            this.icon = icon;

            return this;
        }

        public ModifiableDisplay subtitle(String subtitle) {
            this.subtitle = subtitle;

            return this;
        }

        public ModifiableDisplay tags(String... tags) {
            this.tags = tags;

            return this;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getIcon() {
            return icon;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getSubtitle() {
            return subtitle;
        }

        @Override
        public String[] getTags() {
            return tags == null ? null : tags.clone();
        }
    }

    public static final class ModifiableDisplayOption implements DisplayOption {

        private Map<String, List<Object>> hide;
        private Map<String, List<Object>> show;

        private ModifiableDisplayOption(Map<String, List<Object>> hide, Map<String, List<Object>> show) {
            this.hide = hide;
            this.show = show;
        }

        public Map<String, List<Object>> getHide() {
            return hide == null ? null : new HashMap<>(hide);
        }

        public Map<String, List<Object>> getShow() {
            return show == null ? null : new HashMap<>(show);
        }

        abstract static class DisplayOptionCondition {

            protected final Map<String, List<Object>> conditions;

            public DisplayOptionCondition(Map<String, List<Object>> conditions) {
                this.conditions = conditions;
            }

            public Map<String, List<Object>> getConditions() {
                return conditions;
            }
        }

        public static final class HideDisplayOptionCondition extends DisplayOptionCondition {

            public HideDisplayOptionCondition(Map<String, List<Object>> conditions) {
                super(conditions);
            }
        }

        public static final class ShowDisplayOptionCondition extends DisplayOptionCondition {

            public ShowDisplayOptionCondition(Map<String, List<Object>> conditions) {
                super(conditions);
            }
        }
    }

    // CHECKSTYLE:OFF
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = Property.ArrayProperty.class, name = "ARRAY"),
        @JsonSubTypes.Type(value = Property.BooleanProperty.class, name = "BOOLEAN"),
        @JsonSubTypes.Type(value = Property.DateTimeProperty.class, name = "DATE_TIME"),
        @JsonSubTypes.Type(value = Property.IntegerProperty.class, name = "INTEGER"),
        @JsonSubTypes.Type(value = Property.NumberProperty.class, name = "NUMBER"),
        @JsonSubTypes.Type(value = Property.ObjectProperty.class, name = "OBJECT"),
        @JsonSubTypes.Type(value = Property.OneOfProperty.class, name = "ONE_OF"),
        @JsonSubTypes.Type(value = Property.StringProperty.class, name = "STRING")
    })
    public static sealed abstract class ModifiableProperty<M extends ModifiableProperty<M, P>, P extends Property<P>>
        implements
        Property<P>permits ModifiableProperty.ModifiableOneOfProperty,ModifiableProperty.ModifiableNullProperty,ModifiableProperty.ModifiableValueProperty {

        private Boolean advancedOption;
        private String description;
        private DisplayOption displayOption;
        private Boolean hidden;
        private String label;
        private Map<String, Object> metadata;
        private String placeholder;
        private Boolean required;
        private final String name;
        private final Property.Type type;

        protected ModifiableProperty(String name, Property.Type type) {
            this.name = name;
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        public M advancedOption(boolean advancedOption) {
            this.advancedOption = advancedOption;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M description(String description) {
            this.description = description;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M displayOption(ModifiableDisplayOption.HideDisplayOptionCondition hide) {
            this.displayOption = new ModifiableDisplayOption(hide.conditions, null);

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M displayOption(ModifiableDisplayOption.ShowDisplayOptionCondition show) {
            this.displayOption = new ModifiableDisplayOption(null, show.conditions);

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M displayOption(
            ModifiableDisplayOption.ShowDisplayOptionCondition show,
            ModifiableDisplayOption.HideDisplayOptionCondition hide) {
            this.displayOption = new ModifiableDisplayOption(hide.conditions, show.conditions);

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M displayOption(
            ModifiableDisplayOption.HideDisplayOptionCondition hide,
            ModifiableDisplayOption.ShowDisplayOptionCondition show) {
            this.displayOption = new ModifiableDisplayOption(hide.conditions, show.conditions);

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M hidden(boolean hidden) {
            this.hidden = hidden;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M label(String label) {
            this.label = label;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M metadata(String key, String value) {
            if (metadata == null) {
                metadata = new HashMap<>();
            }

            this.metadata.put(key, value);

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        @SuppressFBWarnings("EI2")
        public M metadata(Map<String, Object> metadata) {
            this.metadata = metadata;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M placeholder(String placeholder) {
            this.placeholder = placeholder;

            return (M) this;
        }

        @SuppressWarnings("unchecked")
        public M required(Boolean required) {
            this.required = required;

            return (M) this;
        }

        @Override
        public Boolean getAdvancedOption() {
            return advancedOption;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public DisplayOption getDisplayOption() {
            return displayOption;
        }

        @Override
        public Boolean getHidden() {
            return hidden;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata == null ? null : new HashMap<>(metadata);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getPlaceholder() {
            return placeholder;
        }

        @Override
        public Boolean getRequired() {
            return required;
        }

        @Override
        public Property.Type getType() {
            return type;
        }

        @JsonTypeName("ARRAY")
        public static final class ModifiableArrayProperty
            extends ModifiableValueProperty<Object[], ModifiableArrayProperty, ArrayProperty>
            implements Property.ArrayProperty {

            protected List<Property<?>> items;
            private Boolean multipleValues; // default true

            private ModifiableArrayProperty(String name) {
                super(name, Type.ARRAY);
            }

            public ModifiableArrayProperty exampleValue(Boolean... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Integer... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Long... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Float... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Double... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(String... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty exampleValue(Map<String, ?>... exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Boolean... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Integer... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Long... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Float... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Double... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(String... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty defaultValue(Map<String, ?>... defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableArrayProperty items(Property<?>... items) {
                if (items != null) {
                    this.items = List.of(items);
                }

                return this;
            }

            public ModifiableArrayProperty multipleValues(boolean multipleValues) {
                this.multipleValues = multipleValues;

                return this;
            }

            @Override
            public List<Property<?>> getItems() {
                return items;
            }

            @Override
            public Boolean getMultipleValues() {
                return multipleValues;
            }
        }

        @JsonTypeName("BOOLEAN")
        public static final class ModifiableBooleanProperty
            extends ModifiableValueProperty<Boolean, ModifiableBooleanProperty, BooleanProperty>
            implements Property.BooleanProperty {

            private ModifiableBooleanProperty(String name) {
                super(name, Type.BOOLEAN);
            }

            public ModifiableBooleanProperty defaultValue(boolean defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableBooleanProperty exampleValue(boolean exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }
        }

        @JsonTypeName("DATE")
        public static final class ModifiableDateProperty extends
            ModifiableValueProperty<LocalDate, ModifiableDateProperty, DateProperty> implements Property.DateProperty {

            private ModifiableDateProperty(String name) {
                super(name, Type.DATE);
            }

            public ModifiableDateProperty defaultValue(LocalDate defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableDateProperty exampleValue(LocalDate exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }
        }

        @JsonTypeName("DATE_TIME")
        public static final class ModifiableDateTimeProperty
            extends ModifiableValueProperty<LocalDateTime, ModifiableDateTimeProperty, DateTimeProperty>
            implements Property.DateTimeProperty {

            private ModifiableDateTimeProperty(String name) {
                super(name, Type.DATE_TIME);
            }

            public ModifiableDateTimeProperty defaultValue(LocalDateTime defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableDateTimeProperty exampleValue(LocalDateTime exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }
        }

        @JsonTypeName("INTEGER")
        public static final class ModifiableIntegerProperty
            extends ModifiableValueProperty<Integer, ModifiableIntegerProperty, IntegerProperty>
            implements Property.IntegerProperty {

            private Integer maxValue;
            private Integer minValue;

            private ModifiableIntegerProperty(String name) {
                super(name, Type.INTEGER);
            }

            public ModifiableIntegerProperty defaultValue(int value) {
                this.defaultValue = value;

                return this;
            }

            public ModifiableIntegerProperty exampleValue(int exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableIntegerProperty maxValue(int maxValue) {
                this.maxValue = maxValue;

                return this;
            }

            public ModifiableIntegerProperty minValue(int minValue) {
                this.minValue = minValue;

                return this;
            }

            @Override
            public Integer getMaxValue() {
                return maxValue;
            }

            @Override
            public Integer getMinValue() {
                return minValue;
            }
        }

        @JsonTypeName("NULL")
        @Schema(name = "NullProperty", description = "A null property type.")
        public static final class ModifiableNullProperty
            extends ModifiableProperty<ModifiableNullProperty, NullProperty> implements Property.NullProperty {

            protected ModifiableNullProperty(String name) {
                super(name, Type.NULL);
            }
        }

        @JsonTypeName("NUMBER")
        public static final class ModifiableNumberProperty
            extends ModifiableValueProperty<Double, ModifiableNumberProperty, NumberProperty>
            implements Property.NumberProperty {

            private Integer maxValue;
            private Integer minValue;
            private Integer numberPrecision;

            private ModifiableNumberProperty(String name) {
                super(name, Type.NUMBER);
            }

            public ModifiableNumberProperty defaultValue(int value) {
                this.defaultValue = (double) value;

                return this;
            }

            public ModifiableNumberProperty defaultValue(long value) {
                this.defaultValue = (double) value;

                return this;
            }

            public ModifiableNumberProperty defaultValue(float value) {
                this.defaultValue = (double) value;

                return this;
            }

            public ModifiableNumberProperty defaultValue(double value) {
                this.defaultValue = value;

                return this;
            }

            public ModifiableNumberProperty exampleValue(int value) {
                this.exampleValue = (double) value;

                return this;
            }

            public ModifiableNumberProperty exampleValue(long value) {
                this.exampleValue = (double) value;

                return this;
            }

            public ModifiableNumberProperty exampleValue(float value) {
                this.exampleValue = (double) value;

                return this;
            }

            public ModifiableNumberProperty exampleValue(double value) {
                this.exampleValue = value;

                return this;
            }

            public ModifiableNumberProperty maxValue(int maxValue) {
                this.maxValue = maxValue;

                return this;
            }

            public ModifiableNumberProperty minValue(int minValue) {
                this.minValue = minValue;

                return this;
            }

            public ModifiableNumberProperty numberPrecision(Integer numberPrecision) {
                this.numberPrecision = numberPrecision;

                return this;
            }

            @Override
            public Integer getMaxValue() {
                return maxValue;
            }

            @Override
            public Integer getMinValue() {
                return minValue;
            }

            @Override
            public Integer getNumberPrecision() {
                return numberPrecision;
            }
        }

        @JsonTypeName("OBJECT")
        public static final class ModifiableObjectProperty
            extends ModifiableValueProperty<Object, ModifiableObjectProperty, ObjectProperty>
            implements Property.ObjectProperty {

            private List<? extends Property<?>> additionalProperties;
            private Boolean multipleValues; // default true
            private String objectType;
            private List<? extends Property<?>> properties;

            private ModifiableObjectProperty(String name) {
                super(name, Type.OBJECT);
            }

            public ModifiableObjectProperty defaultValue(Map<String, Object> defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableObjectProperty exampleValue(Map<String, Object> exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableObjectProperty additionalProperties(Property<?>... additionalProperties) {
                if (additionalProperties != null) {
                    this.additionalProperties = List.of(additionalProperties);
                }

                return this;
            }

            @SuppressWarnings("rawtypes")
            public ModifiableObjectProperty additionalProperties(List<Property> additionalProperties) {
                if (additionalProperties != null) {
                    this.additionalProperties = additionalProperties.stream()
                        .map(property -> (Property<?>) property)
                        .toList();
                }

                return this;
            }

            public ModifiableObjectProperty multipleValues(boolean multipleValues) {
                this.multipleValues = multipleValues;

                return this;
            }

            public ModifiableObjectProperty objectType(String objectType) {
                this.objectType = objectType;

                return this;
            }

            public ModifiableObjectProperty properties(Property<?>... properties) {
                if (properties != null) {
                    this.properties = List.of(properties);
                }

                return this;
            }

            @SuppressWarnings("rawtypes")
            public ModifiableObjectProperty properties(List<Property> properties) {
                if (properties != null) {
                    this.properties = properties.stream()
                        .map(property -> (Property<?>) property)
                        .toList();
                }

                return this;
            }

            @Override
            public List<? extends Property<?>> getAdditionalProperties() {
                return additionalProperties == null ? null : new ArrayList<>(additionalProperties);
            }

            public Boolean getMultipleValues() {
                return multipleValues;
            }

            @Override
            public String getObjectType() {
                return objectType;
            }

            @Override
            public List<? extends Property<?>> getProperties() {
                return properties == null ? null : new ArrayList<>(properties);
            }
        }

        @JsonTypeName("ONE_OF")
        public static final class ModifiableOneOfProperty
            extends ModifiableProperty<ModifiableOneOfProperty, OneOfProperty> implements Property.OneOfProperty {

            private List<? extends Property<?>> types = List.of(
                new ModifiableArrayProperty(null),
                new ModifiableBooleanProperty(null),
                new ModifiableDateProperty(null),
                new ModifiableDateTimeProperty(null),
                new ModifiableIntegerProperty(null),
                new ModifiableNullProperty(null),
                new ModifiableNumberProperty(null),
                new ModifiableObjectProperty(null),
                new ModifiableStringProperty(null));

            private ModifiableOneOfProperty(String name) {
                super(name, Type.ONE_OF);
            }

            public ModifiableOneOfProperty types(Property<?>... properties) {
                this.types = List.of(properties);

                return this;
            }

            public List<? extends Property<?>> getTypes() {
                return types;
            }
        }

        @JsonTypeName("STRING")
        public static final class ModifiableStringProperty
            extends ModifiableValueProperty<String, ModifiableStringProperty, StringProperty>
            implements Property.StringProperty {

            private ControlType controlType;

            private ModifiableStringProperty(String name) {
                super(name, Type.STRING);
            }

            public ModifiableStringProperty defaultValue(String value) {
                this.defaultValue = value;

                return this;
            }

            public ModifiableStringProperty exampleValue(String exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableStringProperty controlType(ControlType controlType) {
                this.controlType = controlType;

                return this;
            }

            @Override
            public ControlType getControlType() {
                return controlType;
            }
        }

        public abstract static sealed class ModifiableValueProperty<V, M extends ModifiableValueProperty<V, M, P>, P extends ValueProperty<V, P>>
            extends ModifiableProperty<M, P>
            implements
            Property.ValueProperty<V, P>permits ModifiableArrayProperty,ModifiableBooleanProperty,ModifiableDateProperty,ModifiableDateTimeProperty,ModifiableIntegerProperty,ModifiableNumberProperty,ModifiableObjectProperty,ModifiableStringProperty {

            protected V defaultValue;
            protected V exampleValue;
            private List<PropertyOption> options;
            private PropertyOptionDataSource optionDataSource;

            private ModifiableValueProperty(Property.Type type) {
                this(null, type);
            }

            protected ModifiableValueProperty(String name, Property.Type type) {
                super(name, type);
            }

            @SuppressWarnings("unchecked")
            public M options(PropertyOption... options) {
                this.options = List.of(options);

                return (M) this;
            }

            @SuppressWarnings("unchecked")
            public M propertyOptionDataSource(PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

                return (M) this;
            }

            @Override
            public V getDefaultValue() {
                return defaultValue;
            }

            @Override
            public V getExampleValue() {
                return exampleValue;
            }

            @Override
            public List<PropertyOption> getOptions() {
                return options;
            }

            @Override
            public PropertyOptionDataSource getOptionsDataSource() {
                return optionDataSource;
            }
        }
    }
    // CHECKSTYLE:ON

    public static final class ModifiablePropertyOption implements PropertyOption {

        private String description;
        private DisplayOption displayOption;
        private String name;
        private Object value;

        private ModifiablePropertyOption(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        private ModifiablePropertyOption(String name, Object value, String description) {
            this.name = name;
            this.value = value;
            this.description = description;
        }

        public ModifiablePropertyOption description(String description) {
            this.description = description;

            return this;
        }

        public ModifiablePropertyOption displayOption(ModifiableDisplayOption.HideDisplayOptionCondition hide) {
            this.displayOption = new ModifiableDisplayOption(hide.conditions, null);

            return this;
        }

        public ModifiablePropertyOption displayOption(ModifiableDisplayOption.ShowDisplayOptionCondition show) {
            this.displayOption = new ModifiableDisplayOption(null, show.conditions);

            return this;
        }

        public ModifiablePropertyOption displayOption(
            ModifiableDisplayOption.ShowDisplayOptionCondition show,
            ModifiableDisplayOption.HideDisplayOptionCondition hide) {
            this.displayOption = new ModifiableDisplayOption(hide.conditions, show.conditions);

            return this;
        }

        public ModifiablePropertyOption displayOption(
            ModifiableDisplayOption.HideDisplayOptionCondition hide,
            ModifiableDisplayOption.ShowDisplayOptionCondition show) {
            this.displayOption = new ModifiableDisplayOption(hide.conditions, show.conditions);

            return this;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public DisplayOption getDisplayOption() {
            return displayOption;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

    public static final class ModifiablePropertyOptionDataSource implements PropertyOptionDataSource {

        protected List<String> loadOptionsDependsOn;

        @JsonIgnore
        private Function<Object, Object> loadOptionsFunction;

        private ModifiablePropertyOptionDataSource() {
        }

        @SuppressWarnings("unchecked")
        public PropertyOptionDataSource loadOptionsDependsOn(String... propertyNames) {
            this.loadOptionsDependsOn = List.of(propertyNames);

            return this;
        }

        @SuppressWarnings("unchecked")
        public PropertyOptionDataSource loadOptionsFunction(Function<Object, Object> loadOptionsFunction) {
            this.loadOptionsFunction = loadOptionsFunction;

            return this;
        }

        @Override
        public List<String> getLoadOptionsDependsOn() {
            return loadOptionsDependsOn;
        }

        @Override
        public Function<Object, Object> getLoadOptionsFunction() {
            return loadOptionsFunction;
        }
    }

    public static final class ModifiableResources implements Resources {

        private String documentationUrl;

        private ModifiableResources() {
        }

        public Resources documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;

            return this;
        }

        @Override
        public String getDocumentationUrl() {
            return documentationUrl;
        }
    }
}
