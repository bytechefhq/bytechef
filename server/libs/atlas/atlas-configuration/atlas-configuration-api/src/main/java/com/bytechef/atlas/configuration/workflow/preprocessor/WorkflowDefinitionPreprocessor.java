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

package com.bytechef.atlas.configuration.workflow.preprocessor;

import java.util.Map;

/**
 * Rewrites a workflow definition before it is saved. Implementations mutate the map in place and return whether they
 * changed anything, so an untouched definition is stored exactly as it was given.
 *
 * @author Ivica Cardic
 */
public interface WorkflowDefinitionPreprocessor {

    boolean process(Map<String, Object> workflowDefinition);
}
