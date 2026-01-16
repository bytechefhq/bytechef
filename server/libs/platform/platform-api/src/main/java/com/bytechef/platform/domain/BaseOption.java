/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.domain;

import com.bytechef.commons.util.OptionalUtils;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public abstract class BaseOption {

    protected String description;
    protected String label;
    protected Object value;

    protected BaseOption() {
    }

    protected BaseOption(com.bytechef.definition.BaseOption<?> option) {
        this.description = OptionalUtils.orElse(option.getDescription(), null);
        this.label = option.getLabel();
        this.value = option.getValue();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof BaseOption option)) {
            return false;
        }

        return Objects.equals(description, option.description) && Objects.equals(label, option.label)
            && Objects.equals(value, option.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, label, value);
    }

    @Override
    public String toString() {
        return "OptionDTO{" +
            "description='" + description + '\'' +
            ", label='" + label + '\'' +
            ", value=" + value +
            '}';
    }
}
