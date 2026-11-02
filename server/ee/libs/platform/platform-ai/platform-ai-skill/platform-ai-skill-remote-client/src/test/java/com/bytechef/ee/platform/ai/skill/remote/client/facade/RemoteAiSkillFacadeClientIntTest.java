/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.ee.platform.ai.skill.remote.grpc.AiSkillFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.ai.skill.remote.grpc.AiSkillProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.ai.skill.file.storage.AiSkillFileStorage;
import com.bytechef.platform.ai.skill.file.storage.AiSkillFileStorageImpl;
import com.bytechef.platform.ai.skill.repository.AiSkillRepository;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(classes = RemoteAiSkillFacadeClientIntTest.RemoteAiSkillFacadeClientIntTestConfiguration.class)
class RemoteAiSkillFacadeClientIntTest {

    @Autowired
    private AiSkillFacade aiSkillFacade;

    @Autowired
    private AiSkillRepository aiSkillRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ManagedChannel managedChannel;
    private RemoteAiSkillFacadeClient remoteAiSkillFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new AiSkillFacadeRemoteServiceGrpcServer(aiSkillFacade, new AiSkillProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteAiSkillFacadeClient = new RemoteAiSkillFacadeClient(new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        aiSkillRepository.deleteAll();
    }

    @Test
    void testCreateAiSkillFromInstructionsStoresTheSkillAndItsFiles() {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkillFromInstructions(
            "release-notes", "Writes release notes", "Summarize the changes.",
            Map.of("templates/notes.md", "# Notes"));

        AiSkill storedAiSkill = remoteAiSkillFacadeClient.getAiSkill(aiSkill.getId());

        assertThat(storedAiSkill.getName()).isEqualTo("release-notes");
        assertThat(storedAiSkill.getDescription()).isEqualTo("Writes release notes");
        assertThat(remoteAiSkillFacadeClient.getAiSkillFilePaths(aiSkill.getId()))
            .containsExactlyInAnyOrder("SKILL.md", "templates/notes.md");
        assertThat(remoteAiSkillFacadeClient.getAiSkillFileContent(aiSkill.getId(), "templates/notes.md"))
            .isEqualTo("# Notes");
    }

    @Test
    void testCreateAiSkillFromAnArchiveWithoutADescription() {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkill(
            "archived", null, "archived.skill", zip("SKILL.md", "Archived instructions"));

        assertThat(aiSkill.getName()).isEqualTo("archived");
        assertThat(remoteAiSkillFacadeClient.getAiSkillFileContent(aiSkill.getId(), "SKILL.md"))
            .contains("Archived instructions");
    }

    @Test
    void testFilesCanBeAddedUpdatedAndRemoved() {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkillFromInstructions(
            "editor", null, "Edit things.", null);

        remoteAiSkillFacadeClient.createAdditionalFilesInSkill(aiSkill.getId(), Map.of("a.txt", "A", "b.txt", "B"));
        remoteAiSkillFacadeClient.updateAiSkillContent(aiSkill.getId(), "a.txt", "A2");
        remoteAiSkillFacadeClient.removeFileInSkill(aiSkill.getId(), "b.txt");

        assertThat(remoteAiSkillFacadeClient.getAiSkillFilePaths(aiSkill.getId()))
            .containsExactlyInAnyOrder("SKILL.md", "a.txt");
        assertThat(remoteAiSkillFacadeClient.getAiSkillFileContent(aiSkill.getId(), "a.txt")).isEqualTo("A2");
    }

    @Test
    void testUpdateAiSkillContentWithoutAPathReplacesTheInstructions() {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkillFromInstructions(
            "instructions", null, "Old instructions.", null);

        remoteAiSkillFacadeClient.updateAiSkillContent(
            aiSkill.getId(), null, "---\nname: instructions\n---\nNew instructions.");

        assertThat(remoteAiSkillFacadeClient.getAiSkillFileContent(aiSkill.getId(), "SKILL.md"))
            .contains("New instructions.")
            .doesNotContain("Old instructions.");
    }

    @Test
    void testUpdateAiSkillRenamesAndListsIt() {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkillFromInstructions(
            "before", "Old", "Do things.", null);

        remoteAiSkillFacadeClient.updateAiSkill(aiSkill.getId(), "after", null);

        assertThat(remoteAiSkillFacadeClient.getAiSkills())
            .singleElement()
            .satisfies(listedAiSkill -> {
                assertThat(listedAiSkill.getName()).isEqualTo("after");
                assertThat(listedAiSkill.getDescription()).isNull();
            });
    }

    @Test
    void testDownloadsReturnTheArchive() throws IOException {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkillFromInstructions(
            "downloadable", null, "Download me.", null);

        AiSkillFacade.AiSkillDownload aiSkillDownload = remoteAiSkillFacadeClient.getAiSkillWithDownload(
            aiSkill.getId());

        assertThat(aiSkillDownload.aiSkill()
            .getName()).isEqualTo("downloadable");
        assertThat(aiSkillDownload.bytes()).isEqualTo(remoteAiSkillFacadeClient.getAiSkillDownload(aiSkill.getId()));
        assertThat(aiSkillDownload.bytes()).startsWith((byte) 'P', (byte) 'K');
    }

    @Test
    void testTagsAreStoredAndResolved() {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkillFromInstructions("tagged", null, "Tag me.", null);

        AiSkill taggedAiSkill = remoteAiSkillFacadeClient.updateAiSkillTags(
            aiSkill.getId(), List.of(new Tag("marketing")));

        assertThat(remoteAiSkillFacadeClient.getTags(taggedAiSkill.getTagIds()))
            .extracting(Tag::getName)
            .containsExactly("marketing");
    }

    @Test
    void testDeleteAiSkillRemovesIt() {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkillFromInstructions("deleted", null, "Bye.", null);

        remoteAiSkillFacadeClient.deleteAiSkill(aiSkill.getId());

        assertThat(remoteAiSkillFacadeClient.getAiSkills()).isEmpty();
    }

    @Test
    void testAPathTraversalIsRejectedAsAnIllegalArgument() {
        AiSkill aiSkill = remoteAiSkillFacadeClient.createAiSkillFromInstructions("guarded", null, "Guard.", null);

        assertThatThrownBy(() -> remoteAiSkillFacadeClient.getAiSkillFileContent(aiSkill.getId(), "../etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] zip(String entryName, String content) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }

        return byteArrayOutputStream.toByteArray();
    }

    @ComponentScan(
        basePackages = {
            "com.bytechef.platform.ai.skill.facade", "com.bytechef.platform.ai.skill.service",
            "com.bytechef.platform.tag.service"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
    @EnableAutoConfiguration
    @Import({
        JacksonConfiguration.class, LiquibaseConfiguration.class
    })
    @Configuration
    static class RemoteAiSkillFacadeClientIntTestConfiguration {

        @Bean
        AiSkillFileStorage aiSkillFileStorage() throws IOException {
            return new AiSkillFileStorageImpl(
                new FilesystemFileStorageService(
                    Files.createTempDirectory("remote-ai-skill-int-test")
                        .toString()));
        }

        @Bean
        AuditorAware<String> aiSkillAuditorProvider() {
            return () -> Optional.of("system");
        }

        @EnableJdbcAuditing(
            auditorAwareRef = "aiSkillAuditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class RemoteAiSkillFacadeClientIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            @Autowired
            private ObjectMapper objectMapper;

            @Override
            protected List<?> userConverters() {
                return List.of(
                    new FileEntryToStringConverter(objectMapper), new StringToFileEntryConverter(objectMapper));
            }
        }
    }
}
