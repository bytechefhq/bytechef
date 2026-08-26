/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint.HttpMethod;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.facade.ApiCollectionFacade;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.automation.promotion.PromotionResourceType;
import com.bytechef.ee.automation.promotion.connection.ConnectionEnvironmentMapper;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SyncResult;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionPreview;
import com.bytechef.ee.automation.promotion.dto.EnvironmentPromotionResult;
import com.bytechef.ee.automation.promotion.dto.PromotionConnectionMapping;
import com.bytechef.ee.automation.promotion.dto.PromotionProjectPreview;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ApiCollectionPromotionHandlerTest {

    private static final String COLLECTION_UUID = "0f7a1e2c-1111-4a3b-8c4d-5e6f70819293";
    private static final long PROJECT_ID = 42L;
    private static final long SOURCE_CONNECTION_ID = 11L;
    private static final long SOURCE_ID = 1L;
    private static final long SOURCE_PROJECT_DEPLOYMENT_ID = 200L;
    private static final long SOURCE_TAG_ID = 9L;
    private static final long TARGET_CONNECTION_ID = 22L;
    private static final long TARGET_ID = 100L;
    private static final long TARGET_PROJECT_DEPLOYMENT_ID = 300L;
    private static final long TARGET_TAG_ID = 7L;
    private static final String WORKFLOW_UUID = "9a8b7c6d-2222-4e3f-8a1b-0c9d8e7f6a5b";
    private static final long WORKSPACE_ID = 5L;

    @Mock
    private ApiCollectionEndpointService apiCollectionEndpointService;

    @Mock
    private ApiCollectionFacade apiCollectionFacade;

    @Mock
    private ApiCollectionService apiCollectionService;

    @Mock
    private ConnectionEnvironmentMapper connectionEnvironmentMapper;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentPromoter projectDeploymentPromoter;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    private ApiCollectionPromotionHandler apiCollectionPromotionHandler;

    @BeforeEach
    void setUp() {
        apiCollectionPromotionHandler = new ApiCollectionPromotionHandler(
            apiCollectionEndpointService, apiCollectionFacade, apiCollectionService, connectionEnvironmentMapper,
            connectionService, projectDeploymentPromoter, projectDeploymentService, projectService);
    }

    @Test
    void testGetResourceTypeIsApiCollection() {
        assertThat(apiCollectionPromotionHandler.getResourceType()).isEqualTo(PromotionResourceType.API_COLLECTION);
    }

    @Test
    void testPreviewOnFreshTargetReportsCreate() {
        ApiCollectionDTO source = stubSource(List.of(sourceEndpointDTO(HttpMethod.GET, "orders", "getOrders", true)));

        stubNoExistingTarget();
        stubSourceBindings(source);
        stubSuggestedMappings(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        stubConnectionLookup();

        EnvironmentPromotionPreview preview =
            apiCollectionPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.resourceType()).isEqualTo(PromotionResourceType.API_COLLECTION);
        assertThat(preview.sourceEnvironment()).isEqualTo(Environment.DEVELOPMENT);
        assertThat(preview.existingTargetId()).isNull();
        assertThat(preview.existingTargetName()).isNull();
        assertThat(preview.projects())
            .containsExactly(new PromotionProjectPreview(PROJECT_ID, "Billing", 3, null));
        assertThat(preview.connections())
            .containsExactly(
                new PromotionConnectionMapping(
                    SOURCE_CONNECTION_ID, "Sheets", "googleSheets", 1, TARGET_CONNECTION_ID,
                    List.of("Orders › googleSheets_1")));
        assertThat(preview.warnings())
            .containsExactly("The promoted collection is created disabled; enable it after reviewing its connections.");
    }

    @Test
    void testPreviewOnExistingTargetWarnsAboutContextPathAndCollectionVersionChanges() {
        ApiCollectionDTO source = stubSource(List.of());

        ApiCollection targetApiCollection = targetApiCollection("billing", "billing-v1", 1);

        stubExistingTarget(targetApiCollection);
        stubTargetProjectDeployment(2);
        stubSourceBindings(source);
        stubSuggestedMappings(Map.of());

        when(projectDeploymentPromoter.existingTargetBindings(any(), any()))
            .thenReturn(Map.of(SOURCE_CONNECTION_ID, 33L));
        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID)))
            .thenReturn(List.of(connection()));

        EnvironmentPromotionPreview preview =
            apiCollectionPromotionHandler.preview(SOURCE_ID, Environment.STAGING);

        assertThat(preview.existingTargetId()).isEqualTo(TARGET_ID);
        assertThat(preview.existingTargetName()).isEqualTo("billing");
        assertThat(preview.projects())
            .containsExactly(new PromotionProjectPreview(PROJECT_ID, "Billing", 3, 2));
        assertThat(preview.warnings())
            .containsExactly(
                "Context path will change from 'billing-v1' to 'billing-v2'",
                "Collection version will change from v1 to v2");

        // An existing target binding outranks the name-match suggestion, which is absent here anyway.
        assertThat(preview.connections())
            .singleElement()
            .extracting(PromotionConnectionMapping::suggestedTargetConnectionId)
            .isEqualTo(33L);
    }

    @Test
    void testPreviewToTheSourceEnvironmentIsRejected() {
        when(apiCollectionFacade.getApiCollection(SOURCE_ID)).thenReturn(sourceApiCollectionDTO(List.of()));

        assertThatThrownBy(() -> apiCollectionPromotionHandler.preview(SOURCE_ID, Environment.DEVELOPMENT))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());
    }

    @Test
    void testPromoteToTheSourceEnvironmentIsRejected() {
        when(apiCollectionFacade.getApiCollection(SOURCE_ID)).thenReturn(sourceApiCollectionDTO(List.of()));

        assertThatThrownBy(
            () -> apiCollectionPromotionHandler.promote(SOURCE_ID, Environment.DEVELOPMENT, Map.of()))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.SAME_ENVIRONMENT.getErrorKey());

        verifyNoInteractions(projectDeploymentPromoter);
    }

    @Test
    void testPromoteCreatesTargetApiCollectionAndItsEndpoints() {
        ApiCollectionDTO source = stubSource(List.of(sourceEndpointDTO(HttpMethod.GET, "orders", "getOrders", true)));

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        stubNoExistingTarget();
        stubTargetCreation();
        stubTargetProjectDeployment(2);
        stubSync(List.of(SOURCE_CONNECTION_ID));

        EnvironmentPromotionResult result =
            apiCollectionPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        ArgumentCaptor<ApiCollectionDTO> apiCollectionDTOCaptor = ArgumentCaptor.forClass(ApiCollectionDTO.class);

        verify(apiCollectionFacade).createApiCollection(apiCollectionDTOCaptor.capture());

        ApiCollectionDTO createdApiCollectionDTO = apiCollectionDTOCaptor.getValue();

        assertThat(createdApiCollectionDTO.uuid()).isEqualTo(COLLECTION_UUID);
        assertThat(createdApiCollectionDTO.environment()).isEqualTo(Environment.STAGING);
        assertThat(createdApiCollectionDTO.id()).isNull();
        assertThat(createdApiCollectionDTO.name()).isEqualTo("billing");
        assertThat(createdApiCollectionDTO.description()).isEqualTo("Billing API");
        assertThat(createdApiCollectionDTO.contextPath()).isEqualTo("billing-v2");
        assertThat(createdApiCollectionDTO.collectionVersion()).isEqualTo(2);
        assertThat(createdApiCollectionDTO.projectId()).isEqualTo(PROJECT_ID);
        assertThat(createdApiCollectionDTO.projectVersion()).isEqualTo(3);
        assertThat(createdApiCollectionDTO.enabled()).isFalse();
        assertThat(createdApiCollectionDTO.tags())
            .extracting(Tag::getId)
            .containsExactly(SOURCE_TAG_ID);

        verify(projectDeploymentPromoter).sync(
            eq(source.projectDeployment()), any(ProjectDeployment.class), eq(Map.of()),
            eq(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID)), eq(true));

        ArgumentCaptor<ApiCollectionEndpointDTO> endpointCaptor =
            ArgumentCaptor.forClass(ApiCollectionEndpointDTO.class);

        verify(apiCollectionFacade).createApiCollectionEndpoint(endpointCaptor.capture());

        ApiCollectionEndpointDTO createdEndpoint = endpointCaptor.getValue();

        assertThat(createdEndpoint.apiCollectionId()).isEqualTo(TARGET_ID);
        assertThat(createdEndpoint.httpMethod()).isEqualTo(HttpMethod.GET);
        assertThat(createdEndpoint.path()).isEqualTo("orders");
        assertThat(createdEndpoint.name()).isEqualTo("getOrders");
        assertThat(createdEndpoint.enabled()).isTrue();
        assertThat(createdEndpoint.workflowUuid()).isEqualTo(WORKFLOW_UUID);

        // A created collection has no endpoints of its own, so the target is never read for reconciliation.
        verify(apiCollectionEndpointService, never()).getApiEndpoints(anyLong());
        verify(apiCollectionService, never()).update(any(ApiCollection.class));

        assertThat(result).isEqualTo(
            new EnvironmentPromotionResult(TARGET_ID, true, null, List.of(SOURCE_CONNECTION_ID)));
    }

    @Test
    void testPromoteUpdatesExistingTargetAndReconcilesEndpointsByHttpMethodAndPath() {
        ApiCollectionDTO source = stubSource(
            List.of(
                sourceEndpointDTO(HttpMethod.GET, "orders", "listOrders", true),
                sourceEndpointDTO(HttpMethod.POST, "invoices", "createInvoice", true)));

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of());

        ApiCollection targetApiCollection = targetApiCollection("target billing", "billing-v1", 1);

        stubExistingTarget(targetApiCollection);
        stubTargetProjectDeployment(2);
        stubSync(List.of());

        when(apiCollectionEndpointService.getApiEndpoints(TARGET_ID))
            .thenReturn(
                List.of(
                    targetEndpoint(501L, HttpMethod.GET, "orders", "getOrders", 901L),
                    targetEndpoint(502L, HttpMethod.DELETE, "orders", "deleteOrder", 902L)));

        EnvironmentPromotionResult result =
            apiCollectionPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        verify(projectDeploymentPromoter).sync(
            eq(source.projectDeployment()), any(ProjectDeployment.class), eq(Map.of()), eq(Map.of()), eq(false));

        ArgumentCaptor<ApiCollectionEndpointDTO> updatedEndpointCaptor =
            ArgumentCaptor.forClass(ApiCollectionEndpointDTO.class);

        verify(apiCollectionFacade).updateApiCollectionEndpoint(updatedEndpointCaptor.capture());

        ApiCollectionEndpointDTO updatedEndpoint = updatedEndpointCaptor.getValue();

        assertThat(updatedEndpoint.id()).isEqualTo(501L);
        assertThat(updatedEndpoint.apiCollectionId()).isEqualTo(TARGET_ID);
        assertThat(updatedEndpoint.name()).isEqualTo("listOrders");
        assertThat(updatedEndpoint.workflowUuid()).isEqualTo(WORKFLOW_UUID);

        ArgumentCaptor<ApiCollectionEndpointDTO> createdEndpointCaptor =
            ArgumentCaptor.forClass(ApiCollectionEndpointDTO.class);

        verify(apiCollectionFacade).createApiCollectionEndpoint(createdEndpointCaptor.capture());

        ApiCollectionEndpointDTO createdEndpoint = createdEndpointCaptor.getValue();

        assertThat(createdEndpoint.httpMethod()).isEqualTo(HttpMethod.POST);
        assertThat(createdEndpoint.path()).isEqualTo("invoices");

        verify(apiCollectionEndpointService).delete(502L);
        verify(apiCollectionFacade, never()).createApiCollection(any());

        ArgumentCaptor<ApiCollection> apiCollectionCaptor = ArgumentCaptor.forClass(ApiCollection.class);

        verify(apiCollectionService).update(apiCollectionCaptor.capture());

        ApiCollection updatedApiCollection = apiCollectionCaptor.getValue();

        assertThat(updatedApiCollection.getContextPath()).isEqualTo("billing-v2");
        assertThat(updatedApiCollection.getCollectionVersion()).isEqualTo(2);

        // Spec §4 ⚑3 lists name, description AND tags as environment-local: promotion moves the route, never the
        // labels the target environment chose for itself.
        assertThat(updatedApiCollection.getName()).isEqualTo("target billing");
        assertThat(updatedApiCollection.getDescription()).isEqualTo("Target-owned description");
        assertThat(updatedApiCollection.getTagIds()).containsExactly(TARGET_TAG_ID);

        assertThat(result).isEqualTo(new EnvironmentPromotionResult(TARGET_ID, false, null, List.of()));
    }

    @Test
    void testPromoteKeepsTargetEndpointProjectDeploymentWorkflowPointerOnRePromotion() {
        ApiCollectionDTO source =
            stubSource(List.of(sourceEndpointDTO(HttpMethod.GET, "orders", "getOrders", true)));

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of());
        stubExistingTarget(targetApiCollection("billing", "billing-v2", 2));
        stubTargetProjectDeployment(2);
        stubSync(List.of());

        when(apiCollectionEndpointService.getApiEndpoints(TARGET_ID))
            .thenReturn(List.of(targetEndpoint(501L, HttpMethod.GET, "orders", "getOrders", 901L)));

        apiCollectionPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        ArgumentCaptor<ApiCollectionEndpointDTO> endpointCaptor =
            ArgumentCaptor.forClass(ApiCollectionEndpointDTO.class);

        verify(apiCollectionFacade).updateApiCollectionEndpoint(endpointCaptor.capture());

        ApiCollectionEndpointDTO updatedEndpoint = endpointCaptor.getValue();

        // The endpoint must keep pointing at the TARGET's own project_deployment_workflow row: every mapping row of
        // the target environment is an FK to it, and re-promotion must not orphan them.
        assertThat(updatedEndpoint.projectDeploymentWorkflowId()).isEqualTo(901L);

        // workflowUuid is what lets the facade re-resolve that pointer when the source moved onto another workflow.
        assertThat(updatedEndpoint.workflowUuid()).isEqualTo(WORKFLOW_UUID);
    }

    @Test
    void testPromoteValidatesPromotableBeforeAnyWrite() {
        ApiCollectionDTO source =
            stubSource(List.of(sourceEndpointDTO(HttpMethod.GET, "orders", "getOrders", true)));

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of());
        stubNoExistingTarget();
        stubTargetCreation();
        stubTargetProjectDeployment(2);
        stubSync(List.of());

        apiCollectionPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of());

        InOrder inOrder = inOrder(projectDeploymentPromoter, apiCollectionFacade);

        inOrder.verify(projectDeploymentPromoter)
            .validatePromotable(PROJECT_ID, 3);
        inOrder.verify(apiCollectionFacade)
            .createApiCollection(any());
        inOrder.verify(projectDeploymentPromoter)
            .sync(any(), any(), anyMap(), anyMap(), eq(true));
        inOrder.verify(apiCollectionFacade)
            .createApiCollectionEndpoint(any());
    }

    @Test
    void testPromoteRejectsTargetNameConflictBeforeCreating() {
        ApiCollectionDTO source = stubSource(List.of());

        stubSourceBindings(source);
        stubMappings(Map.of(), Map.of());
        stubNoExistingTarget();

        when(apiCollectionService.existsByNameAndEnvironment("billing", WORKSPACE_ID, Environment.STAGING, null))
            .thenReturn(true);

        assertThatThrownBy(() -> apiCollectionPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of()))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(EnvironmentPromotionErrorType.TARGET_NAME_CONFLICT.getErrorKey());

        verify(apiCollectionFacade, never()).createApiCollection(any());
    }

    @Test
    void testPromoteRejectsConnectionMappingForConnectionTheSourceDoesNotUse() {
        ApiCollectionDTO source = stubSource(List.of());

        stubSourceBindings(source);

        assertThatThrownBy(
            () -> apiCollectionPromotionHandler.promote(SOURCE_ID, Environment.STAGING, Map.of(999L, 22L)))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());

        // ConnectionEnvironmentMapper#validate constrains the TARGET side only, so a smuggled source id must be
        // rejected here rather than handed to it.
        verifyNoInteractions(connectionEnvironmentMapper);
        verify(apiCollectionFacade, never()).createApiCollection(any());
    }

    @Test
    void testPromotePassesRequestedMappingsForSourceConnectionsThrough() {
        ApiCollectionDTO source = stubSource(List.of());

        stubSourceBindings(source);
        stubMappings(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID), Map.of());
        stubNoExistingTarget();
        stubTargetCreation();
        stubTargetProjectDeployment(2);
        stubSync(List.of());

        apiCollectionPromotionHandler.promote(
            SOURCE_ID, Environment.STAGING, Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));

        verify(connectionEnvironmentMapper).validate(
            WORKSPACE_ID, Environment.STAGING, Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID));
        verify(projectDeploymentPromoter).sync(
            any(), any(), eq(Map.of(SOURCE_CONNECTION_ID, TARGET_CONNECTION_ID)), eq(Map.of()), eq(true));
    }

    private void stubConnectionLookup() {
        when(connectionService.getConnections(List.of(SOURCE_CONNECTION_ID))).thenReturn(List.of(connection()));
    }

    private void stubExistingTarget(ApiCollection targetApiCollection) {
        when(apiCollectionService.fetchApiCollection(UUID.fromString(COLLECTION_UUID), Environment.STAGING))
            .thenReturn(Optional.of(targetApiCollection));
    }

    private void stubMappings(Map<Long, Long> requestedMappings, Map<Long, Long> suggestedMappings) {
        when(connectionEnvironmentMapper.validate(anyLong(), any(Environment.class), anyMap()))
            .thenReturn(requestedMappings);

        stubSuggestedMappings(suggestedMappings);
    }

    private void stubNoExistingTarget() {
        when(apiCollectionService.fetchApiCollection(UUID.fromString(COLLECTION_UUID), Environment.STAGING))
            .thenReturn(Optional.empty());
    }

    private ApiCollectionDTO stubSource(List<ApiCollectionEndpointDTO> endpoints) {
        ApiCollectionDTO source = sourceApiCollectionDTO(endpoints);

        when(apiCollectionFacade.getApiCollection(SOURCE_ID)).thenReturn(source);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project());

        return source;
    }

    private void stubSourceBindings(ApiCollectionDTO source) {
        when(projectDeploymentPromoter.collectSourceBindings(source.projectDeployment()))
            .thenReturn(
                List.of(
                    new SourceBinding(
                        WORKFLOW_UUID, "Orders", "googleSheets_1", "connectionId", SOURCE_CONNECTION_ID)));
    }

    private void stubSuggestedMappings(Map<Long, Long> suggestedMappings) {
        when(connectionEnvironmentMapper.suggest(anyLong(), anySet(), any(Environment.class)))
            .thenReturn(suggestedMappings);
    }

    private void stubSync(List<Long> unresolvedConnectionIds) {
        when(projectDeploymentPromoter.sync(any(), any(), anyMap(), anyMap(), any(Boolean.class)))
            .thenReturn(new SyncResult(Map.of(), unresolvedConnectionIds, List.of()));
    }

    private void stubTargetCreation() {
        ApiCollection createdApiCollection = targetApiCollection("billing", "billing-v2", 2);

        when(apiCollectionFacade.createApiCollection(any())).thenReturn(createdApiCollectionDTO());
        when(apiCollectionService.getApiCollection(TARGET_ID)).thenReturn(createdApiCollection);
    }

    private void stubTargetProjectDeployment(int targetProjectVersion) {
        ProjectDeployment targetProjectDeployment = new ProjectDeployment();

        targetProjectDeployment.setId(TARGET_PROJECT_DEPLOYMENT_ID);
        targetProjectDeployment.setProjectId(PROJECT_ID);
        targetProjectDeployment.setProjectVersion(targetProjectVersion);
        targetProjectDeployment.setEnvironment(Environment.STAGING);

        when(projectDeploymentService.getProjectDeployment(TARGET_PROJECT_DEPLOYMENT_ID))
            .thenReturn(targetProjectDeployment);
    }

    /**
     * What {@code ApiCollectionFacade#createApiCollection} hands back: the freshly minted TARGET collection, whose id
     * the handler re-reads as an entity.
     */
    private static ApiCollectionDTO createdApiCollectionDTO() {
        ProjectDeployment targetProjectDeployment = new ProjectDeployment();

        targetProjectDeployment.setId(TARGET_PROJECT_DEPLOYMENT_ID);
        targetProjectDeployment.setProjectId(PROJECT_ID);
        targetProjectDeployment.setProjectVersion(3);
        targetProjectDeployment.setEnvironment(Environment.STAGING);

        return new ApiCollectionDTO(
            2, "billing-v2", null, null, "Billing API", false, List.of(), Environment.STAGING, TARGET_ID, null, null,
            "billing", null, PROJECT_ID, targetProjectDeployment, TARGET_PROJECT_DEPLOYMENT_ID, 3, List.of(), 0,
            COLLECTION_UUID);
    }

    private static Connection connection() {
        Connection connection = new Connection();

        connection.setId(SOURCE_CONNECTION_ID);
        connection.setName("Sheets");
        connection.setComponentName("googleSheets");
        connection.setConnectionVersion(1);

        return connection;
    }

    private static Project project() {
        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setName("Billing");
        project.setWorkspaceId(WORKSPACE_ID);

        return project;
    }

    private static ApiCollectionDTO sourceApiCollectionDTO(List<ApiCollectionEndpointDTO> endpoints) {
        ProjectDeployment sourceProjectDeployment = new ProjectDeployment();

        sourceProjectDeployment.setId(SOURCE_PROJECT_DEPLOYMENT_ID);
        sourceProjectDeployment.setProjectId(PROJECT_ID);
        sourceProjectDeployment.setProjectVersion(3);
        sourceProjectDeployment.setEnvironment(Environment.DEVELOPMENT);

        return new ApiCollectionDTO(
            2, "billing-v2", null, null, "Billing API", true, endpoints, Environment.DEVELOPMENT, SOURCE_ID, null,
            null, "billing", null, PROJECT_ID, sourceProjectDeployment, SOURCE_PROJECT_DEPLOYMENT_ID, 3,
            List.of(new Tag(SOURCE_TAG_ID, "source-tag")), 0, COLLECTION_UUID);
    }

    private static ApiCollectionEndpointDTO sourceEndpointDTO(
        HttpMethod httpMethod, String path, String name, boolean enabled) {

        return new ApiCollectionEndpointDTO(
            SOURCE_ID, null, null, enabled, httpMethod, 700L, null, null, name, path, 800L, 0, WORKFLOW_UUID);
    }

    private static ApiCollection targetApiCollection(String name, String contextPath, int collectionVersion) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setId(TARGET_ID);
        apiCollection.setName(name);
        apiCollection.setContextPath(contextPath);
        apiCollection.setCollectionVersion(collectionVersion);
        apiCollection.setProjectDeploymentId(TARGET_PROJECT_DEPLOYMENT_ID);
        apiCollection.setUuid(UUID.fromString(COLLECTION_UUID));

        // Deliberately different from the source's, so the update branch can pin that they survive promotion.
        apiCollection.setDescription("Target-owned description");
        apiCollection.setTagIds(List.of(TARGET_TAG_ID));

        return apiCollection;
    }

    private static ApiCollectionEndpoint targetEndpoint(
        long id, HttpMethod httpMethod, String path, String name, long projectDeploymentWorkflowId) {

        ApiCollectionEndpoint apiCollectionEndpoint = new ApiCollectionEndpoint();

        apiCollectionEndpoint.setApiCollectionId(TARGET_ID);
        apiCollectionEndpoint.setId(id);
        apiCollectionEndpoint.setHttpMethod(httpMethod);
        apiCollectionEndpoint.setName(name);
        apiCollectionEndpoint.setPath(path);
        apiCollectionEndpoint.setProjectDeploymentWorkflowId(projectDeploymentWorkflowId);

        return apiCollectionEndpoint;
    }
}
