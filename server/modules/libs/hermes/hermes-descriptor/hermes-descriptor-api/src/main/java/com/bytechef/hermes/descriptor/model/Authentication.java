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

package com.bytechef.hermes.descriptor.model;

import java.util.List;

/**
 * Used for specifying an auth type.
 *
 * @author Ivica Cardic
 */
public class Authentication {

    private List<TaskPropertyOption> options;

    Authentication() {}

    public Authentication options(TaskPropertyOption... options) {
        this.options = List.of(options);

        return this;
    }

    public List<TaskPropertyOption> getOptions() {
        return options;
    }
}
