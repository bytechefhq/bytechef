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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.automation.search.SearchResult;
import com.bytechef.automation.search.facade.AutomationSearchFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationSearchGraphQlController.class)
@GraphQlTest(
    controllers = AutomationSearchGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
class AutomationSearchGraphQlControllerIntTest {

    private static final String SEARCH_QUERY = """
        query {
            automationSearch(query: "invoice", limit: 5) {
                id
                name
                description
                type
            }
        }
        """;

    @MockitoBean
    private AutomationSearchFacade automationSearchFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @ParameterizedTest
    @EnumSource(SearchAssetType.class)
    void testAutomationSearchReturnsEveryAssetType(SearchAssetType searchAssetType) {
        String resultName = "Invoice " + searchAssetType.name();

        List<SearchResult<?>> searchResults = List.of(
            new ConnectionSearchResult(1L, resultName, "description", searchAssetType));

        when(automationSearchFacade.search("invoice", 5)).thenReturn(searchResults);

        graphQlTester.document(SEARCH_QUERY)
            .execute()
            .errors()
            .verify()
            .path("automationSearch[0].id")
            .entity(String.class)
            .isEqualTo("1")
            .path("automationSearch[0].name")
            .entity(String.class)
            .isEqualTo(resultName)
            .path("automationSearch[0].type")
            .entity(String.class)
            .isEqualTo(searchAssetType.name());

        verify(automationSearchFacade).search("invoice", 5);
    }

    @Test
    void testAutomationSearchUsesDefaultLimit() {
        when(automationSearchFacade.search("invoice", 100)).thenReturn(List.of());

        graphQlTester.document("""
            query {
                automationSearch(query: "invoice") {
                    id
                }
            }
            """)
            .execute()
            .path("automationSearch")
            .entityList(Object.class)
            .hasSize(0);

        verify(automationSearchFacade).search("invoice", 100);
    }

    private record ConnectionSearchResult(Long id, String name, String description, SearchAssetType type)
        implements SearchResult<Long> {
    }
}
