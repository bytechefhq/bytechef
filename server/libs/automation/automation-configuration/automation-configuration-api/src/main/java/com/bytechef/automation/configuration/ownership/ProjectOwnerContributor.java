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

package com.bytechef.automation.configuration.ownership;

/**
 * Contributes the SQL condition identifying the projects claimed by one owner. The condition is a boolean SQL
 * expression that is true when the row whose id is the given column is owned. It must not reference outer aliases other
 * than that column, and it must be safe to wrap in {@code NOT (...)} and to combine with {@code OR}.
 *
 * @author Ivica Cardic
 */
public interface ProjectOwnerContributor {

    ProjectOwner getProjectOwner();

    String getProjectClaimCondition(String projectIdColumn);
}
