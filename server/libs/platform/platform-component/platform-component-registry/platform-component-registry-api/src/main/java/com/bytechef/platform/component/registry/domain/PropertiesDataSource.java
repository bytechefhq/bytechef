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

package com.bytechef.platform.component.registry.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class PropertiesDataSource {

    private final List<String> loadPropertiesDependsOn;

    public PropertiesDataSource(com.bytechef.hermes.component.definition.PropertiesDataSource propertiesDataSource) {
        this.loadPropertiesDependsOn = Validate.notNull(
            propertiesDataSource.getLoadPropertiesDependsOn(), "loadPropertiesDependsOn");
    }

    public List<String> getLoadPropertiesDependsOn() {
        return loadPropertiesDependsOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PropertiesDataSource that))
            return false;
        return Objects.equals(loadPropertiesDependsOn, that.loadPropertiesDependsOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loadPropertiesDependsOn);
    }

    @Override
    public String toString() {
        return "PropertiesDataSource{" +
            "loadPropertiesDependsOn=" + loadPropertiesDependsOn +
            '}';
    }
}
