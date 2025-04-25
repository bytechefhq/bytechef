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

package com.bytechef.component.snowflake.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATATYPE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.snowflake.constant.SnowflakeDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SnowflakePropertiesUtils {

    public static List<ValueProperty<?>> createPropertiesForColumn(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext context) {

        List<Map<String, String>> columns = SnowflakeUtils.getTableColumns(inputParameters, context);

        return new ArrayList<>(columns.stream()
            .map(SnowflakePropertiesUtils::createProperty)
            .toList());
    }

    private static ModifiableValueProperty<?, ?> createProperty(Map<String, String> column) {
        String name = column.get(NAME);
        String datatype = column.get(DATATYPE);

        SnowflakeDataType snowflakeDataType = SnowflakeDataType.getSnowflakeDataType(datatype);

        return switch (snowflakeDataType) {
            case NUMBER, DECIMAL, NUMERIC, FLOAT, FLOAT4, DOUBLE, FLOAT8, DOUBLE_PRECISION, REAL,
                BINARY, VARBINARY -> number(name)
                    .label(name)
                    .required(false);
            case INT, INTEGER, BIGINT, SMALLINT, TINYINT, BYTEINT -> integer(name)
                .label(name)
                .required(false);
            case VARCHAR, STRING, TEXT, CHAR, CHARACTER, NVARCHAR, NVARCHAR2, NCHAR -> string(name)
                .label(name)
                .required(false);
            case BOOLEAN -> bool(name)
                .label(name)
                .required(false);
            case DATE, TIME, TIMESTAMP, TIMESTAMP_TZ, TIMESTAMP_LTZ, TIMESTAMP_NTZ, DATETIME -> date(name)
                .label(name)
                .required(false);
            case VARIANT, OBJECT, GEOGRAPHY, GEOMETRY -> object(name)
                .label(name)
                .required(false);
            case ARRAY -> array(name)
                .label(name)
                .required(false);
        };
    }
}
