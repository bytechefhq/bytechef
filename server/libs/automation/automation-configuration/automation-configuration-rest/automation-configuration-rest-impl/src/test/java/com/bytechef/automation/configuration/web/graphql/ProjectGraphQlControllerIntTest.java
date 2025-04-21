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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.automation.configuration.web.graphql.config.ProjectConfigurationGraphQlTestConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@Disabled
@ContextConfiguration(classes = ProjectConfigurationGraphQlTestConfiguration.class)
@GraphQlTest(ProjectGraphQlController.class)
public class ProjectGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testGetProjectById() {
        this.graphQlTester
            .documentName("projectById")
            .variable("id", 1051)
            .execute()
            .path("project")
            .matchesJson("""
                {
                    "id": 1051,
                    "name": "Effective Java"
                }
                """);
    }
}
