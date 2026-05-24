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

package com.bytechef.platform.githubproxy.client.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WorkflowTemplate(
    String slug, String title, String description, String shortDescription, String category, List<String> components,
    String trigger, String triggerLabel, List<String> keyFeatures, List<String> prerequisites, String idealFor,
    List<String> steps, JsonNode workflowDefinition, WorkflowTemplateAuthor author, String lastModifiedDate) {
}
