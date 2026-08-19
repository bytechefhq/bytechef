/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * Lifecycle status of a {@link AiHubChatArtifact}.
 *
 * <ul>
 * <li>{@code APPLIED} — the side-effect was applied.</li>
 * <li>{@code EXPIRED} — the artifact's retention window has elapsed.</li>
 * <li>{@code IRREVERSIBLE} — the kind of side-effect cannot be reversed (e.g. workflow execution).</li>
 * </ul>
 *
 * <p>
 * <strong>Ordinal stability is load-bearing.</strong> The status column is persisted as an INT ordinal (see the
 * Liquibase migration); reordering, renaming, or removing values silently re-attributes historical rows. New values
 * MUST be appended below the {@code // append-only} marker and never inserted between existing entries.
 * {@code EnumOrdinalStabilityTest} pins the current order — it will fail the build before such a change reaches
 * production.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubChatArtifactStatus {

    APPLIED,
    EXPIRED,
    IRREVERSIBLE;
    // append-only — add new values BELOW this line. Reordering breaks ordinal-based persistence.
}
