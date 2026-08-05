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

package com.bytechef.automation.configuration.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional SPI that resolves whether a project is backed by a code workflow and, if so, its language. The
 * implementation lives in the Enterprise Edition; when it is absent (Community Edition) the project is treated as a
 * regular, visually-edited project.
 *
 * @author Ivica Cardic
 */
public interface ProjectCodeWorkflowInfoSupplier {

    Optional<CodeWorkflowInfo> fetchCodeWorkflowInfo(long projectId);

    /**
     * Returns the ids of every project backed by a code workflow — the batch counterpart of
     * {@link #fetchCodeWorkflowInfo(long)} used by list mappings to avoid a per-project lookup.
     */
    List<Long> getCodeWorkflowProjectIds();

    /**
     * Returns the code workflow language of every code-backed project, keyed by project id — the batch counterpart of
     * {@link #fetchCodeWorkflowInfo(long)} used by list mappings so each row can show its language.
     */
    Map<Long, String> getCodeWorkflowLanguages();

    /**
     * Returns the code workflow source backing {@code projectId} — its language and text — so it can be carried into a
     * duplicate or an export. Empty when the project is not code-backed, or when its source is not text (a compiled
     * Java artifact).
     */
    Optional<CodeWorkflowSource> fetchCodeWorkflowSource(long projectId);

    /**
     * Deploys code workflow source into an existing, empty project, generating its workflows. The source's declared
     * project name is rewritten to the target project's own name, since the two must agree for later editor saves — and
     * a duplicate or an import lands under a different name than the source declares.
     *
     * @param projectId the project to deploy into
     * @param language  the source language, as {@link CodeWorkflowSource#language()} reports it
     * @param source    the source text
     */
    void deployCodeWorkflowSource(long projectId, String language, String source);

    record CodeWorkflowInfo(String language) {
    }

    record CodeWorkflowSource(String language, String source) {
    }
}
