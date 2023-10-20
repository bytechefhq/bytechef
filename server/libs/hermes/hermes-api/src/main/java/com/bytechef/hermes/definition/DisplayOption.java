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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Schema(name = "DisplayOption", description = "Defines rules when a property should be shown or hidden.")
public class DisplayOption {

    private Map<String, List<Object>> hideWhen;
    private Map<String, List<Object>> showWhen;

    private DisplayOption() {}

    @Schema(
            name = "hideWhen",
            description =
                    "The map of property names and list of values to check against if the property should be hidden.")
    public Map<String, List<Object>> getHideWhen() {
        return hideWhen;
    }

    @Schema(
            name = "showWhen",
            description =
                    "The map of property names and list of values to check against if the property should be shown.")
    public Map<String, List<Object>> getShowWhen() {
        return showWhen;
    }

    public static DisplayOption build(List<DisplayOptionCondition> displayOptionConditions) {
        DisplayOption displayOption = new DisplayOption();

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

    public static class DisplayOptionProperty {
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

    public static class HideDisplayOptionCondition extends DisplayOptionCondition {

        public HideDisplayOptionCondition(String propertyName) {
            super(propertyName);
        }
    }

    public static class ShowDisplayOptionCondition extends DisplayOptionCondition {

        public ShowDisplayOptionCondition(String propertyName) {
            super(propertyName);
        }
    }
}
