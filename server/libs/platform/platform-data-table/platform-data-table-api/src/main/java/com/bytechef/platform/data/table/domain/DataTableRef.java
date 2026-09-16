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

package com.bytechef.platform.data.table.domain;

import com.bytechef.platform.data.table.internal.PhysicalTableNaming;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.util.Assert;

/**
 * One physical data table, fully addressed: the environment it lives in and the base name it is known by.
 *
 * <p>
 * This is the only way to name a physical table. Every DDL and DML statement takes a ref rather than a base name, so no
 * call site can hand over a base name and leave the callee to work out the rest of the address.
 *
 * <p>
 * The base name is validated here rather than at each statement, so a ref is well formed by construction and the
 * identifier allowlist that keeps the generated SQL injection-free holds for every physical name built from one.
 *
 * @author Ivica Cardic
 */
public record DataTableRef(String baseName, long environmentId) {

    /**
     * Postgres truncates an identifier past {@code NAMEDATALEN - 1} bytes rather than refusing it, and two physical
     * names that truncate to the same 63 bytes are the same table. Checked here, where every name is built, because a
     * long enough base name is all it takes.
     */
    private static final int MAX_IDENTIFIER_BYTES = 63;

    public DataTableRef {
        Assert.hasText(baseName, "baseName must not be empty");

        baseName = baseName.toLowerCase(Locale.ROOT);

        Assert.isTrue(!baseName.startsWith("dt_"), "baseName must not start with 'dt_'");
        Assert.isTrue(baseName.matches("[a-z_][a-z0-9_]*"), "Invalid base name: " + baseName);

        String physicalName = PhysicalTableNaming.buildPhysicalName(environmentId, baseName);
        byte[] bytes = physicalName.getBytes(StandardCharsets.UTF_8);

        Assert.isTrue(
            bytes.length <= MAX_IDENTIFIER_BYTES,
            "Physical table name '" + physicalName + "' exceeds " + MAX_IDENTIFIER_BYTES + " bytes");
    }

    /**
     * The Postgres table this ref addresses: {@code dt_<envId>_<baseName>}.
     */
    public String physicalName() {
        return PhysicalTableNaming.buildPhysicalName(environmentId, baseName);
    }
}
