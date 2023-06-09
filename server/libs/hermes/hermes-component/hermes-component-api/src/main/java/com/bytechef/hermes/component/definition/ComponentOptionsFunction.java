
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.definition.Option;
import com.bytechef.hermes.definition.OptionsDataSource;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ComponentOptionsFunction extends OptionsDataSource.OptionsFunction {

    /**
     * @param connection
     * @param inputParameters
     * @return
     */
    List<Option<?>> apply(Connection connection, Map<String, ?> inputParameters, String searchText);
}
