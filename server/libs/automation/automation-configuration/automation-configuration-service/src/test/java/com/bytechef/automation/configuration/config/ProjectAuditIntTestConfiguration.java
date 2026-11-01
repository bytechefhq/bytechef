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

package com.bytechef.automation.configuration.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Ivica Cardic
 */
@EnableMethodSecurity
public class ProjectAuditIntTestConfiguration {

    @Bean
    AuditAspect auditAspect(
        ApplicationProperties applicationProperties, AuditEventService auditEventService,
        AuditMapperResolver auditMapperResolver) {

        return new AuditAspect(applicationProperties, auditEventService, auditMapperResolver);
    }

    @Bean
    AuditCaptureAspect auditCaptureAspect(
        AuditMapperResolver auditMapperResolver,
        ObjectProvider<PlatformTransactionManager> transactionManagerProvider) {

        return new AuditCaptureAspect(auditMapperResolver, transactionManagerProvider);
    }

    @Bean
    AuditEventService auditEventService(PersistenceAuditEventRepository persistenceAuditEventRepository) {
        return new AuditEventService(persistenceAuditEventRepository);
    }

    @Bean
    AuditMapperResolver auditMapperResolver(ListableBeanFactory listableBeanFactory) {
        return new AuditMapperResolver(listableBeanFactory);
    }
}
