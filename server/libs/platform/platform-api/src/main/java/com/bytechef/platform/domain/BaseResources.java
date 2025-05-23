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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public abstract class BaseResources {

    protected Map<String, String> additionalUrls;
    protected String documentationUrl;

    protected BaseResources() {
    }

    protected BaseResources(com.bytechef.definition.BaseResources resources) {
        this.additionalUrls = OptionalUtils.orElse(resources.getAdditionalUrls(), Map.of());
        this.documentationUrl = Validate.notNull(resources.documentationUrl(), "documentationUrl");
    }

    public Map<String, String> getAdditionalUrls() {
        return additionalUrls;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseResources that)) {
            return false;
        }

        return Objects.equals(additionalUrls, that.additionalUrls) &&
            Objects.equals(documentationUrl, that.documentationUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(additionalUrls, documentationUrl);
    }

    @Override
    public String toString() {
        return "BaseResources{" +
            "additionalUrls=" + additionalUrls +
            ", documentationUrl='" + documentationUrl + '\'' +
            '}';
    }
}
