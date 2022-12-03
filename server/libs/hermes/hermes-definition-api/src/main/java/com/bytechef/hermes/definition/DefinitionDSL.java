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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefinitionDSL {

    public static ModifiableProperty.ModifiableAnyProperty any() {
        return any(null);
    }

    public static ModifiableProperty.ModifiableAnyProperty any(String name) {
        return new ModifiableProperty.ModifiableAnyProperty(name);
    }

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

    public static ModifiableDisplayOption.DisplayOptionProperty hideWhen(String propertyName) {
        return new ModifiableDisplayOption.DisplayOptionProperty(
                new ModifiableDisplayOption.HideDisplayOptionCondition(propertyName));
    }

    public static ModifiableProperty.ModifiableIntegerProperty integer() {
        return new ModifiableProperty.ModifiableIntegerProperty(null);
    }

    public static ModifiableProperty.ModifiableIntegerProperty integer(String name) {
        return new ModifiableProperty.ModifiableIntegerProperty(name);
    }

    public static Property.NullProperty nullable() {
        return new Property.NullProperty(null);
    }

    public static Property.NullProperty nullable(String name) {
        return new Property.NullProperty(name);
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

    public static ModifiableDisplayOption.DisplayOptionProperty showWhen(String propertyName) {
        return new ModifiableDisplayOption.DisplayOptionProperty(
                new ModifiableDisplayOption.ShowDisplayOptionCondition(propertyName));
    }

    public static ModifiableProperty.ModifiableStringProperty string() {
        return new ModifiableProperty.ModifiableStringProperty(null);
    }

    public static ModifiableProperty.ModifiableStringProperty string(String name) {
        return new ModifiableProperty.ModifiableStringProperty(name);
    }

    protected static ModifiableProperty.ModifiableObjectProperty buildObject(
            String name, String description, String objectType, Property<?>... properties) {
        return new ModifiableProperty.ModifiableObjectProperty(name)
                .description(description)
                .objectType(objectType)
                .properties(properties);
    }

    public static final class ModifiableDisplay extends Display {

        private ModifiableDisplay(String label) {
            super(label);
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
    }

    public static final class ModifiableDisplayOption extends DisplayOption {

        private ModifiableDisplayOption() {}

        static ModifiableDisplayOption of(List<DisplayOptionCondition> displayOptionConditions) {
            ModifiableDisplayOption displayOption = new ModifiableDisplayOption();

            for (DisplayOptionCondition displayOptionCondition : displayOptionConditions) {
                if (displayOptionCondition instanceof HideDisplayOptionCondition hideDisplayOptionCondition) {
                    if (displayOption.hideWhen == null) {
                        displayOption.hideWhen = new HashMap<>();
                    }

                    displayOption.hideWhen.computeIfAbsent(
                            hideDisplayOptionCondition.propertyName, key -> displayOptionCondition.values);
                } else if (displayOptionCondition instanceof ShowDisplayOptionCondition showDisplayOptionCondition) {
                    if (displayOption.showWhen == null) {
                        displayOption.showWhen = new HashMap<>();
                    }

                    displayOption.showWhen.computeIfAbsent(
                            showDisplayOptionCondition.propertyName, key -> displayOptionCondition.values);
                }
            }

            return displayOption;
        }

        public static final class DisplayOptionProperty {
            private DisplayOptionCondition displayOptionCondition;

            public DisplayOptionProperty(DisplayOptionCondition displayOptionCondition) {
                this.displayOptionCondition = displayOptionCondition;
            }

            public DisplayOptionCondition eq(Boolean value) {
                displayOptionCondition.values = List.of(value);

                return displayOptionCondition;
            }

            public DisplayOptionCondition eq(Integer value) {
                displayOptionCondition.values = List.of(value);

                return displayOptionCondition;
            }

            public DisplayOptionCondition eq(Long value) {
                displayOptionCondition.values = List.of(value);

                return displayOptionCondition;
            }

            public DisplayOptionCondition eq(Float value) {
                displayOptionCondition.values = List.of(value);

                return displayOptionCondition;
            }

            public DisplayOptionCondition eq(Double value) {
                displayOptionCondition.values = List.of(value);

                return displayOptionCondition;
            }

            public DisplayOptionCondition eq(String value) {
                displayOptionCondition.values = List.of(value);

                return displayOptionCondition;
            }

            public DisplayOptionCondition in(Boolean... values) {
                displayOptionCondition.values = Arrays.stream(values).collect(Collectors.toCollection(ArrayList::new));

                return displayOptionCondition;
            }

            public DisplayOptionCondition in(Integer... values) {
                displayOptionCondition.values = Arrays.stream(values).collect(Collectors.toCollection(ArrayList::new));

                return displayOptionCondition;
            }

            public DisplayOptionCondition in(Long... values) {
                displayOptionCondition.values = Arrays.stream(values).collect(Collectors.toCollection(ArrayList::new));

                return displayOptionCondition;
            }

            public DisplayOptionCondition in(Float... values) {
                displayOptionCondition.values = Arrays.stream(values).collect(Collectors.toCollection(ArrayList::new));

                return displayOptionCondition;
            }

            public DisplayOptionCondition in(Double... values) {
                displayOptionCondition.values = Arrays.stream(values).collect(Collectors.toCollection(ArrayList::new));

                return displayOptionCondition;
            }

            public DisplayOptionCondition in(String... values) {
                displayOptionCondition.values = Arrays.stream(values).collect(Collectors.toCollection(ArrayList::new));

                return displayOptionCondition;
            }
        }

        public abstract static class DisplayOptionCondition {

            protected final String propertyName;
            protected List<Object> values = List.of();

            public DisplayOptionCondition(String propertyName) {
                this.propertyName = propertyName;
            }

            public String getPropertyName() {
                return propertyName;
            }

            public List<Object> getValues() {
                return new ArrayList<>(values);
            }
        }

        public static final class HideDisplayOptionCondition extends DisplayOptionCondition {

            public HideDisplayOptionCondition(String propertyName) {
                super(propertyName);
            }
        }

        public static final class ShowDisplayOptionCondition extends DisplayOptionCondition {

            public ShowDisplayOptionCondition(String propertyName) {
                super(propertyName);
            }
        }
    }

    public static final class ModifiablePropertyOptionDataSource extends PropertyOptionDataSource {

        private ModifiablePropertyOptionDataSource() {}

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
    }

    public static class ModifiableProperty {
        public static final class ModifiableAnyProperty extends Property.AnyProperty {

            private ModifiableAnyProperty(String name) {
                super(name);
            }

            public ModifiableAnyProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableAnyProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableAnyProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableAnyProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableAnyProperty required(Boolean required) {
                this.required = required;

                return this;
            }

            public ModifiableAnyProperty types(Property<?>... properties) {
                this.types = List.of(properties);

                return this;
            }
        }

        public static final class ModifiableArrayProperty extends Property.ArrayProperty {

            private ModifiableArrayProperty(String name) {
                super(name);
            }

            public ModifiableArrayProperty advancedOption(boolean additional) {
                this.advancedOption = additional;

                return this;
            }

            public ModifiableArrayProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableArrayProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableArrayProperty hidden(boolean hidden) {
                this.hidden = hidden;

                return this;
            }

            public ModifiableArrayProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableArrayProperty metadata(String key, String value) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                this.metadata.put(key, value);

                return this;
            }

            @SuppressFBWarnings("EI2")
            public ModifiableArrayProperty metadata(Map<String, Object> metadata) {
                this.metadata = metadata;

                return this;
            }

            public ModifiableArrayProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableArrayProperty required(Boolean required) {
                this.required = required;

                return this;
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

            public ModifiableArrayProperty options(PropertyOption... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableArrayProperty propertyOptionDataSource(PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

                return this;
            }

            public ModifiableArrayProperty items(Property... items) {
                this.items = List.of(items);

                return this;
            }
        }

        public static final class ModifiableBooleanProperty extends Property.BooleanProperty {

            private ModifiableBooleanProperty(String name) {
                super(name);
            }

            public ModifiableBooleanProperty advancedOption(boolean additional) {
                this.advancedOption = additional;

                return this;
            }

            public ModifiableBooleanProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableBooleanProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableBooleanProperty hidden(boolean hidden) {
                this.hidden = hidden;

                return this;
            }

            public ModifiableBooleanProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableBooleanProperty metadata(String key, String value) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                this.metadata.put(key, value);

                return this;
            }

            @SuppressFBWarnings("EI2")
            public ModifiableBooleanProperty metadata(Map<String, Object> metadata) {
                this.metadata = metadata;

                return this;
            }

            public ModifiableBooleanProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableBooleanProperty required(Boolean required) {
                this.required = required;

                return this;
            }

            public ModifiableBooleanProperty defaultValue(boolean defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableBooleanProperty exampleValue(boolean exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableBooleanProperty options(PropertyOption... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableBooleanProperty propertyOptionDataSource(
                    PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

                return this;
            }
        }

        public static final class ModifiableDateProperty extends Property.DateProperty {

            private ModifiableDateProperty(String name) {
                super(name);
            }

            public ModifiableDateProperty advancedOption(boolean additional) {
                this.advancedOption = additional;

                return this;
            }

            public ModifiableDateProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableDateProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableDateProperty hidden(boolean hidden) {
                this.hidden = hidden;

                return this;
            }

            public ModifiableDateProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableDateProperty metadata(String key, String value) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                this.metadata.put(key, value);

                return this;
            }

            @SuppressFBWarnings("EI2")
            public ModifiableDateProperty metadata(Map<String, Object> metadata) {
                this.metadata = metadata;

                return this;
            }

            public ModifiableDateProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableDateProperty required(Boolean required) {
                this.required = required;

                return this;
            }

            public ModifiableDateProperty defaultValue(LocalDate defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableDateProperty exampleValue(LocalDate exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableDateProperty options(PropertyOption... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableDateProperty propertyOptionDataSource(PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

                return this;
            }
        }

        public static final class ModifiableDateTimeProperty extends Property.DateTimeProperty {

            private ModifiableDateTimeProperty(String name) {
                super(name);
            }

            public ModifiableDateTimeProperty advancedOption(boolean additional) {
                this.advancedOption = additional;

                return this;
            }

            public ModifiableDateTimeProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableDateTimeProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableDateTimeProperty hidden(boolean hidden) {
                this.hidden = hidden;

                return this;
            }

            public ModifiableDateTimeProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableDateTimeProperty metadata(String key, String value) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                this.metadata.put(key, value);

                return this;
            }

            @SuppressFBWarnings("EI2")
            public ModifiableDateTimeProperty metadata(Map<String, Object> metadata) {
                this.metadata = metadata;

                return this;
            }

            public ModifiableDateTimeProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableDateTimeProperty required(Boolean required) {
                this.required = required;

                return this;
            }

            public ModifiableDateTimeProperty defaultValue(LocalDateTime defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableDateTimeProperty exampleValue(LocalDateTime exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableDateTimeProperty options(PropertyOption... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableDateTimeProperty propertyOptionDataSource(
                    PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

                return this;
            }
        }

        public static final class ModifiableNumberProperty extends Property.NumberProperty {

            private ModifiableNumberProperty(String name) {
                super(name);
            }

            public ModifiableNumberProperty advancedOption(boolean additional) {
                this.advancedOption = additional;

                return this;
            }

            public ModifiableNumberProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableNumberProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableNumberProperty hidden(boolean hidden) {
                this.hidden = hidden;

                return this;
            }

            public ModifiableNumberProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableNumberProperty metadata(String key, String value) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                this.metadata.put(key, value);

                return this;
            }

            @SuppressFBWarnings("EI2")
            public ModifiableNumberProperty metadata(Map<String, Object> metadata) {
                this.metadata = metadata;

                return this;
            }

            public ModifiableNumberProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableNumberProperty required(Boolean required) {
                this.required = required;

                return this;
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

            public ModifiableNumberProperty options(PropertyOption... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableNumberProperty propertyOptionDataSource(
                    PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

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
        }

        public static final class ModifiableIntegerProperty extends Property.IntegerProperty {

            private ModifiableIntegerProperty(String name) {
                super(name);
            }

            public ModifiableIntegerProperty advancedOption(boolean additional) {
                this.advancedOption = additional;

                return this;
            }

            public ModifiableIntegerProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableIntegerProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableIntegerProperty hidden(boolean hidden) {
                this.hidden = hidden;

                return this;
            }

            public ModifiableIntegerProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableIntegerProperty metadata(String key, String value) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                this.metadata.put(key, value);

                return this;
            }

            @SuppressFBWarnings("EI2")
            public ModifiableIntegerProperty metadata(Map<String, Object> metadata) {
                this.metadata = metadata;

                return this;
            }

            public ModifiableIntegerProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableIntegerProperty required(Boolean required) {
                this.required = required;

                return this;
            }

            public ModifiableIntegerProperty defaultValue(int value) {
                this.defaultValue = value;

                return this;
            }

            public ModifiableIntegerProperty exampleValue(int exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableIntegerProperty options(PropertyOption... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableIntegerProperty propertyOptionDataSource(
                    PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

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
        }

        public static final class ModifiableObjectProperty extends Property.ObjectProperty {

            private ModifiableObjectProperty(String name) {
                super(name);
            }

            public ModifiableObjectProperty advancedOption(boolean additional) {
                this.advancedOption = additional;

                return this;
            }

            public ModifiableObjectProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableObjectProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableObjectProperty hidden(boolean hidden) {
                this.hidden = hidden;

                return this;
            }

            public ModifiableObjectProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableObjectProperty metadata(String key, String value) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                this.metadata.put(key, value);

                return this;
            }

            @SuppressFBWarnings("EI2")
            public ModifiableObjectProperty metadata(Map<String, Object> metadata) {
                this.metadata = metadata;

                return this;
            }

            public ModifiableObjectProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableObjectProperty required(Boolean required) {
                this.required = required;

                return this;
            }

            public ModifiableObjectProperty defaultValue(Map<String, Object> defaultValue) {
                this.defaultValue = defaultValue;

                return this;
            }

            public ModifiableObjectProperty exampleValue(Map<String, Object> exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableObjectProperty options(PropertyOption... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableObjectProperty propertyOptionDataSource(
                    PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

                return this;
            }

            public ModifiableObjectProperty additionalProperties(boolean additionalProperties) {
                this.additionalProperties = additionalProperties;

                return this;
            }

            public ModifiableObjectProperty objectType(String objectType) {
                this.objectType = objectType;

                return this;
            }

            public ModifiableObjectProperty properties(Property<?>... properties) {
                this.properties = List.of(properties);

                return this;
            }
        }

        public static final class ModifiableStringProperty extends Property.StringProperty {

            private ModifiableStringProperty(String name) {
                super(name);
            }

            public ModifiableStringProperty advancedOption(boolean additional) {
                this.advancedOption = additional;

                return this;
            }

            public ModifiableStringProperty description(String description) {
                this.description = description;

                return this;
            }

            public ModifiableStringProperty displayOption(
                    ModifiableDisplayOption.DisplayOptionCondition... displayOptionConditions) {
                this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionConditions));

                return this;
            }

            public ModifiableStringProperty hidden(boolean hidden) {
                this.hidden = hidden;

                return this;
            }

            public ModifiableStringProperty label(String label) {
                this.label = label;

                return this;
            }

            public ModifiableStringProperty metadata(String key, String value) {
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                this.metadata.put(key, value);

                return this;
            }

            @SuppressFBWarnings("EI2")
            public ModifiableStringProperty metadata(Map<String, Object> metadata) {
                this.metadata = metadata;

                return this;
            }

            public ModifiableStringProperty placeholder(String placeholder) {
                this.placeholder = placeholder;

                return this;
            }

            public ModifiableStringProperty required(Boolean required) {
                this.required = required;

                return this;
            }

            public ModifiableStringProperty defaultValue(String value) {
                this.defaultValue = value;

                return this;
            }

            public ModifiableStringProperty exampleValue(String exampleValue) {
                this.exampleValue = exampleValue;

                return this;
            }

            public ModifiableStringProperty options(PropertyOption... options) {
                this.options = List.of(options);

                return this;
            }

            public ModifiableStringProperty propertyOptionDataSource(
                    PropertyOptionDataSource propertyOptionDataSource) {
                this.optionDataSource = propertyOptionDataSource;

                return this;
            }

            public ModifiableStringProperty controlType(ControlType controlType) {
                this.controlType = controlType;

                return this;
            }
        }
    }

    public static final class ModifiablePropertyOption extends PropertyOption {

        private ModifiablePropertyOption(String name, Object value) {
            super(name, value);
        }

        private ModifiablePropertyOption(String name, Object value, String description) {
            super(name, value, description);
        }

        public ModifiablePropertyOption description(String description) {
            this.description = description;

            return this;
        }

        public ModifiablePropertyOption displayOption(
                ModifiableDisplayOption.DisplayOptionCondition... displayOptionEntries) {
            this.displayOption = ModifiableDisplayOption.of(List.of(displayOptionEntries));

            return this;
        }

        public ModifiablePropertyOption value(boolean value) {
            this.value = value;

            return this;
        }

        public ModifiablePropertyOption value(LocalDate value) {
            this.value = value;

            return this;
        }

        public ModifiablePropertyOption value(LocalDateTime value) {
            this.value = value;

            return this;
        }

        public ModifiablePropertyOption value(Object value) {
            this.value = value;

            return this;
        }

        public ModifiablePropertyOption value(String value) {
            this.value = value;

            return this;
        }
    }

    public static final class ModifiableResources extends Resources {

        private ModifiableResources() {}

        public Resources documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;

            return this;
        }
    }
}
