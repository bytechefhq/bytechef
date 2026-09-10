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
import com.bytechef.platform.ai.provider.dto.AiDefaultModelDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.ai.provider.facade.AiProviderCatalogFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for reading the AI provider catalog.
 *
 * <p>
 * Authorization is enforced on {@link AiProviderCatalogFacade}, not here.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
class AiProviderCatalogGraphQlController {

    private final AiProviderCatalogFacade aiProviderCatalogFacade;

    @SuppressFBWarnings("EI")
    AiProviderCatalogGraphQlController(AiProviderCatalogFacade aiProviderCatalogFacade) {
        this.aiProviderCatalogFacade = aiProviderCatalogFacade;
    }

    @QueryMapping
    public List<AiProviderCatalogItemDTO> aiProviderCatalog(@Argument Long environment) {
        return aiProviderCatalogFacade.getAiProviderCatalog(environment.intValue());
    }

    @QueryMapping
    public AiDefaultModelDTO aiDefaultModel(@Argument Long environment) {
        return aiProviderCatalogFacade.getAiDefaultModel(environment.intValue());
    }
}
