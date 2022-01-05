/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.engine.core.task.description;

import java.util.List;

public class TaskAuthentication {

    private List<TaskCredential> credentials;
    private List<TaskProperty> properties;

    public static TaskAuthentication authentication() {
        return new TaskAuthentication();
    }

    public static TaskCredential credential(String name) {
        return TaskCredential.credential().name(name);
    }

    public TaskAuthentication credentials(TaskCredential... credential) {
        this.credentials = List.of(credential);

        return this;
    }

    public TaskAuthentication properties(TaskProperty... properties) {
        this.properties = List.of(properties);

        return this;
    }

    public List<TaskCredential> getCredentials() {
        return credentials;
    }

    public List<TaskProperty> getProperties() {
        return properties;
    }
}
