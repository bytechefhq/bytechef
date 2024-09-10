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

package com.bytechef.platform.component.registry.datastream;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.DataStreamContext;
import com.bytechef.component.definition.DataStreamItemWriter;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.registry.jdbc.DataSourceFactory;
import com.bytechef.platform.component.registry.jdbc.operation.InsertJdbcOperation;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Ivica Cardic
 */
public class JdbcDataStreamItemWriter implements DataStreamItemWriter {

    private final InsertJdbcOperation insertJdbcOperation;
    private Parameters inputParameters;
    private final String databaseJdbcName;
    private SingleConnectionDataSource dataSource;
    private final String jdbcDriverClassName;

    public JdbcDataStreamItemWriter(String databaseJdbcName, String jdbcDriverClassName) {
        this.databaseJdbcName = databaseJdbcName;
        this.jdbcDriverClassName = jdbcDriverClassName;
        this.insertJdbcOperation = new InsertJdbcOperation();
    }

    @Override
    public void close() {
        if (dataSource != null) {

            try {
                dataSource.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void open(Parameters inputParameters, Parameters connectionParameters, DataStreamContext context) {
        this.inputParameters = inputParameters;
        this.dataSource = DataSourceFactory.getDataSource(connectionParameters, databaseJdbcName, jdbcDriverClassName);
    }

    @Override
    public void write(List<? extends Map<String, ?>> items, DataStreamContext context) throws Exception {
        insertJdbcOperation.execute(MapUtils.concat(inputParameters, Map.of("rows", items)), dataSource);
    }
}
