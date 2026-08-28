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

package com.bytechef.platform.data.table.configuration.security;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.owner.OwnerResolver;
import com.bytechef.platform.owner.OwnerResolverGuard;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The presence of this module IS the condition: an app carrying {@code platform-data-table-service} can execute
 * owner-scoped row operations for real, where one carrying only the remote client cannot. So the guard needs no
 * classpath probe -- it exists exactly where the risk does, and {@link ConditionalOnEEVersion} removes it from
 * Community, which has no connected users and correctly has no resolver.
 *
 * <p>
 * Checked on {@link ApplicationReadyEvent} rather than during construction: the resolver is an EE bean with its own
 * dependency graph, and probing it mid-refresh would trade a clear failure for an initialization-order one.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class DataTableOwnerResolverGuard {

    private final ObjectProvider<OwnerResolver> ownerResolverProvider;

    public DataTableOwnerResolverGuard(ObjectProvider<OwnerResolver> ownerResolverProvider) {
        this.ownerResolverProvider = ownerResolverProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkOwnerResolverIsPresent() {
        OwnerResolverGuard.check(ownerResolverProvider, "data tables");
    }
}
