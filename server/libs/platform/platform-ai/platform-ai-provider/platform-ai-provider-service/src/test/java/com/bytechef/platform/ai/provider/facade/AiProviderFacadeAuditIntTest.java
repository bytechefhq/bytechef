/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.provider.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.audit.AiProviderAuditEvents;
import com.bytechef.platform.ai.provider.audit.AiProviderAuditMapper;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(
    classes = {
        AiProviderFacadeIntTest.AiProviderFacadeIntTestConfiguration.class,
        AiProviderFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = "bytechef.edition=ee")
class AiProviderFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final int ENVIRONMENT = 0;
    private static final String SECRET_URL = "https://user:pw@ollama.example.com:8443/api?token=t#f";

    @Autowired
    private AiProviderFacade aiProviderFacade;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyService propertyService;

    @BeforeEach
    void beforeEach() {
        authenticate(AuthorityConstants.ADMIN);

        propertyRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        propertyRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testDeleteRecordsTheProviderAndEnvironment() {
        saveOllamaProperty();

        aiProviderFacade.deleteAiProvider(Provider.OLLAMA.getId(), ENVIRONMENT);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiProviderAuditEvents.AI_PROVIDER_DELETED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiProviderId", String.valueOf(Provider.OLLAMA.getId()))
            .containsEntry("environment", String.valueOf(ENVIRONMENT))
            .doesNotContainKeys("dataError", "errorClass");
        assertThat(propertyRepository.count()).isZero();
    }

    @Test
    void testDeleteOfAnUnconfiguredProviderRecordsAnError() {
        assertThatThrownBy(() -> aiProviderFacade.deleteAiProvider(Provider.OLLAMA.getId(), ENVIRONMENT))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiProviderAuditEvents.AI_PROVIDER_DELETED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("aiProviderId", String.valueOf(Provider.OLLAMA.getId()))
            .containsEntry("environment", String.valueOf(ENVIRONMENT));
    }

    @Test
    void testDeleteByANonAdminIsRecordedAsDenied() {
        saveOllamaProperty();

        authenticate(AuthorityConstants.USER);

        assertThatThrownBy(() -> aiProviderFacade.deleteAiProvider(Provider.OLLAMA.getId(), ENVIRONMENT))
            .isInstanceOf(AccessDeniedException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiProviderAuditEvents.AI_PROVIDER_DELETED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "DENIED")
            .containsEntry("aiProviderId", String.valueOf(Provider.OLLAMA.getId()))
            .containsEntry("environment", String.valueOf(ENVIRONMENT));
        assertThat(propertyRepository.count()).isEqualTo(1);
    }

    @Test
    void testEnablingRecordsTheNewState() {
        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), true, ENVIRONMENT);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiProviderAuditEvents.AI_PROVIDER_UPDATED);

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiProviderId", String.valueOf(Provider.OLLAMA.getId()))
            .containsEntry("enabled", "true")
            .containsEntry("environment", String.valueOf(ENVIRONMENT))
            .doesNotContainKeys("dataError", "errorClass", "url");
    }

    @Test
    void testEnablingAnUnknownProviderRecordsAnError() {
        assertThatThrownBy(() -> aiProviderFacade.updateAiProvider(Integer.MAX_VALUE, false, ENVIRONMENT))
            .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiProviderAuditEvents.AI_PROVIDER_UPDATED);

        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("aiProviderId", String.valueOf(Integer.MAX_VALUE))
            .containsEntry("enabled", "false")
            .containsEntry("environment", String.valueOf(ENVIRONMENT));
    }

    @Test
    void testUpdatingTheUrlRecordsItWithoutCredentialsQueryOrFragment() {
        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), "secret-api-key", SECRET_URL, ENVIRONMENT);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiProviderAuditEvents.AI_PROVIDER_UPDATED);

        Map<String, String> data = persistentAuditEvent.getData();

        assertThat(persistentAuditEvent.getPlatform()).isNull();
        assertThat(data)
            .containsEntry("result", "SUCCESS")
            .containsEntry("aiProviderId", String.valueOf(Provider.OLLAMA.getId()))
            .containsEntry("environment", String.valueOf(ENVIRONMENT))
            .containsEntry("url", "https://ollama.example.com:8443/api")
            .doesNotContainKeys("apiKey", "dataError", "errorClass", "mapperError");
        assertThat(data.values())
            .noneMatch(value -> value.contains("secret-api-key"))
            .noneMatch(value -> value.contains("user:pw"))
            .noneMatch(value -> value.contains("token=t"))
            .noneMatch(value -> value.contains("#f"));
    }

    @Test
    void testUpdatingTheUrlOfAnUnknownProviderRecordsAnErrorWithTheRedactedUrl() {
        assertThatThrownBy(
            () -> aiProviderFacade.updateAiProvider(Integer.MAX_VALUE, "secret-api-key", SECRET_URL, ENVIRONMENT))
                .isInstanceOf(IllegalArgumentException.class);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(AiProviderAuditEvents.AI_PROVIDER_UPDATED);

        Map<String, String> data = persistentAuditEvent.getData();

        assertThat(data)
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalArgumentException.class.getName())
            .containsEntry("aiProviderId", String.valueOf(Integer.MAX_VALUE))
            .containsEntry("environment", String.valueOf(ENVIRONMENT))
            .containsEntry("url", "https://ollama.example.com:8443/api");
        assertThat(data.values())
            .noneMatch(value -> value.contains("secret-api-key"))
            .noneMatch(value -> value.contains("user:pw"));
    }

    private static void authenticate(String authority) {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                ADMIN_LOGIN, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent()
            .stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private void saveOllamaProperty() {
        propertyService.save(
            Provider.OLLAMA.getKey(), Map.of("url", "http://localhost:11434"), Scope.PLATFORM, null,
            (long) ENVIRONMENT);
    }

    @Configuration
    @EnableMethodSecurity
    @Import({
        AiProviderAuditMapper.class, AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class,
        AuditMapperResolver.class
    })
    static class AuditConfiguration {
    }
}
