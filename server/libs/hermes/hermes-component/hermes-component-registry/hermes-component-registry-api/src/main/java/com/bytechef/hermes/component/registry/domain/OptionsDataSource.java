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

package com.bytechef.hermes.component.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class OptionsDataSource {

    private final List<String> loadOptionsDependsOn;

    public OptionsDataSource(com.bytechef.hermes.component.definition.OptionsDataSource optionsDataSource) {
        this.loadOptionsDependsOn = OptionalUtils.orElse(optionsDataSource.getLoadOptionsDependsOn(), List.of());
    }

    public List<String> getLoadOptionsDependsOn() {
        return loadOptionsDependsOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OptionsDataSource that))
            return false;
        return Objects.equals(loadOptionsDependsOn, that.loadOptionsDependsOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loadOptionsDependsOn);
    }

    @Override
    public String toString() {
        return "OptionsDataSource{" +
            "loadOptionsDependsOn=" + loadOptionsDependsOn +
            '}';
    }
}
