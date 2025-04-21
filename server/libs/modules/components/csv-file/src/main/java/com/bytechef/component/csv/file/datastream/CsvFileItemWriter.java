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

package com.bytechef.component.csv.file.datastream;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.WRITE_PROPERTIES;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.datastream.ItemWriter;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class CsvFileItemWriter implements ItemWriter {

    public static final ClusterElementDefinition<CsvFileItemWriter> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<CsvFileItemWriter>clusterElement("writer")
            .title("Write CSV file rows")
            .type(DESTINATION)
            .object(CsvFileItemWriter.class)
            .properties(WRITE_PROPERTIES);

    @Override
    public void write(List<? extends Map<String, ?>> items) throws Exception {
        // TODO write items as stream

        System.out.println(items);
    }
}
