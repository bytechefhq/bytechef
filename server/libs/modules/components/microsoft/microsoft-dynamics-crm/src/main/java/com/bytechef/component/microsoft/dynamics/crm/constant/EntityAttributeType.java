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

package com.bytechef.component.microsoft.dynamics.crm.constant;

/**
 * @author Monika Kušter
 */
public enum EntityAttributeType {

    PICKLIST("Picklist"),
    VIRTUAL("Virtual"),
    UNIQUE_IDENTIFIER("Uniqueidentifier"),
    STRING("String"),
    MEMO("Memo"),
    MONEY("Money"),
    DOUBLE("Double"),
    INTEGER("Integer"),
    LOOKUP("Lookup"),
    DATETIME("DateTime"),
    BOOLEAN("Boolean"),
    BIGINT("BigInt"),
    DECIMAL("Decimal"),
    OWNER("Owner"),
    ENTITY_NAME("EntityName"),
    STATE("State"),
    STATUS("Status");

    private final String name;

    EntityAttributeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static EntityAttributeType getEntityAttributeType(String name) {
        for (EntityAttributeType value : values()) {
            if (name.equals(value.name)) {
                return value;
            }
        }

        return null;
    }
}
