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

package com.bytechef.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class ProjectRepositoryIntTest {

    @Autowired
    private ProjectRepository projectRepository;

    @AfterEach
    public void afterEach() {
        projectRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Project project = projectRepository.save(getProject());

        assertThat(project).isEqualTo(
            OptionalUtils.get(projectRepository.findById(Validate.notNull(project.getId(), "id"))));
    }

    @Test
    public void testDelete() {
        Project project = projectRepository.save(getProject());

        Project resultProject = OptionalUtils.get(
            projectRepository.findById(Validate.notNull(project.getId(), "id")));

        assertThat(resultProject).isEqualTo(project);

        projectRepository.deleteById(Validate.notNull(resultProject.getId(), "id"));

        assertThat(projectRepository.findById(project.getId()))
            .isEmpty();
    }

    @Test
    public void testFindById() {
        Project project = projectRepository.save(getProject());

        Project resultProject = OptionalUtils.get(projectRepository.findById(Validate.notNull(project.getId(), "id")));

        assertThat(resultProject).isEqualTo(project);
    }

    @Test
    public void testUpdate() {
        Project project = projectRepository.save(getProject());

        project.setName("name2");

        projectRepository.save(project);

        assertThat(projectRepository.findById(Validate.notNull(project.getId(), "id")))
            .hasValue(project);
    }

    private static Project getProject() {
        return Project.builder()
            .description("description")
            .name("name")
            .build();
    }
}
