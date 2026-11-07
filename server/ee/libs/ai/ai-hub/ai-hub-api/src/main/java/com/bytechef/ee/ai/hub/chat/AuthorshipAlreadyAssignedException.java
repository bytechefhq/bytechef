/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
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
