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
import com.bytechef.platform.ai.provider.dto.WebSearchProviderDTO;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderTestResultDTO;
import com.bytechef.platform.ai.provider.facade.WebSearchProviderFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the web search providers whose API keys admins manage. The facade restricts every operation to
 * admins.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
class WebSearchProviderGraphQlController {

    private final WebSearchProviderFacade webSearchProviderFacade;

    @SuppressFBWarnings("EI")
    WebSearchProviderGraphQlController(WebSearchProviderFacade webSearchProviderFacade) {
        this.webSearchProviderFacade = webSearchProviderFacade;
    }

    @MutationMapping
    public boolean deleteWebSearchProvider(@Argument Long environment, @Argument String name) {
        webSearchProviderFacade.deleteWebSearchProvider(name, environment.intValue());

        return true;
    }

    @MutationMapping
    public boolean enableWebSearchProvider(
        @Argument boolean enabled, @Argument Long environment, @Argument String name) {

        webSearchProviderFacade.enableWebSearchProvider(name, enabled, environment.intValue());

        return true;
    }

    @MutationMapping
    public WebSearchProviderTestResultDTO testWebSearchProvider(
        @Argument @Nullable String apiKey, @Argument @Nullable String baseUrl, @Argument Long environment,
        @Argument String name) {

        return webSearchProviderFacade.testWebSearchProvider(name, apiKey, baseUrl, environment.intValue());
    }

    @MutationMapping
    public boolean updateWebSearchProvider(
        @Argument @Nullable String apiKey, @Argument @Nullable String baseUrl, @Argument Long environment,
        @Argument String name) {

        webSearchProviderFacade.updateWebSearchProvider(name, apiKey, baseUrl, environment.intValue());

        return true;
    }

    @QueryMapping
    public List<WebSearchProviderDTO> webSearchProviders(@Argument Long environment) {
        return webSearchProviderFacade.getWebSearchProviders(environment.intValue());
    }
}
