/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowFileStorageImpl implements CodeWorkflowFileStorage {

    private static final String CODE_WORKFLOWS_DIR = "code_workflows";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public CodeWorkflowFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteCodeWorkflowFile(FileEntry fileEntry) {
        fileStorageService.deleteFile(CODE_WORKFLOWS_DIR, fileEntry);
    }

    @Override
    public URL getCodeWorkflowFileURL(FileEntry fileEntry) {
        return fileStorageService.getFileEntryURL(CODE_WORKFLOWS_DIR, fileEntry);
    }

    @Override
    public FileEntry storeCodeWorkflowFile(String filename, byte[] bytes) {
        return fileStorageService.storeFileContent(CODE_WORKFLOWS_DIR, filename, bytes);
    }
}
