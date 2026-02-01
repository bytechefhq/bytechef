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

package com.bytechef.ai.mcp.tool.automation.api;

import java.util.List;

/**
 * Interface for project tools operations including create, update, delete and publish.
 *
 * @author Marko Kriskovic
 */
public interface ProjectTools extends ReadProjectTools {

    ProjectInfo createProject(
        String name, String description, Long categoryId, Long workspaceId, List<Long> tagIds);

    String deleteProject(long projectId);

    ProjectInfo updateProject(
        long projectId, String name, String description, Long categoryId, List<Long> tagIds);

    ProjectPublishInfo publishProject(long projectId, String description);
}
