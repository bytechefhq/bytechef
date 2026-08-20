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

package com.bytechef.automation.configuration.audit;

import com.bytechef.automation.configuration.audit.ProjectAuditSubjectResolver.AuditSubject;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes audit events for project-lifecycle mutations.
 *
 * <p>
 * Failures must NOT propagate to callers: if the security context cannot be resolved, the principal falls back to
 * {@code "SYSTEM"} rather than failing the surrounding business transaction. The same applies to the subject resolvers
 * below — an audit record that names the project alone is worth more than a failed write.
 *
 * <p>
 * Every registered {@link ProjectAuditSubjectResolver} is consulted so that a project which is really something else —
 * an AI agent's hidden {@code __AI_AGENT__} project is the case that exists — is recorded under a subject an auditor
 * can resolve, rather than under an opaque project id alone. The projectId stays on the record either way: it is still
 * the row that changed, and one event answering one question is the point.
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProjectAuditPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final List<ProjectAuditSubjectResolver> projectAuditSubjectResolvers;

    @SuppressFBWarnings("EI")
    public ProjectAuditPublisher(
        ApplicationEventPublisher applicationEventPublisher,
        List<ProjectAuditSubjectResolver> projectAuditSubjectResolvers) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.projectAuditSubjectResolvers = projectAuditSubjectResolvers;
    }

    public void publish(ProjectAuditEvent eventType, long projectId) {
        publish(eventType, projectId, null);
    }

    public void publish(ProjectAuditEvent eventType, long projectId, Map<String, Object> additionalData) {
        String principal;

        try {
            principal = SecurityUtils.fetchCurrentUserLogin()
                .orElse("SYSTEM");
        } catch (RuntimeException exception) {
            log.warn(
                "Could not resolve principal for audit event {} on project id={}, using SYSTEM",
                eventType, projectId, exception);

            principal = "SYSTEM";
        }

        Map<String, Object> data = new HashMap<>();

        if (additionalData != null) {
            data.putAll(additionalData);
        }

        data.putIfAbsent("projectId", String.valueOf(projectId));

        fetchSubject(eventType, projectId).ifPresent(auditSubject -> {
            data.putIfAbsent("subjectType", auditSubject.type());
            data.putIfAbsent("subjectId", String.valueOf(auditSubject.id()));
            data.putIfAbsent("subjectName", auditSubject.name());
        });

        AuditEvent auditEvent = new AuditEvent(principal, eventType.name(), data);

        applicationEventPublisher.publishEvent(new AuditApplicationEvent(auditEvent));
    }

    /**
     * The first resolver that claims the project wins; a resolver that throws is logged and skipped rather than allowed
     * to fail the surrounding business transaction, which is the same rule the principal lookup above follows. A
     * project is claimed by at most one feature — the relation is one-to-one both ways — so "first" is not an ordering
     * choice so much as a statement that there is nothing to order.
     */
    private Optional<AuditSubject> fetchSubject(ProjectAuditEvent eventType, long projectId) {
        for (ProjectAuditSubjectResolver projectAuditSubjectResolver : projectAuditSubjectResolvers) {
            try {
                Optional<AuditSubject> auditSubject = projectAuditSubjectResolver.fetchSubject(projectId);

                if (auditSubject.isPresent()) {
                    return auditSubject;
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "Could not resolve the audit subject of event {} on project id={}, recording the project alone",
                    eventType, projectId, exception);
            }
        }

        return Optional.empty();
    }
}
