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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.Field;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.configuration.web.graphql.config.PlatformConfigurationGraphQlTestConfiguration;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    PlatformConfigurationGraphQlTestConfiguration.class,
    ClusterElementFieldGraphQlController.class
})
@GraphQlTest(
    controllers = ClusterElementFieldGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
public class ClusterElementFieldGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Test
    void testClusterElementFieldsReturnsMappedFields() {
        when(clusterElementDefinitionFacade.executeFields(
            eq("airtable"), anyInt(), eq("airtableSource"), anyMap(), eq(77L)))
                .thenReturn(List.of(
                    new Field("name", "Name", "String"),
                    new Field("score", "Score", "Integer")));

        this.graphQlTester
            .document("""
                query {
                    clusterElementFields(
                        componentName: "airtable"
                        componentVersion: 1
                        clusterElementName: "airtableSource"
                        connectionId: 77
                        inputParameters: {baseId: "appXYZ"}
                    ) {
                        name
                        label
                        type
                    }
                }
                """)
            .execute()
            .path("clusterElementFields")
            .entityList(Field.class)
            .satisfies(fields -> Assertions.assertThat(fields)
                .extracting(Field::name)
                .containsExactly("name", "score"));
    }

    @Test
    void testClusterElementFieldsReturnsEmptyListWhenServiceReturnsEmpty() {
        when(clusterElementDefinitionFacade.executeFields(
            anyString(), anyInt(), anyString(), anyMap(), eq(null)))
                .thenReturn(List.of());

        this.graphQlTester
            .document("""
                query {
                    clusterElementFields(
                        componentName: "csvFile"
                        componentVersion: 1
                        clusterElementName: "csvSource"
                    ) {
                        name
                    }
                }
                """)
            .execute()
            .path("clusterElementFields")
            .entityList(Field.class)
            .satisfies(fields -> Assertions.assertThat(fields)
                .isEmpty());
    }
}
