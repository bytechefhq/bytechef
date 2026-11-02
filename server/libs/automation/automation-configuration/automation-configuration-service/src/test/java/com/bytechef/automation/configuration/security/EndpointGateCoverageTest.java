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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class EndpointGateCoverageTest {

    private static final List<String> SCANNED_ROOTS = List.of(
        "libs/ai", "libs/automation", "libs/platform", "ee/libs/ai", "ee/libs/automation", "ee/libs/platform");

    private static final Map<String, String> GATED_DELEGATES = Map.ofEntries(
        Map.entry("AiProviderApiController#deleteAiProvider", "AiProviderFacadeImpl#deleteAiProvider"),
        Map.entry("AiProviderApiController#enableAiProvider", "AiProviderFacadeImpl#updateAiProvider"),
        Map.entry("AiProviderApiController#updateAiProvider", "AiProviderFacadeImpl#updateAiProvider"),
        Map.entry("ApiCollectionApiController#createApiCollection", "ApiCollectionFacadeImpl#createApiCollection"),
        Map.entry("ApiCollectionApiController#deleteApiCollection", "ApiCollectionFacadeImpl#deleteApiCollection"),
        Map.entry("ApiCollectionApiController#updateApiCollection", "ApiCollectionFacadeImpl#updateApiCollection"),
        Map.entry("ApiCollectionEndpointApiController#createApiCollectionEndpoint",
            "ApiCollectionFacadeImpl#createApiCollectionEndpoint"),
        Map.entry("ApiCollectionEndpointApiController#deleteApiCollectionEndpoint",
            "ApiCollectionFacadeImpl#deleteApiCollectionEndpoint"),
        Map.entry("ApiCollectionEndpointApiController#updateApiCollectionEndpoint",
            "ApiCollectionFacadeImpl#updateApiCollectionEndpoint"),
        Map.entry("ApiCollectionTagApiController#updateApiCollectionTags",
            "ApiCollectionFacadeImpl#updateApiCollectionTags"),
        Map.entry("ConnectionApiController#createConnection", "WorkspaceConnectionFacadeImpl#create"),
        Map.entry("ConnectionApiController#deleteConnection", "WorkspaceConnectionFacadeImpl#delete"),
        Map.entry("ConnectionApiController#updateConnection", "WorkspaceConnectionFacadeImpl#update"),
        Map.entry("ConnectionTagApiController#updateConnectionTags", "WorkspaceConnectionFacadeImpl#updateTags"),
        Map.entry("CustomComponentApiController#deployCustomComponent", "CustomComponentFacadeImpl#save"),
        Map.entry("CustomComponentGraphQlController#deleteCustomComponent", "CustomComponentFacadeImpl#delete"),
        Map.entry("CustomComponentGraphQlController#enableCustomComponent",
            "CustomComponentServiceImpl#enableCustomComponent"),
        Map.entry("CustomRoleGraphQlController#createCustomRole", "CustomRoleServiceImpl#createCustomRole"),
        Map.entry("CustomRoleGraphQlController#deleteCustomRole", "CustomRoleServiceImpl#deleteCustomRole"),
        Map.entry("CustomRoleGraphQlController#updateCustomRole", "CustomRoleServiceImpl#updateCustomRole"),
        Map.entry("GitConfigurationApiController#updateGitConfiguration", "GitConfigurationFacadeImpl#save"),
        Map.entry("IdentityProviderGraphQlController#createIdentityProvider", "IdentityProviderFacadeImpl#create"),
        Map.entry("IdentityProviderGraphQlController#deleteIdentityProvider", "IdentityProviderFacadeImpl#delete"),
        Map.entry("IdentityProviderGraphQlController#updateIdentityProvider", "IdentityProviderFacadeImpl#update"),
        Map.entry("KnowledgeBaseGraphQlController#createKnowledgeBase",
            "WorkspaceKnowledgeBaseFacadeImpl#createWorkspaceKnowledgeBase"),
        Map.entry("KnowledgeBaseGraphQlController#deleteKnowledgeBase",
            "WorkspaceKnowledgeBaseFacadeImpl#deleteWorkspaceKnowledgeBase"),
        Map.entry("LogFileGraphQlController#deleteJobFileLogs", "LogFileStorageImpl#deleteLogEntries"),
        Map.entry("McpComponentGraphQlController#createMcpComponent", "McpComponentServiceImpl#create"),
        Map.entry("McpComponentGraphQlController#createMcpComponentWithTools", "McpServerFacadeImpl#create"),
        Map.entry("McpComponentGraphQlController#deleteMcpComponent", "McpServerFacadeImpl#deleteMcpComponent"),
        Map.entry("McpComponentGraphQlController#updateMcpComponentWithTools", "McpServerFacadeImpl#update"),
        Map.entry("McpProjectGraphQlController#createMcpProject", "McpProjectFacadeImpl#createMcpProject"),
        Map.entry("McpProjectGraphQlController#deleteMcpProject", "McpProjectFacadeImpl#deleteMcpProject"),
        Map.entry("McpProjectWorkflowGraphQlController#createMcpProjectWorkflow",
            "McpProjectWorkflowServiceImpl#create"),
        Map.entry("McpProjectWorkflowGraphQlController#deleteMcpProjectWorkflow",
            "McpProjectWorkflowFacadeImpl#deleteMcpProjectWorkflow"),
        Map.entry("McpProjectWorkflowGraphQlController#updateMcpProjectWorkflow",
            "McpProjectWorkflowServiceImpl#update"),
        Map.entry("McpServerGraphQlController#deleteMcpServer", "McpServerFacadeImpl#deleteMcpServer"),
        Map.entry("McpServerGraphQlController#updateMcpServer", "McpServerServiceImpl#update"),
        Map.entry("McpServerGraphQlController#updateMcpServerTags", "McpServerFacadeImpl#updateMcpServerTags"),
        Map.entry("McpServerGraphQlController#updateMcpServerUrl", "McpServerServiceImpl#update"),
        Map.entry("McpToolGraphQlController#createMcpTool", "McpToolServiceImpl#create"),
        Map.entry("McpToolGraphQlController#deleteMcpTool", "McpToolServiceImpl#delete"),
        Map.entry("McpToolGraphQlController#updateMcpTool", "McpToolServiceImpl#update"),
        Map.entry("ProjectApiController#createProject", "ProjectFacadeImpl#createProject"),
        Map.entry("ProjectApiController#deleteProject", "ProjectFacadeImpl#deleteProject"),
        Map.entry("ProjectApiController#duplicateProject", "ProjectFacadeImpl#duplicateProject"),
        Map.entry("ProjectApiController#importProject", "ProjectFacadeImpl#importProject"),
        Map.entry("ProjectApiController#publishProject", "ProjectFacadeImpl#publishProject"),
        Map.entry("ProjectApiController#updateProject", "ProjectFacadeImpl#updateProject"),
        Map.entry("ProjectCodeWorkflowApiController#deployProject", "ProjectCodeWorkflowFacadeImpl#save"),
        Map.entry("ProjectDeploymentApiController#createProjectDeployment",
            "ProjectDeploymentFacadeImpl#createProjectDeployment"),
        Map.entry("ProjectDeploymentApiController#createProjectDeploymentWorkflowJob",
            "ProjectDeploymentFacadeImpl#createProjectDeploymentWorkflowJob"),
        Map.entry("ProjectDeploymentApiController#deleteProjectDeployment",
            "ProjectDeploymentFacadeImpl#deleteProjectDeployment"),
        Map.entry("ProjectDeploymentApiController#enableProjectDeployment",
            "ProjectDeploymentFacadeImpl#enableProjectDeployment"),
        Map.entry("ProjectDeploymentApiController#enableProjectDeploymentWorkflow",
            "ProjectDeploymentFacadeImpl#enableProjectDeploymentWorkflow"),
        Map.entry("ProjectDeploymentApiController#updateProjectDeployment",
            "ProjectDeploymentFacadeImpl#updateProjectDeployment"),
        Map.entry("ProjectDeploymentApiController#updateProjectDeploymentWorkflow",
            "ProjectDeploymentFacadeImpl#updateProjectDeploymentWorkflow"),
        Map.entry("ProjectDeploymentTagApiController#updateProjectDeploymentTags",
            "ProjectDeploymentFacadeImpl#updateProjectDeploymentTags"),
        Map.entry("ProjectGitApiController#pullProjectFromGit", "ProjectGitFacadeImpl#pullProjectFromGit"),
        Map.entry("ProjectGitApiController#updateProjectGitConfiguration", "ProjectGitConfigurationServiceImpl#save"),
        Map.entry("ProjectGraphQlController#deleteSharedProject", "ProjectFacadeImpl#deleteSharedProject"),
        Map.entry("ProjectGraphQlController#exportSharedProject", "ProjectFacadeImpl#exportSharedProject"),
        Map.entry("ProjectGraphQlController#importProjectTemplate", "ProjectFacadeImpl#importProjectTemplate"),
        Map.entry("ProjectWorkflowGraphQlController#deleteSharedWorkflow",
            "ProjectWorkflowFacadeImpl#deleteSharedWorkflow"),
        Map.entry("ProjectWorkflowGraphQlController#exportSharedWorkflow",
            "ProjectWorkflowFacadeImpl#exportSharedWorkflow"),
        Map.entry("ProjectWorkflowGraphQlController#importWorkflowTemplate",
            "ProjectWorkflowFacadeImpl#importWorkflowTemplate"),
        Map.entry("UserGraphQlController#deleteUser", "UserManagementFacadeImpl#deleteUser"),
        Map.entry("UserGraphQlController#inviteUser", "UserManagementFacadeImpl#inviteUser"),
        Map.entry("UserGraphQlController#updateUser", "UserManagementFacadeImpl#updateUserRole"),
        Map.entry("WorkflowApiController#createProjectWorkflow", "ProjectWorkflowFacadeImpl#addWorkflow"),
        Map.entry("WorkflowApiController#deleteWorkflow", "ProjectWorkflowFacadeImpl#deleteWorkflow"),
        Map.entry("WorkflowApiController#duplicateWorkflow", "ProjectWorkflowFacadeImpl#duplicateWorkflow"),
        Map.entry("WorkflowApiController#updateWorkflow", "ProjectWorkflowFacadeImpl#updateWorkflow"),
        Map.entry("WorkspaceApiController#createWorkspace", "AdminWorkspaceFacadeImpl#createWorkspace"),
        Map.entry("WorkspaceApiController#deleteWorkspace", "AdminWorkspaceFacadeImpl#deleteWorkspace"),
        Map.entry("WorkspaceApiController#updateWorkspace", "AdminWorkspaceFacadeImpl#updateWorkspace"),
        Map.entry("WorkspaceApiKeyGraphQlController#deleteWorkspaceApiKey", "WorkspaceApiKeyFacadeImpl#delete"),
        Map.entry("WorkspaceMcpServerGraphQlController#createWorkspaceMcpServer",
            "WorkspaceMcpServerFacadeImpl#createWorkspaceMcpServer"),
        Map.entry("WorkspaceMcpServerGraphQlController#deleteWorkspaceMcpServer",
            "WorkspaceMcpServerFacadeImpl#deleteWorkspaceMcpServer"),
        Map.entry("WorkspaceUserGraphQlController#addWorkspaceUser", "WorkspaceUserServiceImpl#addWorkspaceUser"),
        Map.entry("WorkspaceUserGraphQlController#assignWorkspaceUserCustomRole",
            "WorkspaceUserServiceImpl#assignCustomRole"),
        Map.entry("WorkspaceUserGraphQlController#inviteWorkspaceUser", "WorkspaceUserServiceImpl#inviteWorkspaceUser"),
        Map.entry("WorkspaceUserGraphQlController#removeWorkspaceUser", "WorkspaceUserServiceImpl#removeWorkspaceUser"),
        Map.entry("WorkspaceUserGraphQlController#removeWorkspaceUserEnvironmentRole",
            "WorkspaceUserServiceImpl#removeEnvironmentRole"),
        Map.entry("WorkspaceUserGraphQlController#setWorkspaceUserEnvironmentRole",
            "WorkspaceUserServiceImpl#setEnvironmentRole"),
        Map.entry("WorkspaceUserGraphQlController#updateWorkspaceUserRole",
            "WorkspaceUserServiceImpl#updateWorkspaceUserRole"));

    private static final Map<String, String> AUTHORIZED_OTHERWISE = Map.ofEntries(
        Map.entry("AccountController#changePassword", "acts on the signed-in account only"),
        Map.entry("AccountController#disableMfa", "acts on the signed-in account only"),
        Map.entry("AccountController#enableMfa", "acts on the signed-in account only"),
        Map.entry("AccountController#finishPasswordReset", "pre-login password reset, authorized by the reset key"),
        Map.entry("AccountController#invalidateSession", "acts on the signed-in account only"),
        Map.entry("AccountController#registerAccount", "pre-login self-registration"),
        Map.entry("AccountController#requestPasswordReset", "pre-login password reset request"),
        Map.entry("AccountController#saveAccount", "acts on the signed-in account only"),
        Map.entry("AccountController#sendActivationEmail", "pre-login activation mail"),
        Map.entry("AccountController#setupMfa", "acts on the signed-in account only"),
        Map.entry("AccountController#unlinkProvider", "acts on the signed-in account only"),
        Map.entry("AiAgentTestApiController#stopAiAgentTest", "addressed by the random test id handed to the caller"),
        Map.entry("AiSkillGraphQlController#createAdditionalFilesInSkill",
            "AiSkillApiFacadeImpl checks owner or admin"),
        Map.entry("AiSkillGraphQlController#createAiSkill", "creates a skill owned by the caller"),
        Map.entry("AiSkillGraphQlController#createAiSkillFromInstructions", "creates a skill owned by the caller"),
        Map.entry("AiSkillGraphQlController#deleteAiSkill", "AiSkillApiFacadeImpl checks owner or admin"),
        Map.entry("AiSkillGraphQlController#removeFileInSkill", "AiSkillApiFacadeImpl checks owner or admin"),
        Map.entry("AiSkillGraphQlController#updateAiSkill", "AiSkillApiFacadeImpl checks owner or admin"),
        Map.entry("AiSkillGraphQlController#updateAiSkillContent", "AiSkillApiFacadeImpl checks owner or admin"),
        Map.entry("AiSkillGraphQlController#updateAiSkillTags", "AiSkillApiFacadeImpl checks owner or admin"),
        Map.entry("ApiKeyGraphQlController#createApiKey", "ApiKeyFacadeImpl requires a tenant admin"),
        Map.entry("ApiKeyGraphQlController#deleteApiKey", "ApiKeyFacadeImpl checks the caller owns the key"),
        Map.entry("ApiKeyGraphQlController#updateApiKey", "ApiKeyFacadeImpl checks the caller owns the key"),
        Map.entry("ApiPlatformHandlerController#handleDeleteMethod", "API platform runtime, authenticated by API key"),
        Map.entry("ApiPlatformHandlerController#handlePatchMethod", "API platform runtime, authenticated by API key"),
        Map.entry("ApiPlatformHandlerController#handlePostMethod", "API platform runtime, authenticated by API key"),
        Map.entry("ApiPlatformHandlerController#handlePutMethod", "API platform runtime, authenticated by API key"),
        Map.entry("ApprovalController#approve", "permit-all inbound callback addressed by an approval id"),
        Map.entry("BillingWebhookApiController#handleWebhook", "Stripe webhook, verified by its signature"),
        Map.entry("CopilotApiController#chat", "checks the workflow scope in its body"),
        Map.entry("JobResumeController#resume", "permit-all inbound callback addressed by a resume id"),
        Map.entry(
            "McpProjectGraphQlController#updateMcpProject",
            "McpProjectService and McpProjectWorkflowService gate MCP_VIEW and MCP_EDIT inside the transaction"),
        Map.entry("OAuth2ApiController#getOAuth2AuthorizationParameters",
            "computes parameters, reads no stored resource"),
        Map.entry("ProjectTagApiController#updateProjectTags", "ProjectServiceImpl.update gates WORKFLOW_EDIT"),
        Map.entry("ScimGroupController#createGroup", "SCIM, authenticated by the SCIM bearer token"),
        Map.entry("ScimGroupController#deleteGroup", "SCIM, authenticated by the SCIM bearer token"),
        Map.entry("ScimGroupController#patchGroup", "SCIM, authenticated by the SCIM bearer token"),
        Map.entry("ScimGroupController#replaceGroup", "SCIM, authenticated by the SCIM bearer token"),
        Map.entry("ScimUserController#createUser", "SCIM, authenticated by the SCIM bearer token"),
        Map.entry("ScimUserController#deleteUser", "SCIM, authenticated by the SCIM bearer token"),
        Map.entry("ScimUserController#patchUser", "SCIM, authenticated by the SCIM bearer token"),
        Map.entry("ScimUserController#replaceUser", "SCIM, authenticated by the SCIM bearer token"),
        Map.entry("SsoDiscoveryController#discover", "pre-login identity provider lookup"),
        Map.entry("TwilioCallbackController#handleMediaStreamCallback", "permit-all inbound Twilio callback"),
        Map.entry("TwilioCallbackController#handleRecordingCallback", "permit-all inbound Twilio callback"),
        Map.entry("TwilioCallbackController#handleStatusCallback", "permit-all inbound Twilio callback"),
        Map.entry("TwimlController#serveTwiml", "permit-all inbound Twilio callback"),
        Map.entry("WebhookTriggerController#executeWorkflow",
            "permit-all inbound webhook addressed by an execution id"),
        Map.entry("WebhookTriggerController#sseStreamWorkflow",
            "permit-all inbound webhook addressed by an execution id"),
        Map.entry("WebhookTriggerTestController#executeWorkflow",
            "permit-all inbound webhook addressed by an execution id"),
        Map.entry("WorkflowSchemaGeneratorApiController#generateSchema",
            "computes a schema from the request body only"),
        Map.entry("WorkspaceApiKeyGraphQlController#updateWorkspaceApiKey",
            "ApiKeyFacadeImpl checks the caller owns the key"));

    private static final Map<String, String> KNOWN_UNGATED_WRITES = Map.ofEntries(
        Map.entry("AiAgentTestApiController#testAiAgent",
            "runs any workflow's AI Agent node with its test connections"),
        Map.entry("ApiClientApiController#createApiClient", "any member manages the tenant's API platform clients"),
        Map.entry("ApiClientApiController#deleteApiClient", "any member manages the tenant's API platform clients"),
        Map.entry("ApiClientApiController#updateApiClient", "any member manages the tenant's API platform clients"),
        Map.entry("ApiConnectorGraphQlController#cancelGenerationJob",
            "any member manages the tenant's API connectors"),
        Map.entry("ApiConnectorGraphQlController#createApiConnector", "any member manages the tenant's API connectors"),
        Map.entry("ApiConnectorGraphQlController#deleteApiConnector", "any member manages the tenant's API connectors"),
        Map.entry("ApiConnectorGraphQlController#enableApiConnector", "any member manages the tenant's API connectors"),
        Map.entry(
            "ApiConnectorGraphQlController#generateFromDocumentation",
            "any member manages the tenant's API connectors"),
        Map.entry("ApiConnectorGraphQlController#generateSpecification",
            "any member manages the tenant's API connectors"),
        Map.entry(
            "ApiConnectorGraphQlController#importOpenApiSpecification",
            "any member manages the tenant's API connectors"),
        Map.entry(
            "ApiConnectorGraphQlController#startGenerateFromDocumentationPreview",
            "any member manages the tenant's API connectors"),
        Map.entry("ApiConnectorGraphQlController#updateApiConnector", "any member manages the tenant's API connectors"),
        Map.entry("ApprovalTaskGraphQlController#createApprovalTask", "any member writes any approval task"),
        Map.entry("ApprovalTaskGraphQlController#deleteApprovalTask", "any member writes any approval task"),
        Map.entry("ApprovalTaskGraphQlController#updateApprovalTask", "any member writes any approval task"),
        Map.entry("BillingApiController#cancelSubscription", "any member changes the tenant's subscription"),
        Map.entry("BillingApiController#createCheckoutSession", "any member changes the tenant's subscription"),
        Map.entry("BillingApiController#reactivateSubscription", "any member changes the tenant's subscription"),
        Map.entry("BillingApiController#upgradeSubscription", "any member changes the tenant's subscription"),
        Map.entry(
            "ManagementMcpServerGraphQlController#updateManagementMcpServerUrl",
            "any member rotates the tenant's management MCP server URL"),
        Map.entry("McpServerGraphQlController#createMcpServer", "any member creates a tenant MCP server"),
        Map.entry("NotificationApiController#createNotification", "any member writes the tenant's notifications"),
        Map.entry("NotificationApiController#deleteNotification", "any member writes the tenant's notifications"),
        Map.entry("NotificationApiController#updateNotification", "any member writes the tenant's notifications"),
        Map.entry(
            "WebhookTriggerTestApiController#startWebhookTriggerTest",
            "any member enables any workflow's trigger with its test connection"),
        Map.entry(
            "WebhookTriggerTestApiController#stopWebhookTriggerTest",
            "any member disables any workflow's test trigger"));

    private static final Set<String> SKIPPED_DIRECTORY_NAMES =
        Set.of(".git", ".gradle", "bin", "build", "node_modules", "src");

    private static final Pattern CLASS_DECLARATION = Pattern.compile("\\b(class|interface|record|enum)\\s+(\\w+)");
    private static final Pattern FIELD_DECLARATION = Pattern.compile(
        "(?:private|protected)\\s+(?:final\\s+)?([\\w.]+)(?:<[^;=()]*>)?\\s+(\\w+)\\s*;");
    private static final Pattern HTTP_METHOD_KEY =
        Pattern.compile("^\\s+(get|post|put|patch|delete|head|options):\\s*$");
    private static final Pattern IMPLEMENTS_CLAUSE = Pattern.compile("\\bimplements\\s+(.+)$", Pattern.DOTALL);
    private static final Pattern MEMBER_NAME = Pattern.compile("(\\w+)\\s*$");
    private static final Pattern OPERATION_ID = Pattern.compile("^\\s+operationId:\\s*\"?(\\w+)\"?");
    private static final Pattern REQUEST_METHOD = Pattern.compile("RequestMethod\\.(\\w+)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");
    private static final Set<String> READ_HTTP_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final Map<String, String> WRITE_MAPPING_ANNOTATIONS = Map.of(
        "DeleteMapping", "DELETE", "MutationMapping", "MUTATION", "PatchMapping", "PATCH", "PostMapping", "POST",
        "PutMapping", "PUT");

    private static Map<String, List<Endpoint>> writeEndpointsByKey;
    private static Map<String, List<SourceType>> sourceTypesByName;

    @BeforeAll
    static void scanTheTree() {
        Path serverRoot = MainSourceScan.serverRoot();

        List<Path> mainSourceFiles = MainSourceScan.collectMainSourceFiles(serverRoot);
        Map<String, Set<String>> httpMethodsByOperationId = collectHttpMethodsByOperationId(serverRoot);

        sourceTypesByName = new HashMap<>();
        writeEndpointsByKey = new TreeMap<>();

        for (Path sourceFile : mainSourceFiles) {
            String source = MainSourceScan.readSource(sourceFile);
            String fileName = MainSourceScan.fileName(sourceFile);

            String typeName = fileName.substring(0, fileName.length() - ".java".length());

            if (!source.contains("@PreAuthorize") && !isScannedController(serverRoot, sourceFile, source)) {
                continue;
            }

            SourceType sourceType = parseSourceType(typeName, MainSourceScan.stripComments(source));

            List<SourceType> sourceTypes = sourceTypesByName.computeIfAbsent(typeName, name -> new ArrayList<>());

            sourceTypes.add(sourceType);

            if (isScannedController(serverRoot, sourceFile, source)) {
                collectWriteEndpoints(sourceType, httpMethodsByOperationId);
            }
        }
    }

    @Test
    void testTheScanFoundTheEndpoints() {
        Map<String, Integer> endpointCountsByKind = new TreeMap<>();

        for (List<Endpoint> endpoints : writeEndpointsByKey.values()) {
            for (Endpoint endpoint : endpoints) {
                endpointCountsByKind.merge(endpoint.kind(), 1, Integer::sum);
            }
        }

        assertThat(endpointCountsByKind.getOrDefault("MUTATION", 0))
            .as("the scan found implausibly few GraphQL mutations, so it is broken: %s", endpointCountsByKind)
            .isGreaterThan(100);
        assertThat(endpointCountsByKind.getOrDefault("POST", 0))
            .as("the scan found implausibly few REST POST endpoints, so it is broken: %s", endpointCountsByKind)
            .isGreaterThan(30);
    }

    @Test
    void testEveryWriteEndpointIsGatedOrListed() {
        Set<String> unlistedUngatedEndpoints = new TreeSet<>();

        for (Map.Entry<String, List<Endpoint>> entry : writeEndpointsByKey.entrySet()) {
            String key = entry.getKey();

            boolean listed = GATED_DELEGATES.containsKey(key) || AUTHORIZED_OTHERWISE.containsKey(key) ||
                KNOWN_UNGATED_WRITES.containsKey(key);

            for (Endpoint endpoint : entry.getValue()) {
                if (!endpoint.gated() && !listed) {
                    unlistedUngatedEndpoints.add(key + " (" + endpoint.kind() + ")");
                }
            }
        }

        assertThat(unlistedUngatedEndpoints)
            .as(
                "these write endpoints carry no @PreAuthorize of their own and are not listed: gate them, or add them "
                    + "to GATED_DELEGATES with the gated bean method they call, to AUTHORIZED_OTHERWISE with how "
                    + "it is authorized, or to KNOWN_UNGATED_WRITES")
            .isEmpty();
    }

    @Test
    void testEveryGatedDelegateIsCalledAndGated() {
        Map<String, String> brokenDelegates = new TreeMap<>();

        for (Map.Entry<String, String> entry : GATED_DELEGATES.entrySet()) {
            String problem = findDelegateProblem(entry.getKey(), entry.getValue());

            if (problem != null) {
                brokenDelegates.put(entry.getKey(), problem);
            }
        }

        assertThat(brokenDelegates)
            .as("these GATED_DELEGATES entries no longer hold, so the endpoint may be ungated")
            .isEmpty();
    }

    @Test
    void testEveryListedEndpointStillExistsUngated() {
        Map<String, String> staleEntries = new TreeMap<>();

        for (Map<String, String> listing : List.of(GATED_DELEGATES, AUTHORIZED_OTHERWISE, KNOWN_UNGATED_WRITES)) {
            for (String key : listing.keySet()) {
                List<Endpoint> endpoints = writeEndpointsByKey.get(key);

                if (endpoints == null) {
                    staleEntries.put(key, "no scanned write endpoint has this name any more");
                } else if (endpoints.stream()
                    .allMatch(Endpoint::gated)) {

                    staleEntries.put(key, "the endpoint now carries its own @PreAuthorize");
                }
            }
        }

        assertThat(staleEntries)
            .as("remove these entries, they describe endpoints that are gone or gated in place")
            .isEmpty();
    }

    @Test
    void testNoEndpointIsListedTwice() {
        Set<String> listedTwice = new TreeSet<>();
        Set<String> listed = new TreeSet<>();

        for (Map<String, String> listing : List.of(GATED_DELEGATES, AUTHORIZED_OTHERWISE, KNOWN_UNGATED_WRITES)) {
            for (String key : listing.keySet()) {
                if (!listed.add(key)) {
                    listedTwice.add(key);
                }
            }
        }

        assertThat(listedTwice).isEmpty();
    }

    private static String findDelegateProblem(String endpointKey, String delegate) {
        List<Endpoint> endpoints = writeEndpointsByKey.get(endpointKey);

        if (endpoints == null) {
            return "no scanned write endpoint has this name";
        }

        String[] delegateParts = delegate.split("#");

        String delegateTypeName = delegateParts[0];
        String delegateMethodName = delegateParts[1];

        List<SourceType> delegateTypes = sourceTypesByName.getOrDefault(delegateTypeName, List.of());

        if (delegateTypes.isEmpty()) {
            return "no scanned type " + delegateTypeName + " carries a @PreAuthorize";
        }

        Set<String> receiverTypeNames = new TreeSet<>();

        for (SourceType delegateType : delegateTypes) {
            Set<String> gatedMethodNames = delegateType.gatedMethodNames();

            if (!delegateType.classGated() && !gatedMethodNames.contains(delegateMethodName)) {
                return delegateTypeName + "." + delegateMethodName + " carries no @PreAuthorize";
            }

            receiverTypeNames.add(delegateType.name());
            receiverTypeNames.addAll(delegateType.implementedTypeNames());
        }

        for (Endpoint endpoint : endpoints) {
            if (!callsDelegate(endpoint, receiverTypeNames, delegateMethodName)) {
                SourceType controller = endpoint.controller();

                return controller.name() + "." + endpoint.name() + " does not call " + delegateMethodName +
                    " on a field of type " + receiverTypeNames;
            }
        }

        return null;
    }

    private static boolean callsDelegate(Endpoint endpoint, Set<String> receiverTypeNames, String methodName) {
        SourceType controller = endpoint.controller();
        Map<String, String> fieldTypeNamesByName = controller.fieldTypeNamesByName();

        for (Map.Entry<String, String> field : fieldTypeNamesByName.entrySet()) {

            if (!receiverTypeNames.contains(field.getValue())) {
                continue;
            }

            Pattern callPattern = Pattern.compile(
                "\\b" + Pattern.quote(field.getKey()) + "\\s*\\.\\s*" + Pattern.quote(methodName) + "\\s*\\(");

            Matcher callMatcher = callPattern.matcher(endpoint.body());

            if (callMatcher.find()) {
                return true;
            }
        }

        return false;
    }

    private static boolean isScannedController(Path serverRoot, Path sourceFile, String source) {
        if (!source.contains("@Controller") && !source.contains("@RestController")) {
            return false;
        }

        Path relativePath = serverRoot.relativize(sourceFile);

        String relativePathString = relativePath.toString()
            .replace(File.separatorChar, '/');

        if (relativePathString.contains("/remote/")) {
            return false;
        }

        for (String scannedRoot : SCANNED_ROOTS) {
            if (relativePathString.startsWith(scannedRoot + "/")) {
                return true;
            }
        }

        return false;
    }

    private static void collectWriteEndpoints(
        SourceType controller, Map<String, Set<String>> httpMethodsByOperationId) {

        Map<String, String> classAnnotations = controller.classAnnotations();

        Set<String> classAnnotationNames = classAnnotations.keySet();

        if (!classAnnotationNames.contains("Controller") && !classAnnotationNames.contains("RestController")) {
            return;
        }

        boolean restController = classAnnotationNames.contains("RestController");

        for (Member member : controller.members()) {
            String kind = writeKind(member, restController, httpMethodsByOperationId);

            if (kind == null) {
                continue;
            }

            Map<String, String> memberAnnotations = member.annotations();

            boolean gated = controller.classGated() || memberAnnotations.containsKey("PreAuthorize");

            List<Endpoint> endpoints = writeEndpointsByKey.computeIfAbsent(
                controller.name() + "#" + member.name(), key -> new ArrayList<>());

            endpoints.add(new Endpoint(controller, member.name(), kind, gated, member.body()));
        }
    }

    private static String writeKind(
        Member member, boolean restController, Map<String, Set<String>> httpMethodsByOperationId) {

        Map<String, String> annotations = member.annotations();

        for (Map.Entry<String, String> entry : WRITE_MAPPING_ANNOTATIONS.entrySet()) {
            if (annotations.containsKey(entry.getKey())) {
                return entry.getValue();
            }
        }

        if (annotations.containsKey("RequestMapping")) {
            Set<String> httpMethods = new TreeSet<>();

            Matcher requestMethodMatcher = REQUEST_METHOD.matcher(annotations.get("RequestMapping"));

            while (requestMethodMatcher.find()) {
                httpMethods.add(requestMethodMatcher.group(1));
            }

            return writeKind(httpMethods.isEmpty() ? Set.of("ANY") : httpMethods);
        }

        if (restController && annotations.containsKey("Override") && !hasReadMapping(annotations)) {
            Set<String> httpMethods = httpMethodsByOperationId.get(member.name());

            return writeKind(httpMethods == null ? Set.of("UNKNOWN") : httpMethods);
        }

        return null;
    }

    private static boolean hasReadMapping(Map<String, String> annotations) {
        return annotations.containsKey("GetMapping") || annotations.containsKey("QueryMapping") ||
            annotations.containsKey("SchemaMapping") || annotations.containsKey("BatchMapping");
    }

    private static String writeKind(Set<String> httpMethods) {
        Set<String> writeHttpMethods = new TreeSet<>(httpMethods);

        writeHttpMethods.removeAll(READ_HTTP_METHODS);

        return writeHttpMethods.isEmpty() ? null : String.join("/", writeHttpMethods);
    }

    private static Map<String, Set<String>> collectHttpMethodsByOperationId(Path serverRoot) {
        Map<String, Set<String>> httpMethodsByOperationId = new HashMap<>();

        for (Path specificationFile : collectOpenApiSpecifications(serverRoot)) {
            String currentHttpMethod = null;

            for (String line : readLines(specificationFile)) {
                Matcher httpMethodMatcher = HTTP_METHOD_KEY.matcher(line);

                if (httpMethodMatcher.matches()) {
                    currentHttpMethod = httpMethodMatcher.group(1)
                        .toUpperCase();

                    continue;
                }

                Matcher operationIdMatcher = OPERATION_ID.matcher(line);

                if (operationIdMatcher.find() && currentHttpMethod != null) {
                    Set<String> httpMethods = httpMethodsByOperationId.computeIfAbsent(
                        operationIdMatcher.group(1), operationId -> new TreeSet<>());

                    httpMethods.add(currentHttpMethod);
                }
            }
        }

        assertThat(httpMethodsByOperationId)
            .as("no OpenAPI operation was found, so the specification scan is broken")
            .hasSizeGreaterThan(100);

        return httpMethodsByOperationId;
    }

    private static List<Path> collectOpenApiSpecifications(Path serverRoot) {
        List<Path> specificationFiles = new ArrayList<>();

        try {
            Files.walkFileTree(serverRoot, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    Path name = directory.getFileName();

                    if (name != null && SKIPPED_DIRECTORY_NAMES.contains(name.toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    String fileName = MainSourceScan.fileName(file);

                    if (fileName.startsWith("openapi") && fileName.endsWith(".yaml")) {
                        specificationFiles.add(file);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ioException) {
            throw new UncheckedIOException("Could not walk " + serverRoot, ioException);
        }

        return specificationFiles;
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException ioException) {
            throw new UncheckedIOException("Could not read " + file, ioException);
        }
    }

    private static SourceType parseSourceType(String typeName, String strippedSource) {
        Map<String, String> classAnnotations = new HashMap<>();
        Set<String> implementedTypeNames = new TreeSet<>();
        List<Member> members = new ArrayList<>();

        int depth = 0;
        int index = 0;
        boolean topLevelTypeSeen = false;

        while (index < strippedSource.length()) {
            char character = strippedSource.charAt(index);

            if (character == '"' || character == '\'') {
                index = skipLiteral(strippedSource, index);

                continue;
            }

            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
            }

            if (character != '@' || !isAnnotationStart(strippedSource, index)) {
                index++;

                continue;
            }

            Map<String, String> annotations = new HashMap<>();

            index = parseAnnotationBlock(strippedSource, index, annotations);

            int terminatorIndex = findDeclarationTerminator(strippedSource, index);

            if (terminatorIndex < 0) {
                break;
            }

            String head = strippedSource.substring(index, terminatorIndex);
            char terminator = strippedSource.charAt(terminatorIndex);

            Matcher classMatcher = CLASS_DECLARATION.matcher(head);

            if (terminator == '{' && classMatcher.find() && depth == 0 && !topLevelTypeSeen) {
                topLevelTypeSeen = true;

                classAnnotations.putAll(annotations);
            } else if (terminator == '(' && depth == 1 && !classMatcher.find(0)) {

                Matcher memberNameMatcher = MEMBER_NAME.matcher(head);

                Matcher whitespaceMatcher = WHITESPACE.matcher(head.strip());

                if (memberNameMatcher.find() && whitespaceMatcher.find()) {

                    members.add(new Member(
                        memberNameMatcher.group(1), annotations, extractBody(strippedSource, terminatorIndex)));
                }
            }
        }

        Matcher typeDeclarationMatcher = Pattern.compile("\\bclass\\s+" + Pattern.quote(typeName) + "\\b([^{]*)\\{")
            .matcher(strippedSource);

        if (typeDeclarationMatcher.find()) {
            String typeDeclaration = typeDeclarationMatcher.group(1);

            Matcher implementsMatcher = IMPLEMENTS_CLAUSE.matcher(typeDeclaration.replaceAll("<[^<>]*>", ""));

            if (implementsMatcher.find()) {
                for (String implementedTypeName : implementsMatcher.group(1)
                    .split("[,\\s]+")) {

                    if (!implementedTypeName.isBlank()) {
                        implementedTypeNames.add(implementedTypeName);
                    }
                }
            }
        }

        Map<String, String> fieldTypeNamesByName = new HashMap<>();

        Matcher fieldMatcher = FIELD_DECLARATION.matcher(strippedSource);

        while (fieldMatcher.find()) {
            String fieldTypeName = fieldMatcher.group(1);

            fieldTypeNamesByName.put(
                fieldMatcher.group(2), fieldTypeName.substring(fieldTypeName.lastIndexOf('.') + 1));
        }

        Set<String> gatedMethodNames = new TreeSet<>();

        for (Member member : members) {
            Map<String, String> memberAnnotations = member.annotations();

            if (memberAnnotations.containsKey("PreAuthorize")) {
                gatedMethodNames.add(member.name());
            }
        }

        return new SourceType(
            typeName, classAnnotations, classAnnotations.containsKey("PreAuthorize"), implementedTypeNames,
            fieldTypeNamesByName, members, gatedMethodNames);
    }

    private static boolean isAnnotationStart(String source, int index) {
        return index + 1 < source.length() && Character.isLetter(source.charAt(index + 1)) &&
            !source.startsWith("@interface", index);
    }

    private static int parseAnnotationBlock(String source, int startIndex, Map<String, String> annotations) {
        int index = startIndex;

        while (index < source.length() && source.charAt(index) == '@' && isAnnotationStart(source, index)) {
            int nameEnd = index + 1;

            while (nameEnd < source.length() &&
                (Character.isLetterOrDigit(source.charAt(nameEnd)) || source.charAt(nameEnd) == '.' ||
                    source.charAt(nameEnd) == '_')) {

                nameEnd++;
            }

            String qualifiedName = source.substring(index + 1, nameEnd);

            int argumentStart = skipWhitespace(source, nameEnd);
            int annotationEnd = nameEnd;

            if (argumentStart < source.length() && source.charAt(argumentStart) == '(') {
                annotationEnd = skipBalanced(source, argumentStart, '(', ')');
            }

            annotations.put(
                qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1),
                source.substring(nameEnd, annotationEnd));

            index = skipWhitespace(source, annotationEnd);
        }

        return index;
    }

    private static int findDeclarationTerminator(String source, int startIndex) {
        int index = startIndex;

        while (index < source.length()) {
            char character = source.charAt(index);

            if ("(){};=,".indexOf(character) >= 0) {
                return index;
            }

            index++;
        }

        return -1;
    }

    private static String extractBody(String source, int parameterListStart) {
        int index = skipBalanced(source, parameterListStart, '(', ')');

        while (index < source.length() && source.charAt(index) != '{' && source.charAt(index) != ';') {
            index++;
        }

        if (index >= source.length() || source.charAt(index) == ';') {
            return "";
        }

        return source.substring(index, skipBalanced(source, index, '{', '}'));
    }

    private static int skipBalanced(String source, int openIndex, char open, char close) {
        int depth = 0;
        int index = openIndex;

        while (index < source.length()) {
            char character = source.charAt(index);

            if (character == '"' || character == '\'') {
                index = skipLiteral(source, index);

                continue;
            }

            if (character == open) {
                depth++;
            } else if (character == close) {
                depth--;

                if (depth == 0) {
                    return index + 1;
                }
            }

            index++;
        }

        return index;
    }

    private static int skipLiteral(String source, int startIndex) {
        if (source.startsWith("\"\"\"", startIndex)) {
            int endIndex = source.indexOf("\"\"\"", startIndex + 3);

            return endIndex < 0 ? source.length() : endIndex + 3;
        }

        char quote = source.charAt(startIndex);
        int index = startIndex + 1;

        while (index < source.length() && source.charAt(index) != quote) {
            if (source.charAt(index) == '\\') {
                index++;
            }

            index++;
        }

        return index + 1;
    }

    private static int skipWhitespace(String source, int startIndex) {
        int index = startIndex;

        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }

        return index;
    }

    private record Endpoint(SourceType controller, String name, String kind, boolean gated, String body) {
    }

    private record Member(String name, Map<String, String> annotations, String body) {
    }

    private record SourceType(
        String name, Map<String, String> classAnnotations, boolean classGated, Set<String> implementedTypeNames,
        Map<String, String> fieldTypeNamesByName, List<Member> members, Set<String> gatedMethodNames) {
    }
}
