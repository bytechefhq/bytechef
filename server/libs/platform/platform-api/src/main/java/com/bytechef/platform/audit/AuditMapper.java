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
 * Turns one {@link Audited} invocation into audit record data. One implementation per module, registered as a Spring
 * bean and dispatching on {@link AuditInvocation#event()}.
 *
 * @author Ivica Cardic
 */
public interface AuditMapper {

    /**
     * The {@link Audited#event()} names this mapper handles. Startup fails when an {@code @Audited} method names this
     * mapper with an event outside the set; an empty set, the default, accepts any event.
     */
    default Set<String> events() {
        return Set.of();
    }

    /**
     * Runs after authorization and before the method body. The returned value is handed to {@link #map} as
     * {@link AuditInvocation#captured()}.
     */
    default @Nullable Object capture(AuditInvocation auditInvocation) {
        return null;
    }

    /**
     * Returns the record's data. Every value is chosen explicitly: never a whole argument or result, never a secret or
     * an email address. Keys are the method's parameter names as compiled with {@code -parameters}; renaming a
     * parameter changes which audit data a mapper can see. The keys {@code captureError}, {@code errorClass},
     * {@code mapperError}, {@code method} and {@code result} are reserved for the audit infrastructure and are dropped
     * from the returned map.
     */
    Map<String, String> map(AuditInvocation auditInvocation);

    /**
     * Runs after the call, with the full invocation including {@code result}, {@code captured} and {@code outcome}.
     * Return {@code false} to skip the record entirely, e.g. when the call changed nothing. Denied and errored calls
     * are typically still worth recording even when nothing was captured.
     */
    default boolean shouldRecord(AuditInvocation auditInvocation) {
        return true;
    }
}
