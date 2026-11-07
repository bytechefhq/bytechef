/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.grpc;

import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.DeleteDocumentsByIdsRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.DocumentIdResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.DocumentsRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.DocumentsResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.Empty;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.FilterRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBaseDocumentChunksResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBaseDocumentsResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBaseRemoteServiceGrpc;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBaseResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBasesResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.LoadDocumentRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.SimilaritySearchRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.TagNamesResponse;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.UpdateDocumentRequest;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentChunkFacade;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class KnowledgeBaseRemoteServiceGrpcServer
    extends KnowledgeBaseRemoteServiceGrpc.KnowledgeBaseRemoteServiceImplBase {

    private final KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade;
    private final KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService;
    private final KnowledgeBaseProtoMapper knowledgeBaseProtoMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorStore vectorStore;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseRemoteServiceGrpcServer(
        KnowledgeBaseService knowledgeBaseService, KnowledgeBaseDocumentService knowledgeBaseDocumentService,
        KnowledgeBaseDocumentTagService knowledgeBaseDocumentTagService,
        KnowledgeBaseDocumentChunkFacade knowledgeBaseDocumentChunkFacade,
        KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader, VectorStore vectorStore,
        KnowledgeBaseProtoMapper knowledgeBaseProtoMapper) {

        this.knowledgeBaseDocumentChunkFacade = knowledgeBaseDocumentChunkFacade;
        this.knowledgeBaseDocumentLoader = knowledgeBaseDocumentLoader;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseDocumentTagService = knowledgeBaseDocumentTagService;
        this.knowledgeBaseProtoMapper = knowledgeBaseProtoMapper;
        this.knowledgeBaseService = knowledgeBaseService;
        this.vectorStore = vectorStore;
    }

    @Override
    public void getKnowledgeBase(IdRequest request, StreamObserver<KnowledgeBaseResponse> responseObserver) {
        responseObserver.onNext(
            KnowledgeBaseResponse.newBuilder()
                .setKnowledgeBase(
                    knowledgeBaseProtoMapper.write(knowledgeBaseService.getKnowledgeBase(request.getId())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getKnowledgeBases(Empty request, StreamObserver<KnowledgeBasesResponse> responseObserver) {
        responseObserver.onNext(
            KnowledgeBasesResponse.newBuilder()
                .setKnowledgeBases(knowledgeBaseProtoMapper.write(knowledgeBaseService.getKnowledgeBases()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getKnowledgeBaseDocuments(
        IdRequest request, StreamObserver<KnowledgeBaseDocumentsResponse> responseObserver) {

        responseObserver.onNext(
            KnowledgeBaseDocumentsResponse.newBuilder()
                .setKnowledgeBaseDocuments(
                    knowledgeBaseProtoMapper.write(
                        knowledgeBaseDocumentService.getKnowledgeBaseDocuments(request.getId())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAllTagNames(Empty request, StreamObserver<TagNamesResponse> responseObserver) {
        replyTagNames(knowledgeBaseDocumentTagService.getAllTagNames(), responseObserver);
    }

    @Override
    public void getTagNamesByKnowledgeBaseId(IdRequest request, StreamObserver<TagNamesResponse> responseObserver) {
        replyTagNames(
            knowledgeBaseDocumentTagService.getTagNamesByKnowledgeBaseId(request.getId()), responseObserver);
    }

    @Override
    public void getKnowledgeBaseDocumentChunksByDocumentId(
        IdRequest request, StreamObserver<KnowledgeBaseDocumentChunksResponse> responseObserver) {

        responseObserver.onNext(
            KnowledgeBaseDocumentChunksResponse.newBuilder()
                .setKnowledgeBaseDocumentChunks(
                    knowledgeBaseProtoMapper.write(
                        knowledgeBaseDocumentChunkFacade.getKnowledgeBaseDocumentChunksByDocumentId(request.getId())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void loadDocument(LoadDocumentRequest request, StreamObserver<DocumentIdResponse> responseObserver) {
        Long knowledgeBaseDocumentId = request.hasKnowledgeBaseDocumentId()
            ? request.getKnowledgeBaseDocumentId() : null;

        long id = knowledgeBaseDocumentLoader.load(
            request.getKnowledgeBaseId(), knowledgeBaseDocumentId, request.getDocumentName(),
            knowledgeBaseProtoMapper.toChunks(request.getChunks()),
            knowledgeBaseProtoMapper.toMetadata(request.getAdditionalMetadata()));

        replyDocumentId(id, responseObserver);
    }

    @Override
    public void updateDocument(UpdateDocumentRequest request, StreamObserver<DocumentIdResponse> responseObserver) {
        Long knowledgeBaseDocumentId = request.hasKnowledgeBaseDocumentId()
            ? request.getKnowledgeBaseDocumentId() : null;
        Long knowledgeBaseDocumentChunkId = request.hasKnowledgeBaseDocumentChunkId()
            ? request.getKnowledgeBaseDocumentChunkId() : null;

        long id = knowledgeBaseDocumentLoader.update(
            request.getKnowledgeBaseId(), knowledgeBaseDocumentId, knowledgeBaseDocumentChunkId,
            knowledgeBaseProtoMapper.toChunks(request.getChunks()),
            knowledgeBaseProtoMapper.toMetadata(request.getAdditionalMetadata()));

        replyDocumentId(id, responseObserver);
    }

    @Override
    public void addDocuments(DocumentsRequest request, StreamObserver<Empty> responseObserver) {
        vectorStore.add(knowledgeBaseProtoMapper.toDocuments(request.getDocuments()));

        replyEmpty(responseObserver);
    }

    @Override
    public void deleteDocumentsByIds(DeleteDocumentsByIdsRequest request, StreamObserver<Empty> responseObserver) {
        vectorStore.delete(List.copyOf(request.getIdsList()));

        replyEmpty(responseObserver);
    }

    @Override
    public void deleteDocumentsByFilter(FilterRequest request, StreamObserver<Empty> responseObserver) {
        vectorStore.delete(knowledgeBaseProtoMapper.toFilterExpression(request.getFilter()));

        replyEmpty(responseObserver);
    }

    @Override
    public void similaritySearch(SimilaritySearchRequest request, StreamObserver<DocumentsResponse> responseObserver) {
        SearchRequest.Builder builder = SearchRequest.builder()
            .query(request.getQuery())
            .topK(request.getTopK())
            .similarityThreshold(request.getSimilarityThreshold());

        if (request.hasFilter()) {
            builder.filterExpression(knowledgeBaseProtoMapper.toFilterExpression(request.getFilter()));
        }

        responseObserver.onNext(
            DocumentsResponse.newBuilder()
                .setDocuments(knowledgeBaseProtoMapper.writeDocuments(vectorStore.similaritySearch(builder.build())))
                .build());
        responseObserver.onCompleted();
    }

    private static void replyDocumentId(long id, StreamObserver<DocumentIdResponse> responseObserver) {
        responseObserver.onNext(
            DocumentIdResponse.newBuilder()
                .setKnowledgeBaseDocumentId(id)
                .build());
        responseObserver.onCompleted();
    }

    private static void replyEmpty(StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private static void replyTagNames(List<String> tagNames, StreamObserver<TagNamesResponse> responseObserver) {
        responseObserver.onNext(
            TagNamesResponse.newBuilder()
                .addAllTagNames(tagNames)
                .build());
        responseObserver.onCompleted();
    }
}
