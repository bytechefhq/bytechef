
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

package com.bytechef.hermes.definition.registry.domain;

import com.bytechef.commons.util.OptionalUtils;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class Help {

    private final String body;
    private final String learnMoreUrl;

    public Help(com.bytechef.hermes.component.definition.Help help) {
        this.body = help.getBody();
        this.learnMoreUrl = OptionalUtils.orElse(help.getLearnMoreUrl(), null);
    }

    public String getBody() {
        return body;
    }

    public String getLearnMoreUrl() {
        return learnMoreUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Help help))
            return false;
        return Objects.equals(body, help.body) && Objects.equals(learnMoreUrl, help.learnMoreUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, learnMoreUrl);
    }

    @Override
    public String toString() {
        return "Help{" +
            "body='" + body + '\'' +
            ", learnMoreUrl='" + learnMoreUrl + '\'' +
            '}';
    }
}
