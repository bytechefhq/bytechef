/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppFileStorageImpl implements AppFileStorage {

    private static final String APP_ICONS_DIR = "app_icons";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public AppFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteIcon(FileEntry fileEntry) {
        fileStorageService.deleteFile(APP_ICONS_DIR, fileEntry);
    }

    @Override
    public byte[] readIcon(FileEntry fileEntry) {
        return fileStorageService.readFileToBytes(APP_ICONS_DIR, fileEntry);
    }

    @Override
    public FileEntry storeIcon(String filename, byte[] content) {
        return fileStorageService.storeFileContent(APP_ICONS_DIR, filename, content, true);
    }
}
