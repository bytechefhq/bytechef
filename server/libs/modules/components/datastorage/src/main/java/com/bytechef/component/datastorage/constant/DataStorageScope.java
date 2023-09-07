
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

package com.bytechef.component.datastorage.constant;

/**
 * @author Ivica Cardic
 */
public enum DataStorageScope {
    ACCOUNT(4, "Account"),
    CURRENT_EXECUTION(1, "Current Execution"),
    WORKFLOW(2, "Workflow"),
    INSTANCE(3, "Instance");

    private final int id;
    private final String label;

    DataStorageScope(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public static DataStorageScope valueOf(int id) {
        return switch (id) {
            case 1 -> DataStorageScope.CURRENT_EXECUTION;
            case 2 -> DataStorageScope.WORKFLOW;
            case 3 -> DataStorageScope.INSTANCE;
            case 4 -> DataStorageScope.ACCOUNT;
            default -> throw new IllegalStateException("Unexpected value: %s".formatted(id));
        };
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
