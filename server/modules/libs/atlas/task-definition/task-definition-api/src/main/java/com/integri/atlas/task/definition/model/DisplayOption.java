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

package com.integri.atlas.task.definition.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class DisplayOption {

    private Map<String, DisplayOptionEntryValue> hideWhen;
    private Map<String, DisplayOptionEntryValue> showWhen;

    private DisplayOption() {}

    public Map<String, DisplayOptionEntryValue> getHideWhen() {
        return hideWhen;
    }

    public Map<String, DisplayOptionEntryValue> getShowWhen() {
        return showWhen;
    }

    static DisplayOption build(List<DisplayOptionEntry> displayOptionEntries) {
        DisplayOption displayOption = new DisplayOption();

        for (DisplayOptionEntry displayOptionEntry : displayOptionEntries) {
            if (displayOptionEntry instanceof HideDisplayOptionEntry hideDisplayOptionEntry) {
                if (displayOption.hideWhen == null) {
                    displayOption.hideWhen = new HashMap<>();
                }

                displayOption.hideWhen.computeIfAbsent(
                    hideDisplayOptionEntry.propertyName,
                    key -> displayOptionEntry.value
                );
            } else if (displayOptionEntry instanceof ShowDisplayOptionEntry showDisplayOptionEntry) {
                if (displayOption.showWhen == null) {
                    displayOption.showWhen = new HashMap<>();
                }

                displayOption.showWhen.computeIfAbsent(
                    showDisplayOptionEntry.propertyName,
                    key -> displayOptionEntry.value
                );
            }
        }

        return displayOption;
    }

    public abstract static class DisplayOptionEntry {

        protected final String propertyName;
        protected final DisplayOptionEntryValue value = new DisplayOptionEntryValue();

        public DisplayOptionEntry(String propertyName) {
            this.propertyName = propertyName;
        }

        public DisplayOptionEntry in(Boolean... values) {
            this.value.values = List.of((Boolean[]) values);

            return this;
        }

        public DisplayOptionEntry in(Integer... values) {
            this.value.values = List.of((Integer[]) values);

            return this;
        }

        public DisplayOptionEntry in(Long... values) {
            this.value.values = List.of((Long[]) values);

            return this;
        }

        public DisplayOptionEntry in(Float... values) {
            this.value.values = List.of((Float[]) values);

            return this;
        }

        public DisplayOptionEntry in(Double... values) {
            this.value.values = List.of((Double[]) values);

            return this;
        }

        public DisplayOptionEntry in(String... values) {
            this.value.values = List.of((String[]) values);

            return this;
        }

        public DisplayOptionEntry of(TaskProperty.Type type) {
            this.value.type = type;

            return this;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public DisplayOptionEntryValue getValue() {
            return value;
        }
    }

    public static class DisplayOptionEntryValue {

        private TaskProperty.Type type;
        private List<Object> values;

        public TaskProperty.Type getType() {
            return type;
        }

        public List<?> getValues() {
            return values;
        }
    }

    public static class HideDisplayOptionEntry extends DisplayOptionEntry {

        public HideDisplayOptionEntry(String propertyName) {
            super(propertyName);
        }
    }

    public static class ShowDisplayOptionEntry extends DisplayOptionEntry {

        public ShowDisplayOptionEntry(String propertyName) {
            super(propertyName);
        }
    }
}
