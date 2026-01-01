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

package com.bytechef.platform.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.component.domain.Help;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Exposes Help over GraphQL.
 *
 * @author ByteChef
 */
@Controller
@ConditionalOnCoordinator
public class HelpGraphQlController {

    @SchemaMapping(typeName = "Help", field = "description")
    public String helpDescription(Help help) {
        return help.getBody();
    }

    @SchemaMapping(typeName = "Help", field = "documentationUrl")
    public String helpDocumentationUrl(Help help) {
        return help.getLearnMoreUrl();
    }
}
