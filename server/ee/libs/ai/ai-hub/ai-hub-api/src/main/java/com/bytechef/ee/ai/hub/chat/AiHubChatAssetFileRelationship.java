/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * Classifies how an {@code asset_file} is linked to a {@code ai_hub_chat}.
 *
 * <p>
 * The values:
 * <ul>
 * <li>{@link #AUTHORED} — the chat produced the file (agent generation). Subject to a partial unique index enforcing at
 * most one AUTHORED row per asset_file across all chats: a file has zero or one author.</li>
 * <li>{@link #ATTACHED} — the user attached the file to the chat via @-mention, drag-drop, or paste.</li>
 * <li>{@link #MENTIONED} — the file was referenced by name (e.g. "open report.md") without being explicitly attached.
 * Distinct from ATTACHED so the UI can render the affordance differently and so deletions of the chat don't cascade to
 * mention-only references the user might want to keep.</li>
 * <li>{@link #READ_BY_TOOL} — an agent tool call read the file's content. Useful for cost attribution and the "files
 * this chat has touched" listing.</li>
 * </ul>
 *
 * <p>
 * <b>Append-only.</b> The values are persisted as INT ordinals via Spring Data JDBC and
 * {@link com.bytechef.ee.ai.hub.util.EnumOrdinals} — reordering or deleting a value would silently re-map every
 * historical row to the wrong relationship. New values MUST be appended at the end.
 * {@code ChatAssetFileRelationshipOrdinalStabilityTest} pins each ordinal at build time. The partial unique index in
 * the {@code uk_asset_file_authored_once} migration encodes {@code WHERE relationship = 0} for AUTHORED — that ordinal
 * is permanent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubChatAssetFileRelationship {

    // append-only — see class Javadoc and the partial unique index pinned to relationship=0 (AUTHORED)
    AUTHORED,
    ATTACHED,
    MENTIONED,
    READ_BY_TOOL;
}
