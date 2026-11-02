/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.remote.client.facade;

import com.bytechef.ee.platform.ai.skill.remote.grpc.AiSkillProtoMapper;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillDownloadResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillIdRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.CreateAiSkillFromInstructionsRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.CreateAiSkillRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.SkillFileRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.SkillFilesRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.TagIdsRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.UpdateAiSkillContentRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.UpdateAiSkillRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.UpdateAiSkillTagsRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.tag.domain.Tag;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteAiSkillFacadeClient implements AiSkillFacade {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final AiSkillProtoMapper aiSkillProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteAiSkillFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.aiSkillProtoMapper = new AiSkillProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public AiSkill createAiSkill(String name, @Nullable String description, String filename, byte[] bytes) {
        CreateAiSkillRequest.Builder builder = CreateAiSkillRequest.newBuilder()
            .setArchive(ByteString.copyFrom(bytes))
            .setFilename(filename)
            .setName(name);

        if (description != null) {
            builder.setDescription(description);
        }

        return toAiSkill(call(() -> getStub().createAiSkill(builder.build())));
    }

    @Override
    public AiSkill createAiSkillFromInstructions(
        String name, @Nullable String description, String instructions,
        @Nullable Map<String, String> additionalFiles) {

        CreateAiSkillFromInstructionsRequest.Builder builder = CreateAiSkillFromInstructionsRequest.newBuilder()
            .setInstructions(instructions)
            .setName(name);

        if (description != null) {
            builder.setDescription(description);
        }

        if (additionalFiles != null) {
            builder.putAllAdditionalFiles(additionalFiles);
        }

        return toAiSkill(call(() -> getStub().createAiSkillFromInstructions(builder.build())));
    }

    @Override
    public AiSkill createAdditionalFilesInSkill(long id, Map<String, String> additionalFiles) {
        return toAiSkill(
            call(() -> getStub().createAdditionalFilesInSkill(
                SkillFilesRequest.newBuilder()
                    .setId(id)
                    .putAllFiles(additionalFiles)
                    .build())));
    }

    @Override
    public void deleteAiSkill(long id) {
        call(() -> getStub().deleteAiSkill(toIdRequest(id)));
    }

    @Override
    public AiSkill getAiSkill(long id) {
        return toAiSkill(call(() -> getStub().getAiSkill(toIdRequest(id))));
    }

    @Override
    public byte[] getAiSkillDownload(long id) {
        return call(() -> getStub().getAiSkillDownload(toIdRequest(id))).getArchive()
            .toByteArray();
    }

    @Override
    public AiSkillDownload getAiSkillWithDownload(long id) {
        AiSkillDownloadResponse aiSkillDownloadResponse = call(
            () -> getStub().getAiSkillWithDownload(toIdRequest(id)));

        ByteString archive = aiSkillDownloadResponse.getArchive();

        return new AiSkillDownload(
            aiSkillProtoMapper.toAiSkill(aiSkillDownloadResponse.getAiSkill()), archive.toByteArray());
    }

    @Override
    public String getAiSkillFileContent(long id, String path) {
        return call(() -> getStub().getAiSkillFileContent(toSkillFileRequest(id, path))).getContent();
    }

    @Override
    public List<String> getAiSkillFilePaths(long id) {
        return List.copyOf(call(() -> getStub().getAiSkillFilePaths(toIdRequest(id))).getPathsList());
    }

    @Override
    public AiSkill removeFileInSkill(long id, String path) {
        return toAiSkill(call(() -> getStub().removeFileInSkill(toSkillFileRequest(id, path))));
    }

    @Override
    public List<AiSkill> getAiSkills() {
        return call(() -> getStub().getAiSkills(Empty.getDefaultInstance())).getAiSkillsList()
            .stream()
            .map(aiSkillProtoMapper::toAiSkill)
            .toList();
    }

    @Override
    public List<Tag> getTags(List<Long> tagIds) {
        return aiSkillProtoMapper.toTags(
            call(() -> getStub().getTags(
                TagIdsRequest.newBuilder()
                    .addAllTagIds(tagIds)
                    .build())).getTags());
    }

    @Override
    public AiSkill updateAiSkill(long id, String name, @Nullable String description) {
        UpdateAiSkillRequest.Builder builder = UpdateAiSkillRequest.newBuilder()
            .setId(id)
            .setName(name);

        if (description != null) {
            builder.setDescription(description);
        }

        return toAiSkill(call(() -> getStub().updateAiSkill(builder.build())));
    }

    @Override
    public AiSkill updateAiSkillTags(long id, List<Tag> tags) {
        return toAiSkill(
            call(() -> getStub().updateAiSkillTags(
                UpdateAiSkillTagsRequest.newBuilder()
                    .setId(id)
                    .setTags(aiSkillProtoMapper.write(tags))
                    .build())));
    }

    @Override
    public AiSkill updateAiSkillContent(long id, @Nullable String path, String content) {
        UpdateAiSkillContentRequest.Builder builder = UpdateAiSkillContentRequest.newBuilder()
            .setContent(content)
            .setId(id);

        if (path != null) {
            builder.setPath(path);
        }

        return toAiSkill(call(() -> getStub().updateAiSkillContent(builder.build())));
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (StatusRuntimeException statusRuntimeException) {
            Status status = statusRuntimeException.getStatus();

            if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(status.getDescription(), statusRuntimeException);
            }

            throw statusRuntimeException;
        }
    }

    private AiSkillFacadeRemoteServiceGrpc.AiSkillFacadeRemoteServiceBlockingStub getStub() {
        return AiSkillFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    private AiSkill toAiSkill(AiSkillResponse aiSkillResponse) {
        return aiSkillProtoMapper.toAiSkill(aiSkillResponse.getAiSkill());
    }

    private static AiSkillIdRequest toIdRequest(long id) {
        return AiSkillIdRequest.newBuilder()
            .setId(id)
            .build();
    }

    private static SkillFileRequest toSkillFileRequest(long id, String path) {
        return SkillFileRequest.newBuilder()
            .setId(id)
            .setPath(path)
            .build();
    }
}
