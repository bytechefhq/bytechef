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

    private final List<String> propertiesLookupDependsOn;

    public PropertiesDataSource(com.bytechef.component.definition.PropertiesDataSource<?> propertiesDataSource) {
        this.propertiesLookupDependsOn = Validate.notNull(
            propertiesDataSource.getPropertiesLookupDependsOn(), "propertiesLookupDependsOn");
    }

    public List<String> getPropertiesLookupDependsOn() {
        return propertiesLookupDependsOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PropertiesDataSource that))
            return false;
        return Objects.equals(propertiesLookupDependsOn, that.propertiesLookupDependsOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertiesLookupDependsOn);
    }

    @Override
    public String toString() {
        return "PropertiesDataSource{" +
            "propertiesLookupDependsOn=" + propertiesLookupDependsOn +
            '}';
    }
}
