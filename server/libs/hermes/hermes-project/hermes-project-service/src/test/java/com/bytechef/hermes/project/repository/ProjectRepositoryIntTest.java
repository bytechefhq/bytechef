
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

package com.bytechef.hermes.project.repository;

import com.bytechef.hermes.project.config.ProjectIntTestConfiguration;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
public class ProjectRepositoryIntTest {

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    public void beforeEach() {
        projectRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testCreate() {
        Project project = projectRepository.save(getProject(Collections.emptyList()));

        assertThat(project).isEqualTo(projectRepository.findById(project.getId())
            .get());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Project project = projectRepository.save(getProject(Collections.emptyList()));

        Project resultProject = projectRepository.findById(project.getId())
            .orElseThrow();

        assertThat(resultProject).isEqualTo(project);

        projectRepository.deleteById(resultProject.getId());

        assertThat(projectRepository.findById(project.getId())).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testFindById() {
        Project project = projectRepository.save(getProject(Collections.emptyList()));

        Project resultProject = projectRepository.findById(project.getId())
            .orElseThrow();

        assertThat(resultProject).isEqualTo(project);

        projectRepository.deleteById(project.getId());

        project = getProject(List.of("workflowId"));

        project = projectRepository.save(project);

        resultProject = projectRepository.findById(project.getId())
            .orElseThrow();

        assertThat(resultProject.getWorkflowIds()).isEqualTo(project.getWorkflowIds());

        resultProject.removeWorkflow("workflowId");

        projectRepository.save(resultProject);

        resultProject = projectRepository.findById(project.getId())
            .orElseThrow();

        assertThat(resultProject.getWorkflowIds()).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testUpdate() {
        Project project = projectRepository.save(getProject(List.of("workflow1")));

        project.addWorkflow("workflow2");
        project.setName("name2");

        projectRepository.save(project);

        assertThat(projectRepository.findById(project.getId())).hasValue(project);
    }

    private static Project getProject(List<String> workflowIds) {
        Project project = new Project();

        project.setWorkflowIds(workflowIds);

        project.setDescription("name");
        project.setName("name");

        return project;
    }
}
