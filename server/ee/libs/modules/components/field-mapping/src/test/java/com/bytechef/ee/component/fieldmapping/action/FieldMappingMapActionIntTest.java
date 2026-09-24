/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.component.fieldmapping.FieldMappingComponentHandler;
import com.bytechef.ee.component.fieldmapping.config.FieldMappingIntTestConfiguration;
import com.bytechef.ee.component.fieldmapping.config.IntegrationInstanceFixtures;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.test.annotation.ComponentIntTest;
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
@Import({
    FieldMappingComponentHandler.class, FieldMappingIntTestConfiguration.class
})
class FieldMappingMapActionIntTest {

    private static final long ENVIRONMENT_ID = 1L;
    private static final String WORKFLOW_ID = EncodingUtils.base64EncodeToString("field-mapping-sync-contacts");

    private static final Map<String, Object> SAVED_MAPPING = Map.of(
        "objectType", "contacts",
        "mappings", List.of(
            Map.of(
                "applicationField", Map.of("label", "Title", "value", "title", "custom", false),
                "integrationField", "first_name")));

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private ActionDefinitionService actionDefinitionService;

    @Autowired
    private Encryption encryption;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private IntegrationInstanceFixtures integrationInstanceFixtures;
    private long integrationInstanceId;

    @BeforeEach
    void beforeEach() {
        integrationInstanceFixtures = new IntegrationInstanceFixtures(
            encryption, integrationInstanceWorkflowService, jdbcTemplate);

        long integrationInstanceConfigurationId = integrationInstanceFixtures.createIntegrationInstanceConfiguration();

        long integrationInstanceConfigurationWorkflowId = integrationInstanceFixtures
            .createIntegrationInstanceConfigurationWorkflow(integrationInstanceConfigurationId, WORKFLOW_ID);

        integrationInstanceId = integrationInstanceFixtures.createIntegrationInstance(
            integrationInstanceConfigurationId, "connected-user-1");

        integrationInstanceFixtures.saveIntegrationInstanceWorkflowInputs(
            integrationInstanceId, integrationInstanceConfigurationWorkflowId,
            Map.of("contactMapping", SAVED_MAPPING));
    }

    @AfterEach
    void afterEach() {
        integrationInstanceFixtures.deleteAll();
    }

    @Test
    void testMapToIntegrationResolvesByObjectNameAndAppliesToAnObject() {
        Object result = executePerform(
            "mapToIntegration",
            Map.of("objectName", "Contacts", "inputType", "OBJECT", "data", Map.of("title", "Dr", "x", 1)));

        assertEquals(Map.of("first_name", "Dr"), result);
    }

    @Test
    void testMapToApplicationAppliesToAnArrayAndHonoursIncludeUnmapped() {
        Object result = executePerform(
            "mapToApplication",
            Map.of(
                "objectName", "Contacts", "inputType", "ARRAY", "includeUnmapped", true,
                "data", List.of(Map.of("first_name", "Dr", "stage", "lead"))));

        assertEquals(List.of(Map.of("title", "Dr", "stage", "lead")), result);
    }

    @Test
    void testDefinitionsCarryTheDirectionsNames() {
        List<ActionDefinition> actionDefinitions = actionDefinitionService.getActionDefinitions("fieldMapping", 1);

        assertEquals(
            List.of("mapToIntegration", "mapToApplication"),
            actionDefinitions.stream()
                .map(ActionDefinition::getName)
                .toList());
    }

    private Object executePerform(String actionName, Map<String, ?> inputParameters) {
        return actionDefinitionFacade.executePerform(
            "fieldMapping", 1, actionName, integrationInstanceId, null, null, null, WORKFLOW_ID, inputParameters,
            Map.of(), Map.of(), ENVIRONMENT_ID, PlatformType.EMBEDDED, false, null, null, null);
    }
}
