/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.component.definition.FileEntry;
import org.jspecify.annotations.Nullable;

/**
 * Workflow-chat bridge's local implementation of the SDK's {@link FileEntry} interface. Used by
 * {@link WebhookBridgeAgent} to wrap attachments stored via {@code TempFileStorage} so webhook triggers (notably
 * {@code ChatNewRequestTrigger}, which keys behaviour off {@code list.getFirst() instanceof FileEntry}) see the shape
 * they expect on the in-process bridge path.
 *
 * <p>
 * The platform's adapter type {@code FileEntryImpl} (in {@code platform-component-context-service}) does the same thing
 * for the HTTP-multipart path. We don't reuse that class because pulling {@code platform-component-context-service}
 * into the ai-hub service module would import the entire task-execution context — far more surface than the bridge
 * needs. This 4-field record is the minimum the trigger contract requires.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record BridgedFileEntry(String name, @Nullable String extension, @Nullable String mimeType, String url)
    implements FileEntry {

    @Override
    public String getName() {
        return name;
    }

    @Override
    public @Nullable String getExtension() {
        return extension;
    }

    @Override
    public @Nullable String getMimeType() {
        return mimeType;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
