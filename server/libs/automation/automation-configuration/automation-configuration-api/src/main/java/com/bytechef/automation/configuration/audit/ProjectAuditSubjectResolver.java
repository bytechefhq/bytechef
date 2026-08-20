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

import java.util.Optional;

/**
 * Names the thing a project-keyed audit record is really about, when that is not the project.
 *
 * <p>
 * Some projects are an implementation detail of something else: an AI agent owns a hidden {@code __AI_AGENT__} project
 * one-to-one and every sharing operation on the agent lands on that project, so the audit log carried a project id an
 * auditor could not resolve to anything they had ever seen in the product. This is the seam that lets the owning
 * feature say what the row is, WITHOUT the audit publisher — or {@code ProjectSharingFacade}, which is what publishes
 * on that path — having to know the feature exists. The alternative shapes were both worse: a second, agent-keyed audit
 * event would be two records for one question, and threading an audit subject through every
 * {@code ProjectSharingFacade} method signature would put the agent in the project facade's vocabulary.
 *
 * <p>
 * Registered as a Spring bean and discovered as a list, exactly like {@code ResourceOwnershipResolver} and
 * {@code ResourceVisibilityProvider}. It lives in {@code -api} rather than beside {@code ProjectAuditPublisher} in
 * {@code -service} so that a feature module can implement it without depending on
 * {@code automation-configuration-service} — the same layering the agent module's visibility and ownership providers
 * already observe.
 *
 * <p>
 * Implementations run inside the publisher's own failure boundary and must be cheap: one read, keyed on a column with
 * an index. Returning empty means "not mine", which is the normal answer for the overwhelming majority of projects.
 *
 * @author Ivica Cardic
 */
public interface ProjectAuditSubjectResolver {

    /**
     * The subject this project stands in for, or empty when the project is a project.
     */
    Optional<AuditSubject> fetchSubject(long projectId);

    /**
     * @param type the resource token of the subject, e.g. {@code "AiAgent"}
     * @param id   the subject's own id, which is the one an auditor can look up
     * @param name a human-readable label, so the record is legible after the subject is deleted
     */
    record AuditSubject(String type, long id, String name) {
    }
}
