/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.remote.grpc;

import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillDownloadResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillIdRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.AiSkillsResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.ArchiveResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.CreateAiSkillFromInstructionsRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.CreateAiSkillRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.FileContentResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.FilePathsResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.SkillFileRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.SkillFilesRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.TagIdsRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.TagsResponse;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.UpdateAiSkillContentRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.UpdateAiSkillRequest;
import com.bytechef.ee.platform.ai.skill.remote.grpc.proto.UpdateAiSkillTagsRequest;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiSkillFacadeRemoteServiceGrpcServer
    extends AiSkillFacadeRemoteServiceGrpc.AiSkillFacadeRemoteServiceImplBase {

    private final AiSkillFacade aiSkillFacade;
    private final AiSkillProtoMapper aiSkillProtoMapper;

    @SuppressFBWarnings("EI")
    public AiSkillFacadeRemoteServiceGrpcServer(AiSkillFacade aiSkillFacade, AiSkillProtoMapper aiSkillProtoMapper) {
        this.aiSkillFacade = aiSkillFacade;
        this.aiSkillProtoMapper = aiSkillProtoMapper;
    }

    @Override
    public void createAdditionalFilesInSkill(
        SkillFilesRequest request, StreamObserver<AiSkillResponse> responseObserver) {

        reply(aiSkillFacade.createAdditionalFilesInSkill(request.getId(), request.getFilesMap()), responseObserver);
    }

    @Override
    public void createAiSkill(CreateAiSkillRequest request, StreamObserver<AiSkillResponse> responseObserver) {
        ByteString archive = request.getArchive();

        reply(
            aiSkillFacade.createAiSkill(
                request.getName(), request.hasDescription() ? request.getDescription() : null, request.getFilename(),
                archive.toByteArray()),
            responseObserver);
    }

    @Override
    public void createAiSkillFromInstructions(
        CreateAiSkillFromInstructionsRequest request, StreamObserver<AiSkillResponse> responseObserver) {

        reply(
            aiSkillFacade.createAiSkillFromInstructions(
                request.getName(), request.hasDescription() ? request.getDescription() : null,
                request.getInstructions(), request.getAdditionalFilesMap()),
            responseObserver);
    }

    @Override
    public void deleteAiSkill(AiSkillIdRequest request, StreamObserver<Empty> responseObserver) {
        aiSkillFacade.deleteAiSkill(request.getId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getAiSkill(AiSkillIdRequest request, StreamObserver<AiSkillResponse> responseObserver) {
        reply(aiSkillFacade.getAiSkill(request.getId()), responseObserver);
    }

    @Override
    public void getAiSkillDownload(AiSkillIdRequest request, StreamObserver<ArchiveResponse> responseObserver) {
        responseObserver.onNext(
            ArchiveResponse.newBuilder()
                .setArchive(ByteString.copyFrom(aiSkillFacade.getAiSkillDownload(request.getId())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAiSkillFileContent(SkillFileRequest request, StreamObserver<FileContentResponse> responseObserver) {
        responseObserver.onNext(
            FileContentResponse.newBuilder()
                .setContent(aiSkillFacade.getAiSkillFileContent(request.getId(), request.getPath()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAiSkillFilePaths(AiSkillIdRequest request, StreamObserver<FilePathsResponse> responseObserver) {
        responseObserver.onNext(
            FilePathsResponse.newBuilder()
                .addAllPaths(aiSkillFacade.getAiSkillFilePaths(request.getId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAiSkillWithDownload(
        AiSkillIdRequest request, StreamObserver<AiSkillDownloadResponse> responseObserver) {

        AiSkillFacade.AiSkillDownload aiSkillDownload = aiSkillFacade.getAiSkillWithDownload(request.getId());

        responseObserver.onNext(
            AiSkillDownloadResponse.newBuilder()
                .setAiSkill(aiSkillProtoMapper.write(aiSkillDownload.aiSkill()))
                .setArchive(ByteString.copyFrom(aiSkillDownload.bytes()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAiSkills(Empty request, StreamObserver<AiSkillsResponse> responseObserver) {
        AiSkillsResponse.Builder builder = AiSkillsResponse.newBuilder();

        for (AiSkill aiSkill : aiSkillFacade.getAiSkills()) {
            builder.addAiSkills(aiSkillProtoMapper.write(aiSkill));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTags(TagIdsRequest request, StreamObserver<TagsResponse> responseObserver) {
        responseObserver.onNext(
            TagsResponse.newBuilder()
                .setTags(aiSkillProtoMapper.write(aiSkillFacade.getTags(request.getTagIdsList())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeFileInSkill(SkillFileRequest request, StreamObserver<AiSkillResponse> responseObserver) {
        reply(aiSkillFacade.removeFileInSkill(request.getId(), request.getPath()), responseObserver);
    }

    @Override
    public void updateAiSkill(UpdateAiSkillRequest request, StreamObserver<AiSkillResponse> responseObserver) {
        reply(
            aiSkillFacade.updateAiSkill(
                request.getId(), request.getName(), request.hasDescription() ? request.getDescription() : null),
            responseObserver);
    }

    @Override
    public void updateAiSkillContent(
        UpdateAiSkillContentRequest request, StreamObserver<AiSkillResponse> responseObserver) {

        reply(
            aiSkillFacade.updateAiSkillContent(
                request.getId(), request.hasPath() ? request.getPath() : null, request.getContent()),
            responseObserver);
    }

    @Override
    public void updateAiSkillTags(UpdateAiSkillTagsRequest request, StreamObserver<AiSkillResponse> responseObserver) {
        reply(
            aiSkillFacade.updateAiSkillTags(request.getId(), aiSkillProtoMapper.toTags(request.getTags())),
            responseObserver);
    }

    private void reply(AiSkill aiSkill, StreamObserver<AiSkillResponse> responseObserver) {
        responseObserver.onNext(
            AiSkillResponse.newBuilder()
                .setAiSkill(aiSkillProtoMapper.write(aiSkill))
                .build());
        responseObserver.onCompleted();
    }
}
