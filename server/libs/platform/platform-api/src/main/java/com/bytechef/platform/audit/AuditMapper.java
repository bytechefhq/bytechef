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

package com.bytechef.platform.audit;

import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AuditMapper {

    default Set<String> events() {
        return Set.of();
    }

    default @Nullable Object capture(AuditInvocation auditInvocation) {
        return null;
    }

    Map<String, String> map(AuditInvocation auditInvocation);

    default boolean shouldRecord(AuditInvocation auditInvocation) {
        return true;
    }
}
