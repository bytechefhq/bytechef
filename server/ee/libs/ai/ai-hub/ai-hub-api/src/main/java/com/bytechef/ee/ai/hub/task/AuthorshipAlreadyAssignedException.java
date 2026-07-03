/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

/**
 * Thrown by {@link AiHubTaskAssetFileService#recordAuthorship(long, long)} when the asset_file already has a different
 * authoring task. The caller — typically a generator that produced a new asset_file — uses this signal to fall back to
 * {@code recordRelationship(..., ATTACHED)} when the source file already has an author.
 *
 * <p>
 * Distinct from {@link IllegalStateException} because the violation is a normal-but-rare runtime condition (race with a
 * concurrent generator, or an attempt to re-attribute authorship), not a server-side invariant violation.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AuthorshipAlreadyAssignedException extends RuntimeException {

    private final long assetFileId;
    private final long currentAuthorTaskId;

    public AuthorshipAlreadyAssignedException(long assetFileId, long currentAuthorTaskId) {
        super("asset_file " + assetFileId + " is already AUTHORED by task " + currentAuthorTaskId);

        this.assetFileId = assetFileId;
        this.currentAuthorTaskId = currentAuthorTaskId;
    }

    public long getAssetFileId() {
        return assetFileId;
    }

    public long getCurrentAuthorTaskId() {
        return currentAuthorTaskId;
    }
}
