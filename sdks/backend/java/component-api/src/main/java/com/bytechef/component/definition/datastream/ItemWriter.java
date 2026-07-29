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
import java.util.List;
import java.util.Map;

/**
 * Writes items in chunks to a DESTINATION component during a data stream job. Combines the lifecycle callbacks of
 * {@link ItemStream} with the schema discovery of {@link FieldsProvider}.
 *
 * @author Ivica Cardic
 */
public interface ItemWriter extends ItemStream, FieldsProvider {

    /**
     * Cluster element type identifying a writer as the DESTINATION endpoint of a data stream.
     */
    ClusterElementType DESTINATION = new ClusterElementType("DESTINATION", "destination", "Destination");

    /**
     * Writes a chunk of items to the destination. Each item is represented as a map keyed by field name.
     *
     * @param items the items to write, each expressed as a map of field name to value
     * @throws Exception if writing the items fails
     */
    void write(List<? extends Map<String, Object>> items) throws Exception;
}
