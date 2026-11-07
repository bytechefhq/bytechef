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
