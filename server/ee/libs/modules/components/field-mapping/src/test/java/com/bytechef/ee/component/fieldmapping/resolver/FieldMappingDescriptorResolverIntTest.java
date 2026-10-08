/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.ee.component.fieldmapping.config.FieldMappingIntTestConfiguration;
import com.bytechef.ee.component.fieldmapping.config.IntegrationInstanceFixtures;
import com.bytechef.ee.component.fieldmapping.mapper.FieldMappingDescriptor;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.test.annotation.ComponentIntTest;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentIntTest
@Import(FieldMappingIntTestConfiguration.class)
class FieldMappingDescriptorResolverIntTest {

    private static final long ENVIRONMENT_ID = 1L;
    private static final String WORKFLOW_ID = workflowId("field-mapping-sync-contacts");

    private static final Map<String, Object> SAVED_MAPPING = Map.of(
        "objectType", "contacts",
        "mappings", List.of(
            Map.of(
                "applicationField", Map.of("label", "Title", "value", "title", "custom", false),
                "integrationField", "first_name")));

    private static final FieldMappingDescriptor EXPECTED = new FieldMappingDescriptor(
        "contacts", List.of(new FieldMappingDescriptor.Mapping("title", "first_name")));

    @Autowired
    private ContextFactory contextFactory;

    @Autowired
    private Encryption encryption;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private IntegrationInstanceFixtures integrationInstanceFixtures;
    private long integrationInstanceConfigurationId;
    private long integrationInstanceConfigurationWorkflowId;
    private long integrationInstanceId;
    private FieldMappingDescriptorResolver resolver;

    @BeforeEach
    void beforeEach() {
        integrationInstanceFixtures = new IntegrationInstanceFixtures(
            encryption, integrationInstanceWorkflowService, jdbcTemplate);

        integrationInstanceConfigurationId = integrationInstanceFixtures.createIntegrationInstanceConfiguration();

        integrationInstanceConfigurationWorkflowId = integrationInstanceFixtures
            .createIntegrationInstanceConfigurationWorkflow(integrationInstanceConfigurationId, WORKFLOW_ID);
        integrationInstanceId = integrationInstanceFixtures.createIntegrationInstance(
            integrationInstanceConfigurationId, "connected-user-1");

        resolver = new FieldMappingDescriptorResolver(
            integrationInstanceWorkflowService, workflowService, workflowTestConfigurationService);
    }

    @AfterEach
    void afterEach() {
        integrationInstanceFixtures.deleteAll();
    }

    @Test
    void testRuntimeResolvesTheConnectedUsersSavedMapping() {
        integrationInstanceFixtures.saveIntegrationInstanceWorkflowInputs(
            integrationInstanceId, integrationInstanceConfigurationWorkflowId,
            Map.of("contactMapping", SAVED_MAPPING, "apiKey", "k"));

        FieldMappingDescriptor descriptor = resolver.resolve(
            "Contacts", runtimeContext(WORKFLOW_ID, PlatformType.EMBEDDED, integrationInstanceId));

        assertEquals(EXPECTED, descriptor);
    }

    @Test
    void testRuntimeFailsWhenPlatformTypeIsNotEmbedded() {
        ActionContext context = runtimeContext(WORKFLOW_ID, PlatformType.AUTOMATION, integrationInstanceId);

        IllegalStateException exception =
            assertThrows(IllegalStateException.class, () -> resolver.resolve("Contacts", context));

        assertTrue(exception.getMessage()
            .contains("embedded"), exception.getMessage());
    }

    @Test
    void testEditorFailsWhenPlatformTypeIsNotEmbedded() {
        ActionContext context = editorContext(PlatformType.AUTOMATION);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> resolver.resolve("Contacts", context));

        assertTrue(exception.getMessage()
            .contains("embedded"), exception.getMessage());
    }

    @Test
    void testRuntimeFailsWhenIntegrationInstanceIsMissing() {
        ActionContext context = runtimeContext(WORKFLOW_ID, PlatformType.EMBEDDED, null);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> resolver.resolve("Contacts", context));

        assertTrue(exception.getMessage()
            .contains("integration instance"), exception.getMessage());
    }

    @Test
    void testRuntimeFailsWhenConnectedUserHasNoRow() {
        ActionContext context = runtimeContext(WORKFLOW_ID, PlatformType.EMBEDDED, integrationInstanceId);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> resolver.resolve("Contacts", context));

        assertTrue(exception.getMessage()
            .contains("Contact Mapping"), exception.getMessage());
    }

    @Test
    void testRuntimeFailsWhenSavedMappingIsEmpty() {
        integrationInstanceFixtures.saveIntegrationInstanceWorkflowInputs(
            integrationInstanceId, integrationInstanceConfigurationWorkflowId,
            Map.of("contactMapping", Map.of("objectType", "contacts", "mappings", List.of())));

        ActionContext context = runtimeContext(WORKFLOW_ID, PlatformType.EMBEDDED, integrationInstanceId);

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("Contacts", context));
    }

    @Test
    void testRuntimeNeverReadsAnotherConnectedUsersMapping() {
        integrationInstanceFixtures.saveIntegrationInstanceWorkflowInputs(
            integrationInstanceId, integrationInstanceConfigurationWorkflowId, Map.of("contactMapping", SAVED_MAPPING));

        long otherIntegrationInstanceId = integrationInstanceFixtures.createIntegrationInstance(
            integrationInstanceConfigurationId, "connected-user-2");

        ActionContext otherUsersContext = runtimeContext(
            WORKFLOW_ID, PlatformType.EMBEDDED, otherIntegrationInstanceId);

        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("Contacts", otherUsersContext));
    }

    @Test
    void testUnknownObjectNameListsTheDeclaredOnes() {
        ActionContext context = runtimeContext(WORKFLOW_ID, PlatformType.EMBEDDED, integrationInstanceId);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> resolver.resolve("Deals", context));

        assertTrue(exception.getMessage()
            .contains("Deals"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("Contacts"), exception.getMessage());
    }

    @Test
    void testFindInputIgnoresANonFieldMappingInputWithAMatchingObjectNameExtension() {
        ActionContext context = runtimeContext(
            workflowId("field-mapping-non-field-mapping-input"), PlatformType.EMBEDDED, integrationInstanceId);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve("Contacts", context));

        assertTrue(exception.getMessage()
            .contains("No field mapping input declares object name 'Contacts'"), exception.getMessage());
    }

    @Test
    void testFindInputFailsWhenTwoFieldMappingInputsShareAnObjectName() {
        ActionContext context = runtimeContext(
            workflowId("field-mapping-duplicate-object-name"), PlatformType.EMBEDDED, integrationInstanceId);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve("Contacts", context));

        assertTrue(exception.getMessage()
            .contains("contactMapping"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("contactMapping2"), exception.getMessage());
    }

    @Test
    void testMissingWorkflowIdFails() {
        ActionContext context = runtimeContext(null, PlatformType.EMBEDDED, integrationInstanceId);

        assertThrows(IllegalStateException.class, () -> resolver.resolve("Contacts", context));
    }

    @Test
    void testEditorResolvesSampleMappingFromTheInputTestValue() {
        String testValue = """
            {"Contacts": {
               "objectTypes": [{"label": "Contacts", "value": "contacts"}],
               "integrationFields": [{"label": "First Name", "value": "first_name"}],
               "applicationFields": {"fields": [{"label": "Title", "value": "title"}]},
               "sampleMapping": {
                 "objectType": "contacts",
                 "mappings": [
                   {"applicationField": {"label": "Title", "value": "title", "custom": false},
                    "integrationField": "first_name"}
                 ]
               }
            }}
            """;

        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(
            WORKFLOW_ID, "contactMapping", testValue, ENVIRONMENT_ID);

        FieldMappingDescriptor descriptor = resolver.resolve("Contacts", editorContext(PlatformType.EMBEDDED));

        assertEquals(EXPECTED, descriptor);
    }

    @Test
    void testEditorAcceptsTheMapObjectFieldsEnvelope() {
        String testValue = """
            {"mapObjectFields": {"Contacts": {
               "sampleMapping": {"objectType": "contacts", "mappings": [
                 {"applicationField": {"value": "title"}, "integrationField": "first_name"}]}
            }}}
            """;

        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(
            WORKFLOW_ID, "contactMapping", testValue, ENVIRONMENT_ID);

        assertEquals(EXPECTED, resolver.resolve("Contacts", editorContext(PlatformType.EMBEDDED)));
    }

    @Test
    void testEditorFailsWhenTestValueIsKeyedUnderADifferentObjectName() {
        String testValue = """
            {"Contacts": {
               "sampleMapping": {"objectType": "contacts", "mappings": []}
            }}
            """;

        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(
            WORKFLOW_ID, "accountMapping", testValue, ENVIRONMENT_ID);

        ActionContext context = editorContext(PlatformType.EMBEDDED);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve("Accounts", context));

        assertTrue(exception.getMessage()
            .contains("Accounts"), exception.getMessage());
        assertTrue(exception.getMessage()
            .contains("Contacts"), exception.getMessage());
    }

    @Test
    void testEditorFailsWithGuidanceWhenSampleMappingIsAbsent() {
        workflowTestConfigurationService.saveWorkflowTestConfigurationInputs(
            WORKFLOW_ID, "contactMapping", "{\"Contacts\": {\"applicationFields\": {\"fields\": []}}}",
            ENVIRONMENT_ID);

        ActionContext context = editorContext(PlatformType.EMBEDDED);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve("Contacts", context));

        assertTrue(exception.getMessage()
            .contains("sampleMapping"), exception.getMessage());
    }

    @Test
    void testEditorFailsWithGuidanceWhenThereIsNoTestValue() {
        ActionContext context = editorContext(PlatformType.EMBEDDED);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> resolver.resolve("Contacts", context));

        assertTrue(exception.getMessage()
            .contains("sampleMapping"), exception.getMessage());
    }

    private ActionContext runtimeContext(String workflowId, PlatformType platformType, Long jobPrincipalId) {
        return contextFactory.createActionContext(
            "fieldMapping", 1, "mapToIntegration", jobPrincipalId, null, null, null, workflowId, null,
            ENVIRONMENT_ID, platformType, false);
    }

    private ActionContext editorContext(PlatformType platformType) {
        return contextFactory.createActionContext(
            "fieldMapping", 1, "mapToIntegration", null, null, null, null, WORKFLOW_ID, null, ENVIRONMENT_ID,
            platformType, true);
    }

    private static String workflowId(String workflowFileName) {
        return EncodingUtils.base64EncodeToString(workflowFileName);
    }
}
