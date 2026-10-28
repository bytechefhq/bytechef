/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.proto.RemoteCodeWorkflow;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.proto.RemoteCodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc.proto.RemoteFileEntry;
import com.bytechef.file.storage.domain.FileEntry;
import java.util.Map;
import java.util.UUID;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowContainerProtoMapper {

    public CodeWorkflowContainer toCodeWorkflowContainer(RemoteCodeWorkflowContainer remoteCodeWorkflowContainer) {
        CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(
            UUID.fromString(remoteCodeWorkflowContainer.getUuid()));

        for (RemoteCodeWorkflow remoteCodeWorkflow : remoteCodeWorkflowContainer.getCodeWorkflowsList()) {
            codeWorkflowContainer.addCodeWorkflow(
                UUID.fromString(remoteCodeWorkflow.getId()), remoteCodeWorkflow.getName());
        }

        if (remoteCodeWorkflowContainer.hasExternalVersion()) {
            codeWorkflowContainer.setExternalVersion(remoteCodeWorkflowContainer.getExternalVersion());
        }

        if (remoteCodeWorkflowContainer.hasId()) {
            codeWorkflowContainer.setId(remoteCodeWorkflowContainer.getId());
        }

        codeWorkflowContainer.setLanguage(Language.valueOf(remoteCodeWorkflowContainer.getLanguage()));

        if (remoteCodeWorkflowContainer.hasName()) {
            codeWorkflowContainer.setName(remoteCodeWorkflowContainer.getName());
        }

        if (remoteCodeWorkflowContainer.hasWorkflows()) {
            codeWorkflowContainer.setWorkflows(toFileEntry(remoteCodeWorkflowContainer.getWorkflows()));
        }

        return codeWorkflowContainer;
    }

    public RemoteCodeWorkflowContainer toProto(CodeWorkflowContainer codeWorkflowContainer) {
        RemoteCodeWorkflowContainer.Builder builder = RemoteCodeWorkflowContainer.newBuilder();

        Map<String, String> workflowNameIds = codeWorkflowContainer.getWorkflowNameIds();

        for (Map.Entry<String, String> entry : workflowNameIds.entrySet()) {
            builder.addCodeWorkflows(
                RemoteCodeWorkflow.newBuilder()
                    .setId(entry.getValue())
                    .setName(entry.getKey())
                    .build());
        }

        String externalVersion = codeWorkflowContainer.getExternalVersion();

        if (externalVersion != null) {
            builder.setExternalVersion(externalVersion);
        }

        Long id = codeWorkflowContainer.getId();

        if (id != null) {
            builder.setId(id);
        }

        Language language = codeWorkflowContainer.getLanguage();

        builder.setLanguage(language.name());

        String name = codeWorkflowContainer.getName();

        if (name != null) {
            builder.setName(name);
        }

        builder.setUuid(codeWorkflowContainer.getUuid());

        FileEntry workflows = codeWorkflowContainer.getWorkflows();

        if (workflows != null) {
            builder.setWorkflows(toProto(workflows));
        }

        return builder.build();
    }

    private static FileEntry toFileEntry(RemoteFileEntry remoteFileEntry) {
        return new FileEntry(
            remoteFileEntry.getName(), remoteFileEntry.hasExtension() ? remoteFileEntry.getExtension() : null,
            remoteFileEntry.hasMimeType() ? remoteFileEntry.getMimeType() : null, remoteFileEntry.getUrl());
    }

    private static RemoteFileEntry toProto(FileEntry fileEntry) {
        RemoteFileEntry.Builder builder = RemoteFileEntry.newBuilder()
            .setName(fileEntry.getName())
            .setUrl(fileEntry.getUrl());

        String extension = fileEntry.getExtension();

        if (extension != null) {
            builder.setExtension(extension);
        }

        String mimeType = fileEntry.getMimeType();

        if (mimeType != null) {
            builder.setMimeType(mimeType);
        }

        return builder.build();
    }
}
