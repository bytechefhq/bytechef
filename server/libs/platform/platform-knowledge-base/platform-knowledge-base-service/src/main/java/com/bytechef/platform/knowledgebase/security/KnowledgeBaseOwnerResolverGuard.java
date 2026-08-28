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

package com.bytechef.platform.knowledgebase.security;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.owner.OwnerResolver;
import com.bytechef.platform.owner.OwnerResolverGuard;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * The knowledge base twin of {@code DataTableOwnerResolverGuard}; see that class for why the check lives in the service
 * module and runs on {@link ApplicationReadyEvent}.
 *
 * <p>
 * Additionally gated on the same property as {@code KnowledgeBaseServiceImpl}: with knowledge bases switched off there
 * is no owner-scoped surface here to protect, and failing a boot over an unreachable one would be noise.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class KnowledgeBaseOwnerResolverGuard {

    private final ObjectProvider<OwnerResolver> ownerResolverProvider;

    public KnowledgeBaseOwnerResolverGuard(ObjectProvider<OwnerResolver> ownerResolverProvider) {
        this.ownerResolverProvider = ownerResolverProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkOwnerResolverIsPresent() {
        OwnerResolverGuard.check(ownerResolverProvider, "knowledge bases");
    }
}
