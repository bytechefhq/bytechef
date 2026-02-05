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

package com.bytechef.automation.search.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.search.SearchResult;
import com.bytechef.automation.search.facade.AutomationSearchFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
class AutomationSearchGraphQlController {

    private static final int DEFAULT_LIMIT = 100;

    private final AutomationSearchFacade automationSearchFacade;

    @SuppressFBWarnings("EI")
    AutomationSearchGraphQlController(AutomationSearchFacade automationSearchFacade) {
        this.automationSearchFacade = automationSearchFacade;
    }

    @QueryMapping(name = "automationSearch")
    public List<SearchResult<?>> automationSearch(@Argument String query, @Argument Integer limit) {
        int effectiveLimit = limit != null ? limit : DEFAULT_LIMIT;

        return automationSearchFacade.search(query, effectiveLimit);
    }
}
