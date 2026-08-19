/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * Thrown by {@link AiHubChatAssetFileService#recordAuthorship(long, long)} when the asset_file already has a different
 * authoring chat. The caller — typically a generator that produced a new asset_file — uses this signal to fall back to
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
    private final long currentAuthorChatId;

    public AuthorshipAlreadyAssignedException(long assetFileId, long currentAuthorChatId) {
        super("asset_file " + assetFileId + " is already AUTHORED by chat " + currentAuthorChatId);

        this.assetFileId = assetFileId;
        this.currentAuthorChatId = currentAuthorChatId;
    }

    public long getAssetFileId() {
        return assetFileId;
    }

    public long getCurrentAuthorChatId() {
        return currentAuthorChatId;
    }
}
