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

package com.bytechef.component.definition.datastream;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface ItemReader extends ItemStream {

    ClusterElementType SOURCE = new ClusterElementType("SOURCE", "source", "Source");

    Map<String, Object> read() throws Exception;
}
