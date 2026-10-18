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

package com.bytechef.automation.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.web.rest.adapter.AutomationConfigurationConversionServiceAdapter;
import com.bytechef.automation.configuration.web.rest.mapper.ProjectMapper;
import com.bytechef.automation.configuration.web.rest.mapper.ProjectMapper$ProjectDTOToProjectModelMapperImpl;
import com.bytechef.automation.configuration.web.rest.mapper.ProjectMapper$ProjectToProjectBasicModelMapperImpl;
import com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectModel;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * Pins that {@code visibility} reaches the REST surface through Jackson and MapStruct in both directions.
 *
 * @author Ivica Cardic
 */
class ProjectModelVisibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ProjectMapper.ProjectDTOToProjectModelMapper projectDTOToProjectModelMapper =
        new ProjectMapper$ProjectDTOToProjectModelMapperImpl();
    private final ProjectMapper.ProjectToProjectBasicModelMapper projectToProjectBasicModelMapper =
        new ProjectMapper$ProjectToProjectBasicModelMapperImpl();

    @BeforeEach
    void setUp() {
        AutomationConfigurationConversionServiceAdapter conversionServiceAdapter =
            mock(AutomationConfigurationConversionServiceAdapter.class);
        DateTimeMapper dateTimeMapper = new DateTimeMapper();

        ReflectionTestUtils.setField(projectDTOToProjectModelMapper, "dateTimeMapper", dateTimeMapper);
        ReflectionTestUtils.setField(
            projectDTOToProjectModelMapper, "automationConfigurationConversionServiceAdapter",
            conversionServiceAdapter);
        ReflectionTestUtils.setField(projectToProjectBasicModelMapper, "dateTimeMapper", dateTimeMapper);
    }

    @Test
    void testVisibilitySurvivesJsonRoundTrip() {
        ProjectModel projectModel = new ProjectModel()
            .name("p")
            .workspaceId(1L)
            .visibility(ProjectModel.VisibilityEnum.PRIVATE);

        String json = objectMapper.writeValueAsString(projectModel);

        assertThat(json).contains("\"visibility\":\"PRIVATE\"");

        ProjectModel readProjectModel = objectMapper.readValue(json, ProjectModel.class);

        assertThat(readProjectModel.getVisibility()).isEqualTo(ProjectModel.VisibilityEnum.PRIVATE);
    }

    @Test
    void testBasicVisibilitySurvivesJsonRoundTrip() {
        ProjectBasicModel projectBasicModel = new ProjectBasicModel()
            .name("p")
            .visibility(ProjectBasicModel.VisibilityEnum.PRIVATE);

        String json = objectMapper.writeValueAsString(projectBasicModel);

        assertThat(json).contains("\"visibility\":\"PRIVATE\"");

        ProjectBasicModel readProjectBasicModel = objectMapper.readValue(json, ProjectBasicModel.class);

        assertThat(readProjectBasicModel.getVisibility()).isEqualTo(ProjectBasicModel.VisibilityEnum.PRIVATE);
    }

    @Test
    void testMapperCarriesVisibilityBothWays() {
        ProjectDTO projectDTO = ProjectDTO.builder()
            .name("p")
            .workspaceId(1L)
            .visibility(ResourceVisibility.PRIVATE)
            .build();

        ProjectModel projectModel = projectDTOToProjectModelMapper.convert(projectDTO);

        assertThat(projectModel).isNotNull();
        assertThat(projectModel.getVisibility()).isEqualTo(ProjectModel.VisibilityEnum.PRIVATE);

        ProjectDTO invertedProjectDTO = projectDTOToProjectModelMapper.invertConvert(projectModel);

        assertThat(invertedProjectDTO.visibility()).isEqualTo(ResourceVisibility.PRIVATE);
    }

    @Test
    void testBasicMapperCarriesVisibility() {
        Project project = new Project();

        project.setName("p");
        project.setVisibility(ResourceVisibility.PRIVATE);

        ProjectBasicModel projectBasicModel = projectToProjectBasicModelMapper.convert(project);

        assertThat(projectBasicModel).isNotNull();
        assertThat(projectBasicModel.getVisibility()).isEqualTo(ProjectBasicModel.VisibilityEnum.PRIVATE);
    }

    @Test
    void testMapperRejectsOrganizationVisibility() {
        ProjectDTO projectDTO = ProjectDTO.builder()
            .name("p")
            .workspaceId(1L)
            .visibility(ResourceVisibility.ORGANIZATION)
            .build();

        assertThatThrownBy(() -> projectDTOToProjectModelMapper.convert(projectDTO))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
