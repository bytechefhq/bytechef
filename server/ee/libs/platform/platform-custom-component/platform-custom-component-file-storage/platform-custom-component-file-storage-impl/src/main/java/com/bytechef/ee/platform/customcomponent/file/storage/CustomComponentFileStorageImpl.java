/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomComponentFileStorageImpl implements CustomComponentFileStorage {

    private static final String CUSTOM_COMPONENTS_FILES_DIR = "custom_components";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public CustomComponentFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteCustomComponentFile(@NonNull FileEntry componentFile) {
        fileStorageService.deleteFile(CUSTOM_COMPONENTS_FILES_DIR, componentFile);
    }

    @Override
    public URL getCustomComponentFileURL(@NonNull FileEntry componentFile) {
        return fileStorageService.getFileEntryURL(CUSTOM_COMPONENTS_FILES_DIR, componentFile);
    }

    @Override
    public FileEntry storeCustomComponentFile(@NonNull String filename, @NonNull byte[] bytes) {
        return fileStorageService.storeFileContent(CUSTOM_COMPONENTS_FILES_DIR, filename, bytes, false);
    }
}
