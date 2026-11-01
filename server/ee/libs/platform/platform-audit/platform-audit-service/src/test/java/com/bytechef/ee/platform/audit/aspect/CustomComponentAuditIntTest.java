/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.customcomponent.configuration.audit.CustomComponentAuditEvents;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent.Language;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacadeImpl;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.ee.platform.customcomponent.file.storage.CustomComponentFileStorage;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    CustomComponentAuditIntTest.CustomComponentFacadeConfiguration.class, PostgreSQLContainerConfiguration.class
})
class CustomComponentAuditIntTest {

    private static final long CUSTOM_COMPONENT_ID = 5L;

    @Autowired
    private CustomComponentFacade customComponentFacade;

    @MockitoBean
    private CustomComponentFileStorage customComponentFileStorage;

    @MockitoBean
    private CustomComponentService customComponentService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveOfAnUnloadableComponentIsRecordedAsAnError() {
        byte[] bytes = "not a component".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> customComponentFacade.save(bytes, Language.JAVA))
            .isInstanceOf(RuntimeException.class);

        verify(customComponentFileStorage, never()).storeCustomComponentFile(any(), any());

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(CustomComponentAuditEvents.CUSTOM_COMPONENT_SAVED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("language", "JAVA")
            .doesNotContainKeys("customComponentId", "name", "version", "dataError");
    }

    @Test
    void testSaveOfANewComponentRecordsItsIdNameVersionAndLanguage() throws IOException {
        when(customComponentService.create(any())).thenAnswer(invocation -> {
            CustomComponent customComponent = invocation.getArgument(0);

            customComponent.setId(CUSTOM_COMPONENT_ID);

            return customComponent;
        });

        CustomComponent customComponent = customComponentFacade.save(createComponentJar(), Language.JAVA);

        assertThat(customComponent.getName()).isEqualTo(SampleComponentHandler.NAME);

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(CustomComponentAuditEvents.CUSTOM_COMPONENT_SAVED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("customComponentId", String.valueOf(CUSTOM_COMPONENT_ID))
            .containsEntry("name", SampleComponentHandler.NAME)
            .containsEntry("version", String.valueOf(SampleComponentHandler.VERSION))
            .containsEntry("language", "JAVA")
            .doesNotContainKey("dataError");
    }

    private PersistentAuditEvent findSingle() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private static byte[] createComponentJar() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String sampleComponentHandlerClassName = SampleComponentHandler.class.getName();

        try (JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream)) {
            jarOutputStream.putNextEntry(new JarEntry("META-INF/services/" + ComponentHandler.class.getName()));
            jarOutputStream.write(sampleComponentHandlerClassName.getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();

            jarOutputStream.putNextEntry(new JarEntry("assets/sample.svg"));
            jarOutputStream.write("<svg/>".getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static class SampleComponentHandler implements ComponentHandler {

        static final String NAME = "auditSample";
        static final int VERSION = 2;

        @Override
        public ComponentDefinition getDefinition() {
            return ComponentDsl.component(NAME)
                .title("Audit Sample")
                .version(VERSION);
        }
    }

    @Configuration
    static class CustomComponentFacadeConfiguration {

        @Bean
        CustomComponentFacade customComponentFacade(
            CustomComponentFileStorage customComponentFileStorage, CustomComponentService customComponentService) {

            return new CustomComponentFacadeImpl(
                new ConcurrentMapCacheManager(), customComponentService, customComponentFileStorage);
        }
    }
}
