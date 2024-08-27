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

package com.bytechef.platform.data.storage.domain;

/**
 * @author Ivica Cardic
 */
public enum DataStorageScope {

    CURRENT_EXECUTION("Current Execution"),
    WORKFLOW("Workflow"),
    INSTANCE("Instance"),
    ACCOUNT("Account");

    private final String label;

    DataStorageScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
