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

package com.bytechef.atlas.configuration.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTaskReferenceUtilsTest {

    @Test
    void testExtractReferencedOutputPathsWithSingleReference() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "activeCampaign_1", WorkflowConstants.TYPE,
                    "activeCampaign/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of("phone", "${accelo_1.response.lastname}"))));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).containsExactly("response.lastname");
    }

    @Test
    void testExtractReferencedOutputPathsWithMultipleReferences() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "activeCampaign_1", WorkflowConstants.TYPE,
                    "activeCampaign/v1/createContact",
                    WorkflowConstants.PARAMETERS,
                    Map.of("phone", "${accelo_1.response.lastname}", "email", "${accelo_1.meta.more_info}"))));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).containsExactlyInAnyOrder("response.lastname", "meta.more_info");
    }

    @Test
    void testExtractReferencedOutputPathsAcrossMultipleDownstreamTasks() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "activeCampaign_1", WorkflowConstants.TYPE,
                    "activeCampaign/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of("phone", "${accelo_1.response.lastname}"))),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "ahrefs_1", WorkflowConstants.TYPE, "ahrefs/v1/getMetrics",
                    WorkflowConstants.PARAMETERS, Map.of("date", "${accelo_1.response.firstname}"))));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).containsExactlyInAnyOrder("response.lastname", "response.firstname");
    }

    @Test
    void testExtractReferencedOutputPathsWithNoReferences() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "activeCampaign_1", WorkflowConstants.TYPE,
                    "activeCampaign/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of("phone", "staticValue"))));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).isEmpty();
    }

    @Test
    void testExtractReferencedOutputPathsIgnoresUpstreamTasks() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "upstream_1", WorkflowConstants.TYPE, "upstream/v1/action",
                    WorkflowConstants.PARAMETERS, Map.of("ref", "${accelo_1.response.ignored}"))),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "downstream_1", WorkflowConstants.TYPE, "downstream/v1/action",
                    WorkflowConstants.PARAMETERS, Map.of("value", "${accelo_1.data.id}"))));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).containsExactly("data.id");
    }

    @Test
    void testExtractReferencedOutputPathsWithNestedMapParameters() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "activeCampaign_1", WorkflowConstants.TYPE,
                    "activeCampaign/v1/createContact",
                    WorkflowConstants.PARAMETERS,
                    Map.of("contact", Map.of("name", "${accelo_1.response.fullname}")))));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).containsExactly("response.fullname");
    }

    @Test
    void testExtractReferencedOutputPathsWithListParameters() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "activeCampaign_1", WorkflowConstants.TYPE,
                    "activeCampaign/v1/createContact",
                    WorkflowConstants.PARAMETERS,
                    Map.of("tags", List.of("${accelo_1.tags.primary}", "${accelo_1.tags.secondary}")))));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).containsExactlyInAnyOrder("tags.primary", "tags.secondary");
    }

    @Test
    void testExtractReferencedOutputPathsWithLastTask() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).isEmpty();
    }

    @Test
    void testExtractReferencedOutputPathsIgnoresOtherTaskReferences() {
        List<WorkflowTask> tasks = List.of(
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "accelo_1", WorkflowConstants.TYPE, "accelo/v1/createContact",
                    WorkflowConstants.PARAMETERS, Map.of())),
            new WorkflowTask(
                Map.of(WorkflowConstants.NAME, "activeCampaign_1", WorkflowConstants.TYPE,
                    "activeCampaign/v1/createContact",
                    WorkflowConstants.PARAMETERS,
                    Map.of("email", "${activeCampaign_1.contact.email}", "phone",
                        "${accelo_1.response.phone}"))));

        Set<String> paths = WorkflowTaskReferenceUtils.extractReferencedOutputPaths(tasks, "accelo_1");

        assertThat(paths).containsExactly("response.phone");
    }
}
