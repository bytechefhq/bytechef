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

package com.bytechef.component.snowflake.constant;

/**
 * @author Nikolina Spehar
 */
public enum SnowflakeDataType {

    NUMBER("NUMBER"),
    DECIMAL("DECIMAL"),
    NUMERIC("NUMERIC"),
    INT("INT"),
    INTEGER("INTEGER"),
    BIGINT("BIGINT"),
    SMALLINT("SMALLINT"),
    TINYINT("TINYINT"),
    BYTEINT("BYTEINT"),
    FLOAT("FLOAT"),
    FLOAT4("FLOAT4"),
    FLOAT8("FLOAT8"),
    DOUBLE("DOUBLE"),
    DOUBLE_PRECISION("DOUBLE PRECISION"),
    REAL("REAL"),
    VARCHAR("VARCHAR"),
    CHAR("CHAR"),
    CHARACTER("CHARACTER"),
    STRING("STRING"),
    TEXT("TEXT"),
    NVARCHAR("NVARCHAR"),
    NVARCHAR2("NVARCHAR2"),
    NCHAR("NCHAR"),
    BINARY("BINARY"),
    VARBINARY("VARBINARY"),
    BOOLEAN("BOOLEAN"),
    DATE("DATE"),
    TIME("TIME"),
    TIMESTAMP("TIMESTAMP"),
    TIMESTAMP_NTZ("TIMESTAMP_NTZ"),
    TIMESTAMP_LTZ("TIMESTAMP_LTZ"),
    TIMESTAMP_TZ("TIMESTAMP_TZ"),
    DATETIME("DATETIME"),
    VARIANT("VARIANT"),
    OBJECT("OBJECT"),
    ARRAY("ARRAY"),
    GEOGRAPHY("GEOGRAPHY"),
    GEOMETRY("GEOMETRY");

    private final String name;

    SnowflakeDataType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static SnowflakeDataType getSnowflakeDataType(String text) {
        for (SnowflakeDataType type : values()) {
            if (type.name.equalsIgnoreCase(text)) {
                return type;
            }
        }

        throw new IllegalArgumentException("No SnowflakeDataType constant with text " + text + " found");
    }
}
