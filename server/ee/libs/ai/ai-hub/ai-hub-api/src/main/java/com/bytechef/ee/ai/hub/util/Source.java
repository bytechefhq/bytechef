/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

/**
 * Identifies the agent surface that produced an asset file or other persisted artifact. Persisted as an {@code int}
 * ordinal in {@code asset_file.generated_by_agent_source}.
 *
 * <p>
 * <strong>Append-only contract.</strong> Reordering, renaming, or removing values silently re-attributes every
 * historical row. {@code EnumOrdinalStabilityTest#testSourceOrdinalsAreStable} pins each ordinal — when adding a new
 * value, append it at the end and update the test. To remove or reorder, first migrate the column to a stable string
 * code.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum Source {

    WORKFLOW_EDITOR, CODE_EDITOR, CLUSTER_ELEMENT, FILES, AI_HUB;

    /**
     * Encodes this {@code Source} as the {@code Short} ordinal stored in {@code asset_file.generated_by_agent_source}.
     * Use this instead of {@code (short) source.ordinal()} so call sites are statically typed and cannot accidentally
     * pass an unrelated {@code short} through to persistence.
     */
    public Short toAgentSourceOrdinal() {
        return (short) ordinal();
    }

    /**
     * Decodes a {@code Short} ordinal read from {@code asset_file.generated_by_agent_source} back into a {@code Source}
     * value, returning {@code null} when the input is {@code null}. Throws {@link IllegalStateException} (via
     * {@link EnumOrdinals#fromOrdinal}) when the ordinal is out of range — a corrupt-row signal worth surfacing rather
     * than silently treating as the first enum value.
     */
    public static Source fromAgentSourceOrdinal(Short ordinal) {
        if (ordinal == null) {
            return null;
        }

        return EnumOrdinals.fromOrdinal(ordinal, Source.class);
    }
}
