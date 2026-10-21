/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.async.config.AsyncConfiguration;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.dto.AppActionDTO;
import com.bytechef.ee.automation.app.dto.AppBindingDTO;
import com.bytechef.ee.automation.app.dto.AppToolDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.proxy.AppProxyExecutor;
import com.bytechef.ee.automation.app.proxy.BodyEncoding;
import com.bytechef.ee.automation.app.proxy.ProxyRequest;
import com.bytechef.ee.automation.app.proxy.ProxyResponse;
import com.bytechef.ee.automation.app.proxy.ProxyStorageException;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.security.AppPrincipal;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.util.ActionInputSchemaGenerator;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import com.bytechef.platform.tool.execution.ToolExecutionKind;
import com.bytechef.platform.tool.execution.ToolExecutionOutcome;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.tool.execution.ToolExecutionRecorderImpl;
import com.bytechef.platform.tool.execution.ToolExecutionSurface;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * The app, its bindings and the connection rows are real; the platform component facades and services, the connection
 * and workspace-connection services, the tool execution recorder and the proxy executor's upstream call are the stubbed
 * seams.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppExecutionFacadeIntTest {

    private static final Duration EXECUTION_TIMEOUT = Duration.ofSeconds(2);
    private static final String LANE_TENANT_ID = "tenant1";
    private static final String TENANT_ID = "public";
    private static final long WORKSPACE_ID = 1L;

    @Autowired
    private ActionDefinitionFacade actionDefinitionFacade;

    @Autowired
    private AppConnectionService appConnectionService;

    @Autowired
    private AppExecutionFacade appExecutionFacade;

    @Autowired
    private ApplicationProperties applicationProperties;

    @MockitoSpyBean
    private AppProxyExecutor appProxyExecutor;

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private AppService appService;

    @Autowired
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Autowired
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ToolExecutionRecorder toolExecutionRecorder;

    @Autowired
    private WorkspaceConnectionService workspaceConnectionService;

    private volatile long appId;
    private AppConnection binding;
    private Connection connection;
    private Long connectionId;
    private final List<WorkspaceConnection> workspaceConnections = new ArrayList<>();

    @BeforeEach
    void beforeEach() {

        when(toolExecutionRecorder.record(any(ToolExecutionEvent.Builder.class), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);

            return supplier.get();
        });

        when(toolExecutionRecorder.recordWithErrorMessage(any(ToolExecutionEvent.Builder.class), any(), any()))
            .thenAnswer(
                invocation -> {
                    Supplier<?> supplier = invocation.getArgument(1);

                    return supplier.get();
                });

        appId = Objects.requireNonNull(
            TenantContext.callWithTenantId(TENANT_ID, () -> appService.create(newApp()))
                .app()
                .getId());
        connectionId = insertConnection("salesforce");
        connection = mockConnection(connectionId, "salesforce", 1);

        workspaceConnections.clear();
        workspaceConnections.add(new WorkspaceConnection(connectionId, WORKSPACE_ID));

        when(workspaceConnectionService.getWorkspaceConnections(WORKSPACE_ID)).thenAnswer(
            invocation -> List.copyOf(workspaceConnections));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getVersion()).thenReturn(3);
        when(componentDefinitionService.getComponentDefinition("salesforce", null)).thenReturn(componentDefinition);
        when(componentDefinitionService.fetchComponentDefinition("salesforce", null))
            .thenReturn(Optional.of(componentDefinition));

        binding = bindCrm(List.of("createRecord", "updateRecord"), List.of("searchRecords"), false, List.of());

        SecurityContextHolder.getContext()
            .setAuthentication(
                new TestingAuthenticationToken(
                    new AppPrincipal(TENANT_ID, appId, WORKSPACE_ID, Environment.PRODUCTION), null));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        applicationProperties.getApp()
            .getExecution()
            .setTimeout(EXECUTION_TIMEOUT);

        appRepository.deleteAll();

        jdbcTemplate.update("DELETE FROM connection");

        Mockito.reset(
            actionDefinitionFacade, clusterElementDefinitionFacade, clusterElementDefinitionService,
            componentDefinitionService, connectionService, toolExecutionRecorder, workspaceConnectionService);
    }

    @Test
    void testComponentCallsAreSubmittedToTheAppExecutor() {
        Constructor<?>[] constructors = AppExecutionFacadeImpl.class.getConstructors();

        assertThat(constructors).hasSize(1);

        List<String> executorQualifiers = Arrays.stream(constructors[0].getParameters())
            .filter(parameter -> AsyncTaskExecutor.class.equals(parameter.getType()))
            .map(parameter -> parameter.getAnnotation(Qualifier.class))
            .filter(Objects::nonNull)
            .map(Qualifier::value)
            .toList();

        assertThat(executorQualifiers).containsExactly(AsyncConfiguration.APP_EXECUTOR);
    }

    @Test
    void testAllowedActionReachesThePlatformFacadeWithTheBoundConnectionAndTheTenant() {
        AtomicReference<String> tenantOnLane = new AtomicReference<>();
        AtomicReference<String> loginOnLane = new AtomicReference<>();

        when(
            actionDefinitionFacade.executePerform(
                eq("salesforce"), eq(3), eq("createRecord"), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), anyBoolean(), any(), any(), any()))
                    .thenAnswer(invocation -> {
                        tenantOnLane.set(TenantContext.getCurrentTenantId());
                        loginOnLane.set(SecurityUtils.getCurrentUserLogin());

                        return Map.of("id", "001");
                    });

        Object result;

        TenantContext.setCurrentTenantId(LANE_TENANT_ID);

        try {
            result = appExecutionFacade.executeAction("crm", "createRecord", null, Map.of("name", "ACME"));
        } finally {
            TenantContext.resetCurrentTenantId();
        }

        assertThat(result).isEqualTo(Map.of("id", "001"));
        assertThat(tenantOnLane.get()).isEqualTo(LANE_TENANT_ID);
        assertThat(loginOnLane.get()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);

        verify(actionDefinitionFacade).executePerform(
            eq("salesforce"), eq(3), eq("createRecord"), any(), any(), any(), any(), any(), eq(Map.of("name", "ACME")),
            eq(Map.of("salesforce", connectionId)), eq(Map.of()), eq((long) Environment.PRODUCTION.ordinal()),
            eq(PlatformType.AUTOMATION), eq(false), any(), any(), any());
    }

    @Test
    void testAllowedActionExecutesWithTheBoundConnectionAndRecords() {
        when(
            actionDefinitionFacade.executePerform(
                eq("salesforce"), eq(3), eq("createRecord"), isNull(), isNull(), isNull(), isNull(), isNull(),
                anyMap(), eq(Map.of("salesforce", connectionId)), anyMap(),
                eq((long) Environment.PRODUCTION.ordinal()), eq(PlatformType.AUTOMATION), eq(false), isNull(),
                isNull(), isNull()))
                    .thenReturn(Map.of("id", "001"));

        Object result = appExecutionFacade.executeAction("crm", "createRecord", null, Map.of("name", "ACME"));

        assertThat(result).isEqualTo(Map.of("id", "001"));

        ArgumentCaptor<ToolExecutionEvent.Builder> captor = ArgumentCaptor.forClass(ToolExecutionEvent.Builder.class);

        verify(toolExecutionRecorder).record(captor.capture(), any());

        ToolExecutionEvent event = captor.getValue()
            .build();

        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.AUTOMATION_APP_ACTION);
        assertThat(event.kind()).isEqualTo(ToolExecutionKind.COMPONENT);
        assertThat(event.type()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(event.appId()).isEqualTo(appId);
        assertThat(event.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(event.connectionId()).isEqualTo(connectionId);
        assertThat(event.componentName()).isEqualTo("salesforce");
        assertThat(event.componentVersion()).isEqualTo(3);
        assertThat(event.operationName()).isEqualTo("createRecord");
        assertThat(event.environment()).isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    void testExplicitComponentVersionIsUsed() {
        when(
            actionDefinitionFacade.executePerform(
                eq("salesforce"), eq(2), eq("createRecord"), any(), any(), any(), any(), any(), anyMap(), anyMap(),
                anyMap(), any(), any(), anyBoolean(), any(), any(), any()))
                    .thenReturn("ok");

        assertThat(appExecutionFacade.executeAction("crm", "createRecord", 2, Map.of())).isEqualTo("ok");
    }

    @Test
    void testActionRunsOffTheRequestThreadOnTheAppLane() {
        AtomicReference<Thread> executionThread = new AtomicReference<>();

        when(
            actionDefinitionFacade.executePerform(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), any()))
                    .thenAnswer(invocation -> {
                        executionThread.set(Thread.currentThread());

                        return "ok";
                    });

        appExecutionFacade.executeAction("crm", "createRecord", null, Map.of());

        assertThat(executionThread.get()).isNotNull()
            .isNotSameAs(Thread.currentThread());
        assertThat(executionThread.get()
            .getName()).startsWith("app-");
    }

    @Test
    void testActionOutsideTheAllowListIsRefused() {
        assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "deleteRecord", null, Map.of()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("allowed actions: createRecord")
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_OPERATION_NOT_ALLOWED.getErrorKey());

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testActionNotOnAllowListIsRefusedWithAllowedOperationsListed() {
        assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "deleteRecord", null, Map.of()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("'deleteRecord' is not allowed on binding 'crm'; allowed actions: createRecord, updateRecord");

        assertConfigurationError(
            () -> appExecutionFacade.executeAction("crm", "deleteRecord", null, Map.of()),
            AppErrorType.APP_OPERATION_NOT_ALLOWED);

        verifyNoInteractions(actionDefinitionFacade);
        verifyNoInteractions(toolExecutionRecorder);
    }

    @Test
    void testToolNotOnAllowListIsRefusedWithAllowedOperationsListed() {
        assertThatThrownBy(() -> appExecutionFacade.executeTool("crm", "deleteRecords", Map.of()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("'deleteRecords' is not allowed on binding 'crm'; allowed tools: searchRecords");

        verifyNoInteractions(clusterElementDefinitionFacade);
    }

    @Test
    void testRefusalListsNoneWhenNothingIsAllowed() {
        bindCrm(List.of(), List.of(), false, List.of());

        assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()))
            .hasMessage("'createRecord' is not allowed on binding 'crm'; allowed actions: none");
    }

    @Test
    void testActionAllowListDoesNotAuthorizeATool() {
        assertConfigurationError(
            () -> appExecutionFacade.executeTool("crm", "createRecord", Map.of()),
            AppErrorType.APP_OPERATION_NOT_ALLOWED);
    }

    @Test
    void testUnknownBindingIsNotFound() {
        assertConfigurationError(
            () -> appExecutionFacade.executeAction("hr", "createRecord", null, Map.of()),
            AppErrorType.APP_CONNECTION_NOT_FOUND);
    }

    @Test
    void testConnectionMovedOutOfWorkspaceIsUnavailable() {
        workspaceConnections.clear();

        assertConfigurationError(
            () -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);
    }

    @Test
    void testDeletedConnectionIsUnavailable() {
        when(connectionService.fetchConnection(connectionId)).thenReturn(Optional.empty());

        assertConfigurationError(
            () -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);
    }

    @Test
    void testConnectionInAnotherEnvironmentIsUnavailable() {
        when(connection.getEnvironmentId()).thenReturn(Environment.DEVELOPMENT.ordinal());

        assertConfigurationError(
            () -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);
    }

    @Test
    void testInactiveConnectionIsUnavailableForActionsAndTools() {
        when(connection.getStatus()).thenReturn(ConnectionStatus.REVOKED);

        assertConfigurationError(
            () -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);
        assertConfigurationError(
            () -> appExecutionFacade.executeTool("crm", "searchRecords", Map.of()),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);

        verifyNoInteractions(clusterElementDefinitionFacade);
    }

    @Test
    void testDisablingTheAppStopsExecutionForAnAlreadyAuthenticatedPrincipal() {
        disableApp();

        assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()))
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_DISABLED.getErrorKey());

        verifyNoInteractions(actionDefinitionFacade);
    }

    @Test
    void testDisabledAppIsRefusedEvenWithValidPrincipal() {
        disableApp();

        assertConfigurationError(
            () -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()), AppErrorType.APP_DISABLED);
        assertConfigurationError(() -> appExecutionFacade.getBindings(), AppErrorType.APP_DISABLED);
        assertConfigurationError(() -> appExecutionFacade.getTools(), AppErrorType.APP_DISABLED);
    }

    @Test
    void testNonAppPrincipalIsDenied() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testSlowActionIsCutOffWithTimeout() throws InterruptedException {
        applicationProperties.getApp()
            .getExecution()
            .setTimeout(Duration.ofMillis(200));

        CountDownLatch interrupted = new CountDownLatch(1);

        when(
            actionDefinitionFacade.executePerform(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), any()))
                    .thenAnswer(invocation -> {
                        try {
                            Thread.sleep(5_000);
                        } catch (InterruptedException interruptedException) {
                            interrupted.countDown();

                            throw interruptedException;
                        }

                        return null;
                    });

        assertConfigurationError(
            () -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()),
            AppErrorType.APP_EXECUTION_TIMEOUT);

        assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testSaturatedLaneRefusesAnEleventhCallWithinTheTimeout() throws Exception {
        applicationProperties.getApp()
            .getExecution()
            .setTimeout(Duration.ofSeconds(10));

        CountDownLatch started = new CountDownLatch(10);
        CountDownLatch release = new CountDownLatch(1);

        when(
            actionDefinitionFacade.executePerform(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), any()))
                    .thenAnswer(invocation -> {
                        started.countDown();

                        return release.await(10, TimeUnit.SECONDS);
                    });

        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();

        List<Future<Object>> runningCalls = new ArrayList<>();

        try (ExecutorService callers = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < 10; index++) {
                runningCalls.add(callers.submit(() -> {
                    SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                    return appExecutionFacade.executeAction("crm", "createRecord", null, Map.of());
                }));
            }

            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

            applicationProperties.getApp()
                .getExecution()
                .setTimeout(Duration.ofMillis(300));

            long start = System.nanoTime();

            assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()))
                .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                    assertThat(exception.getErrorKey()).isEqualTo(AppErrorType.APP_EXECUTION_TIMEOUT.getErrorKey());
                    assertThat(exception.getMessage()).startsWith("No app execution slot became free");
                    assertThat(exception.getCause()).isInstanceOf(TimeoutException.class);
                });

            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));

            verify(actionDefinitionFacade, times(10)).executePerform(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), any());

            release.countDown();

            for (Future<Object> runningCall : runningCalls) {
                assertThat(runningCall.get(5, TimeUnit.SECONDS)).isEqualTo(true);
            }
        } finally {
            release.countDown();
        }

        assertThat(appExecutionFacade.executeAction("crm", "createRecord", null, Map.of())).isEqualTo(true);
    }

    @Test
    void testFailureOfTheActionPropagatesUnchanged() {
        ConfigurationException failure = new ConfigurationException("boom", AppErrorType.APP_NOT_FOUND);

        when(
            actionDefinitionFacade.executePerform(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), any()))
                    .thenThrow(failure);

        assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "createRecord", 99, Map.of()))
            .isSameAs(failure);
    }

    @Test
    void testAllowedToolExecutesWithTheBoundConnectionAndRecords() {
        when(
            clusterElementDefinitionFacade.executeTool(
                "salesforce", 3, "searchRecords", Map.of("q", "x"), connectionId))
                    .thenReturn(List.of("hit"));

        Object result = appExecutionFacade.executeTool("crm", "searchRecords", Map.of("q", "x"));

        assertThat(result).isEqualTo(List.of("hit"));

        ArgumentCaptor<ToolExecutionEvent.Builder> captor = ArgumentCaptor.forClass(ToolExecutionEvent.Builder.class);

        verify(toolExecutionRecorder).record(captor.capture(), any());

        ToolExecutionEvent event = captor.getValue()
            .build();

        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.AUTOMATION_APP_TOOL);
        assertThat(event.toolName()).isEqualTo("CRM__SEARCH_RECORDS");
        assertThat(event.operationName()).isEqualTo("searchRecords");
        assertThat(event.appId()).isEqualTo(appId);
        assertThat(event.connectionId()).isEqualTo(connectionId);
        assertThat(event.componentVersion()).isEqualTo(3);
    }

    @Test
    void testGetBindingsMapsEveryBinding() {
        when(connection.getName()).thenReturn("Salesforce prod");

        List<AppBindingDTO> bindings = appExecutionFacade.getBindings();

        assertThat(bindings).hasSize(1);
        assertThat(bindings.getFirst()
            .bindingKey()).isEqualTo("crm");
        assertThat(bindings.getFirst()
            .connectionName()).isEqualTo("Salesforce prod");
        assertThat(bindings.getFirst()
            .actionNames()).containsExactly("createRecord", "updateRecord");
        assertThat(bindings.getFirst()
            .connectionVersion()).isEqualTo(1);
        assertThat(bindings.getFirst()
            .componentVersion()).isEqualTo(3);
    }

    @Test
    void testGetBindingsMapsABindingToAnUnresolvableComponentWithNoComponentVersion() {
        bindOrphaned("legacy", List.of("createRecord"), List.of("searchRecords"));

        List<AppBindingDTO> bindings = appExecutionFacade.getBindings();

        assertThat(bindings).extracting(AppBindingDTO::bindingKey)
            .containsExactly("crm", "legacy");
        assertThat(bindings).extracting(AppBindingDTO::componentVersion)
            .containsExactly(3, null);
    }

    @Test
    void testToolsListLeavesOutABindingToAnUnresolvableComponent() {
        bindOrphaned("legacy", List.of(), List.of("searchRecords"));

        ClusterElementDefinition search = mock(ClusterElementDefinition.class);

        when(search.getName()).thenReturn("searchRecords");
        when(search.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinitions("salesforce", 3, BaseToolFunction.TOOLS))
            .thenReturn(List.of(search));

        assertThat(appExecutionFacade.getTools()).extracting(AppToolDTO::name)
            .containsExactly("CRM__SEARCH_RECORDS");

        verify(clusterElementDefinitionService, never())
            .getClusterElementDefinitions(eq("deleted-component"), anyInt(), any());
    }

    @Test
    void testToolsListUsesBindingScopedFlatNamesAndOnlyAllowedTools() {
        ClusterElementDefinition search = mock(ClusterElementDefinition.class);

        when(search.getName()).thenReturn("searchRecords");
        when(search.getDescription()).thenReturn("Search");
        when(search.getProperties()).thenReturn(List.of());

        ClusterElementDefinition delete = mock(ClusterElementDefinition.class);

        when(delete.getName()).thenReturn("deleteRecords");
        when(clusterElementDefinitionService.getClusterElementDefinitions("salesforce", 3, BaseToolFunction.TOOLS))
            .thenReturn(List.of(search, delete));

        List<AppToolDTO> tools = appExecutionFacade.getTools();

        assertThat(tools).extracting(AppToolDTO::name)
            .containsExactly("CRM__SEARCH_RECORDS");
        assertThat(tools.getFirst()
            .toolName()).isEqualTo("searchRecords");
        assertThat(tools.getFirst()
            .bindingKey()).isEqualTo("crm");
        assertThat(tools.getFirst()
            .componentName()).isEqualTo("salesforce");
        assertThat(tools.getFirst()
            .parameters()).isEqualTo(JsonSchemaGeneratorUtils.generateInputSchema(List.of()));
    }

    @Test
    void testActionsListUsesOnlyAllowedActionsAndGeneratesSchemas() {
        ActionDefinition create = new ActionDefinition(
            ComponentDsl.action("createRecord")
                .description("Creates a record")
                .properties(string("name").required(true), integer("age")),
            "salesforce", 3);
        ActionDefinition delete = new ActionDefinition(ComponentDsl.action("deleteRecord"), "salesforce", 3);
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getActions()).thenReturn(List.of(create, delete));
        when(componentDefinitionService.fetchComponentDefinition("salesforce", null))
            .thenReturn(Optional.of(componentDefinition));

        List<AppActionDTO> actions = appExecutionFacade.getActions();

        assertThat(actions).extracting(AppActionDTO::actionName)
            .containsExactly("createRecord");
        assertThat(actions.getFirst()
            .bindingKey()).isEqualTo("crm");
        assertThat(actions.getFirst()
            .componentName()).isEqualTo("salesforce");
        assertThat(actions.getFirst()
            .description()).isEqualTo("Creates a record");
        assertThat(actions.getFirst()
            .inputSchema())
                .isEqualTo(
                    ActionInputSchemaGenerator.generateInputSchema(
                        List.of(
                            Property.toProperty(string("name").required(true)),
                            Property.toProperty(integer("age")))));
        assertThat(actions.getFirst()
            .inputSchema()
            .get("required")).isEqualTo(List.of("name"));
    }

    @Test
    void testActionsListLeavesOutABindingToAnUnresolvableComponent() {
        when(componentDefinitionService.fetchComponentDefinition("salesforce", null)).thenReturn(Optional.empty());

        assertThat(appExecutionFacade.getActions()).isEmpty();
    }

    @Test
    void testActionsListLeavesOutAnAllowedNameMissingFromTheLatestVersion() {
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getActions()).thenReturn(List.of());
        when(componentDefinitionService.fetchComponentDefinition("salesforce", null))
            .thenReturn(Optional.of(componentDefinition));

        assertThat(appExecutionFacade.getActions()).isEmpty();
    }

    @Test
    void testActionWhoseSchemaFailsIsListedWithAnOpenObjectSchema() {
        ActionDefinition create = new ActionDefinition(ComponentDsl.action("createRecord"), "salesforce", 3);
        ActionDefinition broken = mock(ActionDefinition.class);

        when(broken.getName()).thenReturn("updateRecord");
        when(broken.getDescription()).thenReturn("Updates a record");
        when(broken.getProperties()).thenThrow(new IllegalStateException("unsupported property"));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getActions()).thenReturn(List.of(create, broken));
        when(componentDefinitionService.fetchComponentDefinition("salesforce", null))
            .thenReturn(Optional.of(componentDefinition));

        List<AppActionDTO> actions = appExecutionFacade.getActions();

        assertThat(actions).extracting(AppActionDTO::actionName)
            .containsExactly("createRecord", "updateRecord");
        assertThat(actions.get(1)
            .inputSchema()).isEqualTo(Map.of("type", "object"));
        assertThat(actions.get(1)
            .description()).isEqualTo("Updates a record");
    }

    @Test
    void testToolWhoseSchemaFailsIsListedWithAnOpenObjectSchema() {
        ClusterElementDefinition search = mock(ClusterElementDefinition.class);

        when(search.getName()).thenReturn("searchRecords");
        when(search.getProperties()).thenThrow(new IllegalStateException("unsupported property"));
        when(clusterElementDefinitionService.getClusterElementDefinitions("salesforce", 3, BaseToolFunction.TOOLS))
            .thenReturn(List.of(search));

        List<AppToolDTO> tools = appExecutionFacade.getTools();

        assertThat(tools).extracting(AppToolDTO::name)
            .containsExactly("CRM__SEARCH_RECORDS");
        assertThat(tools.getFirst()
            .parameters()).isEqualTo("{\"type\":\"object\"}");
    }

    @Test
    void testActionsListOfADisabledAppIsRefused() {
        disableApp();

        assertConfigurationError(() -> appExecutionFacade.getActions(), AppErrorType.APP_DISABLED);
    }

    @Test
    void testBindingWithoutActionsContributesNoActions() {
        bindCrm(List.of(), List.of("searchRecords"), false, List.of());

        assertThat(appExecutionFacade.getActions()).isEmpty();

        verify(componentDefinitionService, never()).fetchComponentDefinition(any(), any());
    }

    @Test
    void testBindingWithoutToolsContributesNoTools() {
        bindCrm(List.of("createRecord"), List.of(), false, List.of());

        assertThat(appExecutionFacade.getTools()).isEmpty();

        verifyNoInteractions(clusterElementDefinitionService);
    }

    @Test
    void testTwoBindingsOnOneComponentYieldDistinctFlatNames() {
        appConnectionService.bind(appId, "support-crm", connectionId, List.of(), List.of("searchRecords"), false,
            List.of());

        ClusterElementDefinition search = mock(ClusterElementDefinition.class);

        when(search.getName()).thenReturn("searchRecords");
        when(search.getProperties()).thenReturn(List.of());
        when(clusterElementDefinitionService.getClusterElementDefinitions("salesforce", 3, BaseToolFunction.TOOLS))
            .thenReturn(List.of(search));

        assertThat(appExecutionFacade.getTools()).extracting(AppToolDTO::name)
            .containsExactly("CRM__SEARCH_RECORDS", "SUPPORT_CRM__SEARCH_RECORDS");
    }

    @Test
    void testActionRunsAsSystemOnTheLaneAndTheRequestContextIsUntouched() {
        AtomicReference<String> loginOnLane = new AtomicReference<>();

        when(
            actionDefinitionFacade.executePerform(
                any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), any()))
                    .thenAnswer(invocation -> {
                        loginOnLane.set(SecurityUtils.getCurrentUserLogin());

                        return "ok";
                    });

        appExecutionFacade.executeAction("crm", "createRecord", null, Map.of());

        assertThat(loginOnLane.get()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);
        assertThat(AppPrincipal.current()).isPresent();
    }

    @Test
    void testToolRunsAsSystemOnTheLaneAndTheRequestContextIsUntouched() {
        AtomicReference<String> loginOnLane = new AtomicReference<>();

        when(clusterElementDefinitionFacade.executeTool(any(), anyInt(), any(), any(), any()))
            .thenAnswer(invocation -> {
                loginOnLane.set(SecurityUtils.getCurrentUserLogin());

                return "ok";
            });

        appExecutionFacade.executeTool("crm", "searchRecords", Map.of());

        assertThat(loginOnLane.get()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);
        assertThat(AppPrincipal.current()).isPresent();
    }

    @Test
    void testMissingOrNonPositiveTimeoutFailsBeforeTheComponentRuns() {
        ApplicationProperties.App.Execution execution = applicationProperties.getApp()
            .getExecution();

        execution.setTimeout(null);

        assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> appExecutionFacade.executeTool("crm", "searchRecords", Map.of()))
            .isInstanceOf(IllegalStateException.class);

        execution.setTimeout(Duration.ZERO);

        assertThatThrownBy(() -> appExecutionFacade.executeAction("crm", "createRecord", null, Map.of()))
            .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(actionDefinitionFacade);
        verifyNoInteractions(clusterElementDefinitionFacade);
    }

    @Test
    void testProxyRecordsMethodAndPathWithoutQuery() {
        binding = bindCrm(List.of("createRecord", "updateRecord"), List.of("searchRecords"), true, List.of("GET"));

        ProxyRequest request = new ProxyRequest(
            "GET", "/items", Map.of("q", List.of("secret")), Map.of("X-Token", List.of("secret")), null,
            BodyEncoding.UTF8, null);
        ProxyResponse response = new ProxyResponse(200, Map.of(), "{}", BodyEncoding.UTF8);

        doReturn(response).when(appProxyExecutor)
            .execute(binding, connection, request);

        assertThat(appExecutionFacade.proxy("crm", request)).isSameAs(response);

        ArgumentCaptor<ToolExecutionEvent.Builder> captor = ArgumentCaptor.forClass(ToolExecutionEvent.Builder.class);

        verify(toolExecutionRecorder).recordWithErrorMessage(captor.capture(), any(), any());

        ToolExecutionEvent event = captor.getValue()
            .build();

        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.AUTOMATION_APP_PROXY);
        assertThat(event.kind()).isEqualTo(ToolExecutionKind.HTTP_PROXY);
        assertThat(event.operationName()).isEqualTo("GET /items");
        assertThat(event.toolName()).isEqualTo("GET /items");
        assertThat(event.toString()).doesNotContain("secret");
        assertThat(event.type()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(event.appId()).isEqualTo(appId);
        assertThat(event.workspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(event.connectionId()).isEqualTo(connectionId);
        assertThat(event.componentName()).isEqualTo("salesforce");
        assertThat(event.environment()).isEqualTo(Environment.PRODUCTION.ordinal());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProxyOnABindingWithoutProxyEnabledIsRefusedByTheExecutorAndRecordedAsAnError() {
        List<Object> publishedEvents = new ArrayList<>();

        ToolExecutionRecorderImpl realRecorder = new ToolExecutionRecorderImpl(
            publishedEvents::add, mock(ObjectProvider.class));

        when(toolExecutionRecorder.recordWithErrorMessage(any(ToolExecutionEvent.Builder.class), any(), any()))
            .thenAnswer(
                invocation -> realRecorder.recordWithErrorMessage(invocation.<ToolExecutionEvent.Builder>getArgument(0),
                    invocation.<Supplier<Object>>getArgument(1), invocation.<Function<Object, String>>getArgument(2)));

        assertThatThrownBy(
            () -> appExecutionFacade.proxy(
                "crm", new ProxyRequest("POST", "/items", Map.of(), Map.of(), null, BodyEncoding.UTF8, null)))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessage("POST is not allowed on binding 'crm'; allowed proxy methods: none")
                    .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AppErrorType.APP_OPERATION_NOT_ALLOWED.getErrorKey());

        assertThat(publishedEvents).hasSize(1);

        ToolExecutionEvent event = (ToolExecutionEvent) publishedEvents.getFirst();

        assertThat(event.surface()).isEqualTo(ToolExecutionSurface.AUTOMATION_APP_PROXY);
        assertThat(event.kind()).isEqualTo(ToolExecutionKind.HTTP_PROXY);
        assertThat(event.toolName()).isEqualTo("POST /items");
        assertThat(event.operationName()).isEqualTo("POST /items");
        assertThat(event.appId()).isNotNull();
        assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.ERROR);
        assertThat(event.errorType()).isEqualTo("ConfigurationException");
        assertThat(event.errorMessage()).isEqualTo("POST is not allowed on binding 'crm'; allowed proxy methods: none");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProxyUpstreamErrorStatusIsReturnedAndRecordedAsAnError() {
        List<Object> publishedEvents = new ArrayList<>();

        ToolExecutionRecorderImpl realRecorder = new ToolExecutionRecorderImpl(
            publishedEvents::add, mock(ObjectProvider.class));

        when(toolExecutionRecorder.recordWithErrorMessage(any(ToolExecutionEvent.Builder.class), any(), any()))
            .thenAnswer(
                invocation -> realRecorder.recordWithErrorMessage(
                    invocation.<ToolExecutionEvent.Builder>getArgument(0), invocation.<Supplier<Object>>getArgument(1),
                    invocation.<Function<Object, String>>getArgument(2)));

        for (int status : List.of(302, 404, 503)) {
            publishedEvents.clear();

            ProxyResponse response = new ProxyResponse(status, Map.of(), "{\"error\":\"x\"}", BodyEncoding.UTF8);

            doReturn(response).when(appProxyExecutor)
                .execute(any(), any(), any());

            assertThat(appExecutionFacade.proxy("crm", proxyRequest("GET", "/items"))).isSameAs(response);

            assertThat(publishedEvents).hasSize(1);

            ToolExecutionEvent event = (ToolExecutionEvent) publishedEvents.getFirst();

            if (status < 400) {
                assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.SUCCESS);
                assertThat(event.errorMessage()).isNull();
            } else {
                assertThat(event.outcome()).isEqualTo(ToolExecutionOutcome.ERROR);
                assertThat(event.errorMessage()).isEqualTo("upstream " + status);
            }
        }
    }

    @Test
    void testProxyRecordingNeverCarriesAQueryStringOrFragmentInThePath() {
        ProxyRequest request = new ProxyRequest(
            "get", "/items?token=secret#frag", Map.of(), Map.of(), null, BodyEncoding.UTF8, null);

        doReturn(new ProxyResponse(200, Map.of(), null, BodyEncoding.UTF8)).when(appProxyExecutor)
            .execute(any(), any(), any());

        appExecutionFacade.proxy("crm", request);

        ArgumentCaptor<ToolExecutionEvent.Builder> captor = ArgumentCaptor.forClass(ToolExecutionEvent.Builder.class);

        verify(toolExecutionRecorder).recordWithErrorMessage(captor.capture(), any(), any());

        ToolExecutionEvent event = captor.getValue()
            .build();

        assertThat(event.operationName()).isEqualTo("GET /items");
        assertThat(event.toolName()).isEqualTo("GET /items");
    }

    @Test
    void testProxyRecordingDoesNotEchoAMalformedMethod() {
        ProxyRequest request = new ProxyRequest(
            "GET secret\r\n", "/items", Map.of(), Map.of(), null, BodyEncoding.UTF8, null);

        doThrow(new ConfigurationException("invalid", AppErrorType.APP_PROXY_REQUEST_INVALID)).when(appProxyExecutor)
            .execute(any(), any(), any());

        assertConfigurationError(
            () -> appExecutionFacade.proxy("crm", request), AppErrorType.APP_PROXY_REQUEST_INVALID);

        ArgumentCaptor<ToolExecutionEvent.Builder> captor = ArgumentCaptor.forClass(ToolExecutionEvent.Builder.class);

        verify(toolExecutionRecorder).recordWithErrorMessage(captor.capture(), any(), any());

        ToolExecutionEvent event = captor.getValue()
            .build();

        assertThat(event.toolName()).doesNotContain("secret");
        assertThat(event.operationName()).doesNotContain("secret");
    }

    @Test
    void testProxyLeavesTheMethodCheckToTheExecutor() {
        ConfigurationException refusal = new ConfigurationException(
            "POST is not allowed on binding 'crm'; allowed proxy methods: none",
            AppErrorType.APP_OPERATION_NOT_ALLOWED);

        doThrow(refusal).when(appProxyExecutor)
            .execute(any(), any(), any());

        assertThatThrownBy(() -> appExecutionFacade.proxy("crm", proxyRequest("POST", "/items"))).isSameAs(refusal);

        verify(appProxyExecutor).execute(eq(binding), eq(connection), any(ProxyRequest.class));
    }

    @Test
    void testProxyRunsAsSystemOnTheLaneAndTheRequestContextIsUntouched() {
        AtomicReference<Thread> laneThread = new AtomicReference<>();
        AtomicReference<String> laneLogin = new AtomicReference<>();

        doAnswer(invocation -> {
            laneThread.set(Thread.currentThread());
            laneLogin.set(SecurityUtils.getCurrentUserLogin());

            return new ProxyResponse(204, Map.of(), null, BodyEncoding.UTF8);
        }).when(appProxyExecutor)
            .execute(any(), any(), any());

        appExecutionFacade.proxy("crm", proxyRequest("GET", "/items"));

        assertThat(laneThread.get()).isNotNull()
            .isNotSameAs(Thread.currentThread());
        assertThat(laneLogin.get()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);

        Authentication authentication = Objects.requireNonNull(
            SecurityContextHolder.getContext()
                .getAuthentication());

        assertThat(authentication.getPrincipal()).isInstanceOf(AppPrincipal.class);
    }

    @Test
    void testProxyUnknownBindingIsNotFound() {
        assertConfigurationError(
            () -> appExecutionFacade.proxy("unknown", proxyRequest("GET", "/items")),
            AppErrorType.APP_CONNECTION_NOT_FOUND);

        verifyNoInteractions(appProxyExecutor);
        verifyNoInteractions(toolExecutionRecorder);
    }

    @Test
    void testProxyOnAnUnavailableConnectionIsRefusedBeforeTheExecutor() {
        when(connection.getStatus()).thenReturn(ConnectionStatus.REVOKED);

        assertConfigurationError(
            () -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items")),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);

        verifyNoInteractions(appProxyExecutor);
    }

    @Test
    void testProxyOnAConnectionOutsideTheWorkspaceIsRefusedBeforeTheExecutor() {
        workspaceConnections.clear();

        assertConfigurationError(
            () -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items")),
            AppErrorType.APP_CONNECTION_UNAVAILABLE);

        verifyNoInteractions(appProxyExecutor);
    }

    @Test
    void testProxyOnADisabledAppIsRefused() {
        disableApp();

        assertConfigurationError(
            () -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items")), AppErrorType.APP_DISABLED);

        verifyNoInteractions(appProxyExecutor);
    }

    @Test
    void testProxyNonAppPrincipalIsDenied() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items")))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(appProxyExecutor);
    }

    @Test
    void testProxyUpstreamTransportFailureIsMappedWithAFixedMessage() {
        for (Throwable cause : List.of(
            new ConnectException("Connection refused: secret-upstream-body"),
            new UnknownHostException("secret-upstream-body"), new HttpTimeoutException("secret-upstream-body"),
            new IOException("secret-upstream-body"))) {

            doThrow(new RuntimeException("Unable to execute HTTP request", new RuntimeException(cause)))
                .when(appProxyExecutor)
                .execute(any(), any(), any());

            assertThatThrownBy(() -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("The upstream is unavailable")
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AppErrorType.APP_PROXY_UPSTREAM_UNAVAILABLE.getErrorKey());
        }
    }

    @Test
    void testProxyRequestConstructionFailureIsNoUpstreamOutage() {
        RuntimeException failure = new RuntimeException(
            "Unable to execute HTTP request", new IllegalArgumentException("invalid header name"));

        doThrow(failure).when(appProxyExecutor)
            .execute(any(), any(), any());

        assertThatThrownBy(() -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items"))).isSameAs(failure);
    }

    @Test
    void testProxyLocalStorageFailureIsReportedAsAStorageFailure() {
        ProxyStorageException failure = new ProxyStorageException(
            "Unable to read the proxied response body", new IOException("disk gone"));

        doThrow(failure).when(appProxyExecutor)
            .execute(any(), any(), any());

        assertThatThrownBy(() -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items")))
            .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                assertThat(exception.getErrorKey()).isEqualTo(AppErrorType.APP_PROXY_STORAGE_FAILED.getErrorKey());
                assertThat(exception.getCause()).isSameAs(failure);
            });
    }

    @Test
    void testProxyAppErrorsFromTheExecutorPropagateUnchanged() {
        ConfigurationException failure = new ConfigurationException(
            "Proxy request body exceeds 10 bytes", new IOException("cause"), AppErrorType.APP_PROXY_REQUEST_TOO_LARGE);

        doThrow(failure).when(appProxyExecutor)
            .execute(any(), any(), any());

        assertThatThrownBy(() -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items"))).isSameAs(failure);
    }

    @Test
    void testProxyUnrelatedFailureIsNotMistakenForAnUpstreamOutage() {
        IllegalStateException failure = new IllegalStateException("bytechef.app.proxy.timeout must be set");

        doThrow(failure).when(appProxyExecutor)
            .execute(any(), any(), any());

        assertThatThrownBy(() -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items"))).isSameAs(failure);
    }

    @Test
    void testProxyTimeoutIsReportedAsExecutionTimeoutAndInterruptsTheLane() throws InterruptedException {
        applicationProperties.getApp()
            .getExecution()
            .setTimeout(Duration.ofMillis(200));

        CountDownLatch interrupted = new CountDownLatch(1);

        doAnswer(invocation -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException interruptedException) {
                interrupted.countDown();

                throw interruptedException;
            }

            return null;
        }).when(appProxyExecutor)
            .execute(any(), any(), any());

        assertConfigurationError(
            () -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/items")), AppErrorType.APP_EXECUTION_TIMEOUT);

        assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testProxyLaneTimeoutStopsAnUpstreamThatDripsItsBodyForever() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        CountDownLatch dripStarted = new CountDownLatch(1);
        CountDownLatch dripEnded = new CountDownLatch(1);

        server.createContext("/drip", exchange -> {
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                while (true) {
                    outputStream.write('x');
                    outputStream.flush();

                    dripStarted.countDown();

                    Thread.sleep(50);
                }
            } catch (IOException | InterruptedException exception) {
                dripEnded.countDown();
            }
        });

        server.start();

        AtomicReference<Throwable> readFailure = new AtomicReference<>();
        CountDownLatch laneEnded = new CountDownLatch(1);

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress()
                .getPort() + "/drip");

            doAnswer(invocation -> {
                try {
                    HttpResponse<InputStream> httpResponse = httpClient.send(
                        HttpRequest.newBuilder(uri)
                            .build(),
                        HttpResponse.BodyHandlers.ofInputStream());

                    try (InputStream inputStream = httpResponse.body()) {
                        inputStream.transferTo(OutputStream.nullOutputStream());
                    }

                    return new ProxyResponse(200, Map.of(), null, BodyEncoding.UTF8);
                } catch (IOException ioException) {
                    readFailure.set(ioException);

                    throw new UncheckedIOException(ioException);
                } finally {
                    laneEnded.countDown();
                }
            }).when(appProxyExecutor)
                .execute(any(), any(), any());

            long start = System.nanoTime();

            assertConfigurationError(
                () -> appExecutionFacade.proxy("crm", proxyRequest("GET", "/drip")),
                AppErrorType.APP_EXECUTION_TIMEOUT);

            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
            assertThat(dripStarted.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(laneEnded.await(3, TimeUnit.SECONDS)).as("the lane thread stopped reading the drip")
                .isTrue();
            assertThat(readFailure.get()).hasRootCauseInstanceOf(InterruptedException.class);
            assertThat(dripEnded.await(3, TimeUnit.SECONDS)).as("the upstream connection was closed")
                .isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testRotatingTheSecretKeyInvalidatesTheOldKey() {
        String oldKey = TenantContext.callWithTenantId(TENANT_ID, () -> appService.rotateSecretKey(appId))
            .secretKey();

        String newKey = TenantContext.callWithTenantId(TENANT_ID, () -> appService.rotateSecretKey(appId))
            .secretKey();

        assertThat(appService.fetchAppBySecretKey(oldKey)).isEmpty();
        assertThat(appService.fetchAppBySecretKey(newKey)).isPresent();
    }

    @Test
    void testFlatToolNameIsUpperSnakeCase() {
        assertThat(AppExecutionFacadeImpl.flatToolName("sales-slack", "sendMessage"))
            .isEqualTo("SALES_SLACK__SEND_MESSAGE");
    }

    private static void assertConfigurationError(ThrowingCallable callable, AppErrorType errorType) {
        assertThatThrownBy(callable)
            .isInstanceOf(ConfigurationException.class)
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(errorType.getErrorKey());
    }

    private AppConnection bindCrm(
        List<String> actionNames, List<String> toolNames, boolean proxyEnabled, List<String> proxyAllowedMethods) {

        return appConnectionService.bind(
            appId, "crm", connectionId, actionNames, toolNames, proxyEnabled, proxyAllowedMethods);
    }

    private void bindOrphaned(String bindingKey, List<String> actionNames, List<String> toolNames) {
        long orphanedConnectionId = insertConnection("deleted-component");

        mockConnection(orphanedConnectionId, "deleted-component", 2);

        workspaceConnections.add(new WorkspaceConnection(orphanedConnectionId, WORKSPACE_ID));

        when(componentDefinitionService.fetchComponentDefinition("deleted-component", null))
            .thenReturn(Optional.empty());

        appConnectionService.bind(appId, bindingKey, orphanedConnectionId, actionNames, toolNames, false, List.of());
    }

    private void disableApp() {
        TenantContext.runWithTenantId(TENANT_ID, () -> appService.updateStatus(appId, AppStatus.DISABLED));
    }

    private long insertConnection(String componentName) {
        Long id = jdbcTemplate.queryForObject(
            """
                INSERT INTO connection (
                    name, component_name, environment, connection_version, parameters, credential_status, type,
                    created_date, created_by, last_modified_date, last_modified_by, version
                ) VALUES (?, ?, 2, 1, '{}', 0, 0, now(), 'test', now(), 'test', 0)
                RETURNING id
                """,
            Long.class, componentName + " connection", componentName);

        return Objects.requireNonNull(id);
    }

    private Connection mockConnection(long id, String componentName, int connectionVersion) {
        Connection mockedConnection = mock(Connection.class);

        when(mockedConnection.getId()).thenReturn(id);
        when(mockedConnection.getComponentName()).thenReturn(componentName);
        when(mockedConnection.getConnectionVersion()).thenReturn(connectionVersion);
        when(mockedConnection.getEnvironmentId()).thenReturn(Environment.PRODUCTION.ordinal());
        when(mockedConnection.getStatus()).thenReturn(ConnectionStatus.ACTIVE);
        when(mockedConnection.getType()).thenReturn(PlatformType.AUTOMATION);
        when(connectionService.getConnection(id)).thenReturn(mockedConnection);
        when(connectionService.fetchConnection(id)).thenReturn(Optional.of(mockedConnection));

        return mockedConnection;
    }

    private static ProxyRequest proxyRequest(String method, String path) {
        return new ProxyRequest(method, path, Map.of(), Map.of(), null, BodyEncoding.UTF8, null);
    }

    private static App newApp() {
        App app = new App();

        app.setName("crm");
        app.setWorkspaceId(WORKSPACE_ID);
        app.setEnvironment(Environment.PRODUCTION);
        app.setOwnerUserId(1L);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        return app;
    }
}
