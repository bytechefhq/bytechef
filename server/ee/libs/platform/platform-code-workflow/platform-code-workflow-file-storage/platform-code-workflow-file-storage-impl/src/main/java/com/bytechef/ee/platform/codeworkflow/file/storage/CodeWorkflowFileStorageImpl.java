/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.file.storage;

import com.bytechef.commons.util.CompressionUtils;
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

    private static final String CODE_WORKFLOWS_DEFINITIONS_DIR = "code_workflows/definitions";
    private static final String CODE_WORKFLOWS_FILES_DIR = "code_workflows/files";

    private final FileStorageService fileStorageService;

    @SuppressFBWarnings("EI")
    public CodeWorkflowFileStorageImpl(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void deleteCodeWorkflowDefinition(FileEntry definitionFile) {
        fileStorageService.deleteFile(CODE_WORKFLOWS_DEFINITIONS_DIR, definitionFile);
    }

    @Override
    public void deleteCodeWorkflowFile(FileEntry codeFile) {
        fileStorageService.deleteFile(CODE_WORKFLOWS_FILES_DIR, codeFile);
    }

    @Override
    public String readCodeWorkflowDefinition(FileEntry definitionFile) {
        return CompressionUtils.decompressToString(
            fileStorageService.readFileToBytes(CODE_WORKFLOWS_DEFINITIONS_DIR, definitionFile));
    }

    @Override
    public URL getCodeWorkflowFileURL(FileEntry codeFile) {
        return fileStorageService.getFileEntryURL(CODE_WORKFLOWS_FILES_DIR, codeFile);
    }

    @Override
    public FileEntry storeCodeWorkflowDefinition(String filename, String definition) {
        return fileStorageService.storeFileContent(
            CODE_WORKFLOWS_DEFINITIONS_DIR, filename, CompressionUtils.compress(definition), false);
    }

    @Override
    public FileEntry storeCodeWorkflowFile(String filename, byte[] bytes) {
        return fileStorageService.storeFileContent(CODE_WORKFLOWS_FILES_DIR, filename, bytes, false);
    }
}
