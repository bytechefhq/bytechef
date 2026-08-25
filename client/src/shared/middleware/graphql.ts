/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import type * as Types from './graphql-types';

import type { DocumentTypeDecoration } from '@graphql-typed-document-node/core';
import { useQuery, useMutation, UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import { fetcher } from './graphqlFetcher';
export * from './graphql-types';
import type {KnowledgeBaseDocument as KnowledgeBaseDocumentSchemaType} from './graphql-types';
export type KnowledgeBaseDocument = KnowledgeBaseDocumentSchemaType;
export type AiAgentEvalResultQueryVariables = Exact<{
  id: string | number;
}>;


export type AiAgentEvalResultQuery = { aiAgentEvalResult: { id: string, status: Types.AiAgentEvalResultStatus, score: number | null, errorMessage: string | null, transcriptFile: string | null, createdDate: any, scenario: { id: string, name: string, type: Types.AiAgentScenarioType, userMessage: string | null, expectedOutput: string | null, personaPrompt: string | null, maxTurns: number | null, createdDate: any, lastModifiedDate: any, judges: Array<{ id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any }> }, verdicts: Array<{ id: string, judgeName: string, judgeType: Types.AiAgentJudgeType, judgeScope: Types.AiAgentJudgeScope, passed: boolean, score: number, explanation: string }> } | null };

export type AiAgentEvalResultTranscriptQueryVariables = Exact<{
  id: string | number;
}>;


export type AiAgentEvalResultTranscriptQuery = { aiAgentEvalResultTranscript: string | null };

export type AiAgentEvalRunQueryVariables = Exact<{
  id: string | number;
}>;


export type AiAgentEvalRunQuery = { aiAgentEvalRun: { id: string, name: string, status: Types.AiAgentEvalRunStatus, averageScore: number | null, totalScenarios: number, completedScenarios: number, agentVersion: string | null, totalInputTokens: number | null, totalOutputTokens: number | null, startedDate: any, completedDate: any, createdDate: any, results: Array<{ id: string, status: Types.AiAgentEvalResultStatus, score: number | null, errorMessage: string | null, transcriptFile: string | null, inputTokens: number | null, outputTokens: number | null, runIndex: number | null, createdDate: any, scenario: { id: string, name: string, type: Types.AiAgentScenarioType, userMessage: string | null, expectedOutput: string | null, personaPrompt: string | null, maxTurns: number | null, createdDate: any, lastModifiedDate: any, judges: Array<{ id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any }> }, verdicts: Array<{ id: string, judgeName: string, judgeType: Types.AiAgentJudgeType, judgeScope: Types.AiAgentJudgeScope, passed: boolean, score: number, explanation: string }> }> } | null };

export type AiAgentEvalRunsQueryVariables = Exact<{
  agentEvalTestId: string | number;
  limit?: number | null | undefined;
  offset?: number | null | undefined;
}>;


export type AiAgentEvalRunsQuery = { aiAgentEvalRuns: Array<{ id: string, name: string, status: Types.AiAgentEvalRunStatus, averageScore: number | null, totalScenarios: number, completedScenarios: number, startedDate: any, completedDate: any, createdDate: any }> };

export type AiAgentEvalTestQueryVariables = Exact<{
  id: string | number;
}>;


export type AiAgentEvalTestQuery = { aiAgentEvalTest: { id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any, scenarios: Array<{ id: string, name: string, type: Types.AiAgentScenarioType, userMessage: string | null, expectedOutput: string | null, personaPrompt: string | null, maxTurns: number | null, numberOfRuns: number | null, createdDate: any, lastModifiedDate: any, judges: Array<{ id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any }>, toolSimulations: Array<{ id: string, responsePrompt: string, simulationModel: string | null, toolName: string, createdDate: any, lastModifiedDate: any }> }> } | null };

export type AiAgentEvalTestsQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
}>;


export type AiAgentEvalTestsQuery = { aiAgentEvalTests: Array<{ id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any, scenarios: Array<{ id: string, name: string, type: Types.AiAgentScenarioType, userMessage: string | null, expectedOutput: string | null, personaPrompt: string | null, maxTurns: number | null, numberOfRuns: number | null, createdDate: any, lastModifiedDate: any, toolSimulations: Array<{ id: string, toolName: string, responsePrompt: string, simulationModel: string | null }>, judges: Array<{ id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any }> }> }> };

export type AiAgentJudgesQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
}>;


export type AiAgentJudgesQuery = { aiAgentJudges: Array<{ id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any }> };

export type CancelAiAgentEvalRunMutationVariables = Exact<{
  id: string | number;
}>;


export type CancelAiAgentEvalRunMutation = { cancelAiAgentEvalRun: { id: string, status: Types.AiAgentEvalRunStatus } };

export type CreateAiAgentEvalScenarioMutationVariables = Exact<{
  agentEvalTestId: string | number;
  name: string;
  type: Types.AiAgentScenarioType;
  userMessage?: string | null | undefined;
  expectedOutput?: string | null | undefined;
  personaPrompt?: string | null | undefined;
  maxTurns?: number | null | undefined;
  numberOfRuns?: number | null | undefined;
}>;


export type CreateAiAgentEvalScenarioMutation = { createAiAgentEvalScenario: { id: string, name: string, type: Types.AiAgentScenarioType, userMessage: string | null, expectedOutput: string | null, personaPrompt: string | null, maxTurns: number | null, numberOfRuns: number | null, createdDate: any, lastModifiedDate: any, judges: Array<{ id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any }> } };

export type CreateAiAgentEvalTestMutationVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  name: string;
  description?: string | null | undefined;
}>;


export type CreateAiAgentEvalTestMutation = { createAiAgentEvalTest: { id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any } };

export type CreateAiAgentJudgeMutationVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  name: string;
  type: Types.AiAgentJudgeType;
  configuration: any;
}>;


export type CreateAiAgentJudgeMutation = { createAiAgentJudge: { id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any } };

export type CreateAiAgentScenarioJudgeMutationVariables = Exact<{
  agentEvalScenarioId: string | number;
  name: string;
  type: Types.AiAgentJudgeType;
  configuration: any;
}>;


export type CreateAiAgentScenarioJudgeMutation = { createAiAgentScenarioJudge: { id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any } };

export type CreateAiAgentScenarioToolSimulationMutationVariables = Exact<{
  agentEvalScenarioId: string | number;
  toolName: string;
  responsePrompt: string;
  simulationModel?: string | null | undefined;
}>;


export type CreateAiAgentScenarioToolSimulationMutation = { createAiAgentScenarioToolSimulation: { id: string, toolName: string, responsePrompt: string, simulationModel: string | null } };

export type DeleteAiAgentEvalScenarioMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiAgentEvalScenarioMutation = { deleteAiAgentEvalScenario: boolean };

export type DeleteAiAgentEvalTestMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiAgentEvalTestMutation = { deleteAiAgentEvalTest: boolean };

export type DeleteAiAgentJudgeMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiAgentJudgeMutation = { deleteAiAgentJudge: boolean };

export type DeleteAiAgentScenarioJudgeMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiAgentScenarioJudgeMutation = { deleteAiAgentScenarioJudge: boolean };

export type DeleteAiAgentScenarioToolSimulationMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiAgentScenarioToolSimulationMutation = { deleteAiAgentScenarioToolSimulation: boolean };

export type StartAiAgentEvalRunMutationVariables = Exact<{
  agentEvalTestId: string | number;
  name: string;
  environmentId: string | number;
  scenarioIds?: Array<string | number> | string | number | null | undefined;
  aiAgentJudgeIds?: Array<string | number> | string | number | null | undefined;
}>;


export type StartAiAgentEvalRunMutation = { startAiAgentEvalRun: { id: string, name: string, status: Types.AiAgentEvalRunStatus, totalScenarios: number, completedScenarios: number, agentVersion: string | null, createdDate: any } };

export type UpdateAiAgentEvalScenarioMutationVariables = Exact<{
  id: string | number;
  name?: string | null | undefined;
  userMessage?: string | null | undefined;
  expectedOutput?: string | null | undefined;
  personaPrompt?: string | null | undefined;
  maxTurns?: number | null | undefined;
  numberOfRuns?: number | null | undefined;
}>;


export type UpdateAiAgentEvalScenarioMutation = { updateAiAgentEvalScenario: { id: string, name: string, type: Types.AiAgentScenarioType, userMessage: string | null, expectedOutput: string | null, personaPrompt: string | null, maxTurns: number | null, numberOfRuns: number | null, createdDate: any, lastModifiedDate: any, judges: Array<{ id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any }> } };

export type UpdateAiAgentEvalTestMutationVariables = Exact<{
  id: string | number;
  name?: string | null | undefined;
  description?: string | null | undefined;
}>;


export type UpdateAiAgentEvalTestMutation = { updateAiAgentEvalTest: { id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any } };

export type UpdateAiAgentJudgeMutationVariables = Exact<{
  id: string | number;
  name?: string | null | undefined;
  configuration?: any;
}>;


export type UpdateAiAgentJudgeMutation = { updateAiAgentJudge: { id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any } };

export type UpdateAiAgentScenarioJudgeMutationVariables = Exact<{
  id: string | number;
  name?: string | null | undefined;
  configuration?: any;
}>;


export type UpdateAiAgentScenarioJudgeMutation = { updateAiAgentScenarioJudge: { id: string, name: string, type: Types.AiAgentJudgeType, configuration: any, createdDate: any, lastModifiedDate: any } };

export type UpdateAiAgentScenarioToolSimulationMutationVariables = Exact<{
  id: string | number;
  toolName?: string | null | undefined;
  responsePrompt?: string | null | undefined;
  simulationModel?: string | null | undefined;
}>;


export type UpdateAiAgentScenarioToolSimulationMutation = { updateAiAgentScenarioToolSimulation: { id: string, toolName: string, responsePrompt: string, simulationModel: string | null } };

export type AiHubChatArtifactsQueryVariables = Exact<{
  workspaceId: string | number;
  environment?: number | null | undefined;
  userId?: string | number | null | undefined;
  kind?: Types.AiHubChatArtifactKind | null | undefined;
  from?: any;
  to?: any;
  page?: number | null | undefined;
  size?: number | null | undefined;
}>;


export type AiHubChatArtifactsQuery = { aiHubChatArtifacts: { totalCount: any, hasMore: boolean, pageClamped: boolean, sizeClamped: boolean, items: Array<{ id: string, chatId: string, kind: Types.AiHubChatArtifactKind, status: Types.AiHubChatArtifactStatus, artifactId: string, artifactName: string, metadataJson: string | null, environmentId: any, createdAt: any, statusChangedAt: any }> } };

export type DeleteAiHubChatArtifactMutationVariables = Exact<{
  input: Types.DeleteAiHubChatArtifactInput;
}>;


export type DeleteAiHubChatArtifactMutation = { deleteAiHubChatArtifact: boolean };

export type RecordReferencedAiHubChatArtifactMutationVariables = Exact<{
  input: Types.RecordReferencedAiHubChatArtifactInput;
}>;


export type RecordReferencedAiHubChatArtifactMutation = { recordReferencedAiHubChatArtifact: { id: string, chatId: string, kind: Types.AiHubChatArtifactKind, status: Types.AiHubChatArtifactStatus, artifactId: string, artifactName: string, environmentId: any, createdAt: any } };

export type AddAiHubUserConnectorMutationVariables = Exact<{
  workspaceId: string | number;
  componentName: string;
  componentVersion: number;
  connectionId?: string | number | null | undefined;
  environment: number;
}>;


export type AddAiHubUserConnectorMutation = { addAiHubUserConnector: string };

export type AiHubChatToolableComponentsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiHubChatToolableComponentsQuery = { aiHubChatToolableComponents: Array<{ componentName: string, componentVersion: number, connectionRequired: boolean, description: string | null, icon: string | null, title: string | null, tools: Array<{ description: string | null, name: string, title: string | null }> }> };

export type AiHubChatToolsQueryVariables = Exact<{
  workspaceId: string | number;
  chatId: string | number;
}>;


export type AiHubChatToolsQuery = { aiHubChatTools: Array<{ clusterElementName: string, componentName: string, componentVersion: number, connectionId: string | null, chatComponentId: string, chatId: string, chatToolId: string, environment: number, parameters: any }> };

export type AiHubUserConnectorsQueryVariables = Exact<{
  workspaceId: string | number;
  chatId?: string | number | null | undefined;
}>;


export type AiHubUserConnectorsQuery = { aiHubUserConnectors: Array<{ componentName: string, componentVersion: number, connectionId: string | null, connectionRequired: boolean, description: string | null, enabled: boolean, enabledInChat: boolean, icon: string | null, id: string, title: string | null, tools: Array<{ description: string | null, enabled: boolean, name: string, parameters: any, title: string | null }> }> };

export type AttachAiHubChatToolMutationVariables = Exact<{
  input: Types.AttachAiHubChatToolInput;
}>;


export type AttachAiHubChatToolMutation = { attachAiHubChatTool: { clusterElementName: string, componentName: string, componentVersion: number, connectionId: string | null, chatComponentId: string, chatId: string, chatToolId: string, environment: number, parameters: any } };

export type DetachAiHubChatComponentMutationVariables = Exact<{
  workspaceId: string | number;
  chatComponentId: string | number;
}>;


export type DetachAiHubChatComponentMutation = { detachAiHubChatComponent: boolean };

export type RemoveAiHubChatToolMutationVariables = Exact<{
  workspaceId: string | number;
  chatToolId: string | number;
}>;


export type RemoveAiHubChatToolMutation = { removeAiHubChatTool: boolean };

export type RemoveAiHubUserConnectorMutationVariables = Exact<{
  workspaceId: string | number;
  connectorId: string | number;
}>;


export type RemoveAiHubUserConnectorMutation = { removeAiHubUserConnector: boolean };

export type SetAiHubChatConnectorEnabledMutationVariables = Exact<{
  workspaceId: string | number;
  chatId: string | number;
  connectorId: string | number;
  enabled: boolean;
}>;


export type SetAiHubChatConnectorEnabledMutation = { setAiHubChatConnectorEnabled: boolean };

export type SetAiHubUserConnectorEnabledMutationVariables = Exact<{
  workspaceId: string | number;
  connectorId: string | number;
  enabled: boolean;
}>;


export type SetAiHubUserConnectorEnabledMutation = { setAiHubUserConnectorEnabled: boolean };

export type SetAiHubUserConnectorToolEnabledMutationVariables = Exact<{
  workspaceId: string | number;
  connectorId: string | number;
  toolName: string;
  enabled: boolean;
}>;


export type SetAiHubUserConnectorToolEnabledMutation = { setAiHubUserConnectorToolEnabled: boolean };

export type SetAiHubUserConnectorToolParametersMutationVariables = Exact<{
  workspaceId: string | number;
  connectorId: string | number;
  toolName: string;
  parameters: any;
}>;


export type SetAiHubUserConnectorToolParametersMutation = { setAiHubUserConnectorToolParameters: boolean };

export type UpdateAiHubChatToolParametersMutationVariables = Exact<{
  workspaceId: string | number;
  chatToolId: string | number;
  parameters: any;
}>;


export type UpdateAiHubChatToolParametersMutation = { updateAiHubChatToolParameters: { clusterElementName: string, componentName: string, componentVersion: number, connectionId: string | null, chatComponentId: string, chatId: string, chatToolId: string, environment: number, parameters: any } };

export type AiHubChatArtifactsByAiHubChatQueryVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
}>;


export type AiHubChatArtifactsByAiHubChatQuery = { aiHubChatArtifactsByAiHubChat: Array<{ id: string, chatId: string, kind: Types.AiHubChatArtifactKind, status: Types.AiHubChatArtifactStatus, artifactId: string, artifactName: string, metadataJson: string | null, environmentId: any, createdAt: any, statusChangedAt: any }> };

export type AiHubChatMessagesQueryVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
}>;


export type AiHubChatMessagesQuery = { aiHubChatMessages: Array<{ role: string, content: string, timestamp: any, toolEventsJson: string | null }> };

export type AiHubChatsQueryVariables = Exact<{
  workspaceId: string | number;
  environment: number;
  status?: Types.AiHubChatStatus | null | undefined;
}>;


export type AiHubChatsQuery = { aiHubChats: Array<{ id: string, workspaceId: any, userId: any, threadId: string, title: string | null, lastPreview: string | null, messageCount: number, status: Types.AiHubChatStatus, environmentId: any, createdAt: any, updatedAt: any, kind: Types.AiHubChatKind, workflowExecutionId: string | null, projectDeploymentId: any, autoTitled: boolean, aiAgentId: any }> };

export type AppendAiHubChatAssistantMessageMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  content: string;
}>;


export type AppendAiHubChatAssistantMessageMutation = { appendAiHubChatAssistantMessage: boolean };

export type BulkArchiveWorkflowChatAiHubChatsMutationVariables = Exact<{
  workspaceId: string | number;
  environment: number;
}>;


export type BulkArchiveWorkflowChatAiHubChatsMutation = { bulkArchiveWorkflowChatAiHubChats: number };

export type CancelAiHubRunMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  runId?: string | null | undefined;
}>;


export type CancelAiHubRunMutation = { cancelAiHubRun: boolean };

export type CancelWorkflowChatTurnMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
}>;


export type CancelWorkflowChatTurnMutation = { cancelWorkflowChatTurn: boolean };

export type CreateAgentChatAiHubChatMutationVariables = Exact<{
  workspaceId: string | number;
  environment: number;
  workflowExecutionId: string;
  projectDeploymentId: string | number;
  title?: string | null | undefined;
}>;


export type CreateAgentChatAiHubChatMutation = { createAgentChatAiHubChat: { id: string, workspaceId: any, userId: any, threadId: string, title: string | null, lastPreview: string | null, messageCount: number, status: Types.AiHubChatStatus, environmentId: any, createdAt: any, updatedAt: any, kind: Types.AiHubChatKind, workflowExecutionId: string | null, projectDeploymentId: any, autoTitled: boolean, aiAgentId: any } };

export type CreateAiHubChatMutationVariables = Exact<{
  workspaceId: string | number;
  environment: number;
  threadId: string;
}>;


export type CreateAiHubChatMutation = { createAiHubChat: { id: string, workspaceId: any, userId: any, threadId: string, title: string | null, lastPreview: string | null, messageCount: number, status: Types.AiHubChatStatus, environmentId: any, createdAt: any, updatedAt: any, kind: Types.AiHubChatKind, workflowExecutionId: string | null, projectDeploymentId: any, autoTitled: boolean, aiAgentId: any } };

export type CreateWorkflowChatAiHubChatMutationVariables = Exact<{
  workspaceId: string | number;
  environment: number;
  workflowExecutionId: string;
  projectDeploymentId: string | number;
  title?: string | null | undefined;
}>;


export type CreateWorkflowChatAiHubChatMutation = { createWorkflowChatAiHubChat: { id: string, workspaceId: any, userId: any, threadId: string, title: string | null, lastPreview: string | null, messageCount: number, status: Types.AiHubChatStatus, environmentId: any, createdAt: any, updatedAt: any, kind: Types.AiHubChatKind, workflowExecutionId: string | null, projectDeploymentId: any, autoTitled: boolean, aiAgentId: any } };

export type DeleteAiHubChatMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
}>;


export type DeleteAiHubChatMutation = { deleteAiHubChat: boolean };

export type GenerateAiHubChatTitleMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
}>;


export type GenerateAiHubChatTitleMutation = { generateAiHubChatTitle: { id: string, workspaceId: any, userId: any, threadId: string, title: string | null, lastPreview: string | null, messageCount: number, status: Types.AiHubChatStatus, environmentId: any, createdAt: any, updatedAt: any, kind: Types.AiHubChatKind, workflowExecutionId: string | null, projectDeploymentId: any, autoTitled: boolean, aiAgentId: any } };

export type TruncateAiHubChatMessagesMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  fromMessageIndex: number;
}>;


export type TruncateAiHubChatMessagesMutation = { truncateAiHubChatMessages: number };

export type UpdateAiHubChatMutationVariables = Exact<{
  input: Types.AiHubChatPatchInput;
}>;


export type UpdateAiHubChatMutation = { updateAiHubChat: { id: string, workspaceId: any, userId: any, threadId: string, title: string | null, lastPreview: string | null, messageCount: number, status: Types.AiHubChatStatus, environmentId: any, createdAt: any, updatedAt: any, kind: Types.AiHubChatKind, workflowExecutionId: string | null, projectDeploymentId: any, autoTitled: boolean, aiAgentId: any } };

export type AddAiHubMcpServerMutationVariables = Exact<{
  workspaceId: string | number;
  name: string;
  url: string;
  authToken?: string | null | undefined;
  environment: number;
}>;


export type AddAiHubMcpServerMutation = { addAiHubMcpServer: string };

export type AiHubMcpServerToolsQueryVariables = Exact<{
  workspaceId: string | number;
  mcpServerId: string | number;
}>;


export type AiHubMcpServerToolsQuery = { aiHubMcpServerTools: Array<{ description: string | null, enabled: boolean, name: string }> };

export type AiHubMcpServersQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiHubMcpServersQuery = { aiHubMcpServers: Array<{ enabled: boolean, hasAuthToken: boolean, id: string, name: string, url: string }> };

export type RemoveAiHubMcpServerMutationVariables = Exact<{
  workspaceId: string | number;
  mcpServerId: string | number;
}>;


export type RemoveAiHubMcpServerMutation = { removeAiHubMcpServer: boolean };

export type SetAiHubMcpServerEnabledMutationVariables = Exact<{
  workspaceId: string | number;
  mcpServerId: string | number;
  enabled: boolean;
}>;


export type SetAiHubMcpServerEnabledMutation = { setAiHubMcpServerEnabled: boolean };

export type SetAiHubMcpServerToolEnabledMutationVariables = Exact<{
  workspaceId: string | number;
  mcpServerId: string | number;
  toolName: string;
  enabled: boolean;
}>;


export type SetAiHubMcpServerToolEnabledMutation = { setAiHubMcpServerToolEnabled: boolean };

export type AiHubWorkspaceSettingsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiHubWorkspaceSettingsQuery = { aiHubWorkspaceSettings: { workspaceId: string, voiceWebhookUrl: string | null } | null };

export type UpdateAiHubVoiceWebhookUrlMutationVariables = Exact<{
  input: Types.UpdateAiHubVoiceWebhookUrlInput;
}>;


export type UpdateAiHubVoiceWebhookUrlMutation = { updateAiHubVoiceWebhookUrl: { workspaceId: string, voiceWebhookUrl: string | null } | null };

export type AiAutoMemoriesQueryVariables = Exact<{
  workspaceId: string | number;
  environment: number;
  memoryType?: Types.AiAutoMemoryType | null | undefined;
  principalType?: Types.AiAutoMemoryPrincipalType | null | undefined;
  principalId?: any;
}>;


export type AiAutoMemoriesQuery = { aiAutoMemories: Array<{ id: string, workspaceId: any, principalType: Types.AiAutoMemoryPrincipalType, principalId: any, name: string, title: string, description: string | null, memoryType: Types.AiAutoMemoryType, content: string, environmentId: any, createdAt: any, updatedAt: any }> };

export type AiAutoMemoryQueryVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  environment: number;
  principalType?: Types.AiAutoMemoryPrincipalType | null | undefined;
  principalId?: any;
}>;


export type AiAutoMemoryQuery = { aiAutoMemory: { id: string, workspaceId: any, principalType: Types.AiAutoMemoryPrincipalType, principalId: any, name: string, title: string, description: string | null, memoryType: Types.AiAutoMemoryType, content: string, environmentId: any, createdAt: any, updatedAt: any } | null };

export type AiAutoMemoryPrincipalsQueryVariables = Exact<{
  workspaceId: string | number;
  environment: number;
}>;


export type AiAutoMemoryPrincipalsQuery = { aiAutoMemoryPrincipals: Array<{ principalType: Types.AiAutoMemoryPrincipalType, principalId: any, label: string, memoryCount: number }> };

export type DeleteAiAutoMemoryMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  environment: number;
  principalType?: Types.AiAutoMemoryPrincipalType | null | undefined;
  principalId?: any;
}>;


export type DeleteAiAutoMemoryMutation = { deleteAiAutoMemory: boolean };

export type UpdateAiAutoMemoryMutationVariables = Exact<{
  input: Types.UpdateAiAutoMemoryInput;
}>;


export type UpdateAiAutoMemoryMutation = { updateAiAutoMemory: { id: string, workspaceId: any, principalType: Types.AiAutoMemoryPrincipalType, principalId: any, name: string, title: string, description: string | null, memoryType: Types.AiAutoMemoryType, content: string, environmentId: any, createdAt: any, updatedAt: any } };

export type AiSkillQueryVariables = Exact<{
  id: string | number;
}>;


export type AiSkillQuery = { aiSkill: { id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any } };

export type AiSkillFileContentQueryVariables = Exact<{
  id: string | number;
  path: string;
}>;


export type AiSkillFileContentQuery = { aiSkillFileContent: string };

export type AiSkillFilePathsQueryVariables = Exact<{
  id: string | number;
}>;


export type AiSkillFilePathsQuery = { aiSkillFilePaths: Array<string> };

export type AiSkillTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type AiSkillTagsQuery = { aiSkillTags: Array<{ id: string, name: string }> };

export type AiSkillsQueryVariables = Exact<{ [key: string]: never; }>;


export type AiSkillsQuery = { aiSkills: Array<{ id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any, tags: Array<{ id: string, name: string }> }> };

export type CreateAdditionalFilesInSkillMutationVariables = Exact<{
  id: string | number;
  additionalFiles: any;
}>;


export type CreateAdditionalFilesInSkillMutation = { createAdditionalFilesInSkill: { description: string | null, id: string, lastModifiedDate: any, name: string } };

export type CreateAiSkillMutationVariables = Exact<{
  name: string;
  description?: string | null | undefined;
  filename: string;
  fileBytes: string;
}>;


export type CreateAiSkillMutation = { createAiSkill: { id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any } };

export type CreateAiSkillFromInstructionsMutationVariables = Exact<{
  name: string;
  description?: string | null | undefined;
  instructions: string;
}>;


export type CreateAiSkillFromInstructionsMutation = { createAiSkillFromInstructions: { id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any } };

export type DeleteAiSkillMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiSkillMutation = { deleteAiSkill: boolean };

export type RemoveFileInSkillMutationVariables = Exact<{
  id: string | number;
  path: string;
}>;


export type RemoveFileInSkillMutation = { removeFileInSkill: { description: string | null, id: string, lastModifiedDate: any, name: string } };

export type UpdateAiSkillMutationVariables = Exact<{
  id: string | number;
  name: string;
  description?: string | null | undefined;
}>;


export type UpdateAiSkillMutation = { updateAiSkill: { id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any } };

export type UpdateAiSkillContentMutationVariables = Exact<{
  id: string | number;
  path?: string | null | undefined;
  content: string;
}>;


export type UpdateAiSkillContentMutation = { updateAiSkillContent: { description: string | null, id: string, lastModifiedDate: any, name: string } };

export type UpdateAiSkillTagsMutationVariables = Exact<{
  id: string | number;
  tags?: Array<Types.AiSkillTagInput> | Types.AiSkillTagInput | null | undefined;
}>;


export type UpdateAiSkillTagsMutation = { updateAiSkillTags: { id: string } };

export type AuditEventsQueryVariables = Exact<{
  principal?: string | null | undefined;
  eventType?: string | null | undefined;
  fromDate?: any;
  toDate?: any;
  dataSearch?: string | null | undefined;
  page?: number | null | undefined;
  size?: number | null | undefined;
}>;


export type AuditEventsQuery = { auditEvents: { number: number, size: number, totalElements: number, totalPages: number, content: Array<{ eventDate: any, eventType: string, id: string, principal: string | null, data: Array<{ key: string, value: string }> }> } };

export type AuditEventTypesQueryVariables = Exact<{ [key: string]: never; }>;


export type AuditEventTypesQuery = { auditEventTypes: Array<string> };

export type AddAiAgentChannelMutationVariables = Exact<{
  input: Types.AddAiAgentChannelInput;
}>;


export type AddAiAgentChannelMutation = { addAiAgentChannel: { id: string, channelType: string, position: number, parameters: any, connectionId: string | null } };

export type AddAiAgentElementMutationVariables = Exact<{
  input: Types.AddAiAgentElementInput;
}>;


export type AddAiAgentElementMutation = { addAiAgentElement: { id: string, kind: string, referenceId: string | null, position: number, parameters: any, connectionId: string | null } };

export type AiAgentQueryVariables = Exact<{
  id: string | number;
}>;


export type AiAgentQuery = { aiAgent: { id: string, name: string, title: string, description: string | null, instructions: string | null, workspaceId: string | null, projectId: string, uuid: string, unpublishedChanges: boolean, lastPublishedVersion: number, settings: any, lastModifiedDate: string | null, draftWorkflowId: string, visibility: Types.ResourceVisibility, channels: Array<{ id: string, channelType: string, position: number, parameters: any, connectionId: string | null }>, elements: Array<{ id: string, kind: string, referenceId: string | null, position: number, parameters: any, connectionId: string | null }>, tags: Array<{ id: string, name: string }> } | null };

export type AiAgentChannelDefinitionsQueryVariables = Exact<{ [key: string]: never; }>;


export type AiAgentChannelDefinitionsQuery = { aiAgentChannelDefinitions: Array<{ approvalCapable: boolean, channelType: string, componentName: string, componentVersion: number, connectionRequired: boolean, description: string | null, icon: string | null, pinned: boolean, propertiesConfigurable: boolean, replyActionName: string | null, schedule: boolean, title: string, triggerName: string }> };

export type AiAgentDeploymentTagsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiAgentDeploymentTagsQuery = { aiAgentDeploymentTags: Array<{ id: string, name: string }> };

export type AiAgentDeploymentsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiAgentDeploymentsQuery = { aiAgentDeployments: Array<{ id: string, name: string, agentId: string, agentTitle: string, projectId: string, environmentId: number, enabled: boolean, projectVersion: number, lastExecutionDate: string | null, tags: Array<{ id: string, name: string }> | null, workflows: Array<{ workflowId: string, enabled: boolean, triggers: Array<{ name: string, type: string, parameters: any, staticWebhookUrl: string | null }> }> }> };

export type AiAgentGrantsQueryVariables = Exact<{
  agentId: string | number;
}>;


export type AiAgentGrantsQuery = { aiAgentGrants: Array<any> };

export type AiAgentTagsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiAgentTagsQuery = { aiAgentTags: Array<{ id: string, name: string }> };

export type AiAgentVersionsQueryVariables = Exact<{
  id: string | number;
}>;


export type AiAgentVersionsQuery = { aiAgentVersions: Array<{ description: string | null, publishedDate: string | null, status: string, version: number }> };

export type AiAgentsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiAgentsQuery = { aiAgents: Array<{ id: string, name: string, title: string, description: string | null, projectId: string, unpublishedChanges: boolean, lastPublishedVersion: number, publishedDate: string | null, lastModifiedDate: string | null, visibility: Types.ResourceVisibility, tags: Array<{ id: string, name: string }>, elements: Array<{ id: string, kind: string }>, channels: Array<{ id: string, channelType: string, parameters: any }> }> };

export type CreateAiAgentMutationVariables = Exact<{
  input: Types.CreateAiAgentInput;
}>;


export type CreateAiAgentMutation = { createAiAgent: { id: string } };

export type DeleteAiAgentMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiAgentMutation = { deleteAiAgent: boolean };

export type DeleteAiAgentChannelMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiAgentChannelMutation = { deleteAiAgentChannel: boolean };

export type DeleteAiAgentElementMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiAgentElementMutation = { deleteAiAgentElement: boolean };

export type ExportAiAgentQueryVariables = Exact<{
  id: string | number;
}>;


export type ExportAiAgentQuery = { exportAiAgent: string };

export type GrantAiAgentAccessMutationVariables = Exact<{
  agentId: string | number;
  userId: string | number;
}>;


export type GrantAiAgentAccessMutation = { grantAiAgentAccess: boolean };

export type ImportAiAgentMutationVariables = Exact<{
  workspaceId: string | number;
  json: string;
}>;


export type ImportAiAgentMutation = { importAiAgent: { id: string, title: string } };

export type PublishAiAgentMutationVariables = Exact<{
  id: string | number;
  description?: string | null | undefined;
}>;


export type PublishAiAgentMutation = { publishAiAgent: number };

export type RevokeAiAgentAccessMutationVariables = Exact<{
  agentId: string | number;
  userId: string | number;
}>;


export type RevokeAiAgentAccessMutation = { revokeAiAgentAccess: boolean };

export type SetAiAgentVisibilityMutationVariables = Exact<{
  agentId: string | number;
  visibility: Types.ResourceVisibility;
}>;


export type SetAiAgentVisibilityMutation = { setAiAgentVisibility: boolean };

export type UpdateAiAgentMutationVariables = Exact<{
  input: Types.UpdateAiAgentInput;
}>;


export type UpdateAiAgentMutation = { updateAiAgent: { id: string } };

export type UpdateAiAgentChannelMutationVariables = Exact<{
  input: Types.UpdateAiAgentChannelInput;
}>;


export type UpdateAiAgentChannelMutation = { updateAiAgentChannel: boolean };

export type UpdateAiAgentDeploymentTagsMutationVariables = Exact<{
  input: Types.UpdateAiAgentDeploymentTagsInput;
}>;


export type UpdateAiAgentDeploymentTagsMutation = { updateAiAgentDeploymentTags: boolean };

export type UpdateAiAgentElementMutationVariables = Exact<{
  input: Types.UpdateAiAgentElementInput;
}>;


export type UpdateAiAgentElementMutation = { updateAiAgentElement: boolean };

export type UpdateAiAgentSettingsMutationVariables = Exact<{
  id: string | number;
  settings: any;
}>;


export type UpdateAiAgentSettingsMutation = { updateAiAgentSettings: boolean };

export type UpdateAiAgentTagsMutationVariables = Exact<{
  input: Types.UpdateAiAgentTagsInput;
}>;


export type UpdateAiAgentTagsMutation = { updateAiAgentTags: boolean };

export type WorkspaceChatAgentsQueryVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
}>;


export type WorkspaceChatAgentsQuery = { workspaceChatAgents: Array<{ agentName: string, agentTitle: string, aiAgentId: string, projectDeploymentId: string, workflowExecutionId: string, workflowLabel: string }> };

export type AiDatasetsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiDatasetsQuery = { aiDatasets: Array<{ archivedDate: any, createdDate: any, description: string | null, id: string, name: string, tags: string | null, workspaceId: string }> };

export type AiDatasetVersionsQueryVariables = Exact<{
  datasetId: string | number;
}>;


export type AiDatasetVersionsQuery = { aiDatasetVersions: Array<{ createdDate: any, datasetId: string, frozen: boolean, id: string, itemCount: number, label: string | null }> };

export type AiDatasetItemsQueryVariables = Exact<{
  versionId: string | number;
}>;


export type AiDatasetItemsQuery = { aiDatasetItems: Array<{ createdDate: any, datasetVersionId: string, expectedOutput: string | null, id: string, input: string, metadata: string | null, sourceTraceId: string | null }> };

export type AiEvalRulesQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiEvalRulesQuery = { aiEvalRules: Array<{ createdDate: any, delaySeconds: number | null, enabled: boolean, filters: string | null, id: string, lastModifiedDate: any, model: string, name: string, projectId: string | null, promptTemplate: string, samplingRate: number, scoreConfigId: string, version: number | null, workspaceId: string } | null> | null };

export type AiEvalRuleQueryVariables = Exact<{
  id: string | number;
}>;


export type AiEvalRuleQuery = { aiEvalRule: { createdDate: any, delaySeconds: number | null, enabled: boolean, filters: string | null, id: string, lastModifiedDate: any, model: string, name: string, projectId: string | null, promptTemplate: string, samplingRate: number, scoreConfigId: string, version: number | null, workspaceId: string } | null };

export type AiEvalExecutionsQueryVariables = Exact<{
  evalRuleId: string | number;
}>;


export type AiEvalExecutionsQuery = { aiEvalExecutions: Array<{ createdDate: any, errorMessage: string | null, evalRuleId: string, id: string, scoreId: string | null, status: Types.AiEvalExecutionStatus, traceId: string } | null> | null };

export type CreateAiEvalRuleMutationVariables = Exact<{
  delaySeconds?: number | null | undefined;
  enabled: boolean;
  filters?: string | null | undefined;
  model: string;
  name: string;
  projectId?: string | number | null | undefined;
  promptTemplate: string;
  samplingRate: number;
  scoreConfigId: string | number;
  workspaceId: string | number;
}>;


export type CreateAiEvalRuleMutation = { createAiEvalRule: { id: string, name: string } | null };

export type DeleteAiEvalRuleMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiEvalRuleMutation = { deleteAiEvalRule: boolean | null };

export type RunAiEvalRuleOnHistoricalTracesMutationVariables = Exact<{
  ruleId: string | number;
  startDate: any;
  endDate: any;
}>;


export type RunAiEvalRuleOnHistoricalTracesMutation = { runAiEvalRuleOnHistoricalTraces: number | null };

export type UpdateAiEvalRuleMutationVariables = Exact<{
  delaySeconds?: number | null | undefined;
  enabled: boolean;
  filters?: string | null | undefined;
  id: string | number;
  model: string;
  name: string;
  promptTemplate: string;
  samplingRate: number;
  scoreConfigId: string | number;
}>;


export type UpdateAiEvalRuleMutation = { updateAiEvalRule: { id: string, name: string } | null };

export type AiEvalScoreConfigsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiEvalScoreConfigsQuery = { aiEvalScoreConfigs: Array<{ categories: string | null, createdDate: any, dataType: Types.AiEvalScoreDataType | null, description: string | null, id: string, lastModifiedDate: any, maxValue: number | null, minValue: number | null, name: string, version: number | null, workspaceId: string } | null> | null };

export type AiEvalScoreConfigQueryVariables = Exact<{
  id: string | number;
}>;


export type AiEvalScoreConfigQuery = { aiEvalScoreConfig: { categories: string | null, createdDate: any, dataType: Types.AiEvalScoreDataType | null, description: string | null, id: string, lastModifiedDate: any, maxValue: number | null, minValue: number | null, name: string, version: number | null, workspaceId: string } | null };

export type CreateAiEvalScoreConfigMutationVariables = Exact<{
  categories?: string | null | undefined;
  dataType?: Types.AiEvalScoreDataType | null | undefined;
  description?: string | null | undefined;
  maxValue?: number | null | undefined;
  minValue?: number | null | undefined;
  name: string;
  workspaceId: string | number;
}>;


export type CreateAiEvalScoreConfigMutation = { createAiEvalScoreConfig: { id: string, name: string } | null };

export type DeleteAiEvalScoreConfigMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiEvalScoreConfigMutation = { deleteAiEvalScoreConfig: boolean | null };

export type UpdateAiEvalScoreConfigMutationVariables = Exact<{
  categories?: string | null | undefined;
  dataType?: Types.AiEvalScoreDataType | null | undefined;
  description?: string | null | undefined;
  id: string | number;
  maxValue?: number | null | undefined;
  minValue?: number | null | undefined;
  name: string;
}>;


export type UpdateAiEvalScoreConfigMutation = { updateAiEvalScoreConfig: { id: string, name: string } | null };

export type AiEvalScoresQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiEvalScoresQuery = { aiEvalScores: Array<{ comment: string | null, createdBy: string | null, createdDate: any, dataType: Types.AiEvalScoreDataType | null, evalRuleId: string | null, id: string, name: string, source: Types.AiEvalScoreSource, spanId: string | null, stringValue: string | null, traceId: string, value: number | null, workspaceId: string } | null> | null };

export type AiEvalScoresByTraceQueryVariables = Exact<{
  traceId: string | number;
}>;


export type AiEvalScoresByTraceQuery = { aiEvalScoresByTrace: Array<{ comment: string | null, createdBy: string | null, createdDate: any, dataType: Types.AiEvalScoreDataType | null, evalRuleId: string | null, id: string, name: string, source: Types.AiEvalScoreSource, spanId: string | null, stringValue: string | null, traceId: string, value: number | null, workspaceId: string } | null> | null };

export type CreateAiEvalScoreMutationVariables = Exact<{
  comment?: string | null | undefined;
  dataType: Types.AiEvalScoreDataType;
  name: string;
  source: Types.AiEvalScoreSource;
  spanId?: string | number | null | undefined;
  stringValue?: string | null | undefined;
  traceId: string | number;
  value?: number | null | undefined;
  workspaceId: string | number;
}>;


export type CreateAiEvalScoreMutation = { createAiEvalScore: { id: string, name: string } | null };

export type DeleteAiEvalScoreMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiEvalScoreMutation = { deleteAiEvalScore: boolean | null };

export type AiEvalScoreAnalyticsQueryVariables = Exact<{
  workspaceId: string | number;
  startDate: any;
  endDate: any;
}>;


export type AiEvalScoreAnalyticsQuery = { aiEvalScoreAnalytics: Array<{ average: number | null, count: number | null, dataType: Types.AiEvalScoreDataType | null, max: number | null, min: number | null, name: string | null, distribution: Array<{ count: number | null, value: string | null } | null> | null } | null> | null };

export type AiEvalScoreTrendQueryVariables = Exact<{
  workspaceId: string | number;
  name: string;
  startDate: any;
  endDate: any;
}>;


export type AiEvalScoreTrendQuery = { aiEvalScoreTrend: Array<{ average: number | null, count: number, day: any } | null> | null };

export type AiExperimentComparisonQueryVariables = Exact<{
  experimentIds: Array<string | number> | string | number;
}>;


export type AiExperimentComparisonQuery = { experimentComparison: { aggregateScoreDeltas: Array<{ scoreName: string, deltas: Array<{ average: number | null, count: number, experimentId: string }> }>, experiments: Array<{ averageLatencyMs: number | null, completedRuns: number, failedRuns: number, id: string, model: string | null, totalCost: number | null, totalRuns: number }>, rows: Array<{ datasetItemId: string, runsByExperiment: Array<{ cost: number | null, experimentId: string, latencyMs: number | null, runId: string, status: string, traceId: string | null, scores: Array<{ dataType: string, name: string, stringValue: string | null, value: number | null }> }> }> } | null };

export type AiExperimentsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiExperimentsQuery = { aiExperiments: Array<{ completedDate: any, completedRuns: number, createdDate: any, datasetVersionId: string, failedRuns: number, id: string, metadata: string | null, model: string | null, promptVersionId: string | null, startedDate: any, status: string, stopRequested: boolean, totalRuns: number }> };

export type AiExperimentRunsQueryVariables = Exact<{
  experimentId: string | number;
}>;


export type AiExperimentRunsQuery = { aiExperimentRuns: Array<{ cost: number | null, createdDate: any, datasetItemId: string, errorMessage: string | null, experimentId: string, id: string, latencyMs: number | null, status: string, traceId: string | null }> };

export type AiExperimentRunByTraceIdQueryVariables = Exact<{
  traceId: string | number;
}>;


export type AiExperimentRunByTraceIdQuery = { aiExperimentRunByTraceId: { cost: number | null, createdDate: any, datasetItemId: string, errorMessage: string | null, experimentId: string, id: string, latencyMs: number | null, status: string, traceId: string | null } | null };

export type AiGatewayBudgetQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiGatewayBudgetQuery = { aiGatewayBudget: { alertThreshold: number, amount: string, createdDate: any, enabled: boolean, enforcementMode: Types.AiGatewayBudgetEnforcementMode, id: string, lastModifiedDate: any, period: Types.AiGatewayBudgetPeriod, version: number | null } | null };

export type CreateAiGatewayBudgetMutationVariables = Exact<{
  input: Types.CreateAiGatewayBudgetInput;
}>;


export type CreateAiGatewayBudgetMutation = { createAiGatewayBudget: { alertThreshold: number, amount: string, createdDate: any, enabled: boolean, enforcementMode: Types.AiGatewayBudgetEnforcementMode, id: string, lastModifiedDate: any, period: Types.AiGatewayBudgetPeriod, version: number | null } | null };

export type UpdateAiGatewayBudgetMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateAiGatewayBudgetInput;
}>;


export type UpdateAiGatewayBudgetMutation = { updateAiGatewayBudget: { alertThreshold: number, amount: string, createdDate: any, enabled: boolean, enforcementMode: Types.AiGatewayBudgetEnforcementMode, id: string, lastModifiedDate: any, period: Types.AiGatewayBudgetPeriod, version: number | null } | null };

export type DeleteAiGatewayBudgetMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiGatewayBudgetMutation = { deleteAiGatewayBudget: boolean | null };

export type PlaygroundChatCompletionMutationVariables = Exact<{
  input: Types.PlaygroundChatCompletionInput;
}>;


export type PlaygroundChatCompletionMutation = { playgroundChatCompletion: { completionTokens: number | null, content: string | null, cost: number | null, finishReason: string | null, latencyMs: number | null, model: string | null, promptTokens: number | null, totalTokens: number | null, traceId: string | null } | null };

export type AiGatewayProjectSettingsQueryVariables = Exact<{
  projectId: string | number;
}>;


export type AiGatewayProjectSettingsQuery = { aiGatewayProjectSettings: { blockedTerms: string | null, injectionDetectionEnabled: boolean | null, moderationEnabled: boolean | null, projectId: string, redactPii: boolean | null, redactSecrets: boolean | null, scanResponses: boolean | null } | null };

export type UpdateAiGatewayProjectSettingsMutationVariables = Exact<{
  input: Types.AiGatewayProjectSettingsInput;
}>;


export type UpdateAiGatewayProjectSettingsMutation = { updateAiGatewayProjectSettings: { blockedTerms: string | null, injectionDetectionEnabled: boolean | null, moderationEnabled: boolean | null, projectId: string, redactPii: boolean | null, redactSecrets: boolean | null, scanResponses: boolean | null } | null };

export type AiGatewayProjectsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiGatewayProjectsQuery = { aiGatewayProjects: Array<{ cachingEnabled: boolean | null, cacheTtlMinutes: number | null, compressionEnabled: boolean | null, createdDate: any, description: string | null, id: string, lastModifiedDate: any, logRetentionDays: number | null, name: string, retryMaxAttempts: number | null, routingPolicyId: string | null, slug: string, timeoutSeconds: number | null, version: number | null }> };

export type CreateAiGatewayProjectMutationVariables = Exact<{
  input: Types.CreateAiGatewayProjectInput;
}>;


export type CreateAiGatewayProjectMutation = { createAiGatewayProject: { id: string, name: string, slug: string } | null };

export type UpdateAiGatewayProjectMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateAiGatewayProjectInput;
}>;


export type UpdateAiGatewayProjectMutation = { updateAiGatewayProject: { id: string, name: string, slug: string } | null };

export type DeleteAiGatewayProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiGatewayProjectMutation = { deleteAiGatewayProject: boolean | null };

export type AiGatewayProvidersQueryVariables = Exact<{ [key: string]: never; }>;


export type AiGatewayProvidersQuery = { aiGatewayProviders: Array<{ baseUrl: string | null, config: string | null, createdBy: string | null, createdDate: any, enabled: boolean, id: string, lastModifiedBy: string | null, lastModifiedDate: any, name: string, type: Types.AiGatewayProviderType, version: number | null } | null> | null };

export type AiGatewayProviderQueryVariables = Exact<{
  id: string | number;
}>;


export type AiGatewayProviderQuery = { aiGatewayProvider: { baseUrl: string | null, config: string | null, createdBy: string | null, createdDate: any, enabled: boolean, id: string, lastModifiedBy: string | null, lastModifiedDate: any, name: string, type: Types.AiGatewayProviderType, version: number | null } | null };

export type CreateAiGatewayProviderMutationVariables = Exact<{
  input: Types.CreateAiGatewayProviderInput;
}>;


export type CreateAiGatewayProviderMutation = { createAiGatewayProvider: { baseUrl: string | null, config: string | null, createdBy: string | null, createdDate: any, enabled: boolean, id: string, lastModifiedBy: string | null, lastModifiedDate: any, name: string, type: Types.AiGatewayProviderType, version: number | null } | null };

export type UpdateAiGatewayProviderMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateAiGatewayProviderInput;
}>;


export type UpdateAiGatewayProviderMutation = { updateAiGatewayProvider: { baseUrl: string | null, config: string | null, createdBy: string | null, createdDate: any, enabled: boolean, id: string, lastModifiedBy: string | null, lastModifiedDate: any, name: string, type: Types.AiGatewayProviderType, version: number | null } | null };

export type DeleteAiGatewayProviderMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiGatewayProviderMutation = { deleteAiGatewayProvider: boolean | null };

export type AiGatewayRateLimitsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiGatewayRateLimitsQuery = { aiGatewayRateLimits: Array<{ createdDate: any, enabled: boolean, id: string, lastModifiedDate: any, limitType: Types.AiGatewayRateLimitType, limitValue: number, name: string, projectId: string | null, propertyKey: string | null, scope: Types.AiGatewayRateLimitScope, version: number | null, windowSeconds: number }> };

export type CreateAiGatewayRateLimitMutationVariables = Exact<{
  input: Types.CreateAiGatewayRateLimitInput;
}>;


export type CreateAiGatewayRateLimitMutation = { createAiGatewayRateLimit: { createdDate: any, enabled: boolean, id: string, lastModifiedDate: any, limitType: Types.AiGatewayRateLimitType, limitValue: number, name: string, projectId: string | null, propertyKey: string | null, scope: Types.AiGatewayRateLimitScope, version: number | null, windowSeconds: number } | null };

export type UpdateAiGatewayRateLimitMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateAiGatewayRateLimitInput;
}>;


export type UpdateAiGatewayRateLimitMutation = { updateAiGatewayRateLimit: { createdDate: any, enabled: boolean, id: string, lastModifiedDate: any, limitType: Types.AiGatewayRateLimitType, limitValue: number, name: string, projectId: string | null, propertyKey: string | null, scope: Types.AiGatewayRateLimitScope, version: number | null, windowSeconds: number } | null };

export type DeleteAiGatewayRateLimitMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiGatewayRateLimitMutation = { deleteAiGatewayRateLimit: boolean | null };

export type AiGatewayRequestLogsQueryVariables = Exact<{
  startDate: any;
  endDate: any;
}>;


export type AiGatewayRequestLogsQuery = { aiGatewayRequestLogs: Array<{ apiKeyId: string | null, cacheHit: boolean | null, cost: string | null, createdDate: any, errorMessage: string | null, id: string, inputTokens: number | null, latencyMs: number | null, outputTokens: number | null, requestId: string, requestedModel: string | null, routedModel: string | null, routedProvider: string | null, routingPolicyId: string | null, routingStrategy: string | null, status: number | null } | null> | null };

export type AiGatewayRoutingPoliciesQueryVariables = Exact<{ [key: string]: never; }>;


export type AiGatewayRoutingPoliciesQuery = { aiGatewayRoutingPolicies: Array<{ config: string | null, createdDate: any, enabled: boolean, fallbackModel: string | null, id: string, lastModifiedDate: any, name: string, strategy: Types.AiGatewayRoutingStrategyType, version: number | null, deployments: Array<{ enabled: boolean, id: string, maxRpm: number | null, maxTpm: number | null, modelId: string, priorityOrder: number, routingPolicyId: string, weight: number } | null> | null } | null> | null };

export type CreateAiGatewayRoutingPolicyMutationVariables = Exact<{
  input: Types.CreateAiGatewayRoutingPolicyInput;
}>;


export type CreateAiGatewayRoutingPolicyMutation = { createAiGatewayRoutingPolicy: { config: string | null, createdDate: any, enabled: boolean, fallbackModel: string | null, id: string, lastModifiedDate: any, name: string, strategy: Types.AiGatewayRoutingStrategyType, version: number | null } | null };

export type UpdateAiGatewayRoutingPolicyMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateAiGatewayRoutingPolicyInput;
}>;


export type UpdateAiGatewayRoutingPolicyMutation = { updateAiGatewayRoutingPolicy: { config: string | null, createdDate: any, enabled: boolean, fallbackModel: string | null, id: string, lastModifiedDate: any, name: string, strategy: Types.AiGatewayRoutingStrategyType, version: number | null } | null };

export type DeleteAiGatewayRoutingPolicyMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiGatewayRoutingPolicyMutation = { deleteAiGatewayRoutingPolicy: boolean | null };

export type AiGatewaySpendSummariesQueryVariables = Exact<{
  startDate: any;
  endDate: any;
}>;


export type AiGatewaySpendSummariesQuery = { aiGatewaySpendSummaries: Array<{ apiKeyId: string | null, createdDate: any, id: string, model: string | null, periodEnd: any, periodStart: any, provider: string | null, requestCount: number | null, totalCost: string | null, totalInputTokens: any, totalOutputTokens: any } | null> | null };

export type AiGatewayWorkspaceSettingsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiGatewayWorkspaceSettingsQuery = { aiGatewayWorkspaceSettings: { cacheEnabled: boolean | null, cacheTtlSeconds: number | null, defaultRoutingPolicyId: string | null, logRetentionDays: number | null, retryCount: number | null, softBudgetWarningPct: number | null, timeoutMs: number | null, workspaceId: string } | null };

export type UpdateAiGatewayWorkspaceSettingsMutationVariables = Exact<{
  input: Types.AiGatewayWorkspaceSettingsInput;
}>;


export type UpdateAiGatewayWorkspaceSettingsMutation = { updateAiGatewayWorkspaceSettings: { cacheEnabled: boolean | null, cacheTtlSeconds: number | null, defaultRoutingPolicyId: string | null, logRetentionDays: number | null, retryCount: number | null, softBudgetWarningPct: number | null, timeoutMs: number | null, workspaceId: string } | null };

export type AiModelsQueryVariables = Exact<{ [key: string]: never; }>;


export type AiModelsQuery = { aiModels: Array<{ alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null> | null };

export type AiModelsByProviderQueryVariables = Exact<{
  providerId: string | number;
}>;


export type AiModelsByProviderQuery = { aiModelsByProvider: Array<{ alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null> | null };

export type CreateAiModelMutationVariables = Exact<{
  input: Types.CreateAiModelInput;
}>;


export type CreateAiModelMutation = { createAiModel: { alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null };

export type UpdateAiModelMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateAiModelInput;
}>;


export type UpdateAiModelMutation = { updateAiModel: { alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null };

export type DeleteAiModelMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiModelMutation = { deleteAiModel: boolean | null };

export type UnpinAiModelMutationVariables = Exact<{
  id: string | number;
}>;


export type UnpinAiModelMutation = { unpinAiModel: { alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null };

export type AiObservabilityAlertEventsQueryVariables = Exact<{
  alertRuleId: string | number;
}>;


export type AiObservabilityAlertEventsQuery = { aiObservabilityAlertEvents: Array<{ alertRuleId: string, createdDate: any, id: string, message: string | null, status: Types.AiObservabilityAlertEventStatus, triggeredValue: number | null } | null> | null };

export type AcknowledgeAiObservabilityAlertEventMutationVariables = Exact<{
  id: string | number;
}>;


export type AcknowledgeAiObservabilityAlertEventMutation = { acknowledgeAiObservabilityAlertEvent: { alertRuleId: string, createdDate: any, id: string, message: string | null, status: Types.AiObservabilityAlertEventStatus, triggeredValue: number | null } | null };

export type AiObservabilityAlertRulesQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiObservabilityAlertRulesQuery = { aiObservabilityAlertRules: Array<{ notificationIds: Array<string | null> | null, condition: Types.AiObservabilityAlertCondition, cooldownMinutes: number, createdDate: any, enabled: boolean, filters: string | null, id: string, lastModifiedDate: any, metric: Types.AiObservabilityAlertMetric, name: string, projectId: string | null, snoozedUntil: any, threshold: number, version: number | null, windowMinutes: number } | null> | null };

export type AiObservabilityAlertRuleQueryVariables = Exact<{
  id: string | number;
}>;


export type AiObservabilityAlertRuleQuery = { aiObservabilityAlertRule: { notificationIds: Array<string | null> | null, condition: Types.AiObservabilityAlertCondition, cooldownMinutes: number, createdDate: any, enabled: boolean, filters: string | null, id: string, lastModifiedDate: any, metric: Types.AiObservabilityAlertMetric, name: string, projectId: string | null, snoozedUntil: any, threshold: number, version: number | null, windowMinutes: number } | null };

export type CreateAiObservabilityAlertRuleMutationVariables = Exact<{
  input: Types.AiObservabilityAlertRuleInput;
}>;


export type CreateAiObservabilityAlertRuleMutation = { createAiObservabilityAlertRule: { notificationIds: Array<string | null> | null, condition: Types.AiObservabilityAlertCondition, cooldownMinutes: number, createdDate: any, enabled: boolean, filters: string | null, id: string, lastModifiedDate: any, metric: Types.AiObservabilityAlertMetric, name: string, projectId: string | null, threshold: number, version: number | null, windowMinutes: number } | null };

export type UpdateAiObservabilityAlertRuleMutationVariables = Exact<{
  id: string | number;
  input: Types.AiObservabilityAlertRuleInput;
}>;


export type UpdateAiObservabilityAlertRuleMutation = { updateAiObservabilityAlertRule: { notificationIds: Array<string | null> | null, condition: Types.AiObservabilityAlertCondition, cooldownMinutes: number, createdDate: any, enabled: boolean, filters: string | null, id: string, lastModifiedDate: any, metric: Types.AiObservabilityAlertMetric, name: string, projectId: string | null, threshold: number, version: number | null, windowMinutes: number } | null };

export type DeleteAiObservabilityAlertRuleMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiObservabilityAlertRuleMutation = { deleteAiObservabilityAlertRule: boolean | null };

export type TestAiObservabilityAlertRuleMutationVariables = Exact<{
  id: string | number;
}>;


export type TestAiObservabilityAlertRuleMutation = { testAiObservabilityAlertRule: number | null };

export type SnoozeAiObservabilityAlertRuleMutationVariables = Exact<{
  id: string | number;
  until: any;
}>;


export type SnoozeAiObservabilityAlertRuleMutation = { snoozeAiObservabilityAlertRule: { id: string, snoozedUntil: any } | null };

export type UnsnoozeAiObservabilityAlertRuleMutationVariables = Exact<{
  id: string | number;
}>;


export type UnsnoozeAiObservabilityAlertRuleMutation = { unsnoozeAiObservabilityAlertRule: { id: string, snoozedUntil: any } | null };

export type AiObservabilityExportJobsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiObservabilityExportJobsQuery = { aiObservabilityExportJobs: Array<{ createdBy: string, createdDate: any, errorMessage: string | null, filePath: string | null, filters: string | null, format: Types.AiObservabilityExportFormat, id: string, projectId: string | null, recordCount: number | null, scope: Types.AiObservabilityExportScope, status: Types.AiObservabilityExportJobStatus, type: Types.AiObservabilityExportJobType } | null> | null };

export type AiObservabilityExportJobQueryVariables = Exact<{
  id: string | number;
}>;


export type AiObservabilityExportJobQuery = { aiObservabilityExportJob: { createdBy: string, createdDate: any, errorMessage: string | null, filePath: string | null, filters: string | null, format: Types.AiObservabilityExportFormat, id: string, projectId: string | null, recordCount: number | null, scope: Types.AiObservabilityExportScope, status: Types.AiObservabilityExportJobStatus, type: Types.AiObservabilityExportJobType } | null };

export type CreateAiObservabilityExportJobMutationVariables = Exact<{
  workspaceId: string | number;
  projectId?: string | number | null | undefined;
  format: Types.AiObservabilityExportFormat;
  scope: Types.AiObservabilityExportScope;
  filters?: string | null | undefined;
}>;


export type CreateAiObservabilityExportJobMutation = { createAiObservabilityExportJob: { createdBy: string, createdDate: any, format: Types.AiObservabilityExportFormat, id: string, scope: Types.AiObservabilityExportScope, status: Types.AiObservabilityExportJobStatus, type: Types.AiObservabilityExportJobType } | null };

export type CancelAiObservabilityExportJobMutationVariables = Exact<{
  id: string | number;
}>;


export type CancelAiObservabilityExportJobMutation = { cancelAiObservabilityExportJob: { id: string, status: Types.AiObservabilityExportJobStatus } | null };

export type AiObservabilitySessionsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiObservabilitySessionsQuery = { aiObservabilitySessions: Array<{ createdDate: any, id: string, lastModifiedDate: any, name: string | null, projectId: string | null, traceCount: number | null, userId: string | null, version: number | null, workspaceId: string } | null> | null };

export type AiObservabilitySessionQueryVariables = Exact<{
  id: string | number;
}>;


export type AiObservabilitySessionQuery = { aiObservabilitySession: { createdDate: any, id: string, lastModifiedDate: any, name: string | null, projectId: string | null, userId: string | null, version: number | null, workspaceId: string, traces: Array<{ createdDate: any, id: string, name: string | null, source: Types.AiObservabilityTraceSource, status: Types.AiObservabilityTraceStatus, totalCost: number | null, totalInputTokens: number | null, totalLatencyMs: number | null, totalOutputTokens: number | null, userId: string | null } | null> | null } | null };

export type AiObservabilityTracesQueryVariables = Exact<{
  endDate: any;
  model?: string | null | undefined;
  source?: Types.AiObservabilityTraceSource | null | undefined;
  startDate: any;
  status?: Types.AiObservabilityTraceStatus | null | undefined;
  tagId?: string | number | null | undefined;
  userId?: string | null | undefined;
  workspaceId: string | number;
}>;


export type AiObservabilityTracesQuery = { aiObservabilityTraces: Array<{ createdDate: any, id: string, input: string | null, lastModifiedDate: any, metadata: string | null, name: string | null, output: string | null, projectId: string | null, sessionId: string | null, source: Types.AiObservabilityTraceSource, status: Types.AiObservabilityTraceStatus, totalCost: number | null, totalInputTokens: number | null, totalLatencyMs: number | null, totalOutputTokens: number | null, userId: string | null, version: number | null, workspaceId: string } | null> | null };

export type AiObservabilityTraceQueryVariables = Exact<{
  id: string | number;
}>;


export type AiObservabilityTraceQuery = { aiObservabilityTrace: { createdDate: any, id: string, input: string | null, lastModifiedDate: any, metadata: string | null, name: string | null, output: string | null, projectId: string | null, sessionId: string | null, source: Types.AiObservabilityTraceSource, status: Types.AiObservabilityTraceStatus, tagIds: Array<string> | null, totalCost: number | null, totalInputTokens: number | null, totalLatencyMs: number | null, totalOutputTokens: number | null, userId: string | null, version: number | null, workspaceId: string, spans: Array<{ cost: number | null, createdDate: any, endTime: any, id: string, input: string | null, inputTokens: number | null, latencyMs: number | null, level: Types.AiObservabilitySpanLevel, metadata: string | null, model: string | null, name: string | null, output: string | null, outputTokens: number | null, parentSpanId: string | null, provider: string | null, startTime: any, status: Types.AiObservabilitySpanStatus, traceId: string, type: Types.AiObservabilitySpanType, version: number | null } | null> | null } | null };

export type SetAiObservabilityTraceTagsMutationVariables = Exact<{
  traceId: string | number;
  tagIds: Array<string | number> | string | number;
}>;


export type SetAiObservabilityTraceTagsMutation = { setAiObservabilityTraceTags: { id: string, tagIds: Array<string> | null } | null };

export type AiObservabilityWebhookDeliveriesQueryVariables = Exact<{
  subscriptionId: string | number;
}>;


export type AiObservabilityWebhookDeliveriesQuery = { aiObservabilityWebhookDeliveries: Array<{ attemptCount: number, createdDate: any, deliveredDate: any, errorMessage: string | null, eventType: string | null, httpStatus: number | null, id: string, status: Types.AiObservabilityWebhookDeliveryStatus, subscriptionId: string } | null> | null };

export type AiObservabilityWebhookSubscriptionsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiObservabilityWebhookSubscriptionsQuery = { aiObservabilityWebhookSubscriptions: Array<{ createdDate: any, enabled: boolean, events: string, id: string, lastModifiedDate: any, lastTriggeredDate: any, name: string, projectId: string | null, url: string, version: number | null } | null> | null };

export type AiObservabilityWebhookSubscriptionQueryVariables = Exact<{
  id: string | number;
}>;


export type AiObservabilityWebhookSubscriptionQuery = { aiObservabilityWebhookSubscription: { createdDate: any, enabled: boolean, events: string, id: string, lastModifiedDate: any, lastTriggeredDate: any, name: string, projectId: string | null, url: string, version: number | null } | null };

export type CreateAiObservabilityWebhookSubscriptionMutationVariables = Exact<{
  workspaceId: string | number;
  projectId?: string | number | null | undefined;
  name: string;
  url: string;
  secret?: string | null | undefined;
  events: string;
  enabled: boolean;
}>;


export type CreateAiObservabilityWebhookSubscriptionMutation = { createAiObservabilityWebhookSubscription: { createdDate: any, enabled: boolean, events: string, id: string, name: string, url: string, version: number | null } | null };

export type UpdateAiObservabilityWebhookSubscriptionMutationVariables = Exact<{
  id: string | number;
  name: string;
  url: string;
  secret?: string | null | undefined;
  events: string;
  enabled: boolean;
}>;


export type UpdateAiObservabilityWebhookSubscriptionMutation = { updateAiObservabilityWebhookSubscription: { createdDate: any, enabled: boolean, events: string, id: string, name: string, url: string, version: number | null } | null };

export type DeleteAiObservabilityWebhookSubscriptionMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiObservabilityWebhookSubscriptionMutation = { deleteAiObservabilityWebhookSubscription: boolean | null };

export type TestAiObservabilityWebhookSubscriptionMutationVariables = Exact<{
  id: string | number;
}>;


export type TestAiObservabilityWebhookSubscriptionMutation = { testAiObservabilityWebhookSubscription: boolean | null };

export type AiPromptsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type AiPromptsQuery = { aiPrompts: Array<{ createdDate: any, description: string | null, id: string, lastModifiedDate: any, name: string, projectId: string | null, version: number | null, versions: Array<{ active: boolean, commitMessage: string | null, content: string, createdBy: string, createdDate: any, environment: string | null, id: string, promptId: string, type: Types.AiPromptVersionType, variables: string | null, versionNumber: number, metrics: { avgCostUsd: number | null, avgLatencyMs: number | null, errorRate: number | null, invocationCount: number } | null } | null> | null } | null> | null };

export type AiPromptQueryVariables = Exact<{
  id: string | number;
}>;


export type AiPromptQuery = { aiPrompt: { createdDate: any, description: string | null, id: string, lastModifiedDate: any, name: string, projectId: string | null, version: number | null, versions: Array<{ active: boolean, commitMessage: string | null, content: string, createdBy: string, createdDate: any, environment: string | null, id: string, promptId: string, type: Types.AiPromptVersionType, variables: string | null, versionNumber: number, metrics: { avgCostUsd: number | null, avgLatencyMs: number | null, errorRate: number | null, invocationCount: number } | null } | null> | null } | null };

export type CreateAiPromptMutationVariables = Exact<{
  input: Types.CreateAiPromptInput;
}>;


export type CreateAiPromptMutation = { createAiPrompt: { createdDate: any, description: string | null, id: string, lastModifiedDate: any, name: string, projectId: string | null, version: number | null } | null };

export type UpdateAiPromptMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateAiPromptInput;
}>;


export type UpdateAiPromptMutation = { updateAiPrompt: { createdDate: any, description: string | null, id: string, lastModifiedDate: any, name: string, projectId: string | null, version: number | null } | null };

export type DeleteAiPromptMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAiPromptMutation = { deleteAiPrompt: boolean | null };

export type CreateAiPromptVersionMutationVariables = Exact<{
  input: Types.CreateAiPromptVersionInput;
}>;


export type CreateAiPromptVersionMutation = { createAiPromptVersion: { active: boolean, commitMessage: string | null, content: string, createdBy: string, createdDate: any, environment: string | null, id: string, promptId: string, type: Types.AiPromptVersionType, variables: string | null, versionNumber: number } | null };

export type SetActiveAiPromptVersionMutationVariables = Exact<{
  promptVersionId: string | number;
  environment: string;
}>;


export type SetActiveAiPromptVersionMutation = { setActiveAiPromptVersion: boolean | null };

export type WorkspaceAiGatewayProvidersQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceAiGatewayProvidersQuery = { workspaceAiGatewayProviders: Array<{ baseUrl: string | null, config: string | null, createdBy: string | null, createdDate: any, enabled: boolean, id: string, lastModifiedBy: string | null, lastModifiedDate: any, name: string, type: Types.AiGatewayProviderType, version: number | null } | null> | null };

export type CreateWorkspaceAiGatewayProviderMutationVariables = Exact<{
  input: Types.CreateWorkspaceAiGatewayProviderInput;
}>;


export type CreateWorkspaceAiGatewayProviderMutation = { createWorkspaceAiGatewayProvider: { baseUrl: string | null, config: string | null, createdBy: string | null, createdDate: any, enabled: boolean, id: string, lastModifiedBy: string | null, lastModifiedDate: any, name: string, type: Types.AiGatewayProviderType, version: number | null } | null };

export type DeleteWorkspaceAiGatewayProviderMutationVariables = Exact<{
  workspaceId: string | number;
  providerId: string | number;
}>;


export type DeleteWorkspaceAiGatewayProviderMutation = { deleteWorkspaceAiGatewayProvider: boolean | null };

export type UpdateWorkspaceAiGatewayProviderMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  input: Types.UpdateAiGatewayProviderInput;
}>;


export type UpdateWorkspaceAiGatewayProviderMutation = { updateWorkspaceAiGatewayProvider: { baseUrl: string | null, config: string | null, createdBy: string | null, createdDate: any, enabled: boolean, id: string, lastModifiedBy: string | null, lastModifiedDate: any, name: string, type: Types.AiGatewayProviderType, version: number | null } | null };

export type TestWorkspaceAiGatewayProviderConnectionMutationVariables = Exact<{
  workspaceId: string | number;
  providerId: string | number;
}>;


export type TestWorkspaceAiGatewayProviderConnectionMutation = { testWorkspaceAiGatewayProviderConnection: { errorMessage: string | null, latencyMs: number | null, ok: boolean } | null };

export type WorkspaceAiGatewayRequestLogsQueryVariables = Exact<{
  endDate: any;
  startDate: any;
  workspaceId: string | number;
}>;


export type WorkspaceAiGatewayRequestLogsQuery = { workspaceAiGatewayRequestLogs: Array<{ apiKeyId: string | null, cacheHit: boolean | null, cost: string | null, createdDate: any, errorMessage: string | null, id: string, inputTokens: number | null, latencyMs: number | null, outputTokens: number | null, requestId: string, requestedModel: string | null, routedModel: string | null, routedProvider: string | null, routingPolicyId: string | null, routingStrategy: string | null, status: number | null } | null> | null };

export type WorkspaceAiGatewayRoutingPoliciesQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceAiGatewayRoutingPoliciesQuery = { workspaceAiGatewayRoutingPolicies: Array<{ config: string | null, createdDate: any, enabled: boolean, fallbackModel: string | null, id: string, lastModifiedDate: any, name: string, strategy: Types.AiGatewayRoutingStrategyType, version: number | null, deployments: Array<{ enabled: boolean, id: string, maxRpm: number | null, maxTpm: number | null, modelId: string, priorityOrder: number, routingPolicyId: string, weight: number } | null> | null } | null> | null };

export type CreateWorkspaceAiGatewayRoutingPolicyMutationVariables = Exact<{
  input: Types.CreateWorkspaceAiGatewayRoutingPolicyInput;
}>;


export type CreateWorkspaceAiGatewayRoutingPolicyMutation = { createWorkspaceAiGatewayRoutingPolicy: { config: string | null, createdDate: any, enabled: boolean, fallbackModel: string | null, id: string, lastModifiedDate: any, name: string, strategy: Types.AiGatewayRoutingStrategyType, version: number | null } | null };

export type DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables = Exact<{
  workspaceId: string | number;
  routingPolicyId: string | number;
}>;


export type DeleteWorkspaceAiGatewayRoutingPolicyMutation = { deleteWorkspaceAiGatewayRoutingPolicy: boolean | null };

export type UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  input: Types.UpdateAiGatewayRoutingPolicyInput;
}>;


export type UpdateWorkspaceAiGatewayRoutingPolicyMutation = { updateWorkspaceAiGatewayRoutingPolicy: { config: string | null, createdDate: any, enabled: boolean, fallbackModel: string | null, id: string, lastModifiedDate: any, name: string, strategy: Types.AiGatewayRoutingStrategyType, version: number | null } | null };

export type WorkspaceAiModelsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceAiModelsQuery = { workspaceAiModels: Array<{ alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, defaultRoutingPolicyId: string | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null> | null };

export type CreateWorkspaceAiModelMutationVariables = Exact<{
  input: Types.CreateWorkspaceAiModelInput;
}>;


export type CreateWorkspaceAiModelMutation = { createWorkspaceAiModel: { alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, defaultRoutingPolicyId: string | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null };

export type DeleteWorkspaceAiModelMutationVariables = Exact<{
  workspaceId: string | number;
  modelId: string | number;
}>;


export type DeleteWorkspaceAiModelMutation = { deleteWorkspaceAiModel: boolean | null };

export type UnpinWorkspaceAiModelMutationVariables = Exact<{
  workspaceId: string | number;
  modelId: string | number;
}>;


export type UnpinWorkspaceAiModelMutation = { unpinWorkspaceAiModel: { alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, defaultRoutingPolicyId: string | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null };

export type UpdateWorkspaceAiModelMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  input: Types.UpdateAiModelInput;
}>;


export type UpdateWorkspaceAiModelMutation = { updateWorkspaceAiModel: { alias: string | null, capabilities: string | null, catalogManaged: boolean, catalogPinned: boolean, contextWindow: number | null, defaultRoutingPolicyId: string | null, createdDate: any, enabled: boolean, id: string, inputCostPerMTokens: number | null, lastModifiedDate: any, name: string, outputCostPerMTokens: number | null, providerId: string, version: number | null } | null };

export type ApprovalTaskQueryVariables = Exact<{
  id: string | number;
}>;


export type ApprovalTaskQuery = { approvalTask: { assigneeId: string | null, createdBy: string | null, createdDate: string | null, description: string | null, dueDate: string | null, id: string, jobResumeId: string | null, lastModifiedBy: string | null, lastModifiedDate: string | null, name: string, priority: Types.ApprovalTaskPriority, status: Types.ApprovalTaskStatus, version: number } | null };

export type ApprovalTasksQueryVariables = Exact<{
  environmentId?: number | null | undefined;
}>;


export type ApprovalTasksQuery = { approvalTasks: Array<{ assigneeId: string | null, createdBy: string | null, createdDate: string | null, description: string | null, dueDate: string | null, id: string, jobResumeId: string | null, lastModifiedBy: string | null, lastModifiedDate: string | null, name: string, priority: Types.ApprovalTaskPriority, status: Types.ApprovalTaskStatus, version: number } | null> | null };

export type CreateApprovalTaskMutationVariables = Exact<{
  approvalTask: Types.ApprovalTaskInput;
}>;


export type CreateApprovalTaskMutation = { createApprovalTask: { assigneeId: string | null, description: string | null, id: string, name: string, priority: Types.ApprovalTaskPriority, status: Types.ApprovalTaskStatus } | null };

export type DeleteApprovalTaskMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteApprovalTaskMutation = { deleteApprovalTask: boolean | null };

export type PendingApprovalsQueryVariables = Exact<{
  environmentId?: number | null | undefined;
}>;


export type PendingApprovalsQuery = { pendingApprovals: Array<{ createdDate: string | null, expiresAt: string | null, formUrl: string | null, jobId: string, workflowLabel: string } | null> | null };

export type UpdateApprovalTaskMutationVariables = Exact<{
  approvalTask: Types.ApprovalTaskInput;
}>;


export type UpdateApprovalTaskMutation = { updateApprovalTask: { assigneeId: string | null, description: string | null, dueDate: string | null, id: string, name: string, priority: Types.ApprovalTaskPriority, status: Types.ApprovalTaskStatus, version: number } | null };

export type DeleteAssetFileMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAssetFileMutation = { deleteAssetFile: boolean };

export type DisableAssetFilePublicLinkMutationVariables = Exact<{
  id: string | number;
}>;


export type DisableAssetFilePublicLinkMutation = { disableAssetFilePublicLink: { id: string, publicLinkUrl: string | null } };

export type EnableAssetFilePublicLinkMutationVariables = Exact<{
  id: string | number;
}>;


export type EnableAssetFilePublicLinkMutation = { enableAssetFilePublicLink: { id: string, publicLinkUrl: string | null } };

export type GetAssetFileQueryVariables = Exact<{
  id: string | number;
}>;


export type GetAssetFileQuery = { assetFile: { createdBy: string | null, createdDate: any, description: string | null, downloadUrl: string, environmentId: any, format: string | null, generatedByAgentSource: number | null, generatedFromPrompt: string | null, id: string, lastModifiedBy: string | null, lastModifiedDate: any, metadataJson: string | null, mimeType: string, name: string, publicLinkUrl: string | null, sizeBytes: any, source: Types.AssetFileSource, tags: Array<{ id: string, name: string }> } | null };

export type GetAssetFileSignedDownloadUrlQueryVariables = Exact<{
  id: string | number;
}>;


export type GetAssetFileSignedDownloadUrlQuery = { assetFileSignedDownloadUrl: string };

export type GetAssetFileTagsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type GetAssetFileTagsQuery = { assetFileTags: Array<{ id: string, name: string }> };

export type GetAssetFileTextContentQueryVariables = Exact<{
  id: string | number;
}>;


export type GetAssetFileTextContentQuery = { assetFileTextContent: string | null };

export type GetAssetFileVersionsQueryVariables = Exact<{
  id: string | number;
}>;


export type GetAssetFileVersionsQuery = { assetFileVersions: Array<{ createdBy: string | null, createdDate: any, id: string, mimeType: string, sizeBytes: any, versionNumber: number }> };

export type GetAssetFilesQueryVariables = Exact<{
  workspaceId: string | number;
  environment?: number | null | undefined;
  tagIds?: Array<string | number> | string | number | null | undefined;
  mimeTypePrefix?: string | null | undefined;
}>;


export type GetAssetFilesQuery = { assetFiles: Array<{ createdBy: string | null, createdDate: any, description: string | null, downloadUrl: string, environmentId: any, format: string | null, generatedByAgentSource: number | null, generatedFromPrompt: string | null, id: string, lastModifiedBy: string | null, lastModifiedDate: any, metadataJson: string | null, mimeType: string, name: string, publicLinkUrl: string | null, sizeBytes: any, source: Types.AssetFileSource, tags: Array<{ id: string, name: string }> }> };

export type RestoreAssetFileVersionMutationVariables = Exact<{
  id: string | number;
  versionId: string | number;
}>;


export type RestoreAssetFileVersionMutation = { restoreAssetFileVersion: { id: string, lastModifiedDate: any, mimeType: string, name: string, sizeBytes: any } };

export type UpdateAssetFileMutationVariables = Exact<{
  input: Types.UpdateAssetFileInput;
}>;


export type UpdateAssetFileMutation = { updateAssetFile: { createdBy: string | null, createdDate: any, description: string | null, downloadUrl: string, id: string, lastModifiedBy: string | null, lastModifiedDate: any, mimeType: string, name: string, sizeBytes: any, source: Types.AssetFileSource } };

export type UpdateAssetFileTagsMutationVariables = Exact<{
  input: Types.UpdateAssetFileTagsInput;
}>;


export type UpdateAssetFileTagsMutation = { updateAssetFileTags: { id: string, tags: Array<{ id: string, name: string }> } };

export type UpdateAssetFileTextContentMutationVariables = Exact<{
  id: string | number;
  content: string;
}>;


export type UpdateAssetFileTextContentMutation = { updateAssetFileTextContent: { id: string, lastModifiedDate: any, sizeBytes: any } };

export type A2aProjectWorkflowsByA2aProjectIdQueryVariables = Exact<{
  a2aProjectId: string | number;
}>;


export type A2aProjectWorkflowsByA2aProjectIdQuery = { a2aProjectWorkflowsByA2aProjectId: Array<{ id: string, skillDescription: string | null, skillName: string | null, skillTags: Array<string> | null, workflowId: string | null, workflowLabel: string | null } | null> | null };

export type A2aProjectsByServerIdQueryVariables = Exact<{
  a2aServerId: string | number;
}>;


export type A2aProjectsByServerIdQuery = { a2aProjectsByServerId: Array<{ a2aServerId: string, id: string, projectId: string | null, projectVersion: number | null, workflowIds: Array<string> } | null> | null };

export type A2aServersQueryVariables = Exact<{
  type: Types.PlatformType;
}>;


export type A2aServersQuery = { a2aServers: Array<{ authenticationRequired: boolean, createdDate: any, description: string | null, enabled: boolean, environmentId: string, id: string, lastModifiedDate: any, name: string, secretKey: string, type: Types.PlatformType, version: number | null } | null> | null };

export type AddWorkspaceUserMutationVariables = Exact<{
  workspaceId: string | number;
  userId: string | number;
  role?: Types.WorkspaceRole | null | undefined;
  customRoleId?: string | number | null | undefined;
}>;


export type AddWorkspaceUserMutation = { addWorkspaceUser: { id: string | null, workspaceId: string, userId: string, workspaceRole: Types.WorkspaceRole | null, customRoleId: string | null, user: { email: string, firstName: string | null, lastName: string | null } | null } };

export type AffectedWorkflowsQueryVariables = Exact<{
  workspaceId: string | number;
  userLogin: string;
}>;


export type AffectedWorkflowsQuery = { affectedWorkflows: Array<{ workflowId: string, workflowName: string, connectionIds: Array<string> }> };

export type AssignWorkspaceUserCustomRoleMutationVariables = Exact<{
  workspaceId: string | number;
  userId: string | number;
  customRoleId: string | number;
}>;


export type AssignWorkspaceUserCustomRoleMutation = { assignWorkspaceUserCustomRole: { id: string | null, userId: string, workspaceRole: Types.WorkspaceRole | null, customRoleId: string | null } };

export type ConnectionGrantsQueryVariables = Exact<{
  workspaceId: string | number;
  connectionId: string | number;
}>;


export type ConnectionGrantsQuery = { connectionGrants: Array<any> };

export type CreateA2aProjectMutationVariables = Exact<{
  input: Types.CreateA2aProjectInput;
}>;


export type CreateA2aProjectMutation = { createA2aProject: { id: string } | null };

export type CreateA2aServerMutationVariables = Exact<{
  input: Types.CreateA2aServerInput;
}>;


export type CreateA2aServerMutation = { createA2aServer: { id: string } | null };

export type CreateCustomRoleMutationVariables = Exact<{
  input: Types.CreateCustomRoleInput;
}>;


export type CreateCustomRoleMutation = { createCustomRole: { id: string, name: string, description: string | null, scopes: Array<string> } };

export type CreateMcpProjectMutationVariables = Exact<{
  input: Types.CreateMcpProjectInput;
}>;


export type CreateMcpProjectMutation = { createMcpProject: { id: string, mcpServerId: string, projectDeploymentId: string, projectVersion: number | null } | null };

export type CreateOrganizationConnectionMutationVariables = Exact<{
  input: Types.CreateOrganizationConnectionInput;
}>;


export type CreateOrganizationConnectionMutation = { createOrganizationConnection: string };

export type CreateWorkspaceApiKeyMutationVariables = Exact<{
  workspaceId: string | number;
  name: string;
  environmentId: string | number;
}>;


export type CreateWorkspaceApiKeyMutation = { createWorkspaceApiKey: string };

export type CreateMcpServerMutationVariables = Exact<{
  input: Types.CreateWorkspaceMcpServerInput;
}>;


export type CreateMcpServerMutation = { createWorkspaceMcpServer: { id: string, name: string, type: Types.PlatformType, environmentId: string, enabled: boolean } | null };

export type CustomRolesQueryVariables = Exact<{
  workspaceId?: string | number | null | undefined;
}>;


export type CustomRolesQuery = { customRoles: Array<{ id: string, name: string, description: string | null, scopes: Array<string> }> };

export type DataStreamCompatibleConnectionsQueryVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
}>;


export type DataStreamCompatibleConnectionsQuery = { dataStreamCompatibleConnections: Array<{ id: string, name: string, componentName: string, componentVersion: number }> };

export type DeleteA2aProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteA2aProjectMutation = { deleteA2aProject: boolean | null };

export type DeleteA2aServerMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteA2aServerMutation = { deleteA2aServer: boolean | null };

export type DeleteCustomRoleMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteCustomRoleMutation = { deleteCustomRole: boolean };

export type DeleteMcpProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteMcpProjectMutation = { deleteMcpProject: boolean | null };

export type DeleteMcpProjectWorkflowMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteMcpProjectWorkflowMutation = { deleteMcpProjectWorkflow: boolean | null };

export type DeleteOrganizationConnectionMutationVariables = Exact<{
  connectionId: string | number;
}>;


export type DeleteOrganizationConnectionMutation = { deleteOrganizationConnection: boolean };

export type DeleteSharedProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteSharedProjectMutation = { deleteSharedProject: boolean };

export type DeleteSharedWorkflowMutationVariables = Exact<{
  workflowId: string;
}>;


export type DeleteSharedWorkflowMutation = { deleteSharedWorkflow: boolean };

export type DeleteWorkspaceApiKeyMutationVariables = Exact<{
  apiKeyId: string | number;
}>;


export type DeleteWorkspaceApiKeyMutation = { deleteWorkspaceApiKey: boolean };

export type DeleteWorkspaceMcpServerMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteWorkspaceMcpServerMutation = { deleteWorkspaceMcpServer: boolean | null };

export type DisconnectConnectionMutationVariables = Exact<{
  connectionId: string | number;
}>;


export type DisconnectConnectionMutation = { disconnectConnection: boolean };

export type ProjectErrorWorkflowQueryVariables = Exact<{
  id: string | number;
}>;


export type ProjectErrorWorkflowQuery = { project: { errorProjectWorkflowId: string | null } | null };

export type UpdateProjectErrorWorkflowMutationVariables = Exact<{
  projectId: string | number;
  errorProjectWorkflowId?: string | number | null | undefined;
}>;


export type UpdateProjectErrorWorkflowMutation = { updateProjectErrorWorkflow: boolean | null };

export type EligibleErrorWorkflowsQueryVariables = Exact<{
  projectId: string | number;
  projectVersion: number;
}>;


export type EligibleErrorWorkflowsQuery = { eligibleErrorWorkflows: Array<{ id: string, workflowId: string, workflow: { label: string } }> };

export type ProjectWorkflowErrorConfigQueryVariables = Exact<{
  id: string | number;
}>;


export type ProjectWorkflowErrorConfigQuery = { projectWorkflow: { errorProjectWorkflowId: string | null, errorWorkflowDisabled: boolean } | null };

export type UpdateProjectWorkflowErrorWorkflowMutationVariables = Exact<{
  projectId: string | number;
  projectWorkflowId: string | number;
  errorProjectWorkflowId?: string | number | null | undefined;
  errorWorkflowDisabled: boolean;
}>;


export type UpdateProjectWorkflowErrorWorkflowMutation = { updateProjectWorkflowErrorWorkflow: boolean | null };

export type ExportSharedProjectMutationVariables = Exact<{
  id: string | number;
  description?: string | null | undefined;
}>;


export type ExportSharedProjectMutation = { exportSharedProject: boolean | null };

export type ExportSharedWorkflowMutationVariables = Exact<{
  workflowId: string;
  description?: string | null | undefined;
}>;


export type ExportSharedWorkflowMutation = { exportSharedWorkflow: boolean };

export type GrantConnectionAccessMutationVariables = Exact<{
  workspaceId: string | number;
  connectionId: string | number;
  userId: string | number;
}>;


export type GrantConnectionAccessMutation = { grantConnectionAccess: boolean };

export type GrantProjectAccessMutationVariables = Exact<{
  workspaceId: string | number;
  projectId: string | number;
  userId: string | number;
}>;


export type GrantProjectAccessMutation = { grantProjectAccess: boolean };

export type ImportProjectTemplateMutationVariables = Exact<{
  id: string;
  workspaceId: string | number;
  sharedProject: boolean;
}>;


export type ImportProjectTemplateMutation = { importProjectTemplate: string };

export type ImportWorkflowTemplateMutationVariables = Exact<{
  workflowUuid: string;
  projectId: string | number;
  sharedWorkflow: boolean;
}>;


export type ImportWorkflowTemplateMutation = { importWorkflowTemplate: string };

export type InviteWorkspaceUserMutationVariables = Exact<{
  workspaceId: string | number;
  email: string;
  role?: Types.WorkspaceRole | null | undefined;
  customRoleId?: string | number | null | undefined;
}>;


export type InviteWorkspaceUserMutation = { inviteWorkspaceUser: { id: string | null, userId: string, workspaceId: string, workspaceRole: Types.WorkspaceRole | null, customRoleId: string | null } };

export type McpProjectWorkflowPropertiesQueryVariables = Exact<{
  mcpProjectWorkflowId: string | number;
}>;


export type McpProjectWorkflowPropertiesQuery = { mcpProjectWorkflowProperties: Array<
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, arrayDefaultValue: Array<any> | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, booleanDefaultValue: boolean | null }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, integerDefaultValue: any }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, numberDefaultValue: number | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, objectDefaultValue: any }
    | { controlType: Types.ControlType, defaultValue: string | null, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
   | null> | null };

export type McpProjectsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type McpProjectsQuery = { mcpProjects: Array<{ id: string, mcpServerId: string, project: { id: string, name: string } | null } | null> | null };

export type McpProjectsByServerIdQueryVariables = Exact<{
  mcpServerId: string | number;
}>;


export type McpProjectsByServerIdQuery = { mcpProjectsByServerId: Array<{ id: string, projectDeploymentId: string, mcpServerId: string, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, projectVersion: number | null, project: { id: string, name: string, category: { id: string | null, name: string | null } | null, tags: Array<{ id: string, name: string } | null> | null } | null, mcpProjectWorkflows: Array<{ id: string, mcpProjectId: any, projectDeploymentWorkflowId: any, parameters: any, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, projectDeploymentWorkflow: { id: string, enabled: boolean, inputs: any, projectDeploymentId: string, version: number, workflowId: string, connections: Array<{ connectionId: string | null, workflowConnectionKey: string, workflowNodeName: string }> } | null, workflow: { id: string, label: string } | null } | null> | null } | null> | null };

export type MyWorkspaceRoleQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type MyWorkspaceRoleQuery = { myWorkspaceRole: string | null };

export type MyWorkspaceScopesQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type MyWorkspaceScopesQuery = { myWorkspaceScopes: Array<string> };

export type OrganizationConnectionsQueryVariables = Exact<{
  environmentId?: string | number | null | undefined;
}>;


export type OrganizationConnectionsQuery = { organizationConnections: Array<{ id: string, name: string, componentName: string, environmentId: number, visibility: Types.ResourceVisibility, createdBy: string | null, createdDate: string | null, lastModifiedDate: string | null }> };

export type PermissionScopesQueryVariables = Exact<{ [key: string]: never; }>;


export type PermissionScopesQuery = { permissionScopes: Array<string> };

export type PreBuiltProjectTemplatesQueryVariables = Exact<{
  query?: string | null | undefined;
  category?: string | null | undefined;
}>;


export type PreBuiltProjectTemplatesQuery = { preBuiltProjectTemplates: Array<{ authorName: string | null, categories: Array<string>, description: string | null, id: string | null, projectVersion: number | null, publicUrl: string | null, components: Array<{ key: string | null, value: Array<{ icon: string | null, name: string, title: string | null, version: number | null, connection: { version: number } | null } | null> }>, project: { name: string, description: string | null } | null, workflows: Array<{ id: string, label: string }> }> };

export type PreBuiltWorkflowTemplatesQueryVariables = Exact<{
  query?: string | null | undefined;
  category?: string | null | undefined;
}>;


export type PreBuiltWorkflowTemplatesQuery = { preBuiltWorkflowTemplates: Array<{ authorName: string | null, categories: Array<string>, description: string | null, id: string | null, projectVersion: number | null, publicUrl: string | null, components: Array<{ icon: string | null, name: string, title: string | null, version: number | null, connection: { version: number } | null }>, workflow: { label: string, description: string | null } }> };

export type ProjectByIdQueryVariables = Exact<{
  id: string | number;
}>;


export type ProjectByIdQuery = { project: { id: string, name: string } | null };

export type ProjectGrantsQueryVariables = Exact<{
  workspaceId: string | number;
  projectId: string | number;
}>;


export type ProjectGrantsQuery = { projectGrants: Array<any> };

export type ProjectTemplateQueryVariables = Exact<{
  id: string;
  sharedProject: boolean;
}>;


export type ProjectTemplateQuery = { projectTemplate: { description: string | null, projectVersion: number | null, publicUrl: string | null, components: Array<{ key: string | null, value: Array<{ icon: string | null, name: string, title: string | null, version: number | null, connection: { componentName: string, version: number } | null } | null> }>, project: { name: string } | null, workflows: Array<{ id: string, label: string }> } | null };

export type ReassignAllConnectionsMutationVariables = Exact<{
  workspaceId: string | number;
  userLogin: string;
  newOwnerLogin: string;
}>;


export type ReassignAllConnectionsMutation = { reassignAllConnections: boolean };

export type RemoveWorkspaceUserMutationVariables = Exact<{
  workspaceId: string | number;
  userId: string | number;
}>;


export type RemoveWorkspaceUserMutation = { removeWorkspaceUser: boolean };

export type RemoveWorkspaceUserEnvironmentRoleMutationVariables = Exact<{
  workspaceId: string | number;
  userId: string | number;
  environment: Types.EnvironmentEnum;
}>;


export type RemoveWorkspaceUserEnvironmentRoleMutation = { removeWorkspaceUserEnvironmentRole: boolean };

export type RevokeConnectionAccessMutationVariables = Exact<{
  workspaceId: string | number;
  connectionId: string | number;
  userId: string | number;
}>;


export type RevokeConnectionAccessMutation = { revokeConnectionAccess: boolean };

export type RevokeProjectAccessMutationVariables = Exact<{
  workspaceId: string | number;
  projectId: string | number;
  userId: string | number;
}>;


export type RevokeProjectAccessMutation = { revokeProjectAccess: boolean };

export type SetConnectionVisibilityMutationVariables = Exact<{
  workspaceId: string | number;
  connectionId: string | number;
  visibility: Types.ResourceVisibility;
}>;


export type SetConnectionVisibilityMutation = { setConnectionVisibility: boolean };

export type SetProjectVisibilityMutationVariables = Exact<{
  workspaceId: string | number;
  projectId: string | number;
  visibility: Types.ResourceVisibility;
}>;


export type SetProjectVisibilityMutation = { setProjectVisibility: boolean };

export type SetWorkspaceUserEnvironmentRoleMutationVariables = Exact<{
  workspaceId: string | number;
  userId: string | number;
  environment: Types.EnvironmentEnum;
  role?: Types.WorkspaceRole | null | undefined;
  customRoleId?: string | number | null | undefined;
}>;


export type SetWorkspaceUserEnvironmentRoleMutation = { setWorkspaceUserEnvironmentRole: { id: string | null, userId: string, workspaceRole: Types.WorkspaceRole | null, customRoleId: string | null, environment: Types.EnvironmentEnum | null } };

export type SharedProjectQueryVariables = Exact<{
  projectUuid: string;
}>;


export type SharedProjectQuery = { sharedProject: { description: string | null, exported: boolean, projectVersion: number | null, publicUrl: string | null } | null };

export type SharedWorkflowQueryVariables = Exact<{
  workflowUuid: string;
}>;


export type SharedWorkflowQuery = { sharedWorkflow: { description: string | null, exported: boolean, projectVersion: number | null, publicUrl: string | null } | null };

export type ToolEligibleProjectVersionWorkflowsQueryVariables = Exact<{
  projectId: string | number;
  projectVersion: number;
}>;


export type ToolEligibleProjectVersionWorkflowsQuery = { toolEligibleProjectVersionWorkflows: Array<{ id: string, workflow: { id: string, label: string } }> };

export type UnresolvedConnectionsQueryVariables = Exact<{
  workspaceId: string | number;
  userLogin: string;
}>;


export type UnresolvedConnectionsQuery = { unresolvedConnections: Array<{ connectionId: string, connectionName: string, visibility: Types.ResourceVisibility, environmentId: number, dependentWorkflowCount: number }> };

export type UpdateA2aProjectMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateA2aProjectInput;
}>;


export type UpdateA2aProjectMutation = { updateA2aProject: { id: string } | null };

export type UpdateA2aProjectWorkflowParametersMutationVariables = Exact<{
  id: string | number;
  input: Types.A2aProjectWorkflowParametersInput;
}>;


export type UpdateA2aProjectWorkflowParametersMutation = { updateA2aProjectWorkflowParameters: { id: string } | null };

export type UpdateA2aServerMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateA2aServerInput;
}>;


export type UpdateA2aServerMutation = { updateA2aServer: { id: string } | null };

export type UpdateConnectionCredentialsMutationVariables = Exact<{
  input: Types.UpdateConnectionCredentialsInput;
}>;


export type UpdateConnectionCredentialsMutation = { updateConnectionCredentials: boolean };

export type UpdateCustomRoleMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateCustomRoleInput;
}>;


export type UpdateCustomRoleMutation = { updateCustomRole: { id: string, name: string, description: string | null, scopes: Array<string> } };

export type UpdateMcpProjectMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateMcpProjectInput;
}>;


export type UpdateMcpProjectMutation = { updateMcpProject: { id: string, mcpServerId: string, projectDeploymentId: string, projectVersion: number | null } | null };

export type UpdateMcpProjectWorkflowMutationVariables = Exact<{
  id: string | number;
  input: Types.McpProjectWorkflowUpdateInput;
}>;


export type UpdateMcpProjectWorkflowMutation = { updateMcpProjectWorkflow: { id: string, mcpProjectId: any, projectDeploymentWorkflowId: any, parameters: any } | null };

export type UpdateMcpServerMutationVariables = Exact<{
  id: string | number;
  input: Types.McpServerUpdateInput;
}>;


export type UpdateMcpServerMutation = { updateMcpServer: { id: string, name: string, enabled: boolean, enforceToolAuthorization: boolean, authenticationRequired: boolean } | null };

export type UpdateMcpServerTagsMutationVariables = Exact<{
  id: string | number;
  tags: Array<Types.TagInput> | Types.TagInput;
}>;


export type UpdateMcpServerTagsMutation = { updateMcpServerTags: Array<{ id: string } | null> | null };

export type UpdateOrganizationConnectionMutationVariables = Exact<{
  connectionId: string | number;
  name: string;
  tagIds?: Array<string | number> | string | number | null | undefined;
  version: number;
}>;


export type UpdateOrganizationConnectionMutation = { updateOrganizationConnection: boolean };

export type UpdateWorkspaceApiKeyMutationVariables = Exact<{
  apiKeyId: string | number;
  name: string;
}>;


export type UpdateWorkspaceApiKeyMutation = { updateWorkspaceApiKey: boolean };

export type UpdateWorkspaceUserRoleMutationVariables = Exact<{
  workspaceId: string | number;
  userId: string | number;
  role: Types.WorkspaceRole;
}>;


export type UpdateWorkspaceUserRoleMutation = { updateWorkspaceUserRole: { id: string | null, workspaceRole: Types.WorkspaceRole | null } };

export type WorkflowChatProjectDeploymentWorkflowQueryVariables = Exact<{
  id: string;
}>;


export type WorkflowChatProjectDeploymentWorkflowQuery = { projectDeploymentWorkflow: { projectWorkflow: { sseStreamResponse: boolean, workflow: { label: string } } } | null };

export type WorkflowTemplateQueryVariables = Exact<{
  id: string;
  sharedWorkflow: boolean;
}>;


export type WorkflowTemplateQuery = { workflowTemplate: { description: string | null, projectVersion: number | null, publicUrl: string | null, workflow: { label: string }, components: Array<{ icon: string | null, name: string, title: string | null, version: number | null, connection: { componentName: string, version: number } | null }> } | null };

export type WorkspaceApiKeysQueryVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
}>;


export type WorkspaceApiKeysQuery = { workspaceApiKeys: Array<{ id: string | null, name: string | null, secretKey: string | null, lastUsedDate: any, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any }> };

export type WorkspaceChatWorkflowsQueryVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
}>;


export type WorkspaceChatWorkflowsQuery = { workspaceChatWorkflows: Array<{ projectDeploymentId: string, projectId: string, projectName: string, projectWorkflowId: string, workflowExecutionId: string, workflowId: string, workflowLabel: string }> };

export type WorkspaceMcpServerTagsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceMcpServerTagsQuery = { workspaceMcpServerTags: Array<{ id: string, name: string } | null> | null };

export type WorkspaceMcpServersQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceMcpServersQuery = { workspaceMcpServers: Array<{ id: string, name: string, type: Types.PlatformType, environmentId: string, enabled: boolean, enforceToolAuthorization: boolean, authenticationRequired: boolean, url: string, lastModifiedDate: any, mcpComponents: Array<{ id: string, mcpServerId: string, componentName: string, componentVersion: number, title: string | null } | null> | null, tags: Array<{ id: string, name: string } | null> | null } | null> | null };

export type WorkspaceProjectWorkflowsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceProjectWorkflowsQuery = { workspaceProjectWorkflows: Array<{ projectId: string, projectName: string, projectWorkflowId: string, workflowId: string, workflowLabel: string }> };

export type WorkspaceUsersQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceUsersQuery = { workspaceUsers: Array<{ id: string | null, workspaceId: string, userId: string, workspaceRole: Types.WorkspaceRole | null, customRoleId: string | null, inherited: boolean, createdDate: string | null, environment: Types.EnvironmentEnum | null, user: { email: string, firstName: string | null, lastName: string | null } | null }> };

export type ContextStoreSourceQueryVariables = Exact<{
  id: string | number;
}>;


export type ContextStoreSourceQuery = { contextStoreSource: { id: string, name: string, entityName: string, description: string | null, idField: string, storedFields: any, indexedFields: any, semanticIndexFields: any, parameters: any, sourceComponentName: string, sourceComponentVersion: number, sourceClusterElementName: string | null, connectionId: string | null, cadence: string, status: Types.ContextStoreSourceStatus, enabled: boolean, lastSyncRunAt: any, lastSyncJobExecutionId: string | null, workflowId: string | null, fullReplaceCadence: string | null, tombstoneStrategy: Types.ContextStoreTombstoneStrategy } | null };

export type ContextStoreSourcesQueryVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
  filter?: Types.ContextStoreSourceFilter | null | undefined;
}>;


export type ContextStoreSourcesQuery = { contextStoreSources: Array<{ id: string, contextStoreId: string, name: string, entityName: string, description: string | null, idField: string, indexedFields: any, sourceComponentName: string, sourceComponentVersion: number, sourceClusterElementName: string | null, connectionId: string | null, cadence: string, status: Types.ContextStoreSourceStatus, enabled: boolean, lastSyncRunAt: any, lastSyncJobExecutionId: string | null, workflowId: string | null }> };

export type ContextStoreTagsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type ContextStoreTagsQuery = { contextStoreTags: Array<{ id: string, name: string }> };

export type ContextStoresQueryVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
}>;


export type ContextStoresQuery = { contextStores: Array<{ id: string, name: string, description: string | null, environment: string, tagIds: Array<string>, version: number, tags: Array<{ id: string, name: string }> }> };

export type CreateContextStoreMutationVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
  input: Types.CreateContextStoreInput;
}>;


export type CreateContextStoreMutation = { createContextStore: { id: string, name: string, description: string | null, environment: string, tagIds: Array<string>, version: number } };

export type CreateContextStoreSourceMutationVariables = Exact<{
  input: Types.CreateContextStoreSourceInput;
}>;


export type CreateContextStoreSourceMutation = { createContextStoreSource: { id: string, name: string, status: Types.ContextStoreSourceStatus, enabled: boolean, cadence: string } };

export type DeleteContextStoreMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
}>;


export type DeleteContextStoreMutation = { deleteContextStore: boolean };

export type DeleteContextStoreSourceMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteContextStoreSourceMutation = { deleteContextStoreSource: boolean };

export type RefreshContextStoreSourceMutationVariables = Exact<{
  id: string | number;
}>;


export type RefreshContextStoreSourceMutation = { refreshContextStoreSource: string };

export type SetContextStoreSourceEnabledMutationVariables = Exact<{
  id: string | number;
  enabled: boolean;
}>;


export type SetContextStoreSourceEnabledMutation = { setContextStoreSourceEnabled: { id: string, enabled: boolean, status: Types.ContextStoreSourceStatus } };

export type UpdateContextStoreMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  input: Types.UpdateContextStoreInput;
}>;


export type UpdateContextStoreMutation = { updateContextStore: { id: string, name: string, description: string | null, environment: string, tagIds: Array<string>, version: number } };

export type UpdateContextStoreSourceMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateContextStoreSourceInput;
}>;


export type UpdateContextStoreSourceMutation = { updateContextStoreSource: { id: string, name: string, cadence: string, enabled: boolean, status: Types.ContextStoreSourceStatus } };

export type UpdateContextStoreTagsMutationVariables = Exact<{
  workspaceId: string | number;
  id: string | number;
  tags: Array<Types.TagInput> | Types.TagInput;
}>;


export type UpdateContextStoreTagsMutation = { updateContextStoreTags: Array<{ id: string, name: string }> };

export type AddDataTableColumnMutationVariables = Exact<{
  input: Types.AddColumnInput;
}>;


export type AddDataTableColumnMutation = { addDataTableColumn: boolean };

export type CreateDataTableMutationVariables = Exact<{
  input: Types.CreateDataTableInput;
}>;


export type CreateDataTableMutation = { createDataTable: boolean };

export type DataTableRowsQueryVariables = Exact<{
  environmentId: string | number;
  tableId: string | number;
}>;


export type DataTableRowsQuery = { dataTableRows: Array<{ id: string, values: any }> };

export type DataTableRowsPageQueryVariables = Exact<{
  environmentId: string | number;
  tableId: string | number;
  limit?: number | null | undefined;
  offset?: number | null | undefined;
}>;


export type DataTableRowsPageQuery = { dataTableRowsPage: { hasMore: boolean, nextOffset: number | null, items: Array<{ id: string, values: any }> } };

export type DataTableStorageUsageQueryVariables = Exact<{ [key: string]: never; }>;


export type DataTableStorageUsageQuery = { dataTableStorageUsage: { limitBytes: any, percentage: number, unlimited: boolean, usedBytes: any } };

export type DataTableTagsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type DataTableTagsQuery = { dataTableTags: Array<{ id: string, name: string }> };

export type DataTableTagsByTableQueryVariables = Exact<{ [key: string]: never; }>;


export type DataTableTagsByTableQuery = { dataTableTagsByTable: Array<{ tableId: string, tags: Array<{ id: string, name: string }> }> };

export type DataTablesQueryVariables = Exact<{
  environmentId: string | number;
  workspaceId: string | number;
}>;


export type DataTablesQuery = { dataTables: Array<{ id: string, baseName: string, lastModifiedDate: any, columns: Array<{ id: string, name: string, type: Types.ColumnType }> }> };

export type DeleteDataTableRowMutationVariables = Exact<{
  input: Types.DeleteRowInput;
}>;


export type DeleteDataTableRowMutation = { deleteDataTableRow: boolean };

export type DropDataTableMutationVariables = Exact<{
  input: Types.RemoveTableInput;
}>;


export type DropDataTableMutation = { dropDataTable: boolean };

export type DuplicateDataTableMutationVariables = Exact<{
  input: Types.DuplicateDataTableInput;
}>;


export type DuplicateDataTableMutation = { duplicateDataTable: boolean };

export type ExportDataTableCsvQueryVariables = Exact<{
  environmentId: string | number;
  tableId: string | number;
}>;


export type ExportDataTableCsvQuery = { exportDataTableCsv: string };

export type ImportDataTableCsvMutationVariables = Exact<{
  input: Types.ImportCsvInput;
}>;


export type ImportDataTableCsvMutation = { importDataTableCsv: boolean };

export type InsertDataTableRowMutationVariables = Exact<{
  input: Types.InsertRowInput;
}>;


export type InsertDataTableRowMutation = { insertDataTableRow: { id: string, values: any } };

export type RemoveDataTableColumnMutationVariables = Exact<{
  input: Types.RemoveColumnInput;
}>;


export type RemoveDataTableColumnMutation = { removeDataTableColumn: boolean };

export type RenameDataTableMutationVariables = Exact<{
  input: Types.RenameDataTableInput;
}>;


export type RenameDataTableMutation = { renameDataTable: boolean };

export type RenameDataTableColumnMutationVariables = Exact<{
  input: Types.RenameColumnInput;
}>;


export type RenameDataTableColumnMutation = { renameDataTableColumn: boolean };

export type UpdateDataTableRowMutationVariables = Exact<{
  input: Types.UpdateRowInput;
}>;


export type UpdateDataTableRowMutation = { updateDataTableRow: { id: string, values: any } };

export type UpdateDataTableTagsMutationVariables = Exact<{
  input: Types.UpdateDataTableTagsInput;
}>;


export type UpdateDataTableTagsMutation = { updateDataTableTags: boolean };

export type CreateKnowledgeBaseMutationVariables = Exact<{
  knowledgeBase: Types.KnowledgeBaseInput;
  environmentId: string | number;
  workspaceId: string | number;
}>;


export type CreateKnowledgeBaseMutation = { createKnowledgeBase: { id: string, name: string } | null };

export type CreateKnowledgeBaseSourceMutationVariables = Exact<{
  input: Types.CreateKnowledgeBaseSourceInput;
}>;


export type CreateKnowledgeBaseSourceMutation = { createKnowledgeBaseSource: { id: string, name: string, status: Types.KnowledgeBaseSourceStatus, enabled: boolean, cadence: string } };

export type DeleteKnowledgeBaseMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteKnowledgeBaseMutation = { deleteKnowledgeBase: boolean | null };

export type DeleteKnowledgeBaseDocumentMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteKnowledgeBaseDocumentMutation = { deleteKnowledgeBaseDocument: boolean | null };

export type DeleteKnowledgeBaseDocumentChunkMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteKnowledgeBaseDocumentChunkMutation = { deleteKnowledgeBaseDocumentChunk: boolean | null };

export type DeleteKnowledgeBaseSourceMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteKnowledgeBaseSourceMutation = { deleteKnowledgeBaseSource: boolean };

export type KnowledgeBaseQueryVariables = Exact<{
  id: string | number;
}>;


export type KnowledgeBaseQuery = { knowledgeBase: { id: string, name: string, description: string | null, maxChunkSize: number | null, minChunkSizeChars: number | null, overlap: number | null, createdDate: any, lastModifiedDate: any, documents: Array<{ id: string, name: string, status: number, tags: Array<string> | null, createdDate: any, sourceId: string | null, sourceRecordId: string | null, document: { name: string, extension: string | null, mimeType: string | null, url: string } | null, chunks: Array<{ id: string, knowledgeBaseDocumentId: string } | null> | null } | null> | null } | null };

export type KnowledgeBaseDocumentChunksQueryVariables = Exact<{
  id: string | number;
}>;


export type KnowledgeBaseDocumentChunksQuery = { knowledgeBaseDocumentChunks: Array<{ id: string, knowledgeBaseDocumentId: string, content: string | null, metadata: any } | null> | null };

export type KnowledgeBaseDocumentStatusQueryVariables = Exact<{
  id: string | number;
}>;


export type KnowledgeBaseDocumentStatusQuery = { knowledgeBaseDocumentStatus: { documentId: string, status: number, timestamp: any, message: string | null } | null };

export type KnowledgeBaseDocumentTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseDocumentTagsQuery = { knowledgeBaseDocumentTags: Array<string> | null };

export type KnowledgeBaseDocumentTagsByDocumentQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseDocumentTagsByDocumentQuery = { knowledgeBaseDocumentTagsByDocument: Array<{ knowledgeBaseDocumentId: string, tags: Array<string> }> | null };

export type KnowledgeBaseEmbeddingActiveQueryVariables = Exact<{
  environment: number;
}>;


export type KnowledgeBaseEmbeddingActiveQuery = { knowledgeBaseEmbeddingActive: boolean };

export type KnowledgeBaseSourceQueryVariables = Exact<{
  id: string | number;
}>;


export type KnowledgeBaseSourceQuery = { knowledgeBaseSource: { id: string, name: string, knowledgeBaseId: string, sourceComponentName: string, sourceComponentVersion: number, sourceClusterElementName: string | null, connectionId: string | null, cadence: string, status: Types.KnowledgeBaseSourceStatus, enabled: boolean, lastSyncRunAt: any, lastSyncJobExecutionId: string | null, workflowId: string | null } | null };

export type KnowledgeBaseSourcesQueryVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
  filter?: Types.KnowledgeBaseSourceFilter | null | undefined;
}>;


export type KnowledgeBaseSourcesQuery = { knowledgeBaseSources: Array<{ id: string, name: string, knowledgeBaseId: string, sourceComponentName: string, sourceComponentVersion: number, sourceClusterElementName: string | null, connectionId: string | null, cadence: string, status: Types.KnowledgeBaseSourceStatus, enabled: boolean, lastSyncRunAt: any, lastSyncJobExecutionId: string | null, workflowId: string | null }> };

export type KnowledgeBaseStorageUsageQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseStorageUsageQuery = { knowledgeBaseStorageUsage: { limitBytes: any, percentage: number, unlimited: boolean, usedBytes: any } };

export type KnowledgeBaseTagsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type KnowledgeBaseTagsQuery = { knowledgeBaseTags: Array<{ id: string, name: string }> | null };

export type KnowledgeBaseTagsByKnowledgeBaseQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseTagsByKnowledgeBaseQuery = { knowledgeBaseTagsByKnowledgeBase: Array<{ knowledgeBaseId: string, tags: Array<{ id: string, name: string }> }> | null };

export type KnowledgeBasesQueryVariables = Exact<{
  environmentId: string | number;
  workspaceId: string | number;
}>;


export type KnowledgeBasesQuery = { knowledgeBases: Array<{ id: string, name: string, description: string | null, maxChunkSize: number | null, minChunkSizeChars: number | null, overlap: number | null, createdDate: any, lastModifiedDate: any } | null> | null };

export type RefreshKnowledgeBaseSourceMutationVariables = Exact<{
  id: string | number;
}>;


export type RefreshKnowledgeBaseSourceMutation = { refreshKnowledgeBaseSource: string };

export type SearchKnowledgeBaseQueryVariables = Exact<{
  id: string | number;
  query: string;
  metadataFilters?: string | null | undefined;
}>;


export type SearchKnowledgeBaseQuery = { searchKnowledgeBase: Array<{ id: string, knowledgeBaseDocumentId: string, content: string | null, metadata: any, score: number | null } | null> | null };

export type SetKnowledgeBaseSourceEnabledMutationVariables = Exact<{
  id: string | number;
  enabled: boolean;
}>;


export type SetKnowledgeBaseSourceEnabledMutation = { setKnowledgeBaseSourceEnabled: { id: string, enabled: boolean, status: Types.KnowledgeBaseSourceStatus } };

export type UpdateKnowledgeBaseMutationVariables = Exact<{
  id: string | number;
  knowledgeBase: Types.KnowledgeBaseInput;
}>;


export type UpdateKnowledgeBaseMutation = { updateKnowledgeBase: { id: string, name: string, description: string | null, maxChunkSize: number | null, minChunkSizeChars: number | null, overlap: number | null } | null };

export type UpdateKnowledgeBaseDocumentChunkMutationVariables = Exact<{
  id: string | number;
  knowledgeBaseDocumentChunk: Types.KnowledgeBaseDocumentChunkInput;
}>;


export type UpdateKnowledgeBaseDocumentChunkMutation = { updateKnowledgeBaseDocumentChunk: { id: string, knowledgeBaseDocumentId: string, content: string | null, metadata: any } | null };

export type UpdateKnowledgeBaseDocumentTagsMutationVariables = Exact<{
  input: Types.UpdateKnowledgeBaseDocumentTagsInput;
}>;


export type UpdateKnowledgeBaseDocumentTagsMutation = { updateKnowledgeBaseDocumentTags: boolean };

export type UpdateKnowledgeBaseSourceMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateKnowledgeBaseSourceInput;
}>;


export type UpdateKnowledgeBaseSourceMutation = { updateKnowledgeBaseSource: { id: string, name: string, cadence: string, enabled: boolean, status: Types.KnowledgeBaseSourceStatus } };

export type UpdateKnowledgeBaseTagsMutationVariables = Exact<{
  input: Types.UpdateKnowledgeBaseTagsInput;
}>;


export type UpdateKnowledgeBaseTagsMutation = { updateKnowledgeBaseTags: boolean };

export type AutomationSearchQueryVariables = Exact<{
  query: string;
  limit?: number | null | undefined;
}>;


export type AutomationSearchQuery = { automationSearch: Array<
    | { id: string, name: string, description: string | null, type: Types.SearchAssetType }
    | { collectionId: string, path: string | null, id: string, name: string, description: string | null, type: Types.SearchAssetType }
    | { id: string, name: string, description: string | null, type: Types.SearchAssetType }
    | { id: string, name: string, description: string | null, type: Types.SearchAssetType }
    | { knowledgeBaseId: string, id: string, name: string, description: string | null, type: Types.SearchAssetType }
    | { id: string, name: string, description: string | null, type: Types.SearchAssetType }
    | { projectName: string, id: string, name: string, description: string | null, type: Types.SearchAssetType }
    | { id: string, name: string, description: string | null, type: Types.SearchAssetType }
    | { projectId: string, label: string, id: string, name: string, description: string | null, type: Types.SearchAssetType }
  > };

export type WorkflowAlertRulesQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkflowAlertRulesQuery = { workflowAlertRules: Array<{ cooldownMinutes: number, enabled: boolean, id: string, lastTriggeredDate: string | null, name: string, notificationIds: Array<string>, ruleType: Types.WorkflowAlertRuleType, threshold: number, windowMinutes: number | null, workflowId: string | null }> };

export type WorkflowAlertEventsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkflowAlertEventsQuery = { workflowAlertEvents: Array<{ createdDate: string | null, id: string, jobId: string | null, message: string | null, triggeredValue: number | null, workflowAlertRuleId: string }> };

export type CreateWorkflowAlertRuleMutationVariables = Exact<{
  workspaceId: string | number;
  input: Types.WorkflowAlertRuleInput;
}>;


export type CreateWorkflowAlertRuleMutation = { createWorkflowAlertRule: { id: string } };

export type UpdateWorkflowAlertRuleMutationVariables = Exact<{
  id: string | number;
  input: Types.WorkflowAlertRuleInput;
}>;


export type UpdateWorkflowAlertRuleMutation = { updateWorkflowAlertRule: { id: string } };

export type DeleteWorkflowAlertRuleMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteWorkflowAlertRuleMutation = { deleteWorkflowAlertRule: boolean };

export type SendTestWorkflowAlertMutationVariables = Exact<{
  id: string | number;
}>;


export type SendTestWorkflowAlertMutation = { sendTestWorkflowAlert: boolean };

export type EnableWorkflowAlertRuleMutationVariables = Exact<{
  id: string | number;
  enabled: boolean;
}>;


export type EnableWorkflowAlertRuleMutation = { enableWorkflowAlertRule: { enabled: boolean, id: string } };

export type WorkflowExecutionCostQueryVariables = Exact<{
  jobId: string | number;
}>;


export type WorkflowExecutionCostQuery = { workflowExecutionCost: { aiCost: number, baseRunCharge: number, currency: string, id: string, jobId: string, totalCost: number } | null };

export type CodeWorkflowSourceQueryVariables = Exact<{
  projectId: string | number;
}>;


export type CodeWorkflowSourceQuery = { codeWorkflowSource: string };

export type CreateCodeWorkflowMutationVariables = Exact<{
  workspaceId: string | number;
  name: string;
  language: Types.CodeWorkflowLanguage;
  description?: string | null | undefined;
  categoryId?: string | number | null | undefined;
  tags?: Array<string> | string | null | undefined;
}>;


export type CreateCodeWorkflowMutation = { createCodeWorkflow: string };

export type UpdateCodeWorkflowSourceMutationVariables = Exact<{
  projectId: string | number;
  content: string;
}>;


export type UpdateCodeWorkflowSourceMutation = { updateCodeWorkflowSource: boolean };

export type AutomationWorkflowProjectCategoriesQueryVariables = Exact<{ [key: string]: never; }>;


export type AutomationWorkflowProjectCategoriesQuery = { automationWorkflowProjectCategories: Array<{ id: string, name: string }> };

export type AutomationWorkflowProjectTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type AutomationWorkflowProjectTagsQuery = { automationWorkflowProjectTags: Array<{ id: string, name: string }> };

export type AutomationWorkflowProjectVersionsQueryVariables = Exact<{
  id: string | number;
}>;


export type AutomationWorkflowProjectVersionsQuery = { automationWorkflowProjectVersions: Array<{ version: number, status: string, publishedDate: string | null }> };

export type AutomationWorkflowProjectsQueryVariables = Exact<{ [key: string]: never; }>;


export type AutomationWorkflowProjectsQuery = { automationWorkflowProjects: Array<{ id: string, name: string, description: string | null, categoryId: string | null, tagIds: Array<string>, published: boolean, version: number, lastPublishedVersion: number | null, permissionExpression: string | null, codeWorkflowProject: boolean, workflowTemplates: Array<{ workflowUuid: string, label: string | null, description: string | null, permissionExpression: string | null, lastModifiedDate: string | null, triggers: Array<{ name: string, title: string | null, icon: string | null }>, components: Array<{ name: string, title: string | null, icon: string | null }> }> }> };

export type CreateAutomationWorkflowProjectMutationVariables = Exact<{
  name: string;
  description?: string | null | undefined;
  category?: string | null | undefined;
  tags?: Array<string> | string | null | undefined;
  permissionExpression?: string | null | undefined;
}>;


export type CreateAutomationWorkflowProjectMutation = { createAutomationWorkflowProject: string };

export type UpdateAutomationWorkflowProjectMutationVariables = Exact<{
  id: string | number;
  name: string;
  description?: string | null | undefined;
  category?: string | null | undefined;
  tags?: Array<string> | string | null | undefined;
  permissionExpression?: string | null | undefined;
}>;


export type UpdateAutomationWorkflowProjectMutation = { updateAutomationWorkflowProject: boolean };

export type DeleteAutomationWorkflowProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAutomationWorkflowProjectMutation = { deleteAutomationWorkflowProject: boolean };

export type CreateAutomationWorkflowProjectWorkflowMutationVariables = Exact<{
  projectId: string | number;
  definition?: string | null | undefined;
  permissionExpression?: string | null | undefined;
}>;


export type CreateAutomationWorkflowProjectWorkflowMutation = { createAutomationWorkflowProjectWorkflow: string };

export type UpdateAutomationWorkflowProjectWorkflowMutationVariables = Exact<{
  workflowUuid: string | number;
  label: string;
  description?: string | null | undefined;
}>;


export type UpdateAutomationWorkflowProjectWorkflowMutation = { updateAutomationWorkflowProjectWorkflow: boolean };

export type UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutationVariables = Exact<{
  workflowUuid: string | number;
  permissionExpression?: string | null | undefined;
}>;


export type UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation = { updateAutomationWorkflowProjectWorkflowPermissionExpression: boolean };

export type DeleteAutomationWorkflowProjectWorkflowMutationVariables = Exact<{
  workflowUuid: string | number;
}>;


export type DeleteAutomationWorkflowProjectWorkflowMutation = { deleteAutomationWorkflowProjectWorkflow: boolean };

export type PublishAutomationWorkflowProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type PublishAutomationWorkflowProjectMutation = { publishAutomationWorkflowProject: boolean };

export type ConnectedUserCodeWorkflowReferencesQueryVariables = Exact<{
  catalogWorkflowUuids: Array<string | number> | string | number;
}>;


export type ConnectedUserCodeWorkflowReferencesQuery = { connectedUserCodeWorkflowReferences: Array<{ catalogWorkflowUuid: string, externalUserId: string, environment: string, enabled: boolean, dangling: boolean, danglingReason: string | null }> };

export type ConnectedUserMcpServersQueryVariables = Exact<{
  connectedUserId: string | number;
}>;


export type ConnectedUserMcpServersQuery = { connectedUserMcpServers: Array<{ id: string, name: string, enabled: boolean, environmentId: string, lastModifiedDate: string | null, tools: Array<{ id: string, componentName: string, componentVersion: number, integrationInstanceId: string, name: string, enabled: boolean }> }> };

export type ConnectedUserProjectsQueryVariables = Exact<{
  connectedUserId?: string | number | null | undefined;
  environmentId?: string | number | null | undefined;
}>;


export type ConnectedUserProjectsQuery = { connectedUserProjects: Array<{ id: string, environmentId: string, lastExecutionDate: string | null, projectId: string, projectVersion: number | null, connectedUser: { id: string, environmentId: string, externalId: string }, connectedUserProjectWorkflows: Array<{ id: string, connectedUserId: string, enabled: boolean, lastExecutionDate: string | null, projectId: string, workflowUuid: string, workflowVersion: number | null, workflow: { id: string, label: string, triggers: Array<{ name: string, type: string, parameters: any }> } }> }> };

export type CreateEmbeddedMcpServerMutationVariables = Exact<{
  input: Types.CreateEmbeddedMcpServerInput;
}>;


export type CreateEmbeddedMcpServerMutation = { createEmbeddedMcpServer: { enabled: boolean, environmentId: string, id: string, name: string, type: Types.PlatformType } | null };

export type CreateMcpIntegrationInstanceConfigurationMutationVariables = Exact<{
  input: Types.CreateMcpIntegrationInstanceConfigurationInput;
}>;


export type CreateMcpIntegrationInstanceConfigurationMutation = { createMcpIntegrationInstanceConfiguration: { id: string, integrationInstanceConfigurationId: string, mcpServerId: string } | null };

export type DeleteConnectedUserMcpServerMutationVariables = Exact<{
  connectedUserId: string | number;
  mcpServerId: string | number;
}>;


export type DeleteConnectedUserMcpServerMutation = { deleteConnectedUserMcpServer: boolean | null };

export type DeleteConnectedUserProjectWorkflowMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteConnectedUserProjectWorkflowMutation = { deleteConnectedUserProjectWorkflow: boolean | null };

export type DeleteEmbeddedMcpServerMutationVariables = Exact<{
  mcpServerId: string | number;
}>;


export type DeleteEmbeddedMcpServerMutation = { deleteEmbeddedMcpServer: boolean | null };

export type DeleteMcpIntegrationInstanceConfigurationMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteMcpIntegrationInstanceConfigurationMutation = { deleteMcpIntegrationInstanceConfiguration: boolean | null };

export type DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteMcpIntegrationInstanceConfigurationWorkflowMutation = { deleteMcpIntegrationInstanceConfigurationWorkflow: boolean | null };

export type DuplicateAutomationWorkflowProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type DuplicateAutomationWorkflowProjectMutation = { duplicateAutomationWorkflowProject: string };

export type DuplicateAutomationWorkflowProjectWorkflowMutationVariables = Exact<{
  workflowUuid: string | number;
}>;


export type DuplicateAutomationWorkflowProjectWorkflowMutation = { duplicateAutomationWorkflowProjectWorkflow: string };

export type EmbeddedMcpServerTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type EmbeddedMcpServerTagsQuery = { embeddedMcpServerTags: Array<{ id: string, name: string } | null> | null };

export type EmbeddedMcpServersQueryVariables = Exact<{ [key: string]: never; }>;


export type EmbeddedMcpServersQuery = { embeddedMcpServers: Array<{ id: string, enabled: boolean, enforceToolAuthorization: boolean, authenticationRequired: boolean, environmentId: string, lastModifiedDate: any, name: string, type: Types.PlatformType, url: string, mcpComponents: Array<{ componentName: string, componentVersion: number, connectionId: string | null, id: string, lastModifiedDate: any, mcpServerId: string, title: string | null, mcpTools: Array<{ id: string, enabled: boolean, mcpComponentId: string, name: string, title: string | null, parameters: any } | null> | null } | null> | null, tags: Array<{ id: string, name: string } | null> | null } | null> | null };

export type EnableConnectedUserMcpServerMutationVariables = Exact<{
  connectedUserId: string | number;
  mcpServerId: string | number;
  enable: boolean;
}>;


export type EnableConnectedUserMcpServerMutation = { enableConnectedUserMcpServer: boolean | null };

export type EnableConnectedUserMcpToolMutationVariables = Exact<{
  id: string | number;
  enable: boolean;
}>;


export type EnableConnectedUserMcpToolMutation = { enableConnectedUserMcpTool: boolean | null };

export type EnableConnectedUserProjectWorkflowMutationVariables = Exact<{
  id: string | number;
  enable: boolean;
}>;


export type EnableConnectedUserProjectWorkflowMutation = { enableConnectedUserProjectWorkflow: boolean | null };

export type IntegrationWorkflowsQueryVariables = Exact<{ [key: string]: never; }>;


export type IntegrationWorkflowsQuery = { integrationWorkflows: Array<{ id: string, label: string, description: string | null, integrationWorkflowId: string, workflowUuid: string | null, workflowTaskComponentNames: Array<string>, workflowTriggerComponentNames: Array<string>, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any }> };

export type IntegrationWorkflowsByIntegrationIdQueryVariables = Exact<{
  integrationId: string | number;
}>;


export type IntegrationWorkflowsByIntegrationIdQuery = { integrationWorkflowsByIntegrationId: Array<{ id: string, label: string, description: string | null, integrationWorkflowId: string, workflowUuid: string | null, permissionExpression: string | null, workflowTaskComponentNames: Array<string>, workflowTriggerComponentNames: Array<string>, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any }> };

export type McpComponentDefinitionsQueryVariables = Exact<{ [key: string]: never; }>;


export type McpComponentDefinitionsQuery = { mcpComponentDefinitions: Array<{ clusterElementsCount: any, description: string | null, icon: string | null, name: string, title: string | null, version: number | null }> };

export type McpIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables = Exact<{
  mcpIntegrationInstanceConfigurationWorkflowId: string | number;
}>;


export type McpIntegrationInstanceConfigurationWorkflowPropertiesQuery = { mcpIntegrationInstanceConfigurationWorkflowProperties: Array<
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, arrayDefaultValue: Array<any> | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, booleanDefaultValue: boolean | null }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, integerDefaultValue: any }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, numberDefaultValue: number | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, objectDefaultValue: any }
    | { controlType: Types.ControlType, defaultValue: string | null, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
   | null> | null };

export type McpIntegrationInstanceConfigurationsQueryVariables = Exact<{ [key: string]: never; }>;


export type McpIntegrationInstanceConfigurationsQuery = { mcpIntegrationInstanceConfigurations: Array<{ id: string, integrationInstanceConfigurationId: string, mcpServerId: string, integration: { id: string, name: string } | null, mcpIntegrationInstanceConfigurationWorkflows: Array<{ integrationInstanceConfigurationWorkflow: { workflowId: string } | null } | null> | null } | null> | null };

export type McpIntegrationInstanceConfigurationsByServerIdQueryVariables = Exact<{
  mcpServerId?: string | number | null | undefined;
}>;


export type McpIntegrationInstanceConfigurationsByServerIdQuery = { mcpIntegrationInstanceConfigurationsByServerId: Array<{ id: string, integrationInstanceConfigurationId: string, integrationInstanceConfigurationName: string | null, integrationVersion: number | null, lastModifiedDate: any, mcpServerId: string, integration: { componentName: string, id: string, name: string } | null, mcpIntegrationInstanceConfigurationWorkflows: Array<{ id: string, integrationInstanceConfigurationWorkflowId: any, mcpIntegrationInstanceConfigurationId: any, parameters: any, integrationInstanceConfigurationWorkflow: { id: string, enabled: boolean, inputs: any, integrationInstanceConfigurationId: string, version: number, workflowId: string, connections: Array<{ connectionId: string | null, workflowConnectionKey: string, workflowNodeName: string }> } | null, workflow: { id: string, label: string } | null } | null> | null } | null> | null };

export type ToolEligibleIntegrationInstanceConfigurationWorkflowsQueryVariables = Exact<{
  integrationInstanceConfigurationId: string | number;
}>;


export type ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery = { toolEligibleIntegrationInstanceConfigurationWorkflows: Array<{ id: string, integrationWorkflowId: string, label: string }> };

export type ToolEligibleIntegrationVersionWorkflowsQueryVariables = Exact<{
  integrationId: string | number;
  integrationVersion: number;
}>;


export type ToolEligibleIntegrationVersionWorkflowsQuery = { toolEligibleIntegrationVersionWorkflows: Array<{ id: string, integrationWorkflowId: string, label: string }> };

export type UpdateIntegrationWorkflowPermissionExpressionMutationVariables = Exact<{
  integrationWorkflowId: string | number;
  permissionExpression?: string | null | undefined;
}>;


export type UpdateIntegrationWorkflowPermissionExpressionMutation = { updateIntegrationWorkflowPermissionExpression: { id: string, permissionExpression: string | null } | null };

export type UpdateMcpIntegrationInstanceConfigurationMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateMcpIntegrationInstanceConfigurationInput;
}>;


export type UpdateMcpIntegrationInstanceConfigurationMutation = { updateMcpIntegrationInstanceConfiguration: { id: string, integrationInstanceConfigurationId: string, mcpServerId: string } | null };

export type UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateMcpIntegrationInstanceConfigurationVersionInput;
}>;


export type UpdateMcpIntegrationInstanceConfigurationVersionMutation = { updateMcpIntegrationInstanceConfigurationVersion: boolean | null };

export type UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables = Exact<{
  id: string | number;
  input: Types.McpIntegrationInstanceConfigurationWorkflowUpdateInput;
}>;


export type UpdateMcpIntegrationInstanceConfigurationWorkflowMutation = { updateMcpIntegrationInstanceConfigurationWorkflow: { id: string, mcpIntegrationInstanceConfigurationId: any, integrationInstanceConfigurationWorkflowId: any, parameters: any } | null };

export type CreateIntegrationCodeWorkflowMutationVariables = Exact<{
  componentName: string;
  language: Types.CodeWorkflowLanguage;
  name?: string | null | undefined;
  description?: string | null | undefined;
  categoryId?: string | number | null | undefined;
  tags?: Array<string> | string | null | undefined;
  permissionExpression?: string | null | undefined;
}>;


export type CreateIntegrationCodeWorkflowMutation = { createIntegrationCodeWorkflow: string };

export type IntegrationCodeWorkflowSourceQueryVariables = Exact<{
  integrationId: string | number;
}>;


export type IntegrationCodeWorkflowSourceQuery = { integrationCodeWorkflowSource: string };

export type UpdateIntegrationCodeWorkflowSourceMutationVariables = Exact<{
  integrationId: string | number;
  content: string;
}>;


export type UpdateIntegrationCodeWorkflowSourceMutation = { updateIntegrationCodeWorkflowSource: boolean };

export type AiGuardrailsWorkspaceSettingsQueryVariables = Exact<{
  workspaceId?: string | number | null | undefined;
}>;


export type AiGuardrailsWorkspaceSettingsQuery = { aiGuardrailsWorkspaceSettings: { blockedTerms: string | null, blockingMode: Types.AiGuardrailsBlockingMode | null, injectionDetectionEnabled: boolean | null, moderationEnabled: boolean | null, redactPii: boolean | null, redactSecrets: boolean | null, scanResponses: boolean | null, workspaceId: string | null } | null };

export type UpdateAiGuardrailsWorkspaceSettingsMutationVariables = Exact<{
  input: Types.AiGuardrailsWorkspaceSettingsInput;
}>;


export type UpdateAiGuardrailsWorkspaceSettingsMutation = { updateAiGuardrailsWorkspaceSettings: { blockedTerms: string | null, blockingMode: Types.AiGuardrailsBlockingMode | null, injectionDetectionEnabled: boolean | null, moderationEnabled: boolean | null, redactPii: boolean | null, redactSecrets: boolean | null, scanResponses: boolean | null, workspaceId: string | null } | null };

export type AiDefaultModelQueryVariables = Exact<{
  environment: string | number;
}>;


export type AiDefaultModelQuery = { aiDefaultModel: { provider: string, model: string } | null };

export type AiProviderCatalogQueryVariables = Exact<{
  environment: string | number;
}>;


export type AiProviderCatalogQuery = { aiProviderCatalog: Array<{ key: string, name: string, icon: string | null, enabled: boolean, supportsModelById: boolean, models: Array<{ name: string, label: string }> }> };

export type ApiConnectorQueryVariables = Exact<{
  id: string | number;
}>;


export type ApiConnectorQuery = { apiConnector: { id: string, name: string, title: string | null, description: string | null, icon: string | null, connectorVersion: number, enabled: boolean | null, specification: string | null, definition: string | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, endpoints: Array<{ id: string, name: string, description: string | null, path: string | null, httpMethod: Types.HttpMethod | null }> | null } | null };

export type ApiConnectorsQueryVariables = Exact<{ [key: string]: never; }>;


export type ApiConnectorsQuery = { apiConnectors: Array<{ id: string, name: string, title: string | null, description: string | null, icon: string | null, connectorVersion: number, enabled: boolean | null, specification: string | null, definition: string | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, endpoints: Array<{ id: string, name: string, description: string | null, path: string | null, httpMethod: Types.HttpMethod | null }> | null }> };

export type CancelGenerationJobMutationVariables = Exact<{
  jobId: string;
}>;


export type CancelGenerationJobMutation = { cancelGenerationJob: boolean };

export type DeleteApiConnectorMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteApiConnectorMutation = { deleteApiConnector: boolean };

export type EnableApiConnectorMutationVariables = Exact<{
  id: string | number;
  enable: boolean;
}>;


export type EnableApiConnectorMutation = { enableApiConnector: boolean };

export type GenerateSpecificationMutationVariables = Exact<{
  input: Types.GenerateSpecificationInput;
}>;


export type GenerateSpecificationMutation = { generateSpecification: { specification: string | null } };

export type GenerationJobStatusQueryVariables = Exact<{
  jobId: string;
}>;


export type GenerationJobStatusQuery = { generationJobStatus: { jobId: string, status: Types.GenerationJobStatusEnum, specification: string | null, errorMessage: string | null } | null };

export type ImportOpenApiSpecificationMutationVariables = Exact<{
  input: Types.ImportOpenApiSpecificationInput;
}>;


export type ImportOpenApiSpecificationMutation = { importOpenApiSpecification: { id: string, name: string, title: string | null, description: string | null, icon: string | null, connectorVersion: number, enabled: boolean | null, specification: string | null, definition: string | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, endpoints: Array<{ id: string, name: string, description: string | null, path: string | null, httpMethod: Types.HttpMethod | null }> | null } };

export type StartGenerateFromDocumentationPreviewMutationVariables = Exact<{
  input: Types.GenerateFromDocumentationInput;
}>;


export type StartGenerateFromDocumentationPreviewMutation = { startGenerateFromDocumentationPreview: { jobId: string, status: Types.GenerationJobStatusEnum, specification: string | null, errorMessage: string | null } };

export type UpdateApiConnectorMutationVariables = Exact<{
  id: string | number;
  input: Types.UpdateApiConnectorInput;
}>;


export type UpdateApiConnectorMutation = { updateApiConnector: { id: string, name: string, title: string | null, description: string | null, icon: string | null, connectorVersion: number, enabled: boolean | null, specification: string | null, definition: string | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, endpoints: Array<{ id: string, name: string, description: string | null, path: string | null, httpMethod: Types.HttpMethod | null }> | null } };

export type EditorJobFileLogsQueryVariables = Exact<{
  jobId: string | number;
  filter?: Types.LogFilterInput | null | undefined;
  page?: number | null | undefined;
  size?: number | null | undefined;
}>;


export type EditorJobFileLogsQuery = { editorJobFileLogs: { totalElements: number, totalPages: number, pageNumber: number, pageSize: number, hasNext: boolean, hasPrevious: boolean, content: Array<{ timestamp: string, level: Types.LogLevel, componentName: string, componentOperationName: string | null, taskExecutionId: string, message: string, exceptionType: string | null, exceptionMessage: string | null, stackTrace: string | null }> } };

export type EditorJobFileLogsExistQueryVariables = Exact<{
  jobId: string | number;
}>;


export type EditorJobFileLogsExistQuery = { editorJobFileLogsExist: boolean };

export type EditorTaskExecutionFileLogsQueryVariables = Exact<{
  jobId: string | number;
  taskExecutionId: string | number;
}>;


export type EditorTaskExecutionFileLogsQuery = { editorTaskExecutionFileLogs: Array<{ timestamp: string, level: Types.LogLevel, componentName: string, componentOperationName: string | null, taskExecutionId: string, message: string, exceptionType: string | null, exceptionMessage: string | null, stackTrace: string | null }> };

export type JobFileLogsQueryVariables = Exact<{
  jobId: string | number;
  filter?: Types.LogFilterInput | null | undefined;
  page?: number | null | undefined;
  size?: number | null | undefined;
}>;


export type JobFileLogsQuery = { jobFileLogs: { totalElements: number, totalPages: number, pageNumber: number, pageSize: number, hasNext: boolean, hasPrevious: boolean, content: Array<{ timestamp: string, level: Types.LogLevel, componentName: string, componentOperationName: string | null, taskExecutionId: string, message: string, exceptionType: string | null, exceptionMessage: string | null, stackTrace: string | null }> } };

export type JobFileLogsExistQueryVariables = Exact<{
  jobId: string | number;
}>;


export type JobFileLogsExistQuery = { jobFileLogsExist: boolean };

export type TaskExecutionFileLogsQueryVariables = Exact<{
  jobId: string | number;
  taskExecutionId: string | number;
}>;


export type TaskExecutionFileLogsQuery = { taskExecutionFileLogs: Array<{ timestamp: string, level: Types.LogLevel, componentName: string, componentOperationName: string | null, taskExecutionId: string, message: string, exceptionType: string | null, exceptionMessage: string | null, stackTrace: string | null }> };

export type ComponentOperationPoliciesQueryVariables = Exact<{
  componentName: string;
}>;


export type ComponentOperationPoliciesQuery = { componentOperationPolicies: Array<{ componentName: string, operationType: Types.ComponentOperationType, operationName: string }> };

export type ComponentPoliciesQueryVariables = Exact<{ [key: string]: never; }>;


export type ComponentPoliciesQuery = { componentPolicies: Array<{ name: string, title: string | null, description: string | null, icon: string | null, version: number, enabled: boolean }> };

export type UpdateComponentOperationPolicyMutationVariables = Exact<{
  componentName: string;
  operationType: Types.ComponentOperationType;
  operationName: string;
  enabled: boolean;
}>;


export type UpdateComponentOperationPolicyMutation = { updateComponentOperationPolicy: boolean };

export type UpdateComponentPolicyMutationVariables = Exact<{
  name: string;
  enabled: boolean;
}>;


export type UpdateComponentPolicyMutation = { updateComponentPolicy: { name: string, title: string | null, icon: string | null, version: number, enabled: boolean } };

export type AdminApiKeysQueryVariables = Exact<{
  environmentId: string | number;
}>;


export type AdminApiKeysQuery = { adminApiKeys: Array<{ id: string | null, name: string | null, secretKey: string | null, lastUsedDate: any, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any } | null> | null };

export type ApiKeysQueryVariables = Exact<{
  environmentId: string | number;
  type: Types.PlatformType;
}>;


export type ApiKeysQuery = { apiKeys: Array<{ id: string | null, name: string | null, secretKey: string | null, lastUsedDate: any, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any } | null> | null };

export type ClusterElementComponentConnectionsQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  clusterElementType: string;
  clusterElementWorkflowNodeName: string;
}>;


export type ClusterElementComponentConnectionsQuery = { clusterElementComponentConnections: Array<{ componentName: string, componentVersion: number, key: string, required: boolean, workflowNodeName: string }> };

export type ClusterElementDefinitionQueryVariables = Exact<{
  componentName: string;
  componentVersion: number;
  clusterElementName: string;
}>;


export type ClusterElementDefinitionQuery = { clusterElementDefinition: { componentName: string | null, componentVersion: number | null, description: string | null, name: string, title: string | null, properties: Array<
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, arrayDefaultValue: Array<any> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null, items: Array<
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, arrayDefaultValue: Array<any> | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, booleanDefaultValue: boolean | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, dateDefaultValue: string | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, dateTimeDefaultValue: string | null }
          | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, propertiesDataSource: { propertiesLookupDependsOn: Array<string> | null } | null }
          | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, integerDefaultValue: any, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, numberDefaultValue: number | null, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, objectDefaultValue: any }
          | { controlType: Types.ControlType, defaultValue: string | null, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, timeDefaultValue: string | null }
        > | null }
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, booleanDefaultValue: boolean | null }
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, dateDefaultValue: string | null }
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, dateTimeDefaultValue: string | null }
      | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, propertiesDataSource: { propertiesLookupDependsOn: Array<string> | null } | null }
      | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, integerDefaultValue: any, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, numberDefaultValue: number | null, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, objectDefaultValue: any, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null, properties: Array<
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, arrayDefaultValue: Array<any> | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, booleanDefaultValue: boolean | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, dateDefaultValue: string | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, dateTimeDefaultValue: string | null }
          | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, propertiesDataSource: { propertiesLookupDependsOn: Array<string> | null } | null }
          | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, integerDefaultValue: any, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, numberDefaultValue: number | null, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, objectDefaultValue: any }
          | { controlType: Types.ControlType, defaultValue: string | null, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
          | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, timeDefaultValue: string | null }
        > | null }
      | { controlType: Types.ControlType, defaultValue: string | null, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
      | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, timeDefaultValue: string | null }
    > } };

export type ClusterElementFieldsQueryVariables = Exact<{
  componentName: string;
  componentVersion: number;
  clusterElementName: string;
  connectionId?: any;
  inputParameters?: any;
}>;


export type ClusterElementFieldsQuery = { clusterElementFields: Array<{ name: string, label: string | null, type: string | null }> };

export type ClusterElementMissingRequiredPropertiesQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  clusterElementType: string;
  clusterElementWorkflowNodeName: string;
}>;


export type ClusterElementMissingRequiredPropertiesQuery = { clusterElementMissingRequiredProperties: Array<string> };

export type ClusterElementDynamicPropertiesQueryVariables = Exact<{
  componentName: string;
  componentVersion: number;
  clusterElementName: string;
  propertyName: string;
  connectionId?: any;
  inputParameters?: any;
  lookupDependsOnPaths?: Array<string> | string | null | undefined;
}>;


export type ClusterElementDynamicPropertiesQuery = { clusterElementDynamicProperties: Array<
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null, items: Array<
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, propertiesDataSource: { propertiesLookupDependsOn: Array<string> | null } | null }
        | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
      > | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, propertiesDataSource: { propertiesLookupDependsOn: Array<string> | null } | null }
    | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null, properties: Array<
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, propertiesDataSource: { propertiesLookupDependsOn: Array<string> | null } | null }
        | { advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
        | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
      > | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType, options: Array<{ description: string | null, label: string | null, value: any }> | null, optionsDataSource: { optionsLookupDependsOn: Array<string> | null } | null }
    | { controlType: Types.ControlType, label: string | null, placeholder: string | null, advancedOption: boolean | null, description: string | null, displayCondition: string | null, expressionEnabled: boolean | null, hidden: boolean | null, name: string | null, required: boolean | null, type: Types.PropertyType }
  > };

export type ClusterElementOptionsQueryVariables = Exact<{
  componentName: string;
  componentVersion: number;
  clusterElementName: string;
  propertyName: string;
  connectionId?: any;
  inputParameters?: any;
  lookupDependsOnPaths?: Array<string> | string | null | undefined;
}>;


export type ClusterElementOptionsQuery = { clusterElementOptions: Array<{ description: string | null, label: string | null, value: any }> };

export type ClusterElementScriptInputQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  clusterElementType: string;
  clusterElementWorkflowNodeName: string;
  environmentId: any;
}>;


export type ClusterElementScriptInputQuery = { clusterElementScriptInput: any };

export type ComponentDefinitionSearchQueryVariables = Exact<{
  query: string;
}>;


export type ComponentDefinitionSearchQuery = { componentDefinitionSearch: Array<{ name: string, title: string | null, icon: string | null, description: string | null, version: number | null, actionsCount: number | null, triggersCount: number | null, clusterElementsCount: any, clusterRoot: boolean | null, componentCategories: Array<{ name: string, label: string | null }> | null, actions: Array<{ name: string, title: string | null, description: string | null }> | null, triggers: Array<{ name: string, title: string | null, description: string | null }> | null, clusterElements: Array<{ type: { name: string | null, label: string | null } | null }> | null }> };

export type ComponentPropertyDisplayConditionsQueryVariables = Exact<{
  componentName: string;
  componentVersion: number;
  operationName: string;
  operationType: string;
  parameters?: any;
}>;


export type ComponentPropertyDisplayConditionsQuery = { componentPropertyDisplayConditions: any };

export type CreateApiKeyMutationVariables = Exact<{
  name: string;
  environmentId: string | number;
  type?: Types.PlatformType | null | undefined;
}>;


export type CreateApiKeyMutation = { createApiKey: string };

export type CreateMcpComponentMutationVariables = Exact<{
  input: Types.McpComponentInput;
}>;


export type CreateMcpComponentMutation = { createMcpComponent: { id: string, componentName: string, componentVersion: number, title: string | null, mcpServerId: string, connectionId: string | null } | null };

export type CreateMcpComponentWithToolsMutationVariables = Exact<{
  input: Types.McpComponentWithToolsInput;
}>;


export type CreateMcpComponentWithToolsMutation = { createMcpComponentWithTools: { id: string, componentName: string, componentVersion: number, title: string | null, mcpServerId: string, connectionId: string | null, requiredAuthorities: Array<string>, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null } | null };

export type CreateMcpToolMutationVariables = Exact<{
  input: Types.McpToolInput;
}>;


export type CreateMcpToolMutation = { createMcpTool: { id: string, name: string, mcpComponentId: string, parameters: any } | null };

export type DeleteApiKeyMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteApiKeyMutation = { deleteApiKey: boolean };

export type DeleteMcpComponentMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteMcpComponentMutation = { deleteMcpComponent: boolean | null };

export type DeleteMcpToolMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteMcpToolMutation = { deleteMcpTool: boolean | null };

export type EnvironmentsQueryVariables = Exact<{ [key: string]: never; }>;


export type EnvironmentsQuery = { environments: Array<{ id: string, name: string } | null> | null };

export type EvaluatorFunctionDefinitionsQueryVariables = Exact<{ [key: string]: never; }>;


export type EvaluatorFunctionDefinitionsQuery = { evaluatorFunctionDefinitions: Array<{ name: string, title: string, description: string, category: Types.EvaluatorFunctionCategory, returnType: Types.EvaluatorFunctionType, example: string, parameters: Array<{ name: string, description: string, type: Types.EvaluatorFunctionType, required: boolean }> }> };

export type ManagementMcpServerAuthenticationRequiredQueryVariables = Exact<{ [key: string]: never; }>;


export type ManagementMcpServerAuthenticationRequiredQuery = { managementMcpServerAuthenticationRequired: boolean };

export type ManagementMcpServerUrlQueryVariables = Exact<{ [key: string]: never; }>;


export type ManagementMcpServerUrlQuery = { managementMcpServerUrl: string | null };

export type McpComponentsByServerIdQueryVariables = Exact<{
  mcpServerId: string | number;
}>;


export type McpComponentsByServerIdQuery = { mcpComponentsByServerId: Array<{ id: string, componentName: string, componentVersion: number, title: string | null, connectionId: string | null, lastModifiedDate: any, mcpServerId: string, requiredAuthorities: Array<string>, version: number | null, mcpTools: Array<{ id: string, enabled: boolean, mcpComponentId: string, name: string, parameters: any, title: string | null, version: number | null } | null> | null } | null> | null };

export type McpToolsByComponentIdQueryVariables = Exact<{
  mcpComponentId: string | number;
}>;


export type McpToolsByComponentIdQuery = { mcpToolsByComponentId: Array<{ id: string, enabled: boolean, name: string, title: string | null, mcpComponentId: string, parameters: any, version: number | null } | null> | null };

export type SaveClusterElementTestConfigurationConnectionMutationVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  clusterElementType: string;
  clusterElementWorkflowNodeName: string;
  workflowConnectionKey: string;
  connectionId: any;
  environmentId: any;
}>;


export type SaveClusterElementTestConfigurationConnectionMutation = { saveClusterElementTestConfigurationConnection: boolean | null };

export type SaveClusterElementTestOutputMutationVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  clusterElementType: string;
  clusterElementWorkflowNodeName: string;
  environmentId: any;
  inputParameters?: any;
}>;


export type SaveClusterElementTestOutputMutation = { saveClusterElementTestOutput: { id: any, workflowId: string, workflowNodeName: string } | null };

export type SaveWorkflowTestConfigurationConnectionMutationVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  workflowConnectionKey: string;
  connectionId: any;
  environmentId: any;
}>;


export type SaveWorkflowTestConfigurationConnectionMutation = { saveWorkflowTestConfigurationConnection: boolean | null };

export type TestClusterElementScriptMutationVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  clusterElementType: string;
  clusterElementWorkflowNodeName: string;
  environmentId: any;
  inputParameters?: any;
}>;


export type TestClusterElementScriptMutation = { testClusterElementScript: { output: any, error: { message: string | null, stackTrace: Array<string | null> | null } | null } };

export type TestWorkflowNodeScriptMutationVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  environmentId: any;
  inputParameters?: any;
}>;


export type TestWorkflowNodeScriptMutation = { testWorkflowNodeScript: { output: any, error: { message: string | null, stackTrace: Array<string | null> | null } | null } };

export type UpdateApiKeyMutationVariables = Exact<{
  id: string | number;
  name: string;
}>;


export type UpdateApiKeyMutation = { updateApiKey: boolean };

export type UpdateManagementMcpServerAuthenticationRequiredMutationVariables = Exact<{
  authenticationRequired: boolean;
}>;


export type UpdateManagementMcpServerAuthenticationRequiredMutation = { updateManagementMcpServerAuthenticationRequired: boolean };

export type UpdateManagementMcpServerUrlMutationVariables = Exact<{ [key: string]: never; }>;


export type UpdateManagementMcpServerUrlMutation = { updateManagementMcpServerUrl: string };

export type UpdateMcpComponentWithToolsMutationVariables = Exact<{
  id: string | number;
  input: Types.McpComponentWithToolsInput;
}>;


export type UpdateMcpComponentWithToolsMutation = { updateMcpComponentWithTools: { id: string, componentName: string, componentVersion: number, title: string | null, mcpServerId: string, connectionId: string | null, requiredAuthorities: Array<string>, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null } | null };

export type UpdateMcpServerUrlMutationVariables = Exact<{
  id: string | number;
}>;


export type UpdateMcpServerUrlMutation = { updateMcpServerUrl: string };

export type UpdateMcpToolMutationVariables = Exact<{
  id: string | number;
  input: Types.McpToolInput;
}>;


export type UpdateMcpToolMutation = { updateMcpTool: { id: string, name: string, mcpComponentId: string, parameters: any, version: number | null } | null };

export type UpdateMcpToolEnabledMutationVariables = Exact<{
  id: string | number;
  enabled: boolean;
}>;


export type UpdateMcpToolEnabledMutation = { updateMcpToolEnabled: { id: string, enabled: boolean } | null };

export type ValidateWorkflowQueryVariables = Exact<{
  workflowDefinition: string;
}>;


export type ValidateWorkflowQuery = { validateWorkflow: { errors: Array<string>, warnings: Array<string> } };

export type ValidateWorkflowByIdQueryVariables = Exact<{
  workflowId: string;
}>;


export type ValidateWorkflowByIdQuery = { validateWorkflowById: { errors: Array<string>, warnings: Array<string> } };

export type WorkflowNodeComponentConnectionsQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
}>;


export type WorkflowNodeComponentConnectionsQuery = { workflowNodeComponentConnections: Array<{ componentName: string, componentVersion: number, key: string, required: boolean, workflowNodeName: string }> };

export type WorkflowNodeMissingRequiredPropertiesQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
}>;


export type WorkflowNodeMissingRequiredPropertiesQuery = { workflowNodeMissingRequiredProperties: Array<string> };

export type WorkflowNodeScriptInputQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  environmentId: any;
}>;


export type WorkflowNodeScriptInputQuery = { workflowNodeScriptInput: any };

export type ConnectionCredentialStoresQueryVariables = Exact<{ [key: string]: never; }>;


export type ConnectionCredentialStoresQuery = { connectionCredentialStores: Array<{ type: Types.ConnectionCredentialStoreType, readOnly: boolean }> };

export type RegisterExistingConnectionMutationVariables = Exact<{
  input: Types.RegisterExistingConnectionInput;
}>;


export type RegisterExistingConnectionMutation = { registerExistingConnection: any };

export type GeneratePropertyValueMutationVariables = Exact<{
  input: Types.GeneratePropertyValueInput;
}>;


export type GeneratePropertyValueMutation = { generatePropertyValue: { value: string, valid: boolean, message: string | null } };

export type GenerateWorkflowDescriptionMutationVariables = Exact<{
  input: Types.GenerateWorkflowDescriptionInput;
}>;


export type GenerateWorkflowDescriptionMutation = { generateWorkflowDescription: { value: string } };

export type CreateCustomComponentMutationVariables = Exact<{
  name: string;
  language: Types.CustomComponentLanguage;
}>;


export type CreateCustomComponentMutation = { createCustomComponent: { id: string, name: string, language: Types.CustomComponentLanguage | null } };

export type CustomComponentQueryVariables = Exact<{
  id: string | number;
}>;


export type CustomComponentQuery = { customComponent: { id: string, name: string, title: string | null, description: string | null, icon: string | null, componentVersion: number | null, enabled: boolean | null, language: Types.CustomComponentLanguage | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, status: Types.CustomComponentStatus | null, publishedDate: any, version: number | null } | null };

export type CustomComponentDefinitionQueryVariables = Exact<{
  id: string | number;
}>;


export type CustomComponentDefinitionQuery = { customComponentDefinition: { actions: Array<{ name: string, title: string | null, description: string | null }>, triggers: Array<{ name: string, title: string | null, description: string | null }> } | null };

export type CustomComponentSourceQueryVariables = Exact<{
  id: string | number;
}>;


export type CustomComponentSourceQuery = { customComponentSource: string };

export type CustomComponentsQueryVariables = Exact<{ [key: string]: never; }>;


export type CustomComponentsQuery = { customComponents: Array<{ id: string, name: string, title: string | null, description: string | null, icon: string | null, componentVersion: number | null, enabled: boolean | null, language: Types.CustomComponentLanguage | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, status: Types.CustomComponentStatus | null, publishedDate: any, version: number | null }> };

export type DeleteCustomComponentMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteCustomComponentMutation = { deleteCustomComponent: boolean };

export type EnableCustomComponentMutationVariables = Exact<{
  id: string | number;
  enable: boolean;
}>;


export type EnableCustomComponentMutation = { enableCustomComponent: boolean };

export type PublishCustomComponentMutationVariables = Exact<{
  id: string | number;
}>;


export type PublishCustomComponentMutation = { publishCustomComponent: { id: string, publishedDate: any, status: Types.CustomComponentStatus | null } };

export type UpdateCustomComponentSourceMutationVariables = Exact<{
  id: string | number;
  content: string;
}>;


export type UpdateCustomComponentSourceMutation = { updateCustomComponentSource: { id: string, componentVersion: number | null, status: Types.CustomComponentStatus | null } };

export type DeleteLicenceMutationVariables = Exact<{ [key: string]: never; }>;


export type DeleteLicenceMutation = { deleteLicence: boolean };

export type LicenceQueryVariables = Exact<{ [key: string]: never; }>;


export type LicenceQuery = { licence: { allowedJobs: any, currentMonthJobUsage: any, expiresAt: string | null, features: Array<string>, holderEmail: string | null, holderName: string | null, id: string | null, issuedAt: string | null, maxUsers: number | null, status: string } | null };

export type UploadLicenceMutationVariables = Exact<{
  contents: string;
}>;


export type UploadLicenceMutation = { uploadLicence: { allowedJobs: any, currentMonthJobUsage: any, expiresAt: string | null, features: Array<string>, holderEmail: string | null, holderName: string | null, id: string | null, issuedAt: string | null, maxUsers: number | null, status: string } };

export type DeleteRegisteredClientMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteRegisteredClientMutation = { deleteRegisteredClient: boolean };

export type RegisteredClientsQueryVariables = Exact<{ [key: string]: never; }>;


export type RegisteredClientsQuery = { registeredClients: Array<{ id: string | null, clientId: string | null, clientName: string | null, clientIdIssuedAt: any, scopes: Array<string | null> | null, authorizationGrantTypes: Array<string | null> | null, redirectUris: Array<string | null> | null } | null> | null };

export type AuthoritiesQueryVariables = Exact<{ [key: string]: never; }>;


export type AuthoritiesQuery = { authorities: Array<string> };

export type CreateIdentityProviderMutationVariables = Exact<{
  input: Types.IdentityProviderInput;
}>;


export type CreateIdentityProviderMutation = { createIdentityProvider: { authoritiesClaim: string | null, autoProvision: boolean, clientId: string | null, createdBy: string | null, createdDate: any, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string | null, lastModifiedBy: string | null, lastModifiedDate: any, mcpEmbedded: boolean, mcpAutomation: boolean, mcpManagement: boolean, metadataUri: string | null, mfaMethod: string | null, mfaRequired: boolean, name: string, nameIdFormat: string | null, scopes: string | null, signingCertificate: string | null, type: string, validateMcpAudience: boolean, authorityMappings: Array<{ authority: string, externalGroup: string }> } };

export type DeleteIdentityProviderMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteIdentityProviderMutation = { deleteIdentityProvider: boolean };

export type DeleteUserMutationVariables = Exact<{
  login: string;
}>;


export type DeleteUserMutation = { deleteUser: boolean };

export type IdentityProviderQueryVariables = Exact<{
  id: string | number;
}>;


export type IdentityProviderQuery = { identityProvider: { authoritiesClaim: string | null, autoProvision: boolean, clientId: string | null, createdBy: string | null, createdDate: any, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string | null, lastModifiedBy: string | null, lastModifiedDate: any, mcpEmbedded: boolean, mcpAutomation: boolean, mcpManagement: boolean, metadataUri: string | null, mfaMethod: string | null, mfaRequired: boolean, name: string, nameIdFormat: string | null, scopes: string | null, signingCertificate: string | null, type: string, validateMcpAudience: boolean, authorityMappings: Array<{ authority: string, externalGroup: string }> } | null };

export type IdentityProvidersQueryVariables = Exact<{ [key: string]: never; }>;


export type IdentityProvidersQuery = { identityProviders: Array<{ authoritiesClaim: string | null, autoProvision: boolean, clientId: string | null, createdBy: string | null, createdDate: any, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string | null, lastModifiedBy: string | null, lastModifiedDate: any, mcpEmbedded: boolean, mcpAutomation: boolean, mcpManagement: boolean, metadataUri: string | null, mfaMethod: string | null, mfaRequired: boolean, name: string, nameIdFormat: string | null, scopes: string | null, signingCertificate: string | null, type: string, validateMcpAudience: boolean, authorityMappings: Array<{ authority: string, externalGroup: string }> } | null> };

export type InviteUserMutationVariables = Exact<{
  email: string;
  role: string;
  workspaces?: Array<Types.WorkspaceAssignmentInput> | Types.WorkspaceAssignmentInput | null | undefined;
}>;


export type InviteUserMutation = { inviteUser: boolean };

export type UpdateIdentityProviderMutationVariables = Exact<{
  id: string | number;
  input: Types.IdentityProviderInput;
}>;


export type UpdateIdentityProviderMutation = { updateIdentityProvider: { authoritiesClaim: string | null, autoProvision: boolean, clientId: string | null, createdBy: string | null, createdDate: any, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string | null, lastModifiedBy: string | null, lastModifiedDate: any, mcpEmbedded: boolean, mcpAutomation: boolean, mcpManagement: boolean, metadataUri: string | null, mfaMethod: string | null, mfaRequired: boolean, name: string, nameIdFormat: string | null, scopes: string | null, signingCertificate: string | null, type: string, validateMcpAudience: boolean, authorityMappings: Array<{ authority: string, externalGroup: string }> } };

export type UpdateUserMutationVariables = Exact<{
  login: string;
  role: string;
}>;


export type UpdateUserMutation = { updateUser: { id: string | null, login: string | null, email: string | null, firstName: string | null, lastName: string | null, activated: boolean | null, authorities: Array<string | null> | null } };

export type UsersQueryVariables = Exact<{
  pageNumber?: number | null | undefined;
  pageSize?: number | null | undefined;
}>;


export type UsersQuery = { users: { number: number, size: number, totalElements: number, totalPages: number, content: Array<{ id: string | null, login: string | null, email: string | null, firstName: string | null, lastName: string | null, activated: boolean | null, authorities: Array<string | null> | null } | null> } | null };

export type CreateEmbeddedVariableMutationVariables = Exact<{
  environmentId: string | number;
  input: Types.VariableInput;
}>;


export type CreateEmbeddedVariableMutation = { createEmbeddedVariable: { id: string, name: string, value: string } };

export type CreateWorkspaceVariableMutationVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
  input: Types.VariableInput;
}>;


export type CreateWorkspaceVariableMutation = { createWorkspaceVariable: { id: string, name: string, value: string } };

export type DeleteEmbeddedVariableMutationVariables = Exact<{
  environmentId: string | number;
  id: string | number;
}>;


export type DeleteEmbeddedVariableMutation = { deleteEmbeddedVariable: boolean };

export type DeleteWorkspaceVariableMutationVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
  id: string | number;
}>;


export type DeleteWorkspaceVariableMutation = { deleteWorkspaceVariable: boolean };

export type EmbeddedVariablesQueryVariables = Exact<{
  environmentId: string | number;
}>;


export type EmbeddedVariablesQuery = { embeddedVariables: Array<{ id: string, name: string, value: string, environmentId: string, createdBy: string | null, createdDate: string | null, lastModifiedBy: string | null, lastModifiedDate: string | null }> };

export type UpdateEmbeddedVariableMutationVariables = Exact<{
  environmentId: string | number;
  id: string | number;
  input: Types.VariableInput;
}>;


export type UpdateEmbeddedVariableMutation = { updateEmbeddedVariable: { id: string, name: string, value: string } };

export type UpdateWorkspaceVariableMutationVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
  id: string | number;
  input: Types.VariableInput;
}>;


export type UpdateWorkspaceVariableMutation = { updateWorkspaceVariable: { id: string, name: string, value: string } };

export type WorkspaceVariablesQueryVariables = Exact<{
  workspaceId: string | number;
  environmentId: string | number;
}>;


export type WorkspaceVariablesQuery = { workspaceVariables: Array<{ id: string, name: string, value: string, environmentId: string, createdBy: string | null, createdDate: string | null, lastModifiedBy: string | null, lastModifiedDate: string | null }> };

export type WorkspaceNotificationsQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceNotificationsQuery = { workspaceNotifications: Array<{ id: string, name: string, type: string }> };

export type WorkspaceSystemPromptQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceSystemPromptQuery = { workspaceSystemPrompt: { prompt: string, workspaceId: string } | null };

export type UpdateWorkspaceSystemPromptMutationVariables = Exact<{
  input: Types.WorkspaceSystemPromptInput;
}>;


export type UpdateWorkspaceSystemPromptMutation = { updateWorkspaceSystemPrompt: { prompt: string, workspaceId: string } | null };

export type ToolInvocationLogsQueryVariables = Exact<{
  surface?: string | null | undefined;
  outcome?: string | null | undefined;
  mcpServerId?: any;
  connectedUserId?: any;
  integrationInstanceId?: any;
  fromDate?: any;
  toDate?: any;
  page?: number | null | undefined;
}>;


export type ToolInvocationLogsQuery = { toolInvocationLogs: { number: number, size: number, totalElements: number, totalPages: number, content: Array<{ id: string, surface: string, kind: string, toolName: string | null, componentName: string | null, componentVersion: number | null, operationName: string | null, connectionId: any, environment: number | null, externalUserId: string | null, connectedUserId: any, integrationInstanceId: any, mcpServerId: any, jobId: any, outcome: string, errorType: string | null, errorMessage: string | null, durationMs: number, createdDate: any }> } };


export class TypedDocumentString<TResult, TVariables>
  extends String
  implements DocumentTypeDecoration<TResult, TVariables>
{
  __apiType?: NonNullable<DocumentTypeDecoration<TResult, TVariables>['__apiType']>;
  private value: string;
  public __meta__?: Record<string, any> | undefined;

  constructor(value: string, __meta__?: Record<string, any> | undefined) {
    super(value);
    this.value = value;
    this.__meta__ = __meta__;
  }

  override toString(): string & DocumentTypeDecoration<TResult, TVariables> {
    return this.value;
  }
}

export const AiAgentEvalResultDocument = new TypedDocumentString(`
    query aiAgentEvalResult($id: ID!) {
  aiAgentEvalResult(id: $id) {
    id
    scenario {
      id
      name
      type
      userMessage
      expectedOutput
      personaPrompt
      maxTurns
      judges {
        id
        name
        type
        configuration
        createdDate
        lastModifiedDate
      }
      createdDate
      lastModifiedDate
    }
    status
    score
    errorMessage
    transcriptFile
    verdicts {
      id
      judgeName
      judgeType
      judgeScope
      passed
      score
      explanation
    }
    createdDate
  }
}
    `);

export const useAiAgentEvalResultQuery = <
      TData = AiAgentEvalResultQuery,
      TError = unknown
    >(
      variables: AiAgentEvalResultQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalResultQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalResultQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalResultQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalResult', variables],
    queryFn: fetcher<AiAgentEvalResultQuery, AiAgentEvalResultQueryVariables>(AiAgentEvalResultDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalResultTranscriptDocument = new TypedDocumentString(`
    query aiAgentEvalResultTranscript($id: ID!) {
  aiAgentEvalResultTranscript(id: $id)
}
    `);

export const useAiAgentEvalResultTranscriptQuery = <
      TData = AiAgentEvalResultTranscriptQuery,
      TError = unknown
    >(
      variables: AiAgentEvalResultTranscriptQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalResultTranscriptQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalResultTranscriptQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalResultTranscriptQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalResultTranscript', variables],
    queryFn: fetcher<AiAgentEvalResultTranscriptQuery, AiAgentEvalResultTranscriptQueryVariables>(AiAgentEvalResultTranscriptDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalRunDocument = new TypedDocumentString(`
    query aiAgentEvalRun($id: ID!) {
  aiAgentEvalRun(id: $id) {
    id
    name
    status
    averageScore
    totalScenarios
    completedScenarios
    agentVersion
    totalInputTokens
    totalOutputTokens
    startedDate
    completedDate
    results {
      id
      scenario {
        id
        name
        type
        userMessage
        expectedOutput
        personaPrompt
        maxTurns
        judges {
          id
          name
          type
          configuration
          createdDate
          lastModifiedDate
        }
        createdDate
        lastModifiedDate
      }
      status
      score
      errorMessage
      transcriptFile
      inputTokens
      outputTokens
      runIndex
      verdicts {
        id
        judgeName
        judgeType
        judgeScope
        passed
        score
        explanation
      }
      createdDate
    }
    createdDate
  }
}
    `);

export const useAiAgentEvalRunQuery = <
      TData = AiAgentEvalRunQuery,
      TError = unknown
    >(
      variables: AiAgentEvalRunQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalRunQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalRunQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalRunQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalRun', variables],
    queryFn: fetcher<AiAgentEvalRunQuery, AiAgentEvalRunQueryVariables>(AiAgentEvalRunDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalRunsDocument = new TypedDocumentString(`
    query aiAgentEvalRuns($agentEvalTestId: ID!, $limit: Int, $offset: Int) {
  aiAgentEvalRuns(
    agentEvalTestId: $agentEvalTestId
    limit: $limit
    offset: $offset
  ) {
    id
    name
    status
    averageScore
    totalScenarios
    completedScenarios
    startedDate
    completedDate
    createdDate
  }
}
    `);

export const useAiAgentEvalRunsQuery = <
      TData = AiAgentEvalRunsQuery,
      TError = unknown
    >(
      variables: AiAgentEvalRunsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalRunsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalRunsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalRunsQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalRuns', variables],
    queryFn: fetcher<AiAgentEvalRunsQuery, AiAgentEvalRunsQueryVariables>(AiAgentEvalRunsDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalTestDocument = new TypedDocumentString(`
    query aiAgentEvalTest($id: ID!) {
  aiAgentEvalTest(id: $id) {
    id
    name
    description
    scenarios {
      id
      name
      type
      userMessage
      expectedOutput
      personaPrompt
      maxTurns
      numberOfRuns
      judges {
        id
        name
        type
        configuration
        createdDate
        lastModifiedDate
      }
      toolSimulations {
        id
        responsePrompt
        simulationModel
        toolName
        createdDate
        lastModifiedDate
      }
      createdDate
      lastModifiedDate
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAiAgentEvalTestQuery = <
      TData = AiAgentEvalTestQuery,
      TError = unknown
    >(
      variables: AiAgentEvalTestQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalTestQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalTestQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalTestQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalTest', variables],
    queryFn: fetcher<AiAgentEvalTestQuery, AiAgentEvalTestQueryVariables>(AiAgentEvalTestDocument, variables),
    ...options
  }
    )};

export const AiAgentEvalTestsDocument = new TypedDocumentString(`
    query aiAgentEvalTests($workflowId: String!, $workflowNodeName: String!) {
  aiAgentEvalTests(workflowId: $workflowId, workflowNodeName: $workflowNodeName) {
    id
    name
    description
    scenarios {
      id
      name
      type
      userMessage
      expectedOutput
      personaPrompt
      maxTurns
      numberOfRuns
      toolSimulations {
        id
        toolName
        responsePrompt
        simulationModel
      }
      judges {
        id
        name
        type
        configuration
        createdDate
        lastModifiedDate
      }
      createdDate
      lastModifiedDate
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAiAgentEvalTestsQuery = <
      TData = AiAgentEvalTestsQuery,
      TError = unknown
    >(
      variables: AiAgentEvalTestsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentEvalTestsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentEvalTestsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentEvalTestsQuery, TError, TData>(
      {
    queryKey: ['aiAgentEvalTests', variables],
    queryFn: fetcher<AiAgentEvalTestsQuery, AiAgentEvalTestsQueryVariables>(AiAgentEvalTestsDocument, variables),
    ...options
  }
    )};

export const AiAgentJudgesDocument = new TypedDocumentString(`
    query aiAgentJudges($workflowId: String!, $workflowNodeName: String!) {
  aiAgentJudges(workflowId: $workflowId, workflowNodeName: $workflowNodeName) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAiAgentJudgesQuery = <
      TData = AiAgentJudgesQuery,
      TError = unknown
    >(
      variables: AiAgentJudgesQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentJudgesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentJudgesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentJudgesQuery, TError, TData>(
      {
    queryKey: ['aiAgentJudges', variables],
    queryFn: fetcher<AiAgentJudgesQuery, AiAgentJudgesQueryVariables>(AiAgentJudgesDocument, variables),
    ...options
  }
    )};

export const CancelAiAgentEvalRunDocument = new TypedDocumentString(`
    mutation cancelAiAgentEvalRun($id: ID!) {
  cancelAiAgentEvalRun(id: $id) {
    id
    status
  }
}
    `);

export const useCancelAiAgentEvalRunMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelAiAgentEvalRunMutation, TError, CancelAiAgentEvalRunMutationVariables, TContext>) => {
    
    return useMutation<CancelAiAgentEvalRunMutation, TError, CancelAiAgentEvalRunMutationVariables, TContext>(
      {
    mutationKey: ['cancelAiAgentEvalRun'],
    mutationFn: (variables?: CancelAiAgentEvalRunMutationVariables) => fetcher<CancelAiAgentEvalRunMutation, CancelAiAgentEvalRunMutationVariables>(CancelAiAgentEvalRunDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation createAiAgentEvalScenario($agentEvalTestId: ID!, $name: String!, $type: AiAgentScenarioType!, $userMessage: String, $expectedOutput: String, $personaPrompt: String, $maxTurns: Int, $numberOfRuns: Int) {
  createAiAgentEvalScenario(
    agentEvalTestId: $agentEvalTestId
    name: $name
    type: $type
    userMessage: $userMessage
    expectedOutput: $expectedOutput
    personaPrompt: $personaPrompt
    maxTurns: $maxTurns
    numberOfRuns: $numberOfRuns
  ) {
    id
    name
    type
    userMessage
    expectedOutput
    personaPrompt
    maxTurns
    numberOfRuns
    judges {
      id
      name
      type
      configuration
      createdDate
      lastModifiedDate
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAiAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentEvalScenarioMutation, TError, CreateAiAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentEvalScenarioMutation, TError, CreateAiAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentEvalScenario'],
    mutationFn: (variables?: CreateAiAgentEvalScenarioMutationVariables) => fetcher<CreateAiAgentEvalScenarioMutation, CreateAiAgentEvalScenarioMutationVariables>(CreateAiAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentEvalTestDocument = new TypedDocumentString(`
    mutation createAiAgentEvalTest($workflowId: String!, $workflowNodeName: String!, $name: String!, $description: String) {
  createAiAgentEvalTest(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    name: $name
    description: $description
  ) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAiAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentEvalTestMutation, TError, CreateAiAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentEvalTestMutation, TError, CreateAiAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentEvalTest'],
    mutationFn: (variables?: CreateAiAgentEvalTestMutationVariables) => fetcher<CreateAiAgentEvalTestMutation, CreateAiAgentEvalTestMutationVariables>(CreateAiAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentJudgeDocument = new TypedDocumentString(`
    mutation createAiAgentJudge($workflowId: String!, $workflowNodeName: String!, $name: String!, $type: AiAgentJudgeType!, $configuration: Map!) {
  createAiAgentJudge(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    name: $name
    type: $type
    configuration: $configuration
  ) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAiAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentJudgeMutation, TError, CreateAiAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentJudgeMutation, TError, CreateAiAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentJudge'],
    mutationFn: (variables?: CreateAiAgentJudgeMutationVariables) => fetcher<CreateAiAgentJudgeMutation, CreateAiAgentJudgeMutationVariables>(CreateAiAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation createAiAgentScenarioJudge($agentEvalScenarioId: ID!, $name: String!, $type: AiAgentJudgeType!, $configuration: Map!) {
  createAiAgentScenarioJudge(
    agentEvalScenarioId: $agentEvalScenarioId
    name: $name
    type: $type
    configuration: $configuration
  ) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAiAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentScenarioJudgeMutation, TError, CreateAiAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentScenarioJudgeMutation, TError, CreateAiAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentScenarioJudge'],
    mutationFn: (variables?: CreateAiAgentScenarioJudgeMutationVariables) => fetcher<CreateAiAgentScenarioJudgeMutation, CreateAiAgentScenarioJudgeMutationVariables>(CreateAiAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const CreateAiAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation createAiAgentScenarioToolSimulation($agentEvalScenarioId: ID!, $toolName: String!, $responsePrompt: String!, $simulationModel: String) {
  createAiAgentScenarioToolSimulation(
    agentEvalScenarioId: $agentEvalScenarioId
    toolName: $toolName
    responsePrompt: $responsePrompt
    simulationModel: $simulationModel
  ) {
    id
    toolName
    responsePrompt
    simulationModel
  }
}
    `);

export const useCreateAiAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentScenarioToolSimulationMutation, TError, CreateAiAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentScenarioToolSimulationMutation, TError, CreateAiAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgentScenarioToolSimulation'],
    mutationFn: (variables?: CreateAiAgentScenarioToolSimulationMutationVariables) => fetcher<CreateAiAgentScenarioToolSimulationMutation, CreateAiAgentScenarioToolSimulationMutationVariables>(CreateAiAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation deleteAiAgentEvalScenario($id: ID!) {
  deleteAiAgentEvalScenario(id: $id)
}
    `);

export const useDeleteAiAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentEvalScenarioMutation, TError, DeleteAiAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentEvalScenarioMutation, TError, DeleteAiAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentEvalScenario'],
    mutationFn: (variables?: DeleteAiAgentEvalScenarioMutationVariables) => fetcher<DeleteAiAgentEvalScenarioMutation, DeleteAiAgentEvalScenarioMutationVariables>(DeleteAiAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentEvalTestDocument = new TypedDocumentString(`
    mutation deleteAiAgentEvalTest($id: ID!) {
  deleteAiAgentEvalTest(id: $id)
}
    `);

export const useDeleteAiAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentEvalTestMutation, TError, DeleteAiAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentEvalTestMutation, TError, DeleteAiAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentEvalTest'],
    mutationFn: (variables?: DeleteAiAgentEvalTestMutationVariables) => fetcher<DeleteAiAgentEvalTestMutation, DeleteAiAgentEvalTestMutationVariables>(DeleteAiAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentJudgeDocument = new TypedDocumentString(`
    mutation deleteAiAgentJudge($id: ID!) {
  deleteAiAgentJudge(id: $id)
}
    `);

export const useDeleteAiAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentJudgeMutation, TError, DeleteAiAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentJudgeMutation, TError, DeleteAiAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentJudge'],
    mutationFn: (variables?: DeleteAiAgentJudgeMutationVariables) => fetcher<DeleteAiAgentJudgeMutation, DeleteAiAgentJudgeMutationVariables>(DeleteAiAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation deleteAiAgentScenarioJudge($id: ID!) {
  deleteAiAgentScenarioJudge(id: $id)
}
    `);

export const useDeleteAiAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentScenarioJudgeMutation, TError, DeleteAiAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentScenarioJudgeMutation, TError, DeleteAiAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentScenarioJudge'],
    mutationFn: (variables?: DeleteAiAgentScenarioJudgeMutationVariables) => fetcher<DeleteAiAgentScenarioJudgeMutation, DeleteAiAgentScenarioJudgeMutationVariables>(DeleteAiAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation deleteAiAgentScenarioToolSimulation($id: ID!) {
  deleteAiAgentScenarioToolSimulation(id: $id)
}
    `);

export const useDeleteAiAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentScenarioToolSimulationMutation, TError, DeleteAiAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentScenarioToolSimulationMutation, TError, DeleteAiAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentScenarioToolSimulation'],
    mutationFn: (variables?: DeleteAiAgentScenarioToolSimulationMutationVariables) => fetcher<DeleteAiAgentScenarioToolSimulationMutation, DeleteAiAgentScenarioToolSimulationMutationVariables>(DeleteAiAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const StartAiAgentEvalRunDocument = new TypedDocumentString(`
    mutation startAiAgentEvalRun($agentEvalTestId: ID!, $name: String!, $environmentId: ID!, $scenarioIds: [ID!], $aiAgentJudgeIds: [ID!]) {
  startAiAgentEvalRun(
    agentEvalTestId: $agentEvalTestId
    name: $name
    environmentId: $environmentId
    scenarioIds: $scenarioIds
    aiAgentJudgeIds: $aiAgentJudgeIds
  ) {
    id
    name
    status
    totalScenarios
    completedScenarios
    agentVersion
    createdDate
  }
}
    `);

export const useStartAiAgentEvalRunMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<StartAiAgentEvalRunMutation, TError, StartAiAgentEvalRunMutationVariables, TContext>) => {
    
    return useMutation<StartAiAgentEvalRunMutation, TError, StartAiAgentEvalRunMutationVariables, TContext>(
      {
    mutationKey: ['startAiAgentEvalRun'],
    mutationFn: (variables?: StartAiAgentEvalRunMutationVariables) => fetcher<StartAiAgentEvalRunMutation, StartAiAgentEvalRunMutationVariables>(StartAiAgentEvalRunDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentEvalScenarioDocument = new TypedDocumentString(`
    mutation updateAiAgentEvalScenario($id: ID!, $name: String, $userMessage: String, $expectedOutput: String, $personaPrompt: String, $maxTurns: Int, $numberOfRuns: Int) {
  updateAiAgentEvalScenario(
    id: $id
    name: $name
    userMessage: $userMessage
    expectedOutput: $expectedOutput
    personaPrompt: $personaPrompt
    maxTurns: $maxTurns
    numberOfRuns: $numberOfRuns
  ) {
    id
    name
    type
    userMessage
    expectedOutput
    personaPrompt
    maxTurns
    numberOfRuns
    judges {
      id
      name
      type
      configuration
      createdDate
      lastModifiedDate
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiAgentEvalScenarioMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentEvalScenarioMutation, TError, UpdateAiAgentEvalScenarioMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentEvalScenarioMutation, TError, UpdateAiAgentEvalScenarioMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentEvalScenario'],
    mutationFn: (variables?: UpdateAiAgentEvalScenarioMutationVariables) => fetcher<UpdateAiAgentEvalScenarioMutation, UpdateAiAgentEvalScenarioMutationVariables>(UpdateAiAgentEvalScenarioDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentEvalTestDocument = new TypedDocumentString(`
    mutation updateAiAgentEvalTest($id: ID!, $name: String, $description: String) {
  updateAiAgentEvalTest(id: $id, name: $name, description: $description) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiAgentEvalTestMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentEvalTestMutation, TError, UpdateAiAgentEvalTestMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentEvalTestMutation, TError, UpdateAiAgentEvalTestMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentEvalTest'],
    mutationFn: (variables?: UpdateAiAgentEvalTestMutationVariables) => fetcher<UpdateAiAgentEvalTestMutation, UpdateAiAgentEvalTestMutationVariables>(UpdateAiAgentEvalTestDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentJudgeDocument = new TypedDocumentString(`
    mutation updateAiAgentJudge($id: ID!, $name: String, $configuration: Map) {
  updateAiAgentJudge(id: $id, name: $name, configuration: $configuration) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiAgentJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentJudgeMutation, TError, UpdateAiAgentJudgeMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentJudgeMutation, TError, UpdateAiAgentJudgeMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentJudge'],
    mutationFn: (variables?: UpdateAiAgentJudgeMutationVariables) => fetcher<UpdateAiAgentJudgeMutation, UpdateAiAgentJudgeMutationVariables>(UpdateAiAgentJudgeDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentScenarioJudgeDocument = new TypedDocumentString(`
    mutation updateAiAgentScenarioJudge($id: ID!, $name: String, $configuration: Map) {
  updateAiAgentScenarioJudge(id: $id, name: $name, configuration: $configuration) {
    id
    name
    type
    configuration
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiAgentScenarioJudgeMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentScenarioJudgeMutation, TError, UpdateAiAgentScenarioJudgeMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentScenarioJudgeMutation, TError, UpdateAiAgentScenarioJudgeMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentScenarioJudge'],
    mutationFn: (variables?: UpdateAiAgentScenarioJudgeMutationVariables) => fetcher<UpdateAiAgentScenarioJudgeMutation, UpdateAiAgentScenarioJudgeMutationVariables>(UpdateAiAgentScenarioJudgeDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentScenarioToolSimulationDocument = new TypedDocumentString(`
    mutation updateAiAgentScenarioToolSimulation($id: ID!, $toolName: String, $responsePrompt: String, $simulationModel: String) {
  updateAiAgentScenarioToolSimulation(
    id: $id
    toolName: $toolName
    responsePrompt: $responsePrompt
    simulationModel: $simulationModel
  ) {
    id
    toolName
    responsePrompt
    simulationModel
  }
}
    `);

export const useUpdateAiAgentScenarioToolSimulationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentScenarioToolSimulationMutation, TError, UpdateAiAgentScenarioToolSimulationMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentScenarioToolSimulationMutation, TError, UpdateAiAgentScenarioToolSimulationMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentScenarioToolSimulation'],
    mutationFn: (variables?: UpdateAiAgentScenarioToolSimulationMutationVariables) => fetcher<UpdateAiAgentScenarioToolSimulationMutation, UpdateAiAgentScenarioToolSimulationMutationVariables>(UpdateAiAgentScenarioToolSimulationDocument, variables)(),
    ...options
  }
    )};

export const AiHubChatArtifactsDocument = new TypedDocumentString(`
    query aiHubChatArtifacts($workspaceId: ID!, $environment: Int, $userId: ID, $kind: AiHubChatArtifactKind, $from: Long, $to: Long, $page: Int, $size: Int) {
  aiHubChatArtifacts(
    workspaceId: $workspaceId
    environment: $environment
    userId: $userId
    kind: $kind
    from: $from
    to: $to
    page: $page
    size: $size
  ) {
    items {
      id
      chatId
      kind
      status
      artifactId
      artifactName
      metadataJson
      environmentId
      createdAt
      statusChangedAt
    }
    totalCount
    hasMore
    pageClamped
    sizeClamped
  }
}
    `);

export const useAiHubChatArtifactsQuery = <
      TData = AiHubChatArtifactsQuery,
      TError = unknown
    >(
      variables: AiHubChatArtifactsQueryVariables,
      options?: Omit<UseQueryOptions<AiHubChatArtifactsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubChatArtifactsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubChatArtifactsQuery, TError, TData>(
      {
    queryKey: ['aiHubChatArtifacts', variables],
    queryFn: fetcher<AiHubChatArtifactsQuery, AiHubChatArtifactsQueryVariables>(AiHubChatArtifactsDocument, variables),
    ...options
  }
    )};

export const DeleteAiHubChatArtifactDocument = new TypedDocumentString(`
    mutation deleteAiHubChatArtifact($input: DeleteAiHubChatArtifactInput!) {
  deleteAiHubChatArtifact(input: $input)
}
    `);

export const useDeleteAiHubChatArtifactMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiHubChatArtifactMutation, TError, DeleteAiHubChatArtifactMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiHubChatArtifactMutation, TError, DeleteAiHubChatArtifactMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiHubChatArtifact'],
    mutationFn: (variables?: DeleteAiHubChatArtifactMutationVariables) => fetcher<DeleteAiHubChatArtifactMutation, DeleteAiHubChatArtifactMutationVariables>(DeleteAiHubChatArtifactDocument, variables)(),
    ...options
  }
    )};

export const RecordReferencedAiHubChatArtifactDocument = new TypedDocumentString(`
    mutation recordReferencedAiHubChatArtifact($input: RecordReferencedAiHubChatArtifactInput!) {
  recordReferencedAiHubChatArtifact(input: $input) {
    id
    chatId
    kind
    status
    artifactId
    artifactName
    environmentId
    createdAt
  }
}
    `);

export const useRecordReferencedAiHubChatArtifactMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RecordReferencedAiHubChatArtifactMutation, TError, RecordReferencedAiHubChatArtifactMutationVariables, TContext>) => {
    
    return useMutation<RecordReferencedAiHubChatArtifactMutation, TError, RecordReferencedAiHubChatArtifactMutationVariables, TContext>(
      {
    mutationKey: ['recordReferencedAiHubChatArtifact'],
    mutationFn: (variables?: RecordReferencedAiHubChatArtifactMutationVariables) => fetcher<RecordReferencedAiHubChatArtifactMutation, RecordReferencedAiHubChatArtifactMutationVariables>(RecordReferencedAiHubChatArtifactDocument, variables)(),
    ...options
  }
    )};

export const AddAiHubUserConnectorDocument = new TypedDocumentString(`
    mutation addAiHubUserConnector($workspaceId: ID!, $componentName: String!, $componentVersion: Int!, $connectionId: ID, $environment: Int!) {
  addAiHubUserConnector(
    workspaceId: $workspaceId
    componentName: $componentName
    componentVersion: $componentVersion
    connectionId: $connectionId
    environment: $environment
  )
}
    `);

export const useAddAiHubUserConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddAiHubUserConnectorMutation, TError, AddAiHubUserConnectorMutationVariables, TContext>) => {
    
    return useMutation<AddAiHubUserConnectorMutation, TError, AddAiHubUserConnectorMutationVariables, TContext>(
      {
    mutationKey: ['addAiHubUserConnector'],
    mutationFn: (variables?: AddAiHubUserConnectorMutationVariables) => fetcher<AddAiHubUserConnectorMutation, AddAiHubUserConnectorMutationVariables>(AddAiHubUserConnectorDocument, variables)(),
    ...options
  }
    )};

export const AiHubChatToolableComponentsDocument = new TypedDocumentString(`
    query aiHubChatToolableComponents($workspaceId: ID!) {
  aiHubChatToolableComponents(workspaceId: $workspaceId) {
    componentName
    componentVersion
    connectionRequired
    description
    icon
    title
    tools {
      description
      name
      title
    }
  }
}
    `);

export const useAiHubChatToolableComponentsQuery = <
      TData = AiHubChatToolableComponentsQuery,
      TError = unknown
    >(
      variables: AiHubChatToolableComponentsQueryVariables,
      options?: Omit<UseQueryOptions<AiHubChatToolableComponentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubChatToolableComponentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubChatToolableComponentsQuery, TError, TData>(
      {
    queryKey: ['aiHubChatToolableComponents', variables],
    queryFn: fetcher<AiHubChatToolableComponentsQuery, AiHubChatToolableComponentsQueryVariables>(AiHubChatToolableComponentsDocument, variables),
    ...options
  }
    )};

export const AiHubChatToolsDocument = new TypedDocumentString(`
    query aiHubChatTools($workspaceId: ID!, $chatId: ID!) {
  aiHubChatTools(workspaceId: $workspaceId, chatId: $chatId) {
    clusterElementName
    componentName
    componentVersion
    connectionId
    chatComponentId
    chatId
    chatToolId
    environment
    parameters
  }
}
    `);

export const useAiHubChatToolsQuery = <
      TData = AiHubChatToolsQuery,
      TError = unknown
    >(
      variables: AiHubChatToolsQueryVariables,
      options?: Omit<UseQueryOptions<AiHubChatToolsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubChatToolsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubChatToolsQuery, TError, TData>(
      {
    queryKey: ['aiHubChatTools', variables],
    queryFn: fetcher<AiHubChatToolsQuery, AiHubChatToolsQueryVariables>(AiHubChatToolsDocument, variables),
    ...options
  }
    )};

export const AiHubUserConnectorsDocument = new TypedDocumentString(`
    query aiHubUserConnectors($workspaceId: ID!, $chatId: ID) {
  aiHubUserConnectors(workspaceId: $workspaceId, chatId: $chatId) {
    componentName
    componentVersion
    connectionId
    connectionRequired
    description
    enabled
    enabledInChat
    icon
    id
    title
    tools {
      description
      enabled
      name
      parameters
      title
    }
  }
}
    `);

export const useAiHubUserConnectorsQuery = <
      TData = AiHubUserConnectorsQuery,
      TError = unknown
    >(
      variables: AiHubUserConnectorsQueryVariables,
      options?: Omit<UseQueryOptions<AiHubUserConnectorsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubUserConnectorsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubUserConnectorsQuery, TError, TData>(
      {
    queryKey: ['aiHubUserConnectors', variables],
    queryFn: fetcher<AiHubUserConnectorsQuery, AiHubUserConnectorsQueryVariables>(AiHubUserConnectorsDocument, variables),
    ...options
  }
    )};

export const AttachAiHubChatToolDocument = new TypedDocumentString(`
    mutation attachAiHubChatTool($input: AttachAiHubChatToolInput!) {
  attachAiHubChatTool(input: $input) {
    clusterElementName
    componentName
    componentVersion
    connectionId
    chatComponentId
    chatId
    chatToolId
    environment
    parameters
  }
}
    `);

export const useAttachAiHubChatToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AttachAiHubChatToolMutation, TError, AttachAiHubChatToolMutationVariables, TContext>) => {
    
    return useMutation<AttachAiHubChatToolMutation, TError, AttachAiHubChatToolMutationVariables, TContext>(
      {
    mutationKey: ['attachAiHubChatTool'],
    mutationFn: (variables?: AttachAiHubChatToolMutationVariables) => fetcher<AttachAiHubChatToolMutation, AttachAiHubChatToolMutationVariables>(AttachAiHubChatToolDocument, variables)(),
    ...options
  }
    )};

export const DetachAiHubChatComponentDocument = new TypedDocumentString(`
    mutation detachAiHubChatComponent($workspaceId: ID!, $chatComponentId: ID!) {
  detachAiHubChatComponent(
    workspaceId: $workspaceId
    chatComponentId: $chatComponentId
  )
}
    `);

export const useDetachAiHubChatComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DetachAiHubChatComponentMutation, TError, DetachAiHubChatComponentMutationVariables, TContext>) => {
    
    return useMutation<DetachAiHubChatComponentMutation, TError, DetachAiHubChatComponentMutationVariables, TContext>(
      {
    mutationKey: ['detachAiHubChatComponent'],
    mutationFn: (variables?: DetachAiHubChatComponentMutationVariables) => fetcher<DetachAiHubChatComponentMutation, DetachAiHubChatComponentMutationVariables>(DetachAiHubChatComponentDocument, variables)(),
    ...options
  }
    )};

export const RemoveAiHubChatToolDocument = new TypedDocumentString(`
    mutation removeAiHubChatTool($workspaceId: ID!, $chatToolId: ID!) {
  removeAiHubChatTool(workspaceId: $workspaceId, chatToolId: $chatToolId)
}
    `);

export const useRemoveAiHubChatToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveAiHubChatToolMutation, TError, RemoveAiHubChatToolMutationVariables, TContext>) => {
    
    return useMutation<RemoveAiHubChatToolMutation, TError, RemoveAiHubChatToolMutationVariables, TContext>(
      {
    mutationKey: ['removeAiHubChatTool'],
    mutationFn: (variables?: RemoveAiHubChatToolMutationVariables) => fetcher<RemoveAiHubChatToolMutation, RemoveAiHubChatToolMutationVariables>(RemoveAiHubChatToolDocument, variables)(),
    ...options
  }
    )};

export const RemoveAiHubUserConnectorDocument = new TypedDocumentString(`
    mutation removeAiHubUserConnector($workspaceId: ID!, $connectorId: ID!) {
  removeAiHubUserConnector(workspaceId: $workspaceId, connectorId: $connectorId)
}
    `);

export const useRemoveAiHubUserConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveAiHubUserConnectorMutation, TError, RemoveAiHubUserConnectorMutationVariables, TContext>) => {
    
    return useMutation<RemoveAiHubUserConnectorMutation, TError, RemoveAiHubUserConnectorMutationVariables, TContext>(
      {
    mutationKey: ['removeAiHubUserConnector'],
    mutationFn: (variables?: RemoveAiHubUserConnectorMutationVariables) => fetcher<RemoveAiHubUserConnectorMutation, RemoveAiHubUserConnectorMutationVariables>(RemoveAiHubUserConnectorDocument, variables)(),
    ...options
  }
    )};

export const SetAiHubChatConnectorEnabledDocument = new TypedDocumentString(`
    mutation setAiHubChatConnectorEnabled($workspaceId: ID!, $chatId: ID!, $connectorId: ID!, $enabled: Boolean!) {
  setAiHubChatConnectorEnabled(
    workspaceId: $workspaceId
    chatId: $chatId
    connectorId: $connectorId
    enabled: $enabled
  )
}
    `);

export const useSetAiHubChatConnectorEnabledMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiHubChatConnectorEnabledMutation, TError, SetAiHubChatConnectorEnabledMutationVariables, TContext>) => {
    
    return useMutation<SetAiHubChatConnectorEnabledMutation, TError, SetAiHubChatConnectorEnabledMutationVariables, TContext>(
      {
    mutationKey: ['setAiHubChatConnectorEnabled'],
    mutationFn: (variables?: SetAiHubChatConnectorEnabledMutationVariables) => fetcher<SetAiHubChatConnectorEnabledMutation, SetAiHubChatConnectorEnabledMutationVariables>(SetAiHubChatConnectorEnabledDocument, variables)(),
    ...options
  }
    )};

export const SetAiHubUserConnectorEnabledDocument = new TypedDocumentString(`
    mutation setAiHubUserConnectorEnabled($workspaceId: ID!, $connectorId: ID!, $enabled: Boolean!) {
  setAiHubUserConnectorEnabled(
    workspaceId: $workspaceId
    connectorId: $connectorId
    enabled: $enabled
  )
}
    `);

export const useSetAiHubUserConnectorEnabledMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiHubUserConnectorEnabledMutation, TError, SetAiHubUserConnectorEnabledMutationVariables, TContext>) => {
    
    return useMutation<SetAiHubUserConnectorEnabledMutation, TError, SetAiHubUserConnectorEnabledMutationVariables, TContext>(
      {
    mutationKey: ['setAiHubUserConnectorEnabled'],
    mutationFn: (variables?: SetAiHubUserConnectorEnabledMutationVariables) => fetcher<SetAiHubUserConnectorEnabledMutation, SetAiHubUserConnectorEnabledMutationVariables>(SetAiHubUserConnectorEnabledDocument, variables)(),
    ...options
  }
    )};

export const SetAiHubUserConnectorToolEnabledDocument = new TypedDocumentString(`
    mutation setAiHubUserConnectorToolEnabled($workspaceId: ID!, $connectorId: ID!, $toolName: String!, $enabled: Boolean!) {
  setAiHubUserConnectorToolEnabled(
    workspaceId: $workspaceId
    connectorId: $connectorId
    toolName: $toolName
    enabled: $enabled
  )
}
    `);

export const useSetAiHubUserConnectorToolEnabledMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiHubUserConnectorToolEnabledMutation, TError, SetAiHubUserConnectorToolEnabledMutationVariables, TContext>) => {
    
    return useMutation<SetAiHubUserConnectorToolEnabledMutation, TError, SetAiHubUserConnectorToolEnabledMutationVariables, TContext>(
      {
    mutationKey: ['setAiHubUserConnectorToolEnabled'],
    mutationFn: (variables?: SetAiHubUserConnectorToolEnabledMutationVariables) => fetcher<SetAiHubUserConnectorToolEnabledMutation, SetAiHubUserConnectorToolEnabledMutationVariables>(SetAiHubUserConnectorToolEnabledDocument, variables)(),
    ...options
  }
    )};

export const SetAiHubUserConnectorToolParametersDocument = new TypedDocumentString(`
    mutation setAiHubUserConnectorToolParameters($workspaceId: ID!, $connectorId: ID!, $toolName: String!, $parameters: Any!) {
  setAiHubUserConnectorToolParameters(
    workspaceId: $workspaceId
    connectorId: $connectorId
    toolName: $toolName
    parameters: $parameters
  )
}
    `);

export const useSetAiHubUserConnectorToolParametersMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiHubUserConnectorToolParametersMutation, TError, SetAiHubUserConnectorToolParametersMutationVariables, TContext>) => {
    
    return useMutation<SetAiHubUserConnectorToolParametersMutation, TError, SetAiHubUserConnectorToolParametersMutationVariables, TContext>(
      {
    mutationKey: ['setAiHubUserConnectorToolParameters'],
    mutationFn: (variables?: SetAiHubUserConnectorToolParametersMutationVariables) => fetcher<SetAiHubUserConnectorToolParametersMutation, SetAiHubUserConnectorToolParametersMutationVariables>(SetAiHubUserConnectorToolParametersDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiHubChatToolParametersDocument = new TypedDocumentString(`
    mutation updateAiHubChatToolParameters($workspaceId: ID!, $chatToolId: ID!, $parameters: Any!) {
  updateAiHubChatToolParameters(
    workspaceId: $workspaceId
    chatToolId: $chatToolId
    parameters: $parameters
  ) {
    clusterElementName
    componentName
    componentVersion
    connectionId
    chatComponentId
    chatId
    chatToolId
    environment
    parameters
  }
}
    `);

export const useUpdateAiHubChatToolParametersMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiHubChatToolParametersMutation, TError, UpdateAiHubChatToolParametersMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiHubChatToolParametersMutation, TError, UpdateAiHubChatToolParametersMutationVariables, TContext>(
      {
    mutationKey: ['updateAiHubChatToolParameters'],
    mutationFn: (variables?: UpdateAiHubChatToolParametersMutationVariables) => fetcher<UpdateAiHubChatToolParametersMutation, UpdateAiHubChatToolParametersMutationVariables>(UpdateAiHubChatToolParametersDocument, variables)(),
    ...options
  }
    )};

export const AiHubChatArtifactsByAiHubChatDocument = new TypedDocumentString(`
    query aiHubChatArtifactsByAiHubChat($workspaceId: ID!, $id: ID!) {
  aiHubChatArtifactsByAiHubChat(workspaceId: $workspaceId, id: $id) {
    id
    chatId
    kind
    status
    artifactId
    artifactName
    metadataJson
    environmentId
    createdAt
    statusChangedAt
  }
}
    `);

export const useAiHubChatArtifactsByAiHubChatQuery = <
      TData = AiHubChatArtifactsByAiHubChatQuery,
      TError = unknown
    >(
      variables: AiHubChatArtifactsByAiHubChatQueryVariables,
      options?: Omit<UseQueryOptions<AiHubChatArtifactsByAiHubChatQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubChatArtifactsByAiHubChatQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubChatArtifactsByAiHubChatQuery, TError, TData>(
      {
    queryKey: ['aiHubChatArtifactsByAiHubChat', variables],
    queryFn: fetcher<AiHubChatArtifactsByAiHubChatQuery, AiHubChatArtifactsByAiHubChatQueryVariables>(AiHubChatArtifactsByAiHubChatDocument, variables),
    ...options
  }
    )};

export const AiHubChatMessagesDocument = new TypedDocumentString(`
    query aiHubChatMessages($workspaceId: ID!, $id: ID!) {
  aiHubChatMessages(workspaceId: $workspaceId, id: $id) {
    role
    content
    timestamp
    toolEventsJson
  }
}
    `);

export const useAiHubChatMessagesQuery = <
      TData = AiHubChatMessagesQuery,
      TError = unknown
    >(
      variables: AiHubChatMessagesQueryVariables,
      options?: Omit<UseQueryOptions<AiHubChatMessagesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubChatMessagesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubChatMessagesQuery, TError, TData>(
      {
    queryKey: ['aiHubChatMessages', variables],
    queryFn: fetcher<AiHubChatMessagesQuery, AiHubChatMessagesQueryVariables>(AiHubChatMessagesDocument, variables),
    ...options
  }
    )};

export const AiHubChatsDocument = new TypedDocumentString(`
    query aiHubChats($workspaceId: ID!, $environment: Int!, $status: AiHubChatStatus) {
  aiHubChats(
    workspaceId: $workspaceId
    environment: $environment
    status: $status
  ) {
    id
    workspaceId
    userId
    threadId
    title
    lastPreview
    messageCount
    status
    environmentId
    createdAt
    updatedAt
    kind
    workflowExecutionId
    projectDeploymentId
    autoTitled
    aiAgentId
  }
}
    `);

export const useAiHubChatsQuery = <
      TData = AiHubChatsQuery,
      TError = unknown
    >(
      variables: AiHubChatsQueryVariables,
      options?: Omit<UseQueryOptions<AiHubChatsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubChatsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubChatsQuery, TError, TData>(
      {
    queryKey: ['aiHubChats', variables],
    queryFn: fetcher<AiHubChatsQuery, AiHubChatsQueryVariables>(AiHubChatsDocument, variables),
    ...options
  }
    )};

export const AppendAiHubChatAssistantMessageDocument = new TypedDocumentString(`
    mutation appendAiHubChatAssistantMessage($workspaceId: ID!, $id: ID!, $content: String!) {
  appendAiHubChatAssistantMessage(
    workspaceId: $workspaceId
    id: $id
    content: $content
  )
}
    `);

export const useAppendAiHubChatAssistantMessageMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AppendAiHubChatAssistantMessageMutation, TError, AppendAiHubChatAssistantMessageMutationVariables, TContext>) => {
    
    return useMutation<AppendAiHubChatAssistantMessageMutation, TError, AppendAiHubChatAssistantMessageMutationVariables, TContext>(
      {
    mutationKey: ['appendAiHubChatAssistantMessage'],
    mutationFn: (variables?: AppendAiHubChatAssistantMessageMutationVariables) => fetcher<AppendAiHubChatAssistantMessageMutation, AppendAiHubChatAssistantMessageMutationVariables>(AppendAiHubChatAssistantMessageDocument, variables)(),
    ...options
  }
    )};

export const BulkArchiveWorkflowChatAiHubChatsDocument = new TypedDocumentString(`
    mutation bulkArchiveWorkflowChatAiHubChats($workspaceId: ID!, $environment: Int!) {
  bulkArchiveWorkflowChatAiHubChats(
    workspaceId: $workspaceId
    environment: $environment
  )
}
    `);

export const useBulkArchiveWorkflowChatAiHubChatsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<BulkArchiveWorkflowChatAiHubChatsMutation, TError, BulkArchiveWorkflowChatAiHubChatsMutationVariables, TContext>) => {
    
    return useMutation<BulkArchiveWorkflowChatAiHubChatsMutation, TError, BulkArchiveWorkflowChatAiHubChatsMutationVariables, TContext>(
      {
    mutationKey: ['bulkArchiveWorkflowChatAiHubChats'],
    mutationFn: (variables?: BulkArchiveWorkflowChatAiHubChatsMutationVariables) => fetcher<BulkArchiveWorkflowChatAiHubChatsMutation, BulkArchiveWorkflowChatAiHubChatsMutationVariables>(BulkArchiveWorkflowChatAiHubChatsDocument, variables)(),
    ...options
  }
    )};

export const CancelAiHubRunDocument = new TypedDocumentString(`
    mutation cancelAiHubRun($workspaceId: ID!, $id: ID!, $runId: String) {
  cancelAiHubRun(workspaceId: $workspaceId, id: $id, runId: $runId)
}
    `);

export const useCancelAiHubRunMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelAiHubRunMutation, TError, CancelAiHubRunMutationVariables, TContext>) => {
    
    return useMutation<CancelAiHubRunMutation, TError, CancelAiHubRunMutationVariables, TContext>(
      {
    mutationKey: ['cancelAiHubRun'],
    mutationFn: (variables?: CancelAiHubRunMutationVariables) => fetcher<CancelAiHubRunMutation, CancelAiHubRunMutationVariables>(CancelAiHubRunDocument, variables)(),
    ...options
  }
    )};

export const CancelWorkflowChatTurnDocument = new TypedDocumentString(`
    mutation cancelWorkflowChatTurn($workspaceId: ID!, $id: ID!) {
  cancelWorkflowChatTurn(workspaceId: $workspaceId, id: $id)
}
    `);

export const useCancelWorkflowChatTurnMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelWorkflowChatTurnMutation, TError, CancelWorkflowChatTurnMutationVariables, TContext>) => {
    
    return useMutation<CancelWorkflowChatTurnMutation, TError, CancelWorkflowChatTurnMutationVariables, TContext>(
      {
    mutationKey: ['cancelWorkflowChatTurn'],
    mutationFn: (variables?: CancelWorkflowChatTurnMutationVariables) => fetcher<CancelWorkflowChatTurnMutation, CancelWorkflowChatTurnMutationVariables>(CancelWorkflowChatTurnDocument, variables)(),
    ...options
  }
    )};

export const CreateAgentChatAiHubChatDocument = new TypedDocumentString(`
    mutation createAgentChatAiHubChat($workspaceId: ID!, $environment: Int!, $workflowExecutionId: String!, $projectDeploymentId: ID!, $title: String) {
  createAgentChatAiHubChat(
    workspaceId: $workspaceId
    environment: $environment
    workflowExecutionId: $workflowExecutionId
    projectDeploymentId: $projectDeploymentId
    title: $title
  ) {
    id
    workspaceId
    userId
    threadId
    title
    lastPreview
    messageCount
    status
    environmentId
    createdAt
    updatedAt
    kind
    workflowExecutionId
    projectDeploymentId
    autoTitled
    aiAgentId
  }
}
    `);

export const useCreateAgentChatAiHubChatMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAgentChatAiHubChatMutation, TError, CreateAgentChatAiHubChatMutationVariables, TContext>) => {
    
    return useMutation<CreateAgentChatAiHubChatMutation, TError, CreateAgentChatAiHubChatMutationVariables, TContext>(
      {
    mutationKey: ['createAgentChatAiHubChat'],
    mutationFn: (variables?: CreateAgentChatAiHubChatMutationVariables) => fetcher<CreateAgentChatAiHubChatMutation, CreateAgentChatAiHubChatMutationVariables>(CreateAgentChatAiHubChatDocument, variables)(),
    ...options
  }
    )};

export const CreateAiHubChatDocument = new TypedDocumentString(`
    mutation createAiHubChat($workspaceId: ID!, $environment: Int!, $threadId: String!) {
  createAiHubChat(
    workspaceId: $workspaceId
    environment: $environment
    threadId: $threadId
  ) {
    id
    workspaceId
    userId
    threadId
    title
    lastPreview
    messageCount
    status
    environmentId
    createdAt
    updatedAt
    kind
    workflowExecutionId
    projectDeploymentId
    autoTitled
    aiAgentId
  }
}
    `);

export const useCreateAiHubChatMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiHubChatMutation, TError, CreateAiHubChatMutationVariables, TContext>) => {
    
    return useMutation<CreateAiHubChatMutation, TError, CreateAiHubChatMutationVariables, TContext>(
      {
    mutationKey: ['createAiHubChat'],
    mutationFn: (variables?: CreateAiHubChatMutationVariables) => fetcher<CreateAiHubChatMutation, CreateAiHubChatMutationVariables>(CreateAiHubChatDocument, variables)(),
    ...options
  }
    )};

export const CreateWorkflowChatAiHubChatDocument = new TypedDocumentString(`
    mutation createWorkflowChatAiHubChat($workspaceId: ID!, $environment: Int!, $workflowExecutionId: String!, $projectDeploymentId: ID!, $title: String) {
  createWorkflowChatAiHubChat(
    workspaceId: $workspaceId
    environment: $environment
    workflowExecutionId: $workflowExecutionId
    projectDeploymentId: $projectDeploymentId
    title: $title
  ) {
    id
    workspaceId
    userId
    threadId
    title
    lastPreview
    messageCount
    status
    environmentId
    createdAt
    updatedAt
    kind
    workflowExecutionId
    projectDeploymentId
    autoTitled
    aiAgentId
  }
}
    `);

export const useCreateWorkflowChatAiHubChatMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkflowChatAiHubChatMutation, TError, CreateWorkflowChatAiHubChatMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkflowChatAiHubChatMutation, TError, CreateWorkflowChatAiHubChatMutationVariables, TContext>(
      {
    mutationKey: ['createWorkflowChatAiHubChat'],
    mutationFn: (variables?: CreateWorkflowChatAiHubChatMutationVariables) => fetcher<CreateWorkflowChatAiHubChatMutation, CreateWorkflowChatAiHubChatMutationVariables>(CreateWorkflowChatAiHubChatDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiHubChatDocument = new TypedDocumentString(`
    mutation deleteAiHubChat($workspaceId: ID!, $id: ID!) {
  deleteAiHubChat(workspaceId: $workspaceId, id: $id)
}
    `);

export const useDeleteAiHubChatMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiHubChatMutation, TError, DeleteAiHubChatMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiHubChatMutation, TError, DeleteAiHubChatMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiHubChat'],
    mutationFn: (variables?: DeleteAiHubChatMutationVariables) => fetcher<DeleteAiHubChatMutation, DeleteAiHubChatMutationVariables>(DeleteAiHubChatDocument, variables)(),
    ...options
  }
    )};

export const GenerateAiHubChatTitleDocument = new TypedDocumentString(`
    mutation generateAiHubChatTitle($workspaceId: ID!, $id: ID!) {
  generateAiHubChatTitle(workspaceId: $workspaceId, id: $id) {
    id
    workspaceId
    userId
    threadId
    title
    lastPreview
    messageCount
    status
    environmentId
    createdAt
    updatedAt
    kind
    workflowExecutionId
    projectDeploymentId
    autoTitled
    aiAgentId
  }
}
    `);

export const useGenerateAiHubChatTitleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<GenerateAiHubChatTitleMutation, TError, GenerateAiHubChatTitleMutationVariables, TContext>) => {
    
    return useMutation<GenerateAiHubChatTitleMutation, TError, GenerateAiHubChatTitleMutationVariables, TContext>(
      {
    mutationKey: ['generateAiHubChatTitle'],
    mutationFn: (variables?: GenerateAiHubChatTitleMutationVariables) => fetcher<GenerateAiHubChatTitleMutation, GenerateAiHubChatTitleMutationVariables>(GenerateAiHubChatTitleDocument, variables)(),
    ...options
  }
    )};

export const TruncateAiHubChatMessagesDocument = new TypedDocumentString(`
    mutation truncateAiHubChatMessages($workspaceId: ID!, $id: ID!, $fromMessageIndex: Int!) {
  truncateAiHubChatMessages(
    workspaceId: $workspaceId
    id: $id
    fromMessageIndex: $fromMessageIndex
  )
}
    `);

export const useTruncateAiHubChatMessagesMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TruncateAiHubChatMessagesMutation, TError, TruncateAiHubChatMessagesMutationVariables, TContext>) => {
    
    return useMutation<TruncateAiHubChatMessagesMutation, TError, TruncateAiHubChatMessagesMutationVariables, TContext>(
      {
    mutationKey: ['truncateAiHubChatMessages'],
    mutationFn: (variables?: TruncateAiHubChatMessagesMutationVariables) => fetcher<TruncateAiHubChatMessagesMutation, TruncateAiHubChatMessagesMutationVariables>(TruncateAiHubChatMessagesDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiHubChatDocument = new TypedDocumentString(`
    mutation updateAiHubChat($input: AiHubChatPatchInput!) {
  updateAiHubChat(input: $input) {
    id
    workspaceId
    userId
    threadId
    title
    lastPreview
    messageCount
    status
    environmentId
    createdAt
    updatedAt
    kind
    workflowExecutionId
    projectDeploymentId
    autoTitled
    aiAgentId
  }
}
    `);

export const useUpdateAiHubChatMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiHubChatMutation, TError, UpdateAiHubChatMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiHubChatMutation, TError, UpdateAiHubChatMutationVariables, TContext>(
      {
    mutationKey: ['updateAiHubChat'],
    mutationFn: (variables?: UpdateAiHubChatMutationVariables) => fetcher<UpdateAiHubChatMutation, UpdateAiHubChatMutationVariables>(UpdateAiHubChatDocument, variables)(),
    ...options
  }
    )};

export const AddAiHubMcpServerDocument = new TypedDocumentString(`
    mutation addAiHubMcpServer($workspaceId: ID!, $name: String!, $url: String!, $authToken: String, $environment: Int!) {
  addAiHubMcpServer(
    workspaceId: $workspaceId
    name: $name
    url: $url
    authToken: $authToken
    environment: $environment
  )
}
    `);

export const useAddAiHubMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddAiHubMcpServerMutation, TError, AddAiHubMcpServerMutationVariables, TContext>) => {
    
    return useMutation<AddAiHubMcpServerMutation, TError, AddAiHubMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['addAiHubMcpServer'],
    mutationFn: (variables?: AddAiHubMcpServerMutationVariables) => fetcher<AddAiHubMcpServerMutation, AddAiHubMcpServerMutationVariables>(AddAiHubMcpServerDocument, variables)(),
    ...options
  }
    )};

export const AiHubMcpServerToolsDocument = new TypedDocumentString(`
    query aiHubMcpServerTools($workspaceId: ID!, $mcpServerId: ID!) {
  aiHubMcpServerTools(workspaceId: $workspaceId, mcpServerId: $mcpServerId) {
    description
    enabled
    name
  }
}
    `);

export const useAiHubMcpServerToolsQuery = <
      TData = AiHubMcpServerToolsQuery,
      TError = unknown
    >(
      variables: AiHubMcpServerToolsQueryVariables,
      options?: Omit<UseQueryOptions<AiHubMcpServerToolsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubMcpServerToolsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubMcpServerToolsQuery, TError, TData>(
      {
    queryKey: ['aiHubMcpServerTools', variables],
    queryFn: fetcher<AiHubMcpServerToolsQuery, AiHubMcpServerToolsQueryVariables>(AiHubMcpServerToolsDocument, variables),
    ...options
  }
    )};

export const AiHubMcpServersDocument = new TypedDocumentString(`
    query aiHubMcpServers($workspaceId: ID!) {
  aiHubMcpServers(workspaceId: $workspaceId) {
    enabled
    hasAuthToken
    id
    name
    url
  }
}
    `);

export const useAiHubMcpServersQuery = <
      TData = AiHubMcpServersQuery,
      TError = unknown
    >(
      variables: AiHubMcpServersQueryVariables,
      options?: Omit<UseQueryOptions<AiHubMcpServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubMcpServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubMcpServersQuery, TError, TData>(
      {
    queryKey: ['aiHubMcpServers', variables],
    queryFn: fetcher<AiHubMcpServersQuery, AiHubMcpServersQueryVariables>(AiHubMcpServersDocument, variables),
    ...options
  }
    )};

export const RemoveAiHubMcpServerDocument = new TypedDocumentString(`
    mutation removeAiHubMcpServer($workspaceId: ID!, $mcpServerId: ID!) {
  removeAiHubMcpServer(workspaceId: $workspaceId, mcpServerId: $mcpServerId)
}
    `);

export const useRemoveAiHubMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveAiHubMcpServerMutation, TError, RemoveAiHubMcpServerMutationVariables, TContext>) => {
    
    return useMutation<RemoveAiHubMcpServerMutation, TError, RemoveAiHubMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['removeAiHubMcpServer'],
    mutationFn: (variables?: RemoveAiHubMcpServerMutationVariables) => fetcher<RemoveAiHubMcpServerMutation, RemoveAiHubMcpServerMutationVariables>(RemoveAiHubMcpServerDocument, variables)(),
    ...options
  }
    )};

export const SetAiHubMcpServerEnabledDocument = new TypedDocumentString(`
    mutation setAiHubMcpServerEnabled($workspaceId: ID!, $mcpServerId: ID!, $enabled: Boolean!) {
  setAiHubMcpServerEnabled(
    workspaceId: $workspaceId
    mcpServerId: $mcpServerId
    enabled: $enabled
  )
}
    `);

export const useSetAiHubMcpServerEnabledMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiHubMcpServerEnabledMutation, TError, SetAiHubMcpServerEnabledMutationVariables, TContext>) => {
    
    return useMutation<SetAiHubMcpServerEnabledMutation, TError, SetAiHubMcpServerEnabledMutationVariables, TContext>(
      {
    mutationKey: ['setAiHubMcpServerEnabled'],
    mutationFn: (variables?: SetAiHubMcpServerEnabledMutationVariables) => fetcher<SetAiHubMcpServerEnabledMutation, SetAiHubMcpServerEnabledMutationVariables>(SetAiHubMcpServerEnabledDocument, variables)(),
    ...options
  }
    )};

export const SetAiHubMcpServerToolEnabledDocument = new TypedDocumentString(`
    mutation setAiHubMcpServerToolEnabled($workspaceId: ID!, $mcpServerId: ID!, $toolName: String!, $enabled: Boolean!) {
  setAiHubMcpServerToolEnabled(
    workspaceId: $workspaceId
    mcpServerId: $mcpServerId
    toolName: $toolName
    enabled: $enabled
  )
}
    `);

export const useSetAiHubMcpServerToolEnabledMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiHubMcpServerToolEnabledMutation, TError, SetAiHubMcpServerToolEnabledMutationVariables, TContext>) => {
    
    return useMutation<SetAiHubMcpServerToolEnabledMutation, TError, SetAiHubMcpServerToolEnabledMutationVariables, TContext>(
      {
    mutationKey: ['setAiHubMcpServerToolEnabled'],
    mutationFn: (variables?: SetAiHubMcpServerToolEnabledMutationVariables) => fetcher<SetAiHubMcpServerToolEnabledMutation, SetAiHubMcpServerToolEnabledMutationVariables>(SetAiHubMcpServerToolEnabledDocument, variables)(),
    ...options
  }
    )};

export const AiHubWorkspaceSettingsDocument = new TypedDocumentString(`
    query aiHubWorkspaceSettings($workspaceId: ID!) {
  aiHubWorkspaceSettings(workspaceId: $workspaceId) {
    workspaceId
    voiceWebhookUrl
  }
}
    `);

export const useAiHubWorkspaceSettingsQuery = <
      TData = AiHubWorkspaceSettingsQuery,
      TError = unknown
    >(
      variables: AiHubWorkspaceSettingsQueryVariables,
      options?: Omit<UseQueryOptions<AiHubWorkspaceSettingsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiHubWorkspaceSettingsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiHubWorkspaceSettingsQuery, TError, TData>(
      {
    queryKey: ['aiHubWorkspaceSettings', variables],
    queryFn: fetcher<AiHubWorkspaceSettingsQuery, AiHubWorkspaceSettingsQueryVariables>(AiHubWorkspaceSettingsDocument, variables),
    ...options
  }
    )};

export const UpdateAiHubVoiceWebhookUrlDocument = new TypedDocumentString(`
    mutation updateAiHubVoiceWebhookUrl($input: UpdateAiHubVoiceWebhookUrlInput!) {
  updateAiHubVoiceWebhookUrl(input: $input) {
    workspaceId
    voiceWebhookUrl
  }
}
    `);

export const useUpdateAiHubVoiceWebhookUrlMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiHubVoiceWebhookUrlMutation, TError, UpdateAiHubVoiceWebhookUrlMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiHubVoiceWebhookUrlMutation, TError, UpdateAiHubVoiceWebhookUrlMutationVariables, TContext>(
      {
    mutationKey: ['updateAiHubVoiceWebhookUrl'],
    mutationFn: (variables?: UpdateAiHubVoiceWebhookUrlMutationVariables) => fetcher<UpdateAiHubVoiceWebhookUrlMutation, UpdateAiHubVoiceWebhookUrlMutationVariables>(UpdateAiHubVoiceWebhookUrlDocument, variables)(),
    ...options
  }
    )};

export const AiAutoMemoriesDocument = new TypedDocumentString(`
    query aiAutoMemories($workspaceId: ID!, $environment: Int!, $memoryType: AiAutoMemoryType, $principalType: AiAutoMemoryPrincipalType, $principalId: Long) {
  aiAutoMemories(
    workspaceId: $workspaceId
    environment: $environment
    memoryType: $memoryType
    principalType: $principalType
    principalId: $principalId
  ) {
    id
    workspaceId
    principalType
    principalId
    name
    title
    description
    memoryType
    content
    environmentId
    createdAt
    updatedAt
  }
}
    `);

export const useAiAutoMemoriesQuery = <
      TData = AiAutoMemoriesQuery,
      TError = unknown
    >(
      variables: AiAutoMemoriesQueryVariables,
      options?: Omit<UseQueryOptions<AiAutoMemoriesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAutoMemoriesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAutoMemoriesQuery, TError, TData>(
      {
    queryKey: ['aiAutoMemories', variables],
    queryFn: fetcher<AiAutoMemoriesQuery, AiAutoMemoriesQueryVariables>(AiAutoMemoriesDocument, variables),
    ...options
  }
    )};

export const AiAutoMemoryDocument = new TypedDocumentString(`
    query aiAutoMemory($workspaceId: ID!, $id: ID!, $environment: Int!, $principalType: AiAutoMemoryPrincipalType, $principalId: Long) {
  aiAutoMemory(
    workspaceId: $workspaceId
    id: $id
    environment: $environment
    principalType: $principalType
    principalId: $principalId
  ) {
    id
    workspaceId
    principalType
    principalId
    name
    title
    description
    memoryType
    content
    environmentId
    createdAt
    updatedAt
  }
}
    `);

export const useAiAutoMemoryQuery = <
      TData = AiAutoMemoryQuery,
      TError = unknown
    >(
      variables: AiAutoMemoryQueryVariables,
      options?: Omit<UseQueryOptions<AiAutoMemoryQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAutoMemoryQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAutoMemoryQuery, TError, TData>(
      {
    queryKey: ['aiAutoMemory', variables],
    queryFn: fetcher<AiAutoMemoryQuery, AiAutoMemoryQueryVariables>(AiAutoMemoryDocument, variables),
    ...options
  }
    )};

export const AiAutoMemoryPrincipalsDocument = new TypedDocumentString(`
    query aiAutoMemoryPrincipals($workspaceId: ID!, $environment: Int!) {
  aiAutoMemoryPrincipals(workspaceId: $workspaceId, environment: $environment) {
    principalType
    principalId
    label
    memoryCount
  }
}
    `);

export const useAiAutoMemoryPrincipalsQuery = <
      TData = AiAutoMemoryPrincipalsQuery,
      TError = unknown
    >(
      variables: AiAutoMemoryPrincipalsQueryVariables,
      options?: Omit<UseQueryOptions<AiAutoMemoryPrincipalsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAutoMemoryPrincipalsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAutoMemoryPrincipalsQuery, TError, TData>(
      {
    queryKey: ['aiAutoMemoryPrincipals', variables],
    queryFn: fetcher<AiAutoMemoryPrincipalsQuery, AiAutoMemoryPrincipalsQueryVariables>(AiAutoMemoryPrincipalsDocument, variables),
    ...options
  }
    )};

export const DeleteAiAutoMemoryDocument = new TypedDocumentString(`
    mutation deleteAiAutoMemory($workspaceId: ID!, $id: ID!, $environment: Int!, $principalType: AiAutoMemoryPrincipalType, $principalId: Long) {
  deleteAiAutoMemory(
    workspaceId: $workspaceId
    id: $id
    environment: $environment
    principalType: $principalType
    principalId: $principalId
  )
}
    `);

export const useDeleteAiAutoMemoryMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAutoMemoryMutation, TError, DeleteAiAutoMemoryMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAutoMemoryMutation, TError, DeleteAiAutoMemoryMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAutoMemory'],
    mutationFn: (variables?: DeleteAiAutoMemoryMutationVariables) => fetcher<DeleteAiAutoMemoryMutation, DeleteAiAutoMemoryMutationVariables>(DeleteAiAutoMemoryDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAutoMemoryDocument = new TypedDocumentString(`
    mutation updateAiAutoMemory($input: UpdateAiAutoMemoryInput!) {
  updateAiAutoMemory(input: $input) {
    id
    workspaceId
    principalType
    principalId
    name
    title
    description
    memoryType
    content
    environmentId
    createdAt
    updatedAt
  }
}
    `);

export const useUpdateAiAutoMemoryMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAutoMemoryMutation, TError, UpdateAiAutoMemoryMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAutoMemoryMutation, TError, UpdateAiAutoMemoryMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAutoMemory'],
    mutationFn: (variables?: UpdateAiAutoMemoryMutationVariables) => fetcher<UpdateAiAutoMemoryMutation, UpdateAiAutoMemoryMutationVariables>(UpdateAiAutoMemoryDocument, variables)(),
    ...options
  }
    )};

export const AiSkillDocument = new TypedDocumentString(`
    query aiSkill($id: ID!) {
  aiSkill(id: $id) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAiSkillQuery = <
      TData = AiSkillQuery,
      TError = unknown
    >(
      variables: AiSkillQueryVariables,
      options?: Omit<UseQueryOptions<AiSkillQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiSkillQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiSkillQuery, TError, TData>(
      {
    queryKey: ['aiSkill', variables],
    queryFn: fetcher<AiSkillQuery, AiSkillQueryVariables>(AiSkillDocument, variables),
    ...options
  }
    )};

export const AiSkillFileContentDocument = new TypedDocumentString(`
    query aiSkillFileContent($id: ID!, $path: String!) {
  aiSkillFileContent(id: $id, path: $path)
}
    `);

export const useAiSkillFileContentQuery = <
      TData = AiSkillFileContentQuery,
      TError = unknown
    >(
      variables: AiSkillFileContentQueryVariables,
      options?: Omit<UseQueryOptions<AiSkillFileContentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiSkillFileContentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiSkillFileContentQuery, TError, TData>(
      {
    queryKey: ['aiSkillFileContent', variables],
    queryFn: fetcher<AiSkillFileContentQuery, AiSkillFileContentQueryVariables>(AiSkillFileContentDocument, variables),
    ...options
  }
    )};

export const AiSkillFilePathsDocument = new TypedDocumentString(`
    query aiSkillFilePaths($id: ID!) {
  aiSkillFilePaths(id: $id)
}
    `);

export const useAiSkillFilePathsQuery = <
      TData = AiSkillFilePathsQuery,
      TError = unknown
    >(
      variables: AiSkillFilePathsQueryVariables,
      options?: Omit<UseQueryOptions<AiSkillFilePathsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiSkillFilePathsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiSkillFilePathsQuery, TError, TData>(
      {
    queryKey: ['aiSkillFilePaths', variables],
    queryFn: fetcher<AiSkillFilePathsQuery, AiSkillFilePathsQueryVariables>(AiSkillFilePathsDocument, variables),
    ...options
  }
    )};

export const AiSkillTagsDocument = new TypedDocumentString(`
    query aiSkillTags {
  aiSkillTags {
    id
    name
  }
}
    `);

export const useAiSkillTagsQuery = <
      TData = AiSkillTagsQuery,
      TError = unknown
    >(
      variables?: AiSkillTagsQueryVariables,
      options?: Omit<UseQueryOptions<AiSkillTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiSkillTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiSkillTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiSkillTags'] : ['aiSkillTags', variables],
    queryFn: fetcher<AiSkillTagsQuery, AiSkillTagsQueryVariables>(AiSkillTagsDocument, variables),
    ...options
  }
    )};

export const AiSkillsDocument = new TypedDocumentString(`
    query aiSkills {
  aiSkills {
    id
    name
    description
    tags {
      id
      name
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useAiSkillsQuery = <
      TData = AiSkillsQuery,
      TError = unknown
    >(
      variables?: AiSkillsQueryVariables,
      options?: Omit<UseQueryOptions<AiSkillsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiSkillsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiSkillsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiSkills'] : ['aiSkills', variables],
    queryFn: fetcher<AiSkillsQuery, AiSkillsQueryVariables>(AiSkillsDocument, variables),
    ...options
  }
    )};

export const CreateAdditionalFilesInSkillDocument = new TypedDocumentString(`
    mutation createAdditionalFilesInSkill($id: ID!, $additionalFiles: Map!) {
  createAdditionalFilesInSkill(id: $id, additionalFiles: $additionalFiles) {
    description
    id
    lastModifiedDate
    name
  }
}
    `);

export const useCreateAdditionalFilesInSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAdditionalFilesInSkillMutation, TError, CreateAdditionalFilesInSkillMutationVariables, TContext>) => {
    
    return useMutation<CreateAdditionalFilesInSkillMutation, TError, CreateAdditionalFilesInSkillMutationVariables, TContext>(
      {
    mutationKey: ['createAdditionalFilesInSkill'],
    mutationFn: (variables?: CreateAdditionalFilesInSkillMutationVariables) => fetcher<CreateAdditionalFilesInSkillMutation, CreateAdditionalFilesInSkillMutationVariables>(CreateAdditionalFilesInSkillDocument, variables)(),
    ...options
  }
    )};

export const CreateAiSkillDocument = new TypedDocumentString(`
    mutation createAiSkill($name: String!, $description: String, $filename: String!, $fileBytes: String!) {
  createAiSkill(
    name: $name
    description: $description
    filename: $filename
    fileBytes: $fileBytes
  ) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAiSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiSkillMutation, TError, CreateAiSkillMutationVariables, TContext>) => {
    
    return useMutation<CreateAiSkillMutation, TError, CreateAiSkillMutationVariables, TContext>(
      {
    mutationKey: ['createAiSkill'],
    mutationFn: (variables?: CreateAiSkillMutationVariables) => fetcher<CreateAiSkillMutation, CreateAiSkillMutationVariables>(CreateAiSkillDocument, variables)(),
    ...options
  }
    )};

export const CreateAiSkillFromInstructionsDocument = new TypedDocumentString(`
    mutation createAiSkillFromInstructions($name: String!, $description: String, $instructions: String!) {
  createAiSkillFromInstructions(
    name: $name
    description: $description
    instructions: $instructions
  ) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useCreateAiSkillFromInstructionsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiSkillFromInstructionsMutation, TError, CreateAiSkillFromInstructionsMutationVariables, TContext>) => {
    
    return useMutation<CreateAiSkillFromInstructionsMutation, TError, CreateAiSkillFromInstructionsMutationVariables, TContext>(
      {
    mutationKey: ['createAiSkillFromInstructions'],
    mutationFn: (variables?: CreateAiSkillFromInstructionsMutationVariables) => fetcher<CreateAiSkillFromInstructionsMutation, CreateAiSkillFromInstructionsMutationVariables>(CreateAiSkillFromInstructionsDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiSkillDocument = new TypedDocumentString(`
    mutation deleteAiSkill($id: ID!) {
  deleteAiSkill(id: $id)
}
    `);

export const useDeleteAiSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiSkillMutation, TError, DeleteAiSkillMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiSkillMutation, TError, DeleteAiSkillMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiSkill'],
    mutationFn: (variables?: DeleteAiSkillMutationVariables) => fetcher<DeleteAiSkillMutation, DeleteAiSkillMutationVariables>(DeleteAiSkillDocument, variables)(),
    ...options
  }
    )};

export const RemoveFileInSkillDocument = new TypedDocumentString(`
    mutation removeFileInSkill($id: ID!, $path: String!) {
  removeFileInSkill(id: $id, path: $path) {
    description
    id
    lastModifiedDate
    name
  }
}
    `);

export const useRemoveFileInSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveFileInSkillMutation, TError, RemoveFileInSkillMutationVariables, TContext>) => {
    
    return useMutation<RemoveFileInSkillMutation, TError, RemoveFileInSkillMutationVariables, TContext>(
      {
    mutationKey: ['removeFileInSkill'],
    mutationFn: (variables?: RemoveFileInSkillMutationVariables) => fetcher<RemoveFileInSkillMutation, RemoveFileInSkillMutationVariables>(RemoveFileInSkillDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiSkillDocument = new TypedDocumentString(`
    mutation updateAiSkill($id: ID!, $name: String!, $description: String) {
  updateAiSkill(id: $id, name: $name, description: $description) {
    id
    name
    description
    createdDate
    lastModifiedDate
  }
}
    `);

export const useUpdateAiSkillMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiSkillMutation, TError, UpdateAiSkillMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiSkillMutation, TError, UpdateAiSkillMutationVariables, TContext>(
      {
    mutationKey: ['updateAiSkill'],
    mutationFn: (variables?: UpdateAiSkillMutationVariables) => fetcher<UpdateAiSkillMutation, UpdateAiSkillMutationVariables>(UpdateAiSkillDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiSkillContentDocument = new TypedDocumentString(`
    mutation updateAiSkillContent($id: ID!, $path: String, $content: String!) {
  updateAiSkillContent(id: $id, path: $path, content: $content) {
    description
    id
    lastModifiedDate
    name
  }
}
    `);

export const useUpdateAiSkillContentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiSkillContentMutation, TError, UpdateAiSkillContentMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiSkillContentMutation, TError, UpdateAiSkillContentMutationVariables, TContext>(
      {
    mutationKey: ['updateAiSkillContent'],
    mutationFn: (variables?: UpdateAiSkillContentMutationVariables) => fetcher<UpdateAiSkillContentMutation, UpdateAiSkillContentMutationVariables>(UpdateAiSkillContentDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiSkillTagsDocument = new TypedDocumentString(`
    mutation updateAiSkillTags($id: ID!, $tags: [AiSkillTagInput!]) {
  updateAiSkillTags(id: $id, tags: $tags) {
    id
  }
}
    `);

export const useUpdateAiSkillTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiSkillTagsMutation, TError, UpdateAiSkillTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiSkillTagsMutation, TError, UpdateAiSkillTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateAiSkillTags'],
    mutationFn: (variables?: UpdateAiSkillTagsMutationVariables) => fetcher<UpdateAiSkillTagsMutation, UpdateAiSkillTagsMutationVariables>(UpdateAiSkillTagsDocument, variables)(),
    ...options
  }
    )};

export const AuditEventsDocument = new TypedDocumentString(`
    query AuditEvents($principal: String, $eventType: String, $fromDate: Long, $toDate: Long, $dataSearch: String, $page: Int, $size: Int) {
  auditEvents(
    principal: $principal
    eventType: $eventType
    fromDate: $fromDate
    toDate: $toDate
    dataSearch: $dataSearch
    page: $page
    size: $size
  ) {
    content {
      data {
        key
        value
      }
      eventDate
      eventType
      id
      principal
    }
    number
    size
    totalElements
    totalPages
  }
}
    `);

export const useAuditEventsQuery = <
      TData = AuditEventsQuery,
      TError = unknown
    >(
      variables?: AuditEventsQueryVariables,
      options?: Omit<UseQueryOptions<AuditEventsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AuditEventsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AuditEventsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['AuditEvents'] : ['AuditEvents', variables],
    queryFn: fetcher<AuditEventsQuery, AuditEventsQueryVariables>(AuditEventsDocument, variables),
    ...options
  }
    )};

export const AuditEventTypesDocument = new TypedDocumentString(`
    query AuditEventTypes {
  auditEventTypes
}
    `);

export const useAuditEventTypesQuery = <
      TData = AuditEventTypesQuery,
      TError = unknown
    >(
      variables?: AuditEventTypesQueryVariables,
      options?: Omit<UseQueryOptions<AuditEventTypesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AuditEventTypesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AuditEventTypesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['AuditEventTypes'] : ['AuditEventTypes', variables],
    queryFn: fetcher<AuditEventTypesQuery, AuditEventTypesQueryVariables>(AuditEventTypesDocument, variables),
    ...options
  }
    )};

export const AddAiAgentChannelDocument = new TypedDocumentString(`
    mutation addAiAgentChannel($input: AddAiAgentChannelInput!) {
  addAiAgentChannel(input: $input) {
    id
    channelType
    position
    parameters
    connectionId
  }
}
    `);

export const useAddAiAgentChannelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddAiAgentChannelMutation, TError, AddAiAgentChannelMutationVariables, TContext>) => {
    
    return useMutation<AddAiAgentChannelMutation, TError, AddAiAgentChannelMutationVariables, TContext>(
      {
    mutationKey: ['addAiAgentChannel'],
    mutationFn: (variables?: AddAiAgentChannelMutationVariables) => fetcher<AddAiAgentChannelMutation, AddAiAgentChannelMutationVariables>(AddAiAgentChannelDocument, variables)(),
    ...options
  }
    )};

export const AddAiAgentElementDocument = new TypedDocumentString(`
    mutation addAiAgentElement($input: AddAiAgentElementInput!) {
  addAiAgentElement(input: $input) {
    id
    kind
    referenceId
    position
    parameters
    connectionId
  }
}
    `);

export const useAddAiAgentElementMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddAiAgentElementMutation, TError, AddAiAgentElementMutationVariables, TContext>) => {
    
    return useMutation<AddAiAgentElementMutation, TError, AddAiAgentElementMutationVariables, TContext>(
      {
    mutationKey: ['addAiAgentElement'],
    mutationFn: (variables?: AddAiAgentElementMutationVariables) => fetcher<AddAiAgentElementMutation, AddAiAgentElementMutationVariables>(AddAiAgentElementDocument, variables)(),
    ...options
  }
    )};

export const AiAgentDocument = new TypedDocumentString(`
    query aiAgent($id: ID!) {
  aiAgent(id: $id) {
    id
    name
    title
    description
    instructions
    workspaceId
    projectId
    uuid
    unpublishedChanges
    lastPublishedVersion
    channels {
      id
      channelType
      position
      parameters
      connectionId
    }
    elements {
      id
      kind
      referenceId
      position
      parameters
      connectionId
    }
    settings
    tags {
      id
      name
    }
    lastModifiedDate
    draftWorkflowId
    visibility
  }
}
    `);

export const useAiAgentQuery = <
      TData = AiAgentQuery,
      TError = unknown
    >(
      variables: AiAgentQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentQuery, TError, TData>(
      {
    queryKey: ['aiAgent', variables],
    queryFn: fetcher<AiAgentQuery, AiAgentQueryVariables>(AiAgentDocument, variables),
    ...options
  }
    )};

export const AiAgentChannelDefinitionsDocument = new TypedDocumentString(`
    query aiAgentChannelDefinitions {
  aiAgentChannelDefinitions {
    approvalCapable
    channelType
    componentName
    componentVersion
    connectionRequired
    description
    icon
    pinned
    propertiesConfigurable
    replyActionName
    schedule
    title
    triggerName
  }
}
    `);

export const useAiAgentChannelDefinitionsQuery = <
      TData = AiAgentChannelDefinitionsQuery,
      TError = unknown
    >(
      variables?: AiAgentChannelDefinitionsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentChannelDefinitionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentChannelDefinitionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentChannelDefinitionsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiAgentChannelDefinitions'] : ['aiAgentChannelDefinitions', variables],
    queryFn: fetcher<AiAgentChannelDefinitionsQuery, AiAgentChannelDefinitionsQueryVariables>(AiAgentChannelDefinitionsDocument, variables),
    ...options
  }
    )};

export const AiAgentDeploymentTagsDocument = new TypedDocumentString(`
    query aiAgentDeploymentTags($workspaceId: ID!) {
  aiAgentDeploymentTags(workspaceId: $workspaceId) {
    id
    name
  }
}
    `);

export const useAiAgentDeploymentTagsQuery = <
      TData = AiAgentDeploymentTagsQuery,
      TError = unknown
    >(
      variables: AiAgentDeploymentTagsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentDeploymentTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentDeploymentTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentDeploymentTagsQuery, TError, TData>(
      {
    queryKey: ['aiAgentDeploymentTags', variables],
    queryFn: fetcher<AiAgentDeploymentTagsQuery, AiAgentDeploymentTagsQueryVariables>(AiAgentDeploymentTagsDocument, variables),
    ...options
  }
    )};

export const AiAgentDeploymentsDocument = new TypedDocumentString(`
    query aiAgentDeployments($workspaceId: ID!) {
  aiAgentDeployments(workspaceId: $workspaceId) {
    id
    name
    agentId
    agentTitle
    projectId
    environmentId
    enabled
    projectVersion
    lastExecutionDate
    tags {
      id
      name
    }
    workflows {
      workflowId
      enabled
      triggers {
        name
        type
        parameters
        staticWebhookUrl
      }
    }
  }
}
    `);

export const useAiAgentDeploymentsQuery = <
      TData = AiAgentDeploymentsQuery,
      TError = unknown
    >(
      variables: AiAgentDeploymentsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentDeploymentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentDeploymentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentDeploymentsQuery, TError, TData>(
      {
    queryKey: ['aiAgentDeployments', variables],
    queryFn: fetcher<AiAgentDeploymentsQuery, AiAgentDeploymentsQueryVariables>(AiAgentDeploymentsDocument, variables),
    ...options
  }
    )};

export const AiAgentGrantsDocument = new TypedDocumentString(`
    query AiAgentGrants($agentId: ID!) {
  aiAgentGrants(agentId: $agentId)
}
    `);

export const useAiAgentGrantsQuery = <
      TData = AiAgentGrantsQuery,
      TError = unknown
    >(
      variables: AiAgentGrantsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentGrantsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentGrantsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentGrantsQuery, TError, TData>(
      {
    queryKey: ['AiAgentGrants', variables],
    queryFn: fetcher<AiAgentGrantsQuery, AiAgentGrantsQueryVariables>(AiAgentGrantsDocument, variables),
    ...options
  }
    )};

export const AiAgentTagsDocument = new TypedDocumentString(`
    query aiAgentTags($workspaceId: ID!) {
  aiAgentTags(workspaceId: $workspaceId) {
    id
    name
  }
}
    `);

export const useAiAgentTagsQuery = <
      TData = AiAgentTagsQuery,
      TError = unknown
    >(
      variables: AiAgentTagsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentTagsQuery, TError, TData>(
      {
    queryKey: ['aiAgentTags', variables],
    queryFn: fetcher<AiAgentTagsQuery, AiAgentTagsQueryVariables>(AiAgentTagsDocument, variables),
    ...options
  }
    )};

export const AiAgentVersionsDocument = new TypedDocumentString(`
    query aiAgentVersions($id: ID!) {
  aiAgentVersions(id: $id) {
    description
    publishedDate
    status
    version
  }
}
    `);

export const useAiAgentVersionsQuery = <
      TData = AiAgentVersionsQuery,
      TError = unknown
    >(
      variables: AiAgentVersionsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentVersionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentVersionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentVersionsQuery, TError, TData>(
      {
    queryKey: ['aiAgentVersions', variables],
    queryFn: fetcher<AiAgentVersionsQuery, AiAgentVersionsQueryVariables>(AiAgentVersionsDocument, variables),
    ...options
  }
    )};

export const AiAgentsDocument = new TypedDocumentString(`
    query aiAgents($workspaceId: ID!) {
  aiAgents(workspaceId: $workspaceId) {
    id
    name
    title
    description
    projectId
    unpublishedChanges
    lastPublishedVersion
    publishedDate
    lastModifiedDate
    visibility
    tags {
      id
      name
    }
    elements {
      id
      kind
    }
    channels {
      id
      channelType
      parameters
    }
  }
}
    `);

export const useAiAgentsQuery = <
      TData = AiAgentsQuery,
      TError = unknown
    >(
      variables: AiAgentsQueryVariables,
      options?: Omit<UseQueryOptions<AiAgentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiAgentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiAgentsQuery, TError, TData>(
      {
    queryKey: ['aiAgents', variables],
    queryFn: fetcher<AiAgentsQuery, AiAgentsQueryVariables>(AiAgentsDocument, variables),
    ...options
  }
    )};

export const CreateAiAgentDocument = new TypedDocumentString(`
    mutation createAiAgent($input: CreateAiAgentInput!) {
  createAiAgent(input: $input) {
    id
  }
}
    `);

export const useCreateAiAgentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiAgentMutation, TError, CreateAiAgentMutationVariables, TContext>) => {
    
    return useMutation<CreateAiAgentMutation, TError, CreateAiAgentMutationVariables, TContext>(
      {
    mutationKey: ['createAiAgent'],
    mutationFn: (variables?: CreateAiAgentMutationVariables) => fetcher<CreateAiAgentMutation, CreateAiAgentMutationVariables>(CreateAiAgentDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentDocument = new TypedDocumentString(`
    mutation deleteAiAgent($id: ID!) {
  deleteAiAgent(id: $id)
}
    `);

export const useDeleteAiAgentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentMutation, TError, DeleteAiAgentMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentMutation, TError, DeleteAiAgentMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgent'],
    mutationFn: (variables?: DeleteAiAgentMutationVariables) => fetcher<DeleteAiAgentMutation, DeleteAiAgentMutationVariables>(DeleteAiAgentDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentChannelDocument = new TypedDocumentString(`
    mutation deleteAiAgentChannel($id: ID!) {
  deleteAiAgentChannel(id: $id)
}
    `);

export const useDeleteAiAgentChannelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentChannelMutation, TError, DeleteAiAgentChannelMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentChannelMutation, TError, DeleteAiAgentChannelMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentChannel'],
    mutationFn: (variables?: DeleteAiAgentChannelMutationVariables) => fetcher<DeleteAiAgentChannelMutation, DeleteAiAgentChannelMutationVariables>(DeleteAiAgentChannelDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiAgentElementDocument = new TypedDocumentString(`
    mutation deleteAiAgentElement($id: ID!) {
  deleteAiAgentElement(id: $id)
}
    `);

export const useDeleteAiAgentElementMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiAgentElementMutation, TError, DeleteAiAgentElementMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiAgentElementMutation, TError, DeleteAiAgentElementMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiAgentElement'],
    mutationFn: (variables?: DeleteAiAgentElementMutationVariables) => fetcher<DeleteAiAgentElementMutation, DeleteAiAgentElementMutationVariables>(DeleteAiAgentElementDocument, variables)(),
    ...options
  }
    )};

export const ExportAiAgentDocument = new TypedDocumentString(`
    query exportAiAgent($id: ID!) {
  exportAiAgent(id: $id)
}
    `);

export const useExportAiAgentQuery = <
      TData = ExportAiAgentQuery,
      TError = unknown
    >(
      variables: ExportAiAgentQueryVariables,
      options?: Omit<UseQueryOptions<ExportAiAgentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ExportAiAgentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ExportAiAgentQuery, TError, TData>(
      {
    queryKey: ['exportAiAgent', variables],
    queryFn: fetcher<ExportAiAgentQuery, ExportAiAgentQueryVariables>(ExportAiAgentDocument, variables),
    ...options
  }
    )};

export const GrantAiAgentAccessDocument = new TypedDocumentString(`
    mutation GrantAiAgentAccess($agentId: ID!, $userId: ID!) {
  grantAiAgentAccess(agentId: $agentId, userId: $userId)
}
    `);

export const useGrantAiAgentAccessMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<GrantAiAgentAccessMutation, TError, GrantAiAgentAccessMutationVariables, TContext>) => {
    
    return useMutation<GrantAiAgentAccessMutation, TError, GrantAiAgentAccessMutationVariables, TContext>(
      {
    mutationKey: ['GrantAiAgentAccess'],
    mutationFn: (variables?: GrantAiAgentAccessMutationVariables) => fetcher<GrantAiAgentAccessMutation, GrantAiAgentAccessMutationVariables>(GrantAiAgentAccessDocument, variables)(),
    ...options
  }
    )};

export const ImportAiAgentDocument = new TypedDocumentString(`
    mutation importAiAgent($workspaceId: ID!, $json: String!) {
  importAiAgent(workspaceId: $workspaceId, json: $json) {
    id
    title
  }
}
    `);

export const useImportAiAgentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportAiAgentMutation, TError, ImportAiAgentMutationVariables, TContext>) => {
    
    return useMutation<ImportAiAgentMutation, TError, ImportAiAgentMutationVariables, TContext>(
      {
    mutationKey: ['importAiAgent'],
    mutationFn: (variables?: ImportAiAgentMutationVariables) => fetcher<ImportAiAgentMutation, ImportAiAgentMutationVariables>(ImportAiAgentDocument, variables)(),
    ...options
  }
    )};

export const PublishAiAgentDocument = new TypedDocumentString(`
    mutation publishAiAgent($id: ID!, $description: String) {
  publishAiAgent(id: $id, description: $description)
}
    `);

export const usePublishAiAgentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<PublishAiAgentMutation, TError, PublishAiAgentMutationVariables, TContext>) => {
    
    return useMutation<PublishAiAgentMutation, TError, PublishAiAgentMutationVariables, TContext>(
      {
    mutationKey: ['publishAiAgent'],
    mutationFn: (variables?: PublishAiAgentMutationVariables) => fetcher<PublishAiAgentMutation, PublishAiAgentMutationVariables>(PublishAiAgentDocument, variables)(),
    ...options
  }
    )};

export const RevokeAiAgentAccessDocument = new TypedDocumentString(`
    mutation RevokeAiAgentAccess($agentId: ID!, $userId: ID!) {
  revokeAiAgentAccess(agentId: $agentId, userId: $userId)
}
    `);

export const useRevokeAiAgentAccessMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RevokeAiAgentAccessMutation, TError, RevokeAiAgentAccessMutationVariables, TContext>) => {
    
    return useMutation<RevokeAiAgentAccessMutation, TError, RevokeAiAgentAccessMutationVariables, TContext>(
      {
    mutationKey: ['RevokeAiAgentAccess'],
    mutationFn: (variables?: RevokeAiAgentAccessMutationVariables) => fetcher<RevokeAiAgentAccessMutation, RevokeAiAgentAccessMutationVariables>(RevokeAiAgentAccessDocument, variables)(),
    ...options
  }
    )};

export const SetAiAgentVisibilityDocument = new TypedDocumentString(`
    mutation SetAiAgentVisibility($agentId: ID!, $visibility: ResourceVisibility!) {
  setAiAgentVisibility(agentId: $agentId, visibility: $visibility)
}
    `);

export const useSetAiAgentVisibilityMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiAgentVisibilityMutation, TError, SetAiAgentVisibilityMutationVariables, TContext>) => {
    
    return useMutation<SetAiAgentVisibilityMutation, TError, SetAiAgentVisibilityMutationVariables, TContext>(
      {
    mutationKey: ['SetAiAgentVisibility'],
    mutationFn: (variables?: SetAiAgentVisibilityMutationVariables) => fetcher<SetAiAgentVisibilityMutation, SetAiAgentVisibilityMutationVariables>(SetAiAgentVisibilityDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentDocument = new TypedDocumentString(`
    mutation updateAiAgent($input: UpdateAiAgentInput!) {
  updateAiAgent(input: $input) {
    id
  }
}
    `);

export const useUpdateAiAgentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentMutation, TError, UpdateAiAgentMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentMutation, TError, UpdateAiAgentMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgent'],
    mutationFn: (variables?: UpdateAiAgentMutationVariables) => fetcher<UpdateAiAgentMutation, UpdateAiAgentMutationVariables>(UpdateAiAgentDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentChannelDocument = new TypedDocumentString(`
    mutation updateAiAgentChannel($input: UpdateAiAgentChannelInput!) {
  updateAiAgentChannel(input: $input)
}
    `);

export const useUpdateAiAgentChannelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentChannelMutation, TError, UpdateAiAgentChannelMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentChannelMutation, TError, UpdateAiAgentChannelMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentChannel'],
    mutationFn: (variables?: UpdateAiAgentChannelMutationVariables) => fetcher<UpdateAiAgentChannelMutation, UpdateAiAgentChannelMutationVariables>(UpdateAiAgentChannelDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentDeploymentTagsDocument = new TypedDocumentString(`
    mutation updateAiAgentDeploymentTags($input: UpdateAiAgentDeploymentTagsInput!) {
  updateAiAgentDeploymentTags(input: $input)
}
    `);

export const useUpdateAiAgentDeploymentTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentDeploymentTagsMutation, TError, UpdateAiAgentDeploymentTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentDeploymentTagsMutation, TError, UpdateAiAgentDeploymentTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentDeploymentTags'],
    mutationFn: (variables?: UpdateAiAgentDeploymentTagsMutationVariables) => fetcher<UpdateAiAgentDeploymentTagsMutation, UpdateAiAgentDeploymentTagsMutationVariables>(UpdateAiAgentDeploymentTagsDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentElementDocument = new TypedDocumentString(`
    mutation updateAiAgentElement($input: UpdateAiAgentElementInput!) {
  updateAiAgentElement(input: $input)
}
    `);

export const useUpdateAiAgentElementMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentElementMutation, TError, UpdateAiAgentElementMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentElementMutation, TError, UpdateAiAgentElementMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentElement'],
    mutationFn: (variables?: UpdateAiAgentElementMutationVariables) => fetcher<UpdateAiAgentElementMutation, UpdateAiAgentElementMutationVariables>(UpdateAiAgentElementDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentSettingsDocument = new TypedDocumentString(`
    mutation updateAiAgentSettings($id: ID!, $settings: Map!) {
  updateAiAgentSettings(id: $id, settings: $settings)
}
    `);

export const useUpdateAiAgentSettingsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentSettingsMutation, TError, UpdateAiAgentSettingsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentSettingsMutation, TError, UpdateAiAgentSettingsMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentSettings'],
    mutationFn: (variables?: UpdateAiAgentSettingsMutationVariables) => fetcher<UpdateAiAgentSettingsMutation, UpdateAiAgentSettingsMutationVariables>(UpdateAiAgentSettingsDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiAgentTagsDocument = new TypedDocumentString(`
    mutation updateAiAgentTags($input: UpdateAiAgentTagsInput!) {
  updateAiAgentTags(input: $input)
}
    `);

export const useUpdateAiAgentTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiAgentTagsMutation, TError, UpdateAiAgentTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiAgentTagsMutation, TError, UpdateAiAgentTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateAiAgentTags'],
    mutationFn: (variables?: UpdateAiAgentTagsMutationVariables) => fetcher<UpdateAiAgentTagsMutation, UpdateAiAgentTagsMutationVariables>(UpdateAiAgentTagsDocument, variables)(),
    ...options
  }
    )};

export const WorkspaceChatAgentsDocument = new TypedDocumentString(`
    query workspaceChatAgents($workspaceId: ID!, $environmentId: ID!) {
  workspaceChatAgents(workspaceId: $workspaceId, environmentId: $environmentId) {
    agentName
    agentTitle
    aiAgentId
    projectDeploymentId
    workflowExecutionId
    workflowLabel
  }
}
    `);

export const useWorkspaceChatAgentsQuery = <
      TData = WorkspaceChatAgentsQuery,
      TError = unknown
    >(
      variables: WorkspaceChatAgentsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceChatAgentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceChatAgentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceChatAgentsQuery, TError, TData>(
      {
    queryKey: ['workspaceChatAgents', variables],
    queryFn: fetcher<WorkspaceChatAgentsQuery, WorkspaceChatAgentsQueryVariables>(WorkspaceChatAgentsDocument, variables),
    ...options
  }
    )};

export const AiDatasetsDocument = new TypedDocumentString(`
    query aiDatasets($workspaceId: ID!) {
  aiDatasets: aiEvalDatasets(workspaceId: $workspaceId) {
    archivedDate
    createdDate
    description
    id
    name
    tags
    workspaceId
  }
}
    `);

export const useAiDatasetsQuery = <
      TData = AiDatasetsQuery,
      TError = unknown
    >(
      variables: AiDatasetsQueryVariables,
      options?: Omit<UseQueryOptions<AiDatasetsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiDatasetsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiDatasetsQuery, TError, TData>(
      {
    queryKey: ['aiDatasets', variables],
    queryFn: fetcher<AiDatasetsQuery, AiDatasetsQueryVariables>(AiDatasetsDocument, variables),
    ...options
  }
    )};

export const AiDatasetVersionsDocument = new TypedDocumentString(`
    query aiDatasetVersions($datasetId: ID!) {
  aiDatasetVersions: aiEvalDatasetVersions(datasetId: $datasetId) {
    createdDate
    datasetId
    frozen
    id
    itemCount
    label
  }
}
    `);

export const useAiDatasetVersionsQuery = <
      TData = AiDatasetVersionsQuery,
      TError = unknown
    >(
      variables: AiDatasetVersionsQueryVariables,
      options?: Omit<UseQueryOptions<AiDatasetVersionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiDatasetVersionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiDatasetVersionsQuery, TError, TData>(
      {
    queryKey: ['aiDatasetVersions', variables],
    queryFn: fetcher<AiDatasetVersionsQuery, AiDatasetVersionsQueryVariables>(AiDatasetVersionsDocument, variables),
    ...options
  }
    )};

export const AiDatasetItemsDocument = new TypedDocumentString(`
    query aiDatasetItems($versionId: ID!) {
  aiDatasetItems: aiEvalDatasetItems(versionId: $versionId) {
    createdDate
    datasetVersionId
    expectedOutput
    id
    input
    metadata
    sourceTraceId
  }
}
    `);

export const useAiDatasetItemsQuery = <
      TData = AiDatasetItemsQuery,
      TError = unknown
    >(
      variables: AiDatasetItemsQueryVariables,
      options?: Omit<UseQueryOptions<AiDatasetItemsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiDatasetItemsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiDatasetItemsQuery, TError, TData>(
      {
    queryKey: ['aiDatasetItems', variables],
    queryFn: fetcher<AiDatasetItemsQuery, AiDatasetItemsQueryVariables>(AiDatasetItemsDocument, variables),
    ...options
  }
    )};

export const AiEvalRulesDocument = new TypedDocumentString(`
    query aiEvalRules($workspaceId: ID!) {
  aiEvalRules(workspaceId: $workspaceId) {
    createdDate
    delaySeconds
    enabled
    filters
    id
    lastModifiedDate
    model
    name
    projectId
    promptTemplate
    samplingRate
    scoreConfigId
    version
    workspaceId
  }
}
    `);

export const useAiEvalRulesQuery = <
      TData = AiEvalRulesQuery,
      TError = unknown
    >(
      variables: AiEvalRulesQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalRulesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalRulesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalRulesQuery, TError, TData>(
      {
    queryKey: ['aiEvalRules', variables],
    queryFn: fetcher<AiEvalRulesQuery, AiEvalRulesQueryVariables>(AiEvalRulesDocument, variables),
    ...options
  }
    )};

export const AiEvalRuleDocument = new TypedDocumentString(`
    query aiEvalRule($id: ID!) {
  aiEvalRule(id: $id) {
    createdDate
    delaySeconds
    enabled
    filters
    id
    lastModifiedDate
    model
    name
    projectId
    promptTemplate
    samplingRate
    scoreConfigId
    version
    workspaceId
  }
}
    `);

export const useAiEvalRuleQuery = <
      TData = AiEvalRuleQuery,
      TError = unknown
    >(
      variables: AiEvalRuleQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalRuleQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalRuleQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalRuleQuery, TError, TData>(
      {
    queryKey: ['aiEvalRule', variables],
    queryFn: fetcher<AiEvalRuleQuery, AiEvalRuleQueryVariables>(AiEvalRuleDocument, variables),
    ...options
  }
    )};

export const AiEvalExecutionsDocument = new TypedDocumentString(`
    query aiEvalExecutions($evalRuleId: ID!) {
  aiEvalExecutions(evalRuleId: $evalRuleId) {
    createdDate
    errorMessage
    evalRuleId
    id
    scoreId
    status
    traceId
  }
}
    `);

export const useAiEvalExecutionsQuery = <
      TData = AiEvalExecutionsQuery,
      TError = unknown
    >(
      variables: AiEvalExecutionsQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalExecutionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalExecutionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalExecutionsQuery, TError, TData>(
      {
    queryKey: ['aiEvalExecutions', variables],
    queryFn: fetcher<AiEvalExecutionsQuery, AiEvalExecutionsQueryVariables>(AiEvalExecutionsDocument, variables),
    ...options
  }
    )};

export const CreateAiEvalRuleDocument = new TypedDocumentString(`
    mutation createAiEvalRule($delaySeconds: Int, $enabled: Boolean!, $filters: String, $model: String!, $name: String!, $projectId: ID, $promptTemplate: String!, $samplingRate: Float!, $scoreConfigId: ID!, $workspaceId: ID!) {
  createAiEvalRule(
    delaySeconds: $delaySeconds
    enabled: $enabled
    filters: $filters
    model: $model
    name: $name
    projectId: $projectId
    promptTemplate: $promptTemplate
    samplingRate: $samplingRate
    scoreConfigId: $scoreConfigId
    workspaceId: $workspaceId
  ) {
    id
    name
  }
}
    `);

export const useCreateAiEvalRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiEvalRuleMutation, TError, CreateAiEvalRuleMutationVariables, TContext>) => {
    
    return useMutation<CreateAiEvalRuleMutation, TError, CreateAiEvalRuleMutationVariables, TContext>(
      {
    mutationKey: ['createAiEvalRule'],
    mutationFn: (variables?: CreateAiEvalRuleMutationVariables) => fetcher<CreateAiEvalRuleMutation, CreateAiEvalRuleMutationVariables>(CreateAiEvalRuleDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiEvalRuleDocument = new TypedDocumentString(`
    mutation deleteAiEvalRule($id: ID!) {
  deleteAiEvalRule(id: $id)
}
    `);

export const useDeleteAiEvalRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiEvalRuleMutation, TError, DeleteAiEvalRuleMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiEvalRuleMutation, TError, DeleteAiEvalRuleMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiEvalRule'],
    mutationFn: (variables?: DeleteAiEvalRuleMutationVariables) => fetcher<DeleteAiEvalRuleMutation, DeleteAiEvalRuleMutationVariables>(DeleteAiEvalRuleDocument, variables)(),
    ...options
  }
    )};

export const RunAiEvalRuleOnHistoricalTracesDocument = new TypedDocumentString(`
    mutation runAiEvalRuleOnHistoricalTraces($ruleId: ID!, $startDate: Long!, $endDate: Long!) {
  runAiEvalRuleOnHistoricalTraces(
    ruleId: $ruleId
    startDate: $startDate
    endDate: $endDate
  )
}
    `);

export const useRunAiEvalRuleOnHistoricalTracesMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RunAiEvalRuleOnHistoricalTracesMutation, TError, RunAiEvalRuleOnHistoricalTracesMutationVariables, TContext>) => {
    
    return useMutation<RunAiEvalRuleOnHistoricalTracesMutation, TError, RunAiEvalRuleOnHistoricalTracesMutationVariables, TContext>(
      {
    mutationKey: ['runAiEvalRuleOnHistoricalTraces'],
    mutationFn: (variables?: RunAiEvalRuleOnHistoricalTracesMutationVariables) => fetcher<RunAiEvalRuleOnHistoricalTracesMutation, RunAiEvalRuleOnHistoricalTracesMutationVariables>(RunAiEvalRuleOnHistoricalTracesDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiEvalRuleDocument = new TypedDocumentString(`
    mutation updateAiEvalRule($delaySeconds: Int, $enabled: Boolean!, $filters: String, $id: ID!, $model: String!, $name: String!, $promptTemplate: String!, $samplingRate: Float!, $scoreConfigId: ID!) {
  updateAiEvalRule(
    delaySeconds: $delaySeconds
    enabled: $enabled
    filters: $filters
    id: $id
    model: $model
    name: $name
    promptTemplate: $promptTemplate
    samplingRate: $samplingRate
    scoreConfigId: $scoreConfigId
  ) {
    id
    name
  }
}
    `);

export const useUpdateAiEvalRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiEvalRuleMutation, TError, UpdateAiEvalRuleMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiEvalRuleMutation, TError, UpdateAiEvalRuleMutationVariables, TContext>(
      {
    mutationKey: ['updateAiEvalRule'],
    mutationFn: (variables?: UpdateAiEvalRuleMutationVariables) => fetcher<UpdateAiEvalRuleMutation, UpdateAiEvalRuleMutationVariables>(UpdateAiEvalRuleDocument, variables)(),
    ...options
  }
    )};

export const AiEvalScoreConfigsDocument = new TypedDocumentString(`
    query aiEvalScoreConfigs($workspaceId: ID!) {
  aiEvalScoreConfigs(workspaceId: $workspaceId) {
    categories
    createdDate
    dataType
    description
    id
    lastModifiedDate
    maxValue
    minValue
    name
    version
    workspaceId
  }
}
    `);

export const useAiEvalScoreConfigsQuery = <
      TData = AiEvalScoreConfigsQuery,
      TError = unknown
    >(
      variables: AiEvalScoreConfigsQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoreConfigsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoreConfigsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoreConfigsQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoreConfigs', variables],
    queryFn: fetcher<AiEvalScoreConfigsQuery, AiEvalScoreConfigsQueryVariables>(AiEvalScoreConfigsDocument, variables),
    ...options
  }
    )};

export const AiEvalScoreConfigDocument = new TypedDocumentString(`
    query aiEvalScoreConfig($id: ID!) {
  aiEvalScoreConfig(id: $id) {
    categories
    createdDate
    dataType
    description
    id
    lastModifiedDate
    maxValue
    minValue
    name
    version
    workspaceId
  }
}
    `);

export const useAiEvalScoreConfigQuery = <
      TData = AiEvalScoreConfigQuery,
      TError = unknown
    >(
      variables: AiEvalScoreConfigQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoreConfigQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoreConfigQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoreConfigQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoreConfig', variables],
    queryFn: fetcher<AiEvalScoreConfigQuery, AiEvalScoreConfigQueryVariables>(AiEvalScoreConfigDocument, variables),
    ...options
  }
    )};

export const CreateAiEvalScoreConfigDocument = new TypedDocumentString(`
    mutation createAiEvalScoreConfig($categories: String, $dataType: AiEvalScoreDataType, $description: String, $maxValue: Float, $minValue: Float, $name: String!, $workspaceId: ID!) {
  createAiEvalScoreConfig(
    categories: $categories
    dataType: $dataType
    description: $description
    maxValue: $maxValue
    minValue: $minValue
    name: $name
    workspaceId: $workspaceId
  ) {
    id
    name
  }
}
    `);

export const useCreateAiEvalScoreConfigMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiEvalScoreConfigMutation, TError, CreateAiEvalScoreConfigMutationVariables, TContext>) => {
    
    return useMutation<CreateAiEvalScoreConfigMutation, TError, CreateAiEvalScoreConfigMutationVariables, TContext>(
      {
    mutationKey: ['createAiEvalScoreConfig'],
    mutationFn: (variables?: CreateAiEvalScoreConfigMutationVariables) => fetcher<CreateAiEvalScoreConfigMutation, CreateAiEvalScoreConfigMutationVariables>(CreateAiEvalScoreConfigDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiEvalScoreConfigDocument = new TypedDocumentString(`
    mutation deleteAiEvalScoreConfig($id: ID!) {
  deleteAiEvalScoreConfig(id: $id)
}
    `);

export const useDeleteAiEvalScoreConfigMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiEvalScoreConfigMutation, TError, DeleteAiEvalScoreConfigMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiEvalScoreConfigMutation, TError, DeleteAiEvalScoreConfigMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiEvalScoreConfig'],
    mutationFn: (variables?: DeleteAiEvalScoreConfigMutationVariables) => fetcher<DeleteAiEvalScoreConfigMutation, DeleteAiEvalScoreConfigMutationVariables>(DeleteAiEvalScoreConfigDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiEvalScoreConfigDocument = new TypedDocumentString(`
    mutation updateAiEvalScoreConfig($categories: String, $dataType: AiEvalScoreDataType, $description: String, $id: ID!, $maxValue: Float, $minValue: Float, $name: String!) {
  updateAiEvalScoreConfig(
    categories: $categories
    dataType: $dataType
    description: $description
    id: $id
    maxValue: $maxValue
    minValue: $minValue
    name: $name
  ) {
    id
    name
  }
}
    `);

export const useUpdateAiEvalScoreConfigMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiEvalScoreConfigMutation, TError, UpdateAiEvalScoreConfigMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiEvalScoreConfigMutation, TError, UpdateAiEvalScoreConfigMutationVariables, TContext>(
      {
    mutationKey: ['updateAiEvalScoreConfig'],
    mutationFn: (variables?: UpdateAiEvalScoreConfigMutationVariables) => fetcher<UpdateAiEvalScoreConfigMutation, UpdateAiEvalScoreConfigMutationVariables>(UpdateAiEvalScoreConfigDocument, variables)(),
    ...options
  }
    )};

export const AiEvalScoresDocument = new TypedDocumentString(`
    query aiEvalScores($workspaceId: ID!) {
  aiEvalScores(workspaceId: $workspaceId) {
    comment
    createdBy
    createdDate
    dataType
    evalRuleId
    id
    name
    source
    spanId
    stringValue
    traceId
    value
    workspaceId
  }
}
    `);

export const useAiEvalScoresQuery = <
      TData = AiEvalScoresQuery,
      TError = unknown
    >(
      variables: AiEvalScoresQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoresQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoresQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoresQuery, TError, TData>(
      {
    queryKey: ['aiEvalScores', variables],
    queryFn: fetcher<AiEvalScoresQuery, AiEvalScoresQueryVariables>(AiEvalScoresDocument, variables),
    ...options
  }
    )};

export const AiEvalScoresByTraceDocument = new TypedDocumentString(`
    query aiEvalScoresByTrace($traceId: ID!) {
  aiEvalScoresByTrace(traceId: $traceId) {
    comment
    createdBy
    createdDate
    dataType
    evalRuleId
    id
    name
    source
    spanId
    stringValue
    traceId
    value
    workspaceId
  }
}
    `);

export const useAiEvalScoresByTraceQuery = <
      TData = AiEvalScoresByTraceQuery,
      TError = unknown
    >(
      variables: AiEvalScoresByTraceQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoresByTraceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoresByTraceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoresByTraceQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoresByTrace', variables],
    queryFn: fetcher<AiEvalScoresByTraceQuery, AiEvalScoresByTraceQueryVariables>(AiEvalScoresByTraceDocument, variables),
    ...options
  }
    )};

export const CreateAiEvalScoreDocument = new TypedDocumentString(`
    mutation createAiEvalScore($comment: String, $dataType: AiEvalScoreDataType!, $name: String!, $source: AiEvalScoreSource!, $spanId: ID, $stringValue: String, $traceId: ID!, $value: Float, $workspaceId: ID!) {
  createAiEvalScore(
    comment: $comment
    dataType: $dataType
    name: $name
    source: $source
    spanId: $spanId
    stringValue: $stringValue
    traceId: $traceId
    value: $value
    workspaceId: $workspaceId
  ) {
    id
    name
  }
}
    `);

export const useCreateAiEvalScoreMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiEvalScoreMutation, TError, CreateAiEvalScoreMutationVariables, TContext>) => {
    
    return useMutation<CreateAiEvalScoreMutation, TError, CreateAiEvalScoreMutationVariables, TContext>(
      {
    mutationKey: ['createAiEvalScore'],
    mutationFn: (variables?: CreateAiEvalScoreMutationVariables) => fetcher<CreateAiEvalScoreMutation, CreateAiEvalScoreMutationVariables>(CreateAiEvalScoreDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiEvalScoreDocument = new TypedDocumentString(`
    mutation deleteAiEvalScore($id: ID!) {
  deleteAiEvalScore(id: $id)
}
    `);

export const useDeleteAiEvalScoreMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiEvalScoreMutation, TError, DeleteAiEvalScoreMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiEvalScoreMutation, TError, DeleteAiEvalScoreMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiEvalScore'],
    mutationFn: (variables?: DeleteAiEvalScoreMutationVariables) => fetcher<DeleteAiEvalScoreMutation, DeleteAiEvalScoreMutationVariables>(DeleteAiEvalScoreDocument, variables)(),
    ...options
  }
    )};

export const AiEvalScoreAnalyticsDocument = new TypedDocumentString(`
    query aiEvalScoreAnalytics($workspaceId: ID!, $startDate: Long!, $endDate: Long!) {
  aiEvalScoreAnalytics(
    workspaceId: $workspaceId
    startDate: $startDate
    endDate: $endDate
  ) {
    average
    count
    dataType
    distribution {
      count
      value
    }
    max
    min
    name
  }
}
    `);

export const useAiEvalScoreAnalyticsQuery = <
      TData = AiEvalScoreAnalyticsQuery,
      TError = unknown
    >(
      variables: AiEvalScoreAnalyticsQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoreAnalyticsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoreAnalyticsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoreAnalyticsQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoreAnalytics', variables],
    queryFn: fetcher<AiEvalScoreAnalyticsQuery, AiEvalScoreAnalyticsQueryVariables>(AiEvalScoreAnalyticsDocument, variables),
    ...options
  }
    )};

export const AiEvalScoreTrendDocument = new TypedDocumentString(`
    query aiEvalScoreTrend($workspaceId: ID!, $name: String!, $startDate: Long!, $endDate: Long!) {
  aiEvalScoreTrend(
    workspaceId: $workspaceId
    name: $name
    startDate: $startDate
    endDate: $endDate
  ) {
    average
    count
    day
  }
}
    `);

export const useAiEvalScoreTrendQuery = <
      TData = AiEvalScoreTrendQuery,
      TError = unknown
    >(
      variables: AiEvalScoreTrendQueryVariables,
      options?: Omit<UseQueryOptions<AiEvalScoreTrendQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiEvalScoreTrendQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiEvalScoreTrendQuery, TError, TData>(
      {
    queryKey: ['aiEvalScoreTrend', variables],
    queryFn: fetcher<AiEvalScoreTrendQuery, AiEvalScoreTrendQueryVariables>(AiEvalScoreTrendDocument, variables),
    ...options
  }
    )};

export const AiExperimentComparisonDocument = new TypedDocumentString(`
    query aiExperimentComparison($experimentIds: [ID!]!) {
  experimentComparison(experimentIds: $experimentIds) {
    aggregateScoreDeltas {
      deltas {
        average
        count
        experimentId
      }
      scoreName
    }
    experiments {
      averageLatencyMs
      completedRuns
      failedRuns
      id
      model
      totalCost
      totalRuns
    }
    rows {
      datasetItemId
      runsByExperiment {
        cost
        experimentId
        latencyMs
        runId
        scores {
          dataType
          name
          stringValue
          value
        }
        status
        traceId
      }
    }
  }
}
    `);

export const useAiExperimentComparisonQuery = <
      TData = AiExperimentComparisonQuery,
      TError = unknown
    >(
      variables: AiExperimentComparisonQueryVariables,
      options?: Omit<UseQueryOptions<AiExperimentComparisonQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiExperimentComparisonQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiExperimentComparisonQuery, TError, TData>(
      {
    queryKey: ['aiExperimentComparison', variables],
    queryFn: fetcher<AiExperimentComparisonQuery, AiExperimentComparisonQueryVariables>(AiExperimentComparisonDocument, variables),
    ...options
  }
    )};

export const AiExperimentsDocument = new TypedDocumentString(`
    query aiExperiments($workspaceId: ID!) {
  aiExperiments: aiEvalExperiments(workspaceId: $workspaceId) {
    completedDate
    completedRuns
    createdDate
    datasetVersionId
    failedRuns
    id
    metadata
    model
    promptVersionId
    startedDate
    status
    stopRequested
    totalRuns
  }
}
    `);

export const useAiExperimentsQuery = <
      TData = AiExperimentsQuery,
      TError = unknown
    >(
      variables: AiExperimentsQueryVariables,
      options?: Omit<UseQueryOptions<AiExperimentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiExperimentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiExperimentsQuery, TError, TData>(
      {
    queryKey: ['aiExperiments', variables],
    queryFn: fetcher<AiExperimentsQuery, AiExperimentsQueryVariables>(AiExperimentsDocument, variables),
    ...options
  }
    )};

export const AiExperimentRunsDocument = new TypedDocumentString(`
    query aiExperimentRuns($experimentId: ID!) {
  aiExperimentRuns: aiEvalExperimentRuns(experimentId: $experimentId) {
    cost
    createdDate
    datasetItemId
    errorMessage
    experimentId
    id
    latencyMs
    status
    traceId
  }
}
    `);

export const useAiExperimentRunsQuery = <
      TData = AiExperimentRunsQuery,
      TError = unknown
    >(
      variables: AiExperimentRunsQueryVariables,
      options?: Omit<UseQueryOptions<AiExperimentRunsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiExperimentRunsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiExperimentRunsQuery, TError, TData>(
      {
    queryKey: ['aiExperimentRuns', variables],
    queryFn: fetcher<AiExperimentRunsQuery, AiExperimentRunsQueryVariables>(AiExperimentRunsDocument, variables),
    ...options
  }
    )};

export const AiExperimentRunByTraceIdDocument = new TypedDocumentString(`
    query aiExperimentRunByTraceId($traceId: ID!) {
  aiExperimentRunByTraceId: aiEvalExperimentRunByTraceId(traceId: $traceId) {
    cost
    createdDate
    datasetItemId
    errorMessage
    experimentId
    id
    latencyMs
    status
    traceId
  }
}
    `);

export const useAiExperimentRunByTraceIdQuery = <
      TData = AiExperimentRunByTraceIdQuery,
      TError = unknown
    >(
      variables: AiExperimentRunByTraceIdQueryVariables,
      options?: Omit<UseQueryOptions<AiExperimentRunByTraceIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiExperimentRunByTraceIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiExperimentRunByTraceIdQuery, TError, TData>(
      {
    queryKey: ['aiExperimentRunByTraceId', variables],
    queryFn: fetcher<AiExperimentRunByTraceIdQuery, AiExperimentRunByTraceIdQueryVariables>(AiExperimentRunByTraceIdDocument, variables),
    ...options
  }
    )};

export const AiGatewayBudgetDocument = new TypedDocumentString(`
    query aiGatewayBudget($workspaceId: ID!) {
  aiGatewayBudget(workspaceId: $workspaceId) {
    alertThreshold
    amount
    createdDate
    enabled
    enforcementMode
    id
    lastModifiedDate
    period
    version
  }
}
    `);

export const useAiGatewayBudgetQuery = <
      TData = AiGatewayBudgetQuery,
      TError = unknown
    >(
      variables: AiGatewayBudgetQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayBudgetQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayBudgetQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayBudgetQuery, TError, TData>(
      {
    queryKey: ['aiGatewayBudget', variables],
    queryFn: fetcher<AiGatewayBudgetQuery, AiGatewayBudgetQueryVariables>(AiGatewayBudgetDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayBudgetDocument = new TypedDocumentString(`
    mutation createAiGatewayBudget($input: CreateAiGatewayBudgetInput!) {
  createAiGatewayBudget(input: $input) {
    alertThreshold
    amount
    createdDate
    enabled
    enforcementMode
    id
    lastModifiedDate
    period
    version
  }
}
    `);

export const useCreateAiGatewayBudgetMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayBudgetMutation, TError, CreateAiGatewayBudgetMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayBudgetMutation, TError, CreateAiGatewayBudgetMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayBudget'],
    mutationFn: (variables?: CreateAiGatewayBudgetMutationVariables) => fetcher<CreateAiGatewayBudgetMutation, CreateAiGatewayBudgetMutationVariables>(CreateAiGatewayBudgetDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayBudgetDocument = new TypedDocumentString(`
    mutation updateAiGatewayBudget($id: ID!, $input: UpdateAiGatewayBudgetInput!) {
  updateAiGatewayBudget(id: $id, input: $input) {
    alertThreshold
    amount
    createdDate
    enabled
    enforcementMode
    id
    lastModifiedDate
    period
    version
  }
}
    `);

export const useUpdateAiGatewayBudgetMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayBudgetMutation, TError, UpdateAiGatewayBudgetMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayBudgetMutation, TError, UpdateAiGatewayBudgetMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayBudget'],
    mutationFn: (variables?: UpdateAiGatewayBudgetMutationVariables) => fetcher<UpdateAiGatewayBudgetMutation, UpdateAiGatewayBudgetMutationVariables>(UpdateAiGatewayBudgetDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayBudgetDocument = new TypedDocumentString(`
    mutation deleteAiGatewayBudget($id: ID!) {
  deleteAiGatewayBudget(id: $id)
}
    `);

export const useDeleteAiGatewayBudgetMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayBudgetMutation, TError, DeleteAiGatewayBudgetMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayBudgetMutation, TError, DeleteAiGatewayBudgetMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayBudget'],
    mutationFn: (variables?: DeleteAiGatewayBudgetMutationVariables) => fetcher<DeleteAiGatewayBudgetMutation, DeleteAiGatewayBudgetMutationVariables>(DeleteAiGatewayBudgetDocument, variables)(),
    ...options
  }
    )};

export const PlaygroundChatCompletionDocument = new TypedDocumentString(`
    mutation playgroundChatCompletion($input: PlaygroundChatCompletionInput!) {
  playgroundChatCompletion(input: $input) {
    completionTokens
    content
    cost
    finishReason
    latencyMs
    model
    promptTokens
    totalTokens
    traceId
  }
}
    `);

export const usePlaygroundChatCompletionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<PlaygroundChatCompletionMutation, TError, PlaygroundChatCompletionMutationVariables, TContext>) => {
    
    return useMutation<PlaygroundChatCompletionMutation, TError, PlaygroundChatCompletionMutationVariables, TContext>(
      {
    mutationKey: ['playgroundChatCompletion'],
    mutationFn: (variables?: PlaygroundChatCompletionMutationVariables) => fetcher<PlaygroundChatCompletionMutation, PlaygroundChatCompletionMutationVariables>(PlaygroundChatCompletionDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayProjectSettingsDocument = new TypedDocumentString(`
    query aiGatewayProjectSettings($projectId: ID!) {
  aiGatewayProjectSettings(projectId: $projectId) {
    blockedTerms
    injectionDetectionEnabled
    moderationEnabled
    projectId
    redactPii
    redactSecrets
    scanResponses
  }
}
    `);

export const useAiGatewayProjectSettingsQuery = <
      TData = AiGatewayProjectSettingsQuery,
      TError = unknown
    >(
      variables: AiGatewayProjectSettingsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayProjectSettingsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayProjectSettingsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayProjectSettingsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayProjectSettings', variables],
    queryFn: fetcher<AiGatewayProjectSettingsQuery, AiGatewayProjectSettingsQueryVariables>(AiGatewayProjectSettingsDocument, variables),
    ...options
  }
    )};

export const UpdateAiGatewayProjectSettingsDocument = new TypedDocumentString(`
    mutation updateAiGatewayProjectSettings($input: AiGatewayProjectSettingsInput!) {
  updateAiGatewayProjectSettings(input: $input) {
    blockedTerms
    injectionDetectionEnabled
    moderationEnabled
    projectId
    redactPii
    redactSecrets
    scanResponses
  }
}
    `);

export const useUpdateAiGatewayProjectSettingsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayProjectSettingsMutation, TError, UpdateAiGatewayProjectSettingsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayProjectSettingsMutation, TError, UpdateAiGatewayProjectSettingsMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayProjectSettings'],
    mutationFn: (variables?: UpdateAiGatewayProjectSettingsMutationVariables) => fetcher<UpdateAiGatewayProjectSettingsMutation, UpdateAiGatewayProjectSettingsMutationVariables>(UpdateAiGatewayProjectSettingsDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayProjectsDocument = new TypedDocumentString(`
    query aiGatewayProjects($workspaceId: ID!) {
  aiGatewayProjects(workspaceId: $workspaceId) {
    cachingEnabled
    cacheTtlMinutes
    compressionEnabled
    createdDate
    description
    id
    lastModifiedDate
    logRetentionDays
    name
    retryMaxAttempts
    routingPolicyId
    slug
    timeoutSeconds
    version
  }
}
    `);

export const useAiGatewayProjectsQuery = <
      TData = AiGatewayProjectsQuery,
      TError = unknown
    >(
      variables: AiGatewayProjectsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayProjectsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayProjectsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayProjectsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayProjects', variables],
    queryFn: fetcher<AiGatewayProjectsQuery, AiGatewayProjectsQueryVariables>(AiGatewayProjectsDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayProjectDocument = new TypedDocumentString(`
    mutation createAiGatewayProject($input: CreateAiGatewayProjectInput!) {
  createAiGatewayProject(input: $input) {
    id
    name
    slug
  }
}
    `);

export const useCreateAiGatewayProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayProjectMutation, TError, CreateAiGatewayProjectMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayProjectMutation, TError, CreateAiGatewayProjectMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayProject'],
    mutationFn: (variables?: CreateAiGatewayProjectMutationVariables) => fetcher<CreateAiGatewayProjectMutation, CreateAiGatewayProjectMutationVariables>(CreateAiGatewayProjectDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayProjectDocument = new TypedDocumentString(`
    mutation updateAiGatewayProject($id: ID!, $input: UpdateAiGatewayProjectInput!) {
  updateAiGatewayProject(id: $id, input: $input) {
    id
    name
    slug
  }
}
    `);

export const useUpdateAiGatewayProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayProjectMutation, TError, UpdateAiGatewayProjectMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayProjectMutation, TError, UpdateAiGatewayProjectMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayProject'],
    mutationFn: (variables?: UpdateAiGatewayProjectMutationVariables) => fetcher<UpdateAiGatewayProjectMutation, UpdateAiGatewayProjectMutationVariables>(UpdateAiGatewayProjectDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayProjectDocument = new TypedDocumentString(`
    mutation deleteAiGatewayProject($id: ID!) {
  deleteAiGatewayProject(id: $id)
}
    `);

export const useDeleteAiGatewayProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayProjectMutation, TError, DeleteAiGatewayProjectMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayProjectMutation, TError, DeleteAiGatewayProjectMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayProject'],
    mutationFn: (variables?: DeleteAiGatewayProjectMutationVariables) => fetcher<DeleteAiGatewayProjectMutation, DeleteAiGatewayProjectMutationVariables>(DeleteAiGatewayProjectDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayProvidersDocument = new TypedDocumentString(`
    query aiGatewayProviders {
  aiGatewayProviders {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useAiGatewayProvidersQuery = <
      TData = AiGatewayProvidersQuery,
      TError = unknown
    >(
      variables?: AiGatewayProvidersQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayProvidersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayProvidersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayProvidersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiGatewayProviders'] : ['aiGatewayProviders', variables],
    queryFn: fetcher<AiGatewayProvidersQuery, AiGatewayProvidersQueryVariables>(AiGatewayProvidersDocument, variables),
    ...options
  }
    )};

export const AiGatewayProviderDocument = new TypedDocumentString(`
    query aiGatewayProvider($id: ID!) {
  aiGatewayProvider(id: $id) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useAiGatewayProviderQuery = <
      TData = AiGatewayProviderQuery,
      TError = unknown
    >(
      variables: AiGatewayProviderQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayProviderQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayProviderQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayProviderQuery, TError, TData>(
      {
    queryKey: ['aiGatewayProvider', variables],
    queryFn: fetcher<AiGatewayProviderQuery, AiGatewayProviderQueryVariables>(AiGatewayProviderDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayProviderDocument = new TypedDocumentString(`
    mutation createAiGatewayProvider($input: CreateAiGatewayProviderInput!) {
  createAiGatewayProvider(input: $input) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useCreateAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayProviderMutation, TError, CreateAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayProviderMutation, TError, CreateAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayProvider'],
    mutationFn: (variables?: CreateAiGatewayProviderMutationVariables) => fetcher<CreateAiGatewayProviderMutation, CreateAiGatewayProviderMutationVariables>(CreateAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayProviderDocument = new TypedDocumentString(`
    mutation updateAiGatewayProvider($id: ID!, $input: UpdateAiGatewayProviderInput!) {
  updateAiGatewayProvider(id: $id, input: $input) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useUpdateAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayProviderMutation, TError, UpdateAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayProviderMutation, TError, UpdateAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayProvider'],
    mutationFn: (variables?: UpdateAiGatewayProviderMutationVariables) => fetcher<UpdateAiGatewayProviderMutation, UpdateAiGatewayProviderMutationVariables>(UpdateAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayProviderDocument = new TypedDocumentString(`
    mutation deleteAiGatewayProvider($id: ID!) {
  deleteAiGatewayProvider(id: $id)
}
    `);

export const useDeleteAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayProviderMutation, TError, DeleteAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayProviderMutation, TError, DeleteAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayProvider'],
    mutationFn: (variables?: DeleteAiGatewayProviderMutationVariables) => fetcher<DeleteAiGatewayProviderMutation, DeleteAiGatewayProviderMutationVariables>(DeleteAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayRateLimitsDocument = new TypedDocumentString(`
    query aiGatewayRateLimits($workspaceId: ID!) {
  aiGatewayRateLimits(workspaceId: $workspaceId) {
    createdDate
    enabled
    id
    lastModifiedDate
    limitType
    limitValue
    name
    projectId
    propertyKey
    scope
    version
    windowSeconds
  }
}
    `);

export const useAiGatewayRateLimitsQuery = <
      TData = AiGatewayRateLimitsQuery,
      TError = unknown
    >(
      variables: AiGatewayRateLimitsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayRateLimitsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayRateLimitsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayRateLimitsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayRateLimits', variables],
    queryFn: fetcher<AiGatewayRateLimitsQuery, AiGatewayRateLimitsQueryVariables>(AiGatewayRateLimitsDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayRateLimitDocument = new TypedDocumentString(`
    mutation createAiGatewayRateLimit($input: CreateAiGatewayRateLimitInput!) {
  createAiGatewayRateLimit(input: $input) {
    createdDate
    enabled
    id
    lastModifiedDate
    limitType
    limitValue
    name
    projectId
    propertyKey
    scope
    version
    windowSeconds
  }
}
    `);

export const useCreateAiGatewayRateLimitMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayRateLimitMutation, TError, CreateAiGatewayRateLimitMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayRateLimitMutation, TError, CreateAiGatewayRateLimitMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayRateLimit'],
    mutationFn: (variables?: CreateAiGatewayRateLimitMutationVariables) => fetcher<CreateAiGatewayRateLimitMutation, CreateAiGatewayRateLimitMutationVariables>(CreateAiGatewayRateLimitDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayRateLimitDocument = new TypedDocumentString(`
    mutation updateAiGatewayRateLimit($id: ID!, $input: UpdateAiGatewayRateLimitInput!) {
  updateAiGatewayRateLimit(id: $id, input: $input) {
    createdDate
    enabled
    id
    lastModifiedDate
    limitType
    limitValue
    name
    projectId
    propertyKey
    scope
    version
    windowSeconds
  }
}
    `);

export const useUpdateAiGatewayRateLimitMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayRateLimitMutation, TError, UpdateAiGatewayRateLimitMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayRateLimitMutation, TError, UpdateAiGatewayRateLimitMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayRateLimit'],
    mutationFn: (variables?: UpdateAiGatewayRateLimitMutationVariables) => fetcher<UpdateAiGatewayRateLimitMutation, UpdateAiGatewayRateLimitMutationVariables>(UpdateAiGatewayRateLimitDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayRateLimitDocument = new TypedDocumentString(`
    mutation deleteAiGatewayRateLimit($id: ID!) {
  deleteAiGatewayRateLimit(id: $id)
}
    `);

export const useDeleteAiGatewayRateLimitMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayRateLimitMutation, TError, DeleteAiGatewayRateLimitMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayRateLimitMutation, TError, DeleteAiGatewayRateLimitMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayRateLimit'],
    mutationFn: (variables?: DeleteAiGatewayRateLimitMutationVariables) => fetcher<DeleteAiGatewayRateLimitMutation, DeleteAiGatewayRateLimitMutationVariables>(DeleteAiGatewayRateLimitDocument, variables)(),
    ...options
  }
    )};

export const AiGatewayRequestLogsDocument = new TypedDocumentString(`
    query aiGatewayRequestLogs($startDate: Long!, $endDate: Long!) {
  aiGatewayRequestLogs(startDate: $startDate, endDate: $endDate) {
    apiKeyId
    cacheHit
    cost
    createdDate
    errorMessage
    id
    inputTokens
    latencyMs
    outputTokens
    requestId
    requestedModel
    routedModel
    routedProvider
    routingPolicyId
    routingStrategy
    status
  }
}
    `);

export const useAiGatewayRequestLogsQuery = <
      TData = AiGatewayRequestLogsQuery,
      TError = unknown
    >(
      variables: AiGatewayRequestLogsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayRequestLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayRequestLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayRequestLogsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayRequestLogs', variables],
    queryFn: fetcher<AiGatewayRequestLogsQuery, AiGatewayRequestLogsQueryVariables>(AiGatewayRequestLogsDocument, variables),
    ...options
  }
    )};

export const AiGatewayRoutingPoliciesDocument = new TypedDocumentString(`
    query aiGatewayRoutingPolicies {
  aiGatewayRoutingPolicies {
    config
    createdDate
    deployments {
      enabled
      id
      maxRpm
      maxTpm
      modelId
      priorityOrder
      routingPolicyId
      weight
    }
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useAiGatewayRoutingPoliciesQuery = <
      TData = AiGatewayRoutingPoliciesQuery,
      TError = unknown
    >(
      variables?: AiGatewayRoutingPoliciesQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayRoutingPoliciesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayRoutingPoliciesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayRoutingPoliciesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiGatewayRoutingPolicies'] : ['aiGatewayRoutingPolicies', variables],
    queryFn: fetcher<AiGatewayRoutingPoliciesQuery, AiGatewayRoutingPoliciesQueryVariables>(AiGatewayRoutingPoliciesDocument, variables),
    ...options
  }
    )};

export const CreateAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation createAiGatewayRoutingPolicy($input: CreateAiGatewayRoutingPolicyInput!) {
  createAiGatewayRoutingPolicy(input: $input) {
    config
    createdDate
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useCreateAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiGatewayRoutingPolicyMutation, TError, CreateAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<CreateAiGatewayRoutingPolicyMutation, TError, CreateAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['createAiGatewayRoutingPolicy'],
    mutationFn: (variables?: CreateAiGatewayRoutingPolicyMutationVariables) => fetcher<CreateAiGatewayRoutingPolicyMutation, CreateAiGatewayRoutingPolicyMutationVariables>(CreateAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation updateAiGatewayRoutingPolicy($id: ID!, $input: UpdateAiGatewayRoutingPolicyInput!) {
  updateAiGatewayRoutingPolicy(id: $id, input: $input) {
    config
    createdDate
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useUpdateAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayRoutingPolicyMutation, TError, UpdateAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayRoutingPolicyMutation, TError, UpdateAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayRoutingPolicy'],
    mutationFn: (variables?: UpdateAiGatewayRoutingPolicyMutationVariables) => fetcher<UpdateAiGatewayRoutingPolicyMutation, UpdateAiGatewayRoutingPolicyMutationVariables>(UpdateAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation deleteAiGatewayRoutingPolicy($id: ID!) {
  deleteAiGatewayRoutingPolicy(id: $id)
}
    `);

export const useDeleteAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiGatewayRoutingPolicyMutation, TError, DeleteAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiGatewayRoutingPolicyMutation, TError, DeleteAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiGatewayRoutingPolicy'],
    mutationFn: (variables?: DeleteAiGatewayRoutingPolicyMutationVariables) => fetcher<DeleteAiGatewayRoutingPolicyMutation, DeleteAiGatewayRoutingPolicyMutationVariables>(DeleteAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const AiGatewaySpendSummariesDocument = new TypedDocumentString(`
    query aiGatewaySpendSummaries($startDate: Long!, $endDate: Long!) {
  aiGatewaySpendSummaries(startDate: $startDate, endDate: $endDate) {
    apiKeyId
    createdDate
    id
    model
    periodEnd
    periodStart
    provider
    requestCount
    totalCost
    totalInputTokens
    totalOutputTokens
  }
}
    `);

export const useAiGatewaySpendSummariesQuery = <
      TData = AiGatewaySpendSummariesQuery,
      TError = unknown
    >(
      variables: AiGatewaySpendSummariesQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewaySpendSummariesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewaySpendSummariesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewaySpendSummariesQuery, TError, TData>(
      {
    queryKey: ['aiGatewaySpendSummaries', variables],
    queryFn: fetcher<AiGatewaySpendSummariesQuery, AiGatewaySpendSummariesQueryVariables>(AiGatewaySpendSummariesDocument, variables),
    ...options
  }
    )};

export const AiGatewayWorkspaceSettingsDocument = new TypedDocumentString(`
    query aiGatewayWorkspaceSettings($workspaceId: ID!) {
  aiGatewayWorkspaceSettings(workspaceId: $workspaceId) {
    cacheEnabled
    cacheTtlSeconds
    defaultRoutingPolicyId
    logRetentionDays
    retryCount
    softBudgetWarningPct
    timeoutMs
    workspaceId
  }
}
    `);

export const useAiGatewayWorkspaceSettingsQuery = <
      TData = AiGatewayWorkspaceSettingsQuery,
      TError = unknown
    >(
      variables: AiGatewayWorkspaceSettingsQueryVariables,
      options?: Omit<UseQueryOptions<AiGatewayWorkspaceSettingsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGatewayWorkspaceSettingsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGatewayWorkspaceSettingsQuery, TError, TData>(
      {
    queryKey: ['aiGatewayWorkspaceSettings', variables],
    queryFn: fetcher<AiGatewayWorkspaceSettingsQuery, AiGatewayWorkspaceSettingsQueryVariables>(AiGatewayWorkspaceSettingsDocument, variables),
    ...options
  }
    )};

export const UpdateAiGatewayWorkspaceSettingsDocument = new TypedDocumentString(`
    mutation updateAiGatewayWorkspaceSettings($input: AiGatewayWorkspaceSettingsInput!) {
  updateAiGatewayWorkspaceSettings(input: $input) {
    cacheEnabled
    cacheTtlSeconds
    defaultRoutingPolicyId
    logRetentionDays
    retryCount
    softBudgetWarningPct
    timeoutMs
    workspaceId
  }
}
    `);

export const useUpdateAiGatewayWorkspaceSettingsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGatewayWorkspaceSettingsMutation, TError, UpdateAiGatewayWorkspaceSettingsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGatewayWorkspaceSettingsMutation, TError, UpdateAiGatewayWorkspaceSettingsMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGatewayWorkspaceSettings'],
    mutationFn: (variables?: UpdateAiGatewayWorkspaceSettingsMutationVariables) => fetcher<UpdateAiGatewayWorkspaceSettingsMutation, UpdateAiGatewayWorkspaceSettingsMutationVariables>(UpdateAiGatewayWorkspaceSettingsDocument, variables)(),
    ...options
  }
    )};

export const AiModelsDocument = new TypedDocumentString(`
    query aiModels {
  aiModels {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useAiModelsQuery = <
      TData = AiModelsQuery,
      TError = unknown
    >(
      variables?: AiModelsQueryVariables,
      options?: Omit<UseQueryOptions<AiModelsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiModelsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiModelsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiModels'] : ['aiModels', variables],
    queryFn: fetcher<AiModelsQuery, AiModelsQueryVariables>(AiModelsDocument, variables),
    ...options
  }
    )};

export const AiModelsByProviderDocument = new TypedDocumentString(`
    query aiModelsByProvider($providerId: ID!) {
  aiModelsByProvider(providerId: $providerId) {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useAiModelsByProviderQuery = <
      TData = AiModelsByProviderQuery,
      TError = unknown
    >(
      variables: AiModelsByProviderQueryVariables,
      options?: Omit<UseQueryOptions<AiModelsByProviderQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiModelsByProviderQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiModelsByProviderQuery, TError, TData>(
      {
    queryKey: ['aiModelsByProvider', variables],
    queryFn: fetcher<AiModelsByProviderQuery, AiModelsByProviderQueryVariables>(AiModelsByProviderDocument, variables),
    ...options
  }
    )};

export const CreateAiModelDocument = new TypedDocumentString(`
    mutation createAiModel($input: CreateAiModelInput!) {
  createAiModel(input: $input) {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useCreateAiModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiModelMutation, TError, CreateAiModelMutationVariables, TContext>) => {
    
    return useMutation<CreateAiModelMutation, TError, CreateAiModelMutationVariables, TContext>(
      {
    mutationKey: ['createAiModel'],
    mutationFn: (variables?: CreateAiModelMutationVariables) => fetcher<CreateAiModelMutation, CreateAiModelMutationVariables>(CreateAiModelDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiModelDocument = new TypedDocumentString(`
    mutation updateAiModel($id: ID!, $input: UpdateAiModelInput!) {
  updateAiModel(id: $id, input: $input) {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useUpdateAiModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiModelMutation, TError, UpdateAiModelMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiModelMutation, TError, UpdateAiModelMutationVariables, TContext>(
      {
    mutationKey: ['updateAiModel'],
    mutationFn: (variables?: UpdateAiModelMutationVariables) => fetcher<UpdateAiModelMutation, UpdateAiModelMutationVariables>(UpdateAiModelDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiModelDocument = new TypedDocumentString(`
    mutation deleteAiModel($id: ID!) {
  deleteAiModel(id: $id)
}
    `);

export const useDeleteAiModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiModelMutation, TError, DeleteAiModelMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiModelMutation, TError, DeleteAiModelMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiModel'],
    mutationFn: (variables?: DeleteAiModelMutationVariables) => fetcher<DeleteAiModelMutation, DeleteAiModelMutationVariables>(DeleteAiModelDocument, variables)(),
    ...options
  }
    )};

export const UnpinAiModelDocument = new TypedDocumentString(`
    mutation unpinAiModel($id: ID!) {
  unpinAiModel(id: $id) {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useUnpinAiModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UnpinAiModelMutation, TError, UnpinAiModelMutationVariables, TContext>) => {
    
    return useMutation<UnpinAiModelMutation, TError, UnpinAiModelMutationVariables, TContext>(
      {
    mutationKey: ['unpinAiModel'],
    mutationFn: (variables?: UnpinAiModelMutationVariables) => fetcher<UnpinAiModelMutation, UnpinAiModelMutationVariables>(UnpinAiModelDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityAlertEventsDocument = new TypedDocumentString(`
    query aiObservabilityAlertEvents($alertRuleId: ID!) {
  aiObservabilityAlertEvents(alertRuleId: $alertRuleId) {
    alertRuleId
    createdDate
    id
    message
    status
    triggeredValue
  }
}
    `);

export const useAiObservabilityAlertEventsQuery = <
      TData = AiObservabilityAlertEventsQuery,
      TError = unknown
    >(
      variables: AiObservabilityAlertEventsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityAlertEventsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityAlertEventsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityAlertEventsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityAlertEvents', variables],
    queryFn: fetcher<AiObservabilityAlertEventsQuery, AiObservabilityAlertEventsQueryVariables>(AiObservabilityAlertEventsDocument, variables),
    ...options
  }
    )};

export const AcknowledgeAiObservabilityAlertEventDocument = new TypedDocumentString(`
    mutation acknowledgeAiObservabilityAlertEvent($id: ID!) {
  acknowledgeAiObservabilityAlertEvent(id: $id) {
    alertRuleId
    createdDate
    id
    message
    status
    triggeredValue
  }
}
    `);

export const useAcknowledgeAiObservabilityAlertEventMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AcknowledgeAiObservabilityAlertEventMutation, TError, AcknowledgeAiObservabilityAlertEventMutationVariables, TContext>) => {
    
    return useMutation<AcknowledgeAiObservabilityAlertEventMutation, TError, AcknowledgeAiObservabilityAlertEventMutationVariables, TContext>(
      {
    mutationKey: ['acknowledgeAiObservabilityAlertEvent'],
    mutationFn: (variables?: AcknowledgeAiObservabilityAlertEventMutationVariables) => fetcher<AcknowledgeAiObservabilityAlertEventMutation, AcknowledgeAiObservabilityAlertEventMutationVariables>(AcknowledgeAiObservabilityAlertEventDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityAlertRulesDocument = new TypedDocumentString(`
    query aiObservabilityAlertRules($workspaceId: ID!) {
  aiObservabilityAlertRules(workspaceId: $workspaceId) {
    notificationIds
    condition
    cooldownMinutes
    createdDate
    enabled
    filters
    id
    lastModifiedDate
    metric
    name
    projectId
    snoozedUntil
    threshold
    version
    windowMinutes
  }
}
    `);

export const useAiObservabilityAlertRulesQuery = <
      TData = AiObservabilityAlertRulesQuery,
      TError = unknown
    >(
      variables: AiObservabilityAlertRulesQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityAlertRulesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityAlertRulesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityAlertRulesQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityAlertRules', variables],
    queryFn: fetcher<AiObservabilityAlertRulesQuery, AiObservabilityAlertRulesQueryVariables>(AiObservabilityAlertRulesDocument, variables),
    ...options
  }
    )};

export const AiObservabilityAlertRuleDocument = new TypedDocumentString(`
    query aiObservabilityAlertRule($id: ID!) {
  aiObservabilityAlertRule(id: $id) {
    notificationIds
    condition
    cooldownMinutes
    createdDate
    enabled
    filters
    id
    lastModifiedDate
    metric
    name
    projectId
    snoozedUntil
    threshold
    version
    windowMinutes
  }
}
    `);

export const useAiObservabilityAlertRuleQuery = <
      TData = AiObservabilityAlertRuleQuery,
      TError = unknown
    >(
      variables: AiObservabilityAlertRuleQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityAlertRuleQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityAlertRuleQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityAlertRuleQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityAlertRule', variables],
    queryFn: fetcher<AiObservabilityAlertRuleQuery, AiObservabilityAlertRuleQueryVariables>(AiObservabilityAlertRuleDocument, variables),
    ...options
  }
    )};

export const CreateAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation createAiObservabilityAlertRule($input: AiObservabilityAlertRuleInput!) {
  createAiObservabilityAlertRule(input: $input) {
    notificationIds
    condition
    cooldownMinutes
    createdDate
    enabled
    filters
    id
    lastModifiedDate
    metric
    name
    projectId
    threshold
    version
    windowMinutes
  }
}
    `);

export const useCreateAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiObservabilityAlertRuleMutation, TError, CreateAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<CreateAiObservabilityAlertRuleMutation, TError, CreateAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['createAiObservabilityAlertRule'],
    mutationFn: (variables?: CreateAiObservabilityAlertRuleMutationVariables) => fetcher<CreateAiObservabilityAlertRuleMutation, CreateAiObservabilityAlertRuleMutationVariables>(CreateAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation updateAiObservabilityAlertRule($id: ID!, $input: AiObservabilityAlertRuleInput!) {
  updateAiObservabilityAlertRule(id: $id, input: $input) {
    notificationIds
    condition
    cooldownMinutes
    createdDate
    enabled
    filters
    id
    lastModifiedDate
    metric
    name
    projectId
    threshold
    version
    windowMinutes
  }
}
    `);

export const useUpdateAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiObservabilityAlertRuleMutation, TError, UpdateAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiObservabilityAlertRuleMutation, TError, UpdateAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['updateAiObservabilityAlertRule'],
    mutationFn: (variables?: UpdateAiObservabilityAlertRuleMutationVariables) => fetcher<UpdateAiObservabilityAlertRuleMutation, UpdateAiObservabilityAlertRuleMutationVariables>(UpdateAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation deleteAiObservabilityAlertRule($id: ID!) {
  deleteAiObservabilityAlertRule(id: $id)
}
    `);

export const useDeleteAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiObservabilityAlertRuleMutation, TError, DeleteAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiObservabilityAlertRuleMutation, TError, DeleteAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiObservabilityAlertRule'],
    mutationFn: (variables?: DeleteAiObservabilityAlertRuleMutationVariables) => fetcher<DeleteAiObservabilityAlertRuleMutation, DeleteAiObservabilityAlertRuleMutationVariables>(DeleteAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const TestAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation testAiObservabilityAlertRule($id: ID!) {
  testAiObservabilityAlertRule(id: $id)
}
    `);

export const useTestAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestAiObservabilityAlertRuleMutation, TError, TestAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<TestAiObservabilityAlertRuleMutation, TError, TestAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['testAiObservabilityAlertRule'],
    mutationFn: (variables?: TestAiObservabilityAlertRuleMutationVariables) => fetcher<TestAiObservabilityAlertRuleMutation, TestAiObservabilityAlertRuleMutationVariables>(TestAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const SnoozeAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation snoozeAiObservabilityAlertRule($id: ID!, $until: Long!) {
  snoozeAiObservabilityAlertRule(id: $id, until: $until) {
    id
    snoozedUntil
  }
}
    `);

export const useSnoozeAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SnoozeAiObservabilityAlertRuleMutation, TError, SnoozeAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<SnoozeAiObservabilityAlertRuleMutation, TError, SnoozeAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['snoozeAiObservabilityAlertRule'],
    mutationFn: (variables?: SnoozeAiObservabilityAlertRuleMutationVariables) => fetcher<SnoozeAiObservabilityAlertRuleMutation, SnoozeAiObservabilityAlertRuleMutationVariables>(SnoozeAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const UnsnoozeAiObservabilityAlertRuleDocument = new TypedDocumentString(`
    mutation unsnoozeAiObservabilityAlertRule($id: ID!) {
  unsnoozeAiObservabilityAlertRule(id: $id) {
    id
    snoozedUntil
  }
}
    `);

export const useUnsnoozeAiObservabilityAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UnsnoozeAiObservabilityAlertRuleMutation, TError, UnsnoozeAiObservabilityAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<UnsnoozeAiObservabilityAlertRuleMutation, TError, UnsnoozeAiObservabilityAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['unsnoozeAiObservabilityAlertRule'],
    mutationFn: (variables?: UnsnoozeAiObservabilityAlertRuleMutationVariables) => fetcher<UnsnoozeAiObservabilityAlertRuleMutation, UnsnoozeAiObservabilityAlertRuleMutationVariables>(UnsnoozeAiObservabilityAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityExportJobsDocument = new TypedDocumentString(`
    query aiObservabilityExportJobs($workspaceId: ID!) {
  aiObservabilityExportJobs(workspaceId: $workspaceId) {
    createdBy
    createdDate
    errorMessage
    filePath
    filters
    format
    id
    projectId
    recordCount
    scope
    status
    type
  }
}
    `);

export const useAiObservabilityExportJobsQuery = <
      TData = AiObservabilityExportJobsQuery,
      TError = unknown
    >(
      variables: AiObservabilityExportJobsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityExportJobsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityExportJobsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityExportJobsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityExportJobs', variables],
    queryFn: fetcher<AiObservabilityExportJobsQuery, AiObservabilityExportJobsQueryVariables>(AiObservabilityExportJobsDocument, variables),
    ...options
  }
    )};

export const AiObservabilityExportJobDocument = new TypedDocumentString(`
    query aiObservabilityExportJob($id: ID!) {
  aiObservabilityExportJob(id: $id) {
    createdBy
    createdDate
    errorMessage
    filePath
    filters
    format
    id
    projectId
    recordCount
    scope
    status
    type
  }
}
    `);

export const useAiObservabilityExportJobQuery = <
      TData = AiObservabilityExportJobQuery,
      TError = unknown
    >(
      variables: AiObservabilityExportJobQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityExportJobQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityExportJobQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityExportJobQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityExportJob', variables],
    queryFn: fetcher<AiObservabilityExportJobQuery, AiObservabilityExportJobQueryVariables>(AiObservabilityExportJobDocument, variables),
    ...options
  }
    )};

export const CreateAiObservabilityExportJobDocument = new TypedDocumentString(`
    mutation createAiObservabilityExportJob($workspaceId: ID!, $projectId: ID, $format: AiObservabilityExportFormat!, $scope: AiObservabilityExportScope!, $filters: String) {
  createAiObservabilityExportJob(
    workspaceId: $workspaceId
    projectId: $projectId
    format: $format
    scope: $scope
    filters: $filters
  ) {
    createdBy
    createdDate
    format
    id
    scope
    status
    type
  }
}
    `);

export const useCreateAiObservabilityExportJobMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiObservabilityExportJobMutation, TError, CreateAiObservabilityExportJobMutationVariables, TContext>) => {
    
    return useMutation<CreateAiObservabilityExportJobMutation, TError, CreateAiObservabilityExportJobMutationVariables, TContext>(
      {
    mutationKey: ['createAiObservabilityExportJob'],
    mutationFn: (variables?: CreateAiObservabilityExportJobMutationVariables) => fetcher<CreateAiObservabilityExportJobMutation, CreateAiObservabilityExportJobMutationVariables>(CreateAiObservabilityExportJobDocument, variables)(),
    ...options
  }
    )};

export const CancelAiObservabilityExportJobDocument = new TypedDocumentString(`
    mutation cancelAiObservabilityExportJob($id: ID!) {
  cancelAiObservabilityExportJob(id: $id) {
    id
    status
  }
}
    `);

export const useCancelAiObservabilityExportJobMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelAiObservabilityExportJobMutation, TError, CancelAiObservabilityExportJobMutationVariables, TContext>) => {
    
    return useMutation<CancelAiObservabilityExportJobMutation, TError, CancelAiObservabilityExportJobMutationVariables, TContext>(
      {
    mutationKey: ['cancelAiObservabilityExportJob'],
    mutationFn: (variables?: CancelAiObservabilityExportJobMutationVariables) => fetcher<CancelAiObservabilityExportJobMutation, CancelAiObservabilityExportJobMutationVariables>(CancelAiObservabilityExportJobDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilitySessionsDocument = new TypedDocumentString(`
    query aiObservabilitySessions($workspaceId: ID!) {
  aiObservabilitySessions(workspaceId: $workspaceId) {
    createdDate
    id
    lastModifiedDate
    name
    projectId
    traceCount
    userId
    version
    workspaceId
  }
}
    `);

export const useAiObservabilitySessionsQuery = <
      TData = AiObservabilitySessionsQuery,
      TError = unknown
    >(
      variables: AiObservabilitySessionsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilitySessionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilitySessionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilitySessionsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilitySessions', variables],
    queryFn: fetcher<AiObservabilitySessionsQuery, AiObservabilitySessionsQueryVariables>(AiObservabilitySessionsDocument, variables),
    ...options
  }
    )};

export const AiObservabilitySessionDocument = new TypedDocumentString(`
    query aiObservabilitySession($id: ID!) {
  aiObservabilitySession(id: $id) {
    createdDate
    id
    lastModifiedDate
    name
    projectId
    traces {
      createdDate
      id
      name
      source
      status
      totalCost
      totalInputTokens
      totalLatencyMs
      totalOutputTokens
      userId
    }
    userId
    version
    workspaceId
  }
}
    `);

export const useAiObservabilitySessionQuery = <
      TData = AiObservabilitySessionQuery,
      TError = unknown
    >(
      variables: AiObservabilitySessionQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilitySessionQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilitySessionQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilitySessionQuery, TError, TData>(
      {
    queryKey: ['aiObservabilitySession', variables],
    queryFn: fetcher<AiObservabilitySessionQuery, AiObservabilitySessionQueryVariables>(AiObservabilitySessionDocument, variables),
    ...options
  }
    )};

export const AiObservabilityTracesDocument = new TypedDocumentString(`
    query aiObservabilityTraces($endDate: Long!, $model: String, $source: AiObservabilityTraceSource, $startDate: Long!, $status: AiObservabilityTraceStatus, $tagId: ID, $userId: String, $workspaceId: ID!) {
  aiObservabilityTraces(
    endDate: $endDate
    model: $model
    source: $source
    startDate: $startDate
    status: $status
    tagId: $tagId
    userId: $userId
    workspaceId: $workspaceId
  ) {
    createdDate
    id
    input
    lastModifiedDate
    metadata
    name
    output
    projectId
    sessionId
    source
    status
    totalCost
    totalInputTokens
    totalLatencyMs
    totalOutputTokens
    userId
    version
    workspaceId
  }
}
    `);

export const useAiObservabilityTracesQuery = <
      TData = AiObservabilityTracesQuery,
      TError = unknown
    >(
      variables: AiObservabilityTracesQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityTracesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityTracesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityTracesQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityTraces', variables],
    queryFn: fetcher<AiObservabilityTracesQuery, AiObservabilityTracesQueryVariables>(AiObservabilityTracesDocument, variables),
    ...options
  }
    )};

export const AiObservabilityTraceDocument = new TypedDocumentString(`
    query aiObservabilityTrace($id: ID!) {
  aiObservabilityTrace(id: $id) {
    createdDate
    id
    input
    lastModifiedDate
    metadata
    name
    output
    projectId
    sessionId
    source
    spans {
      cost
      createdDate
      endTime
      id
      input
      inputTokens
      latencyMs
      level
      metadata
      model
      name
      output
      outputTokens
      parentSpanId
      provider
      startTime
      status
      traceId
      type
      version
    }
    status
    tagIds
    totalCost
    totalInputTokens
    totalLatencyMs
    totalOutputTokens
    userId
    version
    workspaceId
  }
}
    `);

export const useAiObservabilityTraceQuery = <
      TData = AiObservabilityTraceQuery,
      TError = unknown
    >(
      variables: AiObservabilityTraceQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityTraceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityTraceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityTraceQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityTrace', variables],
    queryFn: fetcher<AiObservabilityTraceQuery, AiObservabilityTraceQueryVariables>(AiObservabilityTraceDocument, variables),
    ...options
  }
    )};

export const SetAiObservabilityTraceTagsDocument = new TypedDocumentString(`
    mutation setAiObservabilityTraceTags($traceId: ID!, $tagIds: [ID!]!) {
  setAiObservabilityTraceTags(traceId: $traceId, tagIds: $tagIds) {
    id
    tagIds
  }
}
    `);

export const useSetAiObservabilityTraceTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetAiObservabilityTraceTagsMutation, TError, SetAiObservabilityTraceTagsMutationVariables, TContext>) => {
    
    return useMutation<SetAiObservabilityTraceTagsMutation, TError, SetAiObservabilityTraceTagsMutationVariables, TContext>(
      {
    mutationKey: ['setAiObservabilityTraceTags'],
    mutationFn: (variables?: SetAiObservabilityTraceTagsMutationVariables) => fetcher<SetAiObservabilityTraceTagsMutation, SetAiObservabilityTraceTagsMutationVariables>(SetAiObservabilityTraceTagsDocument, variables)(),
    ...options
  }
    )};

export const AiObservabilityWebhookDeliveriesDocument = new TypedDocumentString(`
    query aiObservabilityWebhookDeliveries($subscriptionId: ID!) {
  aiObservabilityWebhookDeliveries(subscriptionId: $subscriptionId) {
    attemptCount
    createdDate
    deliveredDate
    errorMessage
    eventType
    httpStatus
    id
    status
    subscriptionId
  }
}
    `);

export const useAiObservabilityWebhookDeliveriesQuery = <
      TData = AiObservabilityWebhookDeliveriesQuery,
      TError = unknown
    >(
      variables: AiObservabilityWebhookDeliveriesQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityWebhookDeliveriesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityWebhookDeliveriesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityWebhookDeliveriesQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityWebhookDeliveries', variables],
    queryFn: fetcher<AiObservabilityWebhookDeliveriesQuery, AiObservabilityWebhookDeliveriesQueryVariables>(AiObservabilityWebhookDeliveriesDocument, variables),
    ...options
  }
    )};

export const AiObservabilityWebhookSubscriptionsDocument = new TypedDocumentString(`
    query aiObservabilityWebhookSubscriptions($workspaceId: ID!) {
  aiObservabilityWebhookSubscriptions(workspaceId: $workspaceId) {
    createdDate
    enabled
    events
    id
    lastModifiedDate
    lastTriggeredDate
    name
    projectId
    url
    version
  }
}
    `);

export const useAiObservabilityWebhookSubscriptionsQuery = <
      TData = AiObservabilityWebhookSubscriptionsQuery,
      TError = unknown
    >(
      variables: AiObservabilityWebhookSubscriptionsQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityWebhookSubscriptionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityWebhookSubscriptionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityWebhookSubscriptionsQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityWebhookSubscriptions', variables],
    queryFn: fetcher<AiObservabilityWebhookSubscriptionsQuery, AiObservabilityWebhookSubscriptionsQueryVariables>(AiObservabilityWebhookSubscriptionsDocument, variables),
    ...options
  }
    )};

export const AiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    query aiObservabilityWebhookSubscription($id: ID!) {
  aiObservabilityWebhookSubscription(id: $id) {
    createdDate
    enabled
    events
    id
    lastModifiedDate
    lastTriggeredDate
    name
    projectId
    url
    version
  }
}
    `);

export const useAiObservabilityWebhookSubscriptionQuery = <
      TData = AiObservabilityWebhookSubscriptionQuery,
      TError = unknown
    >(
      variables: AiObservabilityWebhookSubscriptionQueryVariables,
      options?: Omit<UseQueryOptions<AiObservabilityWebhookSubscriptionQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiObservabilityWebhookSubscriptionQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiObservabilityWebhookSubscriptionQuery, TError, TData>(
      {
    queryKey: ['aiObservabilityWebhookSubscription', variables],
    queryFn: fetcher<AiObservabilityWebhookSubscriptionQuery, AiObservabilityWebhookSubscriptionQueryVariables>(AiObservabilityWebhookSubscriptionDocument, variables),
    ...options
  }
    )};

export const CreateAiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    mutation createAiObservabilityWebhookSubscription($workspaceId: ID!, $projectId: ID, $name: String!, $url: String!, $secret: String, $events: String!, $enabled: Boolean!) {
  createAiObservabilityWebhookSubscription(
    workspaceId: $workspaceId
    projectId: $projectId
    name: $name
    url: $url
    secret: $secret
    events: $events
    enabled: $enabled
  ) {
    createdDate
    enabled
    events
    id
    name
    url
    version
  }
}
    `);

export const useCreateAiObservabilityWebhookSubscriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiObservabilityWebhookSubscriptionMutation, TError, CreateAiObservabilityWebhookSubscriptionMutationVariables, TContext>) => {
    
    return useMutation<CreateAiObservabilityWebhookSubscriptionMutation, TError, CreateAiObservabilityWebhookSubscriptionMutationVariables, TContext>(
      {
    mutationKey: ['createAiObservabilityWebhookSubscription'],
    mutationFn: (variables?: CreateAiObservabilityWebhookSubscriptionMutationVariables) => fetcher<CreateAiObservabilityWebhookSubscriptionMutation, CreateAiObservabilityWebhookSubscriptionMutationVariables>(CreateAiObservabilityWebhookSubscriptionDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    mutation updateAiObservabilityWebhookSubscription($id: ID!, $name: String!, $url: String!, $secret: String, $events: String!, $enabled: Boolean!) {
  updateAiObservabilityWebhookSubscription(
    id: $id
    name: $name
    url: $url
    secret: $secret
    events: $events
    enabled: $enabled
  ) {
    createdDate
    enabled
    events
    id
    name
    url
    version
  }
}
    `);

export const useUpdateAiObservabilityWebhookSubscriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiObservabilityWebhookSubscriptionMutation, TError, UpdateAiObservabilityWebhookSubscriptionMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiObservabilityWebhookSubscriptionMutation, TError, UpdateAiObservabilityWebhookSubscriptionMutationVariables, TContext>(
      {
    mutationKey: ['updateAiObservabilityWebhookSubscription'],
    mutationFn: (variables?: UpdateAiObservabilityWebhookSubscriptionMutationVariables) => fetcher<UpdateAiObservabilityWebhookSubscriptionMutation, UpdateAiObservabilityWebhookSubscriptionMutationVariables>(UpdateAiObservabilityWebhookSubscriptionDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    mutation deleteAiObservabilityWebhookSubscription($id: ID!) {
  deleteAiObservabilityWebhookSubscription(id: $id)
}
    `);

export const useDeleteAiObservabilityWebhookSubscriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiObservabilityWebhookSubscriptionMutation, TError, DeleteAiObservabilityWebhookSubscriptionMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiObservabilityWebhookSubscriptionMutation, TError, DeleteAiObservabilityWebhookSubscriptionMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiObservabilityWebhookSubscription'],
    mutationFn: (variables?: DeleteAiObservabilityWebhookSubscriptionMutationVariables) => fetcher<DeleteAiObservabilityWebhookSubscriptionMutation, DeleteAiObservabilityWebhookSubscriptionMutationVariables>(DeleteAiObservabilityWebhookSubscriptionDocument, variables)(),
    ...options
  }
    )};

export const TestAiObservabilityWebhookSubscriptionDocument = new TypedDocumentString(`
    mutation testAiObservabilityWebhookSubscription($id: ID!) {
  testAiObservabilityWebhookSubscription(id: $id)
}
    `);

export const useTestAiObservabilityWebhookSubscriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestAiObservabilityWebhookSubscriptionMutation, TError, TestAiObservabilityWebhookSubscriptionMutationVariables, TContext>) => {
    
    return useMutation<TestAiObservabilityWebhookSubscriptionMutation, TError, TestAiObservabilityWebhookSubscriptionMutationVariables, TContext>(
      {
    mutationKey: ['testAiObservabilityWebhookSubscription'],
    mutationFn: (variables?: TestAiObservabilityWebhookSubscriptionMutationVariables) => fetcher<TestAiObservabilityWebhookSubscriptionMutation, TestAiObservabilityWebhookSubscriptionMutationVariables>(TestAiObservabilityWebhookSubscriptionDocument, variables)(),
    ...options
  }
    )};

export const AiPromptsDocument = new TypedDocumentString(`
    query aiPrompts($workspaceId: ID!) {
  aiPrompts(workspaceId: $workspaceId) {
    createdDate
    description
    id
    lastModifiedDate
    name
    projectId
    version
    versions {
      active
      commitMessage
      content
      createdBy
      createdDate
      environment
      id
      metrics {
        avgCostUsd
        avgLatencyMs
        errorRate
        invocationCount
      }
      promptId
      type
      variables
      versionNumber
    }
  }
}
    `);

export const useAiPromptsQuery = <
      TData = AiPromptsQuery,
      TError = unknown
    >(
      variables: AiPromptsQueryVariables,
      options?: Omit<UseQueryOptions<AiPromptsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiPromptsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiPromptsQuery, TError, TData>(
      {
    queryKey: ['aiPrompts', variables],
    queryFn: fetcher<AiPromptsQuery, AiPromptsQueryVariables>(AiPromptsDocument, variables),
    ...options
  }
    )};

export const AiPromptDocument = new TypedDocumentString(`
    query aiPrompt($id: ID!) {
  aiPrompt(id: $id) {
    createdDate
    description
    id
    lastModifiedDate
    name
    projectId
    version
    versions {
      active
      commitMessage
      content
      createdBy
      createdDate
      environment
      id
      metrics {
        avgCostUsd
        avgLatencyMs
        errorRate
        invocationCount
      }
      promptId
      type
      variables
      versionNumber
    }
  }
}
    `);

export const useAiPromptQuery = <
      TData = AiPromptQuery,
      TError = unknown
    >(
      variables: AiPromptQueryVariables,
      options?: Omit<UseQueryOptions<AiPromptQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiPromptQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiPromptQuery, TError, TData>(
      {
    queryKey: ['aiPrompt', variables],
    queryFn: fetcher<AiPromptQuery, AiPromptQueryVariables>(AiPromptDocument, variables),
    ...options
  }
    )};

export const CreateAiPromptDocument = new TypedDocumentString(`
    mutation createAiPrompt($input: CreateAiPromptInput!) {
  createAiPrompt(input: $input) {
    createdDate
    description
    id
    lastModifiedDate
    name
    projectId
    version
  }
}
    `);

export const useCreateAiPromptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiPromptMutation, TError, CreateAiPromptMutationVariables, TContext>) => {
    
    return useMutation<CreateAiPromptMutation, TError, CreateAiPromptMutationVariables, TContext>(
      {
    mutationKey: ['createAiPrompt'],
    mutationFn: (variables?: CreateAiPromptMutationVariables) => fetcher<CreateAiPromptMutation, CreateAiPromptMutationVariables>(CreateAiPromptDocument, variables)(),
    ...options
  }
    )};

export const UpdateAiPromptDocument = new TypedDocumentString(`
    mutation updateAiPrompt($id: ID!, $input: UpdateAiPromptInput!) {
  updateAiPrompt(id: $id, input: $input) {
    createdDate
    description
    id
    lastModifiedDate
    name
    projectId
    version
  }
}
    `);

export const useUpdateAiPromptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiPromptMutation, TError, UpdateAiPromptMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiPromptMutation, TError, UpdateAiPromptMutationVariables, TContext>(
      {
    mutationKey: ['updateAiPrompt'],
    mutationFn: (variables?: UpdateAiPromptMutationVariables) => fetcher<UpdateAiPromptMutation, UpdateAiPromptMutationVariables>(UpdateAiPromptDocument, variables)(),
    ...options
  }
    )};

export const DeleteAiPromptDocument = new TypedDocumentString(`
    mutation deleteAiPrompt($id: ID!) {
  deleteAiPrompt(id: $id)
}
    `);

export const useDeleteAiPromptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAiPromptMutation, TError, DeleteAiPromptMutationVariables, TContext>) => {
    
    return useMutation<DeleteAiPromptMutation, TError, DeleteAiPromptMutationVariables, TContext>(
      {
    mutationKey: ['deleteAiPrompt'],
    mutationFn: (variables?: DeleteAiPromptMutationVariables) => fetcher<DeleteAiPromptMutation, DeleteAiPromptMutationVariables>(DeleteAiPromptDocument, variables)(),
    ...options
  }
    )};

export const CreateAiPromptVersionDocument = new TypedDocumentString(`
    mutation createAiPromptVersion($input: CreateAiPromptVersionInput!) {
  createAiPromptVersion(input: $input) {
    active
    commitMessage
    content
    createdBy
    createdDate
    environment
    id
    promptId
    type
    variables
    versionNumber
  }
}
    `);

export const useCreateAiPromptVersionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAiPromptVersionMutation, TError, CreateAiPromptVersionMutationVariables, TContext>) => {
    
    return useMutation<CreateAiPromptVersionMutation, TError, CreateAiPromptVersionMutationVariables, TContext>(
      {
    mutationKey: ['createAiPromptVersion'],
    mutationFn: (variables?: CreateAiPromptVersionMutationVariables) => fetcher<CreateAiPromptVersionMutation, CreateAiPromptVersionMutationVariables>(CreateAiPromptVersionDocument, variables)(),
    ...options
  }
    )};

export const SetActiveAiPromptVersionDocument = new TypedDocumentString(`
    mutation setActiveAiPromptVersion($promptVersionId: ID!, $environment: String!) {
  setActiveAiPromptVersion(
    promptVersionId: $promptVersionId
    environment: $environment
  )
}
    `);

export const useSetActiveAiPromptVersionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetActiveAiPromptVersionMutation, TError, SetActiveAiPromptVersionMutationVariables, TContext>) => {
    
    return useMutation<SetActiveAiPromptVersionMutation, TError, SetActiveAiPromptVersionMutationVariables, TContext>(
      {
    mutationKey: ['setActiveAiPromptVersion'],
    mutationFn: (variables?: SetActiveAiPromptVersionMutationVariables) => fetcher<SetActiveAiPromptVersionMutation, SetActiveAiPromptVersionMutationVariables>(SetActiveAiPromptVersionDocument, variables)(),
    ...options
  }
    )};

export const WorkspaceAiGatewayProvidersDocument = new TypedDocumentString(`
    query workspaceAiGatewayProviders($workspaceId: ID!) {
  workspaceAiGatewayProviders(workspaceId: $workspaceId) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useWorkspaceAiGatewayProvidersQuery = <
      TData = WorkspaceAiGatewayProvidersQuery,
      TError = unknown
    >(
      variables: WorkspaceAiGatewayProvidersQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceAiGatewayProvidersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceAiGatewayProvidersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceAiGatewayProvidersQuery, TError, TData>(
      {
    queryKey: ['workspaceAiGatewayProviders', variables],
    queryFn: fetcher<WorkspaceAiGatewayProvidersQuery, WorkspaceAiGatewayProvidersQueryVariables>(WorkspaceAiGatewayProvidersDocument, variables),
    ...options
  }
    )};

export const CreateWorkspaceAiGatewayProviderDocument = new TypedDocumentString(`
    mutation createWorkspaceAiGatewayProvider($input: CreateWorkspaceAiGatewayProviderInput!) {
  createWorkspaceAiGatewayProvider(input: $input) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useCreateWorkspaceAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceAiGatewayProviderMutation, TError, CreateWorkspaceAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceAiGatewayProviderMutation, TError, CreateWorkspaceAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceAiGatewayProvider'],
    mutationFn: (variables?: CreateWorkspaceAiGatewayProviderMutationVariables) => fetcher<CreateWorkspaceAiGatewayProviderMutation, CreateWorkspaceAiGatewayProviderMutationVariables>(CreateWorkspaceAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceAiGatewayProviderDocument = new TypedDocumentString(`
    mutation deleteWorkspaceAiGatewayProvider($workspaceId: ID!, $providerId: ID!) {
  deleteWorkspaceAiGatewayProvider(
    workspaceId: $workspaceId
    providerId: $providerId
  )
}
    `);

export const useDeleteWorkspaceAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceAiGatewayProviderMutation, TError, DeleteWorkspaceAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceAiGatewayProviderMutation, TError, DeleteWorkspaceAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceAiGatewayProvider'],
    mutationFn: (variables?: DeleteWorkspaceAiGatewayProviderMutationVariables) => fetcher<DeleteWorkspaceAiGatewayProviderMutation, DeleteWorkspaceAiGatewayProviderMutationVariables>(DeleteWorkspaceAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceAiGatewayProviderDocument = new TypedDocumentString(`
    mutation updateWorkspaceAiGatewayProvider($workspaceId: ID!, $id: ID!, $input: UpdateAiGatewayProviderInput!) {
  updateWorkspaceAiGatewayProvider(
    workspaceId: $workspaceId
    id: $id
    input: $input
  ) {
    baseUrl
    config
    createdBy
    createdDate
    enabled
    id
    lastModifiedBy
    lastModifiedDate
    name
    type
    version
  }
}
    `);

export const useUpdateWorkspaceAiGatewayProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceAiGatewayProviderMutation, TError, UpdateWorkspaceAiGatewayProviderMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceAiGatewayProviderMutation, TError, UpdateWorkspaceAiGatewayProviderMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceAiGatewayProvider'],
    mutationFn: (variables?: UpdateWorkspaceAiGatewayProviderMutationVariables) => fetcher<UpdateWorkspaceAiGatewayProviderMutation, UpdateWorkspaceAiGatewayProviderMutationVariables>(UpdateWorkspaceAiGatewayProviderDocument, variables)(),
    ...options
  }
    )};

export const TestWorkspaceAiGatewayProviderConnectionDocument = new TypedDocumentString(`
    mutation testWorkspaceAiGatewayProviderConnection($workspaceId: ID!, $providerId: ID!) {
  testWorkspaceAiGatewayProviderConnection(
    workspaceId: $workspaceId
    providerId: $providerId
  ) {
    errorMessage
    latencyMs
    ok
  }
}
    `);

export const useTestWorkspaceAiGatewayProviderConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestWorkspaceAiGatewayProviderConnectionMutation, TError, TestWorkspaceAiGatewayProviderConnectionMutationVariables, TContext>) => {
    
    return useMutation<TestWorkspaceAiGatewayProviderConnectionMutation, TError, TestWorkspaceAiGatewayProviderConnectionMutationVariables, TContext>(
      {
    mutationKey: ['testWorkspaceAiGatewayProviderConnection'],
    mutationFn: (variables?: TestWorkspaceAiGatewayProviderConnectionMutationVariables) => fetcher<TestWorkspaceAiGatewayProviderConnectionMutation, TestWorkspaceAiGatewayProviderConnectionMutationVariables>(TestWorkspaceAiGatewayProviderConnectionDocument, variables)(),
    ...options
  }
    )};

export const WorkspaceAiGatewayRequestLogsDocument = new TypedDocumentString(`
    query workspaceAiGatewayRequestLogs($endDate: Long!, $startDate: Long!, $workspaceId: ID!) {
  workspaceAiGatewayRequestLogs(
    endDate: $endDate
    startDate: $startDate
    workspaceId: $workspaceId
  ) {
    apiKeyId
    cacheHit
    cost
    createdDate
    errorMessage
    id
    inputTokens
    latencyMs
    outputTokens
    requestId
    requestedModel
    routedModel
    routedProvider
    routingPolicyId
    routingStrategy
    status
  }
}
    `);

export const useWorkspaceAiGatewayRequestLogsQuery = <
      TData = WorkspaceAiGatewayRequestLogsQuery,
      TError = unknown
    >(
      variables: WorkspaceAiGatewayRequestLogsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceAiGatewayRequestLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceAiGatewayRequestLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceAiGatewayRequestLogsQuery, TError, TData>(
      {
    queryKey: ['workspaceAiGatewayRequestLogs', variables],
    queryFn: fetcher<WorkspaceAiGatewayRequestLogsQuery, WorkspaceAiGatewayRequestLogsQueryVariables>(WorkspaceAiGatewayRequestLogsDocument, variables),
    ...options
  }
    )};

export const WorkspaceAiGatewayRoutingPoliciesDocument = new TypedDocumentString(`
    query workspaceAiGatewayRoutingPolicies($workspaceId: ID!) {
  workspaceAiGatewayRoutingPolicies(workspaceId: $workspaceId) {
    config
    createdDate
    deployments {
      enabled
      id
      maxRpm
      maxTpm
      modelId
      priorityOrder
      routingPolicyId
      weight
    }
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useWorkspaceAiGatewayRoutingPoliciesQuery = <
      TData = WorkspaceAiGatewayRoutingPoliciesQuery,
      TError = unknown
    >(
      variables: WorkspaceAiGatewayRoutingPoliciesQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceAiGatewayRoutingPoliciesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceAiGatewayRoutingPoliciesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceAiGatewayRoutingPoliciesQuery, TError, TData>(
      {
    queryKey: ['workspaceAiGatewayRoutingPolicies', variables],
    queryFn: fetcher<WorkspaceAiGatewayRoutingPoliciesQuery, WorkspaceAiGatewayRoutingPoliciesQueryVariables>(WorkspaceAiGatewayRoutingPoliciesDocument, variables),
    ...options
  }
    )};

export const CreateWorkspaceAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation createWorkspaceAiGatewayRoutingPolicy($input: CreateWorkspaceAiGatewayRoutingPolicyInput!) {
  createWorkspaceAiGatewayRoutingPolicy(input: $input) {
    config
    createdDate
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useCreateWorkspaceAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceAiGatewayRoutingPolicyMutation, TError, CreateWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceAiGatewayRoutingPolicyMutation, TError, CreateWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceAiGatewayRoutingPolicy'],
    mutationFn: (variables?: CreateWorkspaceAiGatewayRoutingPolicyMutationVariables) => fetcher<CreateWorkspaceAiGatewayRoutingPolicyMutation, CreateWorkspaceAiGatewayRoutingPolicyMutationVariables>(CreateWorkspaceAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation deleteWorkspaceAiGatewayRoutingPolicy($workspaceId: ID!, $routingPolicyId: ID!) {
  deleteWorkspaceAiGatewayRoutingPolicy(
    workspaceId: $workspaceId
    routingPolicyId: $routingPolicyId
  )
}
    `);

export const useDeleteWorkspaceAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceAiGatewayRoutingPolicyMutation, TError, DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceAiGatewayRoutingPolicyMutation, TError, DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceAiGatewayRoutingPolicy'],
    mutationFn: (variables?: DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables) => fetcher<DeleteWorkspaceAiGatewayRoutingPolicyMutation, DeleteWorkspaceAiGatewayRoutingPolicyMutationVariables>(DeleteWorkspaceAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceAiGatewayRoutingPolicyDocument = new TypedDocumentString(`
    mutation updateWorkspaceAiGatewayRoutingPolicy($workspaceId: ID!, $id: ID!, $input: UpdateAiGatewayRoutingPolicyInput!) {
  updateWorkspaceAiGatewayRoutingPolicy(
    workspaceId: $workspaceId
    id: $id
    input: $input
  ) {
    config
    createdDate
    enabled
    fallbackModel
    id
    lastModifiedDate
    name
    strategy
    version
  }
}
    `);

export const useUpdateWorkspaceAiGatewayRoutingPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceAiGatewayRoutingPolicyMutation, TError, UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceAiGatewayRoutingPolicyMutation, TError, UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceAiGatewayRoutingPolicy'],
    mutationFn: (variables?: UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables) => fetcher<UpdateWorkspaceAiGatewayRoutingPolicyMutation, UpdateWorkspaceAiGatewayRoutingPolicyMutationVariables>(UpdateWorkspaceAiGatewayRoutingPolicyDocument, variables)(),
    ...options
  }
    )};

export const WorkspaceAiModelsDocument = new TypedDocumentString(`
    query workspaceAiModels($workspaceId: ID!) {
  workspaceAiModels(workspaceId: $workspaceId) {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    defaultRoutingPolicyId
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useWorkspaceAiModelsQuery = <
      TData = WorkspaceAiModelsQuery,
      TError = unknown
    >(
      variables: WorkspaceAiModelsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceAiModelsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceAiModelsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceAiModelsQuery, TError, TData>(
      {
    queryKey: ['workspaceAiModels', variables],
    queryFn: fetcher<WorkspaceAiModelsQuery, WorkspaceAiModelsQueryVariables>(WorkspaceAiModelsDocument, variables),
    ...options
  }
    )};

export const CreateWorkspaceAiModelDocument = new TypedDocumentString(`
    mutation createWorkspaceAiModel($input: CreateWorkspaceAiModelInput!) {
  createWorkspaceAiModel(input: $input) {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    defaultRoutingPolicyId
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useCreateWorkspaceAiModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceAiModelMutation, TError, CreateWorkspaceAiModelMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceAiModelMutation, TError, CreateWorkspaceAiModelMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceAiModel'],
    mutationFn: (variables?: CreateWorkspaceAiModelMutationVariables) => fetcher<CreateWorkspaceAiModelMutation, CreateWorkspaceAiModelMutationVariables>(CreateWorkspaceAiModelDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceAiModelDocument = new TypedDocumentString(`
    mutation deleteWorkspaceAiModel($workspaceId: ID!, $modelId: ID!) {
  deleteWorkspaceAiModel(workspaceId: $workspaceId, modelId: $modelId)
}
    `);

export const useDeleteWorkspaceAiModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceAiModelMutation, TError, DeleteWorkspaceAiModelMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceAiModelMutation, TError, DeleteWorkspaceAiModelMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceAiModel'],
    mutationFn: (variables?: DeleteWorkspaceAiModelMutationVariables) => fetcher<DeleteWorkspaceAiModelMutation, DeleteWorkspaceAiModelMutationVariables>(DeleteWorkspaceAiModelDocument, variables)(),
    ...options
  }
    )};

export const UnpinWorkspaceAiModelDocument = new TypedDocumentString(`
    mutation unpinWorkspaceAiModel($workspaceId: ID!, $modelId: ID!) {
  unpinWorkspaceAiModel(workspaceId: $workspaceId, modelId: $modelId) {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    defaultRoutingPolicyId
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useUnpinWorkspaceAiModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UnpinWorkspaceAiModelMutation, TError, UnpinWorkspaceAiModelMutationVariables, TContext>) => {
    
    return useMutation<UnpinWorkspaceAiModelMutation, TError, UnpinWorkspaceAiModelMutationVariables, TContext>(
      {
    mutationKey: ['unpinWorkspaceAiModel'],
    mutationFn: (variables?: UnpinWorkspaceAiModelMutationVariables) => fetcher<UnpinWorkspaceAiModelMutation, UnpinWorkspaceAiModelMutationVariables>(UnpinWorkspaceAiModelDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceAiModelDocument = new TypedDocumentString(`
    mutation updateWorkspaceAiModel($workspaceId: ID!, $id: ID!, $input: UpdateAiModelInput!) {
  updateWorkspaceAiModel(workspaceId: $workspaceId, id: $id, input: $input) {
    alias
    capabilities
    catalogManaged
    catalogPinned
    contextWindow
    defaultRoutingPolicyId
    createdDate
    enabled
    id
    inputCostPerMTokens
    lastModifiedDate
    name
    outputCostPerMTokens
    providerId
    version
  }
}
    `);

export const useUpdateWorkspaceAiModelMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceAiModelMutation, TError, UpdateWorkspaceAiModelMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceAiModelMutation, TError, UpdateWorkspaceAiModelMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceAiModel'],
    mutationFn: (variables?: UpdateWorkspaceAiModelMutationVariables) => fetcher<UpdateWorkspaceAiModelMutation, UpdateWorkspaceAiModelMutationVariables>(UpdateWorkspaceAiModelDocument, variables)(),
    ...options
  }
    )};

export const ApprovalTaskDocument = new TypedDocumentString(`
    query approvalTask($id: ID!) {
  approvalTask(id: $id) {
    assigneeId
    createdBy
    createdDate
    description
    dueDate
    id
    jobResumeId
    lastModifiedBy
    lastModifiedDate
    name
    priority
    status
    version
  }
}
    `);

export const useApprovalTaskQuery = <
      TData = ApprovalTaskQuery,
      TError = unknown
    >(
      variables: ApprovalTaskQueryVariables,
      options?: Omit<UseQueryOptions<ApprovalTaskQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApprovalTaskQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApprovalTaskQuery, TError, TData>(
      {
    queryKey: ['approvalTask', variables],
    queryFn: fetcher<ApprovalTaskQuery, ApprovalTaskQueryVariables>(ApprovalTaskDocument, variables),
    ...options
  }
    )};

export const ApprovalTasksDocument = new TypedDocumentString(`
    query approvalTasks($environmentId: Int) {
  approvalTasks(environmentId: $environmentId) {
    assigneeId
    createdBy
    createdDate
    description
    dueDate
    id
    jobResumeId
    lastModifiedBy
    lastModifiedDate
    name
    priority
    status
    version
  }
}
    `);

export const useApprovalTasksQuery = <
      TData = ApprovalTasksQuery,
      TError = unknown
    >(
      variables?: ApprovalTasksQueryVariables,
      options?: Omit<UseQueryOptions<ApprovalTasksQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApprovalTasksQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApprovalTasksQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['approvalTasks'] : ['approvalTasks', variables],
    queryFn: fetcher<ApprovalTasksQuery, ApprovalTasksQueryVariables>(ApprovalTasksDocument, variables),
    ...options
  }
    )};

export const CreateApprovalTaskDocument = new TypedDocumentString(`
    mutation createApprovalTask($approvalTask: ApprovalTaskInput!) {
  createApprovalTask(approvalTask: $approvalTask) {
    assigneeId
    description
    id
    name
    priority
    status
  }
}
    `);

export const useCreateApprovalTaskMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateApprovalTaskMutation, TError, CreateApprovalTaskMutationVariables, TContext>) => {
    
    return useMutation<CreateApprovalTaskMutation, TError, CreateApprovalTaskMutationVariables, TContext>(
      {
    mutationKey: ['createApprovalTask'],
    mutationFn: (variables?: CreateApprovalTaskMutationVariables) => fetcher<CreateApprovalTaskMutation, CreateApprovalTaskMutationVariables>(CreateApprovalTaskDocument, variables)(),
    ...options
  }
    )};

export const DeleteApprovalTaskDocument = new TypedDocumentString(`
    mutation deleteApprovalTask($id: ID!) {
  deleteApprovalTask(id: $id)
}
    `);

export const useDeleteApprovalTaskMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteApprovalTaskMutation, TError, DeleteApprovalTaskMutationVariables, TContext>) => {
    
    return useMutation<DeleteApprovalTaskMutation, TError, DeleteApprovalTaskMutationVariables, TContext>(
      {
    mutationKey: ['deleteApprovalTask'],
    mutationFn: (variables?: DeleteApprovalTaskMutationVariables) => fetcher<DeleteApprovalTaskMutation, DeleteApprovalTaskMutationVariables>(DeleteApprovalTaskDocument, variables)(),
    ...options
  }
    )};

export const PendingApprovalsDocument = new TypedDocumentString(`
    query pendingApprovals($environmentId: Int) {
  pendingApprovals(environmentId: $environmentId) {
    createdDate
    expiresAt
    formUrl
    jobId
    workflowLabel
  }
}
    `);

export const usePendingApprovalsQuery = <
      TData = PendingApprovalsQuery,
      TError = unknown
    >(
      variables?: PendingApprovalsQueryVariables,
      options?: Omit<UseQueryOptions<PendingApprovalsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<PendingApprovalsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<PendingApprovalsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['pendingApprovals'] : ['pendingApprovals', variables],
    queryFn: fetcher<PendingApprovalsQuery, PendingApprovalsQueryVariables>(PendingApprovalsDocument, variables),
    ...options
  }
    )};

export const UpdateApprovalTaskDocument = new TypedDocumentString(`
    mutation updateApprovalTask($approvalTask: ApprovalTaskInput!) {
  updateApprovalTask(approvalTask: $approvalTask) {
    assigneeId
    description
    dueDate
    id
    name
    priority
    status
    version
  }
}
    `);

export const useUpdateApprovalTaskMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateApprovalTaskMutation, TError, UpdateApprovalTaskMutationVariables, TContext>) => {
    
    return useMutation<UpdateApprovalTaskMutation, TError, UpdateApprovalTaskMutationVariables, TContext>(
      {
    mutationKey: ['updateApprovalTask'],
    mutationFn: (variables?: UpdateApprovalTaskMutationVariables) => fetcher<UpdateApprovalTaskMutation, UpdateApprovalTaskMutationVariables>(UpdateApprovalTaskDocument, variables)(),
    ...options
  }
    )};

export const DeleteAssetFileDocument = new TypedDocumentString(`
    mutation DeleteAssetFile($id: ID!) {
  deleteAssetFile(id: $id)
}
    `);

export const useDeleteAssetFileMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAssetFileMutation, TError, DeleteAssetFileMutationVariables, TContext>) => {
    
    return useMutation<DeleteAssetFileMutation, TError, DeleteAssetFileMutationVariables, TContext>(
      {
    mutationKey: ['DeleteAssetFile'],
    mutationFn: (variables?: DeleteAssetFileMutationVariables) => fetcher<DeleteAssetFileMutation, DeleteAssetFileMutationVariables>(DeleteAssetFileDocument, variables)(),
    ...options
  }
    )};

export const DisableAssetFilePublicLinkDocument = new TypedDocumentString(`
    mutation DisableAssetFilePublicLink($id: ID!) {
  disableAssetFilePublicLink(id: $id) {
    id
    publicLinkUrl
  }
}
    `);

export const useDisableAssetFilePublicLinkMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DisableAssetFilePublicLinkMutation, TError, DisableAssetFilePublicLinkMutationVariables, TContext>) => {
    
    return useMutation<DisableAssetFilePublicLinkMutation, TError, DisableAssetFilePublicLinkMutationVariables, TContext>(
      {
    mutationKey: ['DisableAssetFilePublicLink'],
    mutationFn: (variables?: DisableAssetFilePublicLinkMutationVariables) => fetcher<DisableAssetFilePublicLinkMutation, DisableAssetFilePublicLinkMutationVariables>(DisableAssetFilePublicLinkDocument, variables)(),
    ...options
  }
    )};

export const EnableAssetFilePublicLinkDocument = new TypedDocumentString(`
    mutation EnableAssetFilePublicLink($id: ID!) {
  enableAssetFilePublicLink(id: $id) {
    id
    publicLinkUrl
  }
}
    `);

export const useEnableAssetFilePublicLinkMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableAssetFilePublicLinkMutation, TError, EnableAssetFilePublicLinkMutationVariables, TContext>) => {
    
    return useMutation<EnableAssetFilePublicLinkMutation, TError, EnableAssetFilePublicLinkMutationVariables, TContext>(
      {
    mutationKey: ['EnableAssetFilePublicLink'],
    mutationFn: (variables?: EnableAssetFilePublicLinkMutationVariables) => fetcher<EnableAssetFilePublicLinkMutation, EnableAssetFilePublicLinkMutationVariables>(EnableAssetFilePublicLinkDocument, variables)(),
    ...options
  }
    )};

export const GetAssetFileDocument = new TypedDocumentString(`
    query GetAssetFile($id: ID!) {
  assetFile(id: $id) {
    createdBy
    createdDate
    description
    downloadUrl
    environmentId
    format
    generatedByAgentSource
    generatedFromPrompt
    id
    lastModifiedBy
    lastModifiedDate
    metadataJson
    mimeType
    name
    publicLinkUrl
    sizeBytes
    source
    tags {
      id
      name
    }
  }
}
    `);

export const useGetAssetFileQuery = <
      TData = GetAssetFileQuery,
      TError = unknown
    >(
      variables: GetAssetFileQueryVariables,
      options?: Omit<UseQueryOptions<GetAssetFileQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetAssetFileQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetAssetFileQuery, TError, TData>(
      {
    queryKey: ['GetAssetFile', variables],
    queryFn: fetcher<GetAssetFileQuery, GetAssetFileQueryVariables>(GetAssetFileDocument, variables),
    ...options
  }
    )};

export const GetAssetFileSignedDownloadUrlDocument = new TypedDocumentString(`
    query GetAssetFileSignedDownloadUrl($id: ID!) {
  assetFileSignedDownloadUrl(id: $id)
}
    `);

export const useGetAssetFileSignedDownloadUrlQuery = <
      TData = GetAssetFileSignedDownloadUrlQuery,
      TError = unknown
    >(
      variables: GetAssetFileSignedDownloadUrlQueryVariables,
      options?: Omit<UseQueryOptions<GetAssetFileSignedDownloadUrlQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetAssetFileSignedDownloadUrlQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetAssetFileSignedDownloadUrlQuery, TError, TData>(
      {
    queryKey: ['GetAssetFileSignedDownloadUrl', variables],
    queryFn: fetcher<GetAssetFileSignedDownloadUrlQuery, GetAssetFileSignedDownloadUrlQueryVariables>(GetAssetFileSignedDownloadUrlDocument, variables),
    ...options
  }
    )};

export const GetAssetFileTagsDocument = new TypedDocumentString(`
    query GetAssetFileTags($workspaceId: ID!) {
  assetFileTags(workspaceId: $workspaceId) {
    id
    name
  }
}
    `);

export const useGetAssetFileTagsQuery = <
      TData = GetAssetFileTagsQuery,
      TError = unknown
    >(
      variables: GetAssetFileTagsQueryVariables,
      options?: Omit<UseQueryOptions<GetAssetFileTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetAssetFileTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetAssetFileTagsQuery, TError, TData>(
      {
    queryKey: ['GetAssetFileTags', variables],
    queryFn: fetcher<GetAssetFileTagsQuery, GetAssetFileTagsQueryVariables>(GetAssetFileTagsDocument, variables),
    ...options
  }
    )};

export const GetAssetFileTextContentDocument = new TypedDocumentString(`
    query GetAssetFileTextContent($id: ID!) {
  assetFileTextContent(id: $id)
}
    `);

export const useGetAssetFileTextContentQuery = <
      TData = GetAssetFileTextContentQuery,
      TError = unknown
    >(
      variables: GetAssetFileTextContentQueryVariables,
      options?: Omit<UseQueryOptions<GetAssetFileTextContentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetAssetFileTextContentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetAssetFileTextContentQuery, TError, TData>(
      {
    queryKey: ['GetAssetFileTextContent', variables],
    queryFn: fetcher<GetAssetFileTextContentQuery, GetAssetFileTextContentQueryVariables>(GetAssetFileTextContentDocument, variables),
    ...options
  }
    )};

export const GetAssetFileVersionsDocument = new TypedDocumentString(`
    query GetAssetFileVersions($id: ID!) {
  assetFileVersions(id: $id) {
    createdBy
    createdDate
    id
    mimeType
    sizeBytes
    versionNumber
  }
}
    `);

export const useGetAssetFileVersionsQuery = <
      TData = GetAssetFileVersionsQuery,
      TError = unknown
    >(
      variables: GetAssetFileVersionsQueryVariables,
      options?: Omit<UseQueryOptions<GetAssetFileVersionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetAssetFileVersionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetAssetFileVersionsQuery, TError, TData>(
      {
    queryKey: ['GetAssetFileVersions', variables],
    queryFn: fetcher<GetAssetFileVersionsQuery, GetAssetFileVersionsQueryVariables>(GetAssetFileVersionsDocument, variables),
    ...options
  }
    )};

export const GetAssetFilesDocument = new TypedDocumentString(`
    query GetAssetFiles($workspaceId: ID!, $environment: Int, $tagIds: [ID!], $mimeTypePrefix: String) {
  assetFiles(
    workspaceId: $workspaceId
    environment: $environment
    tagIds: $tagIds
    mimeTypePrefix: $mimeTypePrefix
  ) {
    createdBy
    createdDate
    description
    downloadUrl
    environmentId
    format
    generatedByAgentSource
    generatedFromPrompt
    id
    lastModifiedBy
    lastModifiedDate
    metadataJson
    mimeType
    name
    publicLinkUrl
    sizeBytes
    source
    tags {
      id
      name
    }
  }
}
    `);

export const useGetAssetFilesQuery = <
      TData = GetAssetFilesQuery,
      TError = unknown
    >(
      variables: GetAssetFilesQueryVariables,
      options?: Omit<UseQueryOptions<GetAssetFilesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GetAssetFilesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GetAssetFilesQuery, TError, TData>(
      {
    queryKey: ['GetAssetFiles', variables],
    queryFn: fetcher<GetAssetFilesQuery, GetAssetFilesQueryVariables>(GetAssetFilesDocument, variables),
    ...options
  }
    )};

export const RestoreAssetFileVersionDocument = new TypedDocumentString(`
    mutation RestoreAssetFileVersion($id: ID!, $versionId: ID!) {
  restoreAssetFileVersion(id: $id, versionId: $versionId) {
    id
    lastModifiedDate
    mimeType
    name
    sizeBytes
  }
}
    `);

export const useRestoreAssetFileVersionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RestoreAssetFileVersionMutation, TError, RestoreAssetFileVersionMutationVariables, TContext>) => {
    
    return useMutation<RestoreAssetFileVersionMutation, TError, RestoreAssetFileVersionMutationVariables, TContext>(
      {
    mutationKey: ['RestoreAssetFileVersion'],
    mutationFn: (variables?: RestoreAssetFileVersionMutationVariables) => fetcher<RestoreAssetFileVersionMutation, RestoreAssetFileVersionMutationVariables>(RestoreAssetFileVersionDocument, variables)(),
    ...options
  }
    )};

export const UpdateAssetFileDocument = new TypedDocumentString(`
    mutation UpdateAssetFile($input: UpdateAssetFileInput!) {
  updateAssetFile(input: $input) {
    createdBy
    createdDate
    description
    downloadUrl
    id
    lastModifiedBy
    lastModifiedDate
    mimeType
    name
    sizeBytes
    source
  }
}
    `);

export const useUpdateAssetFileMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAssetFileMutation, TError, UpdateAssetFileMutationVariables, TContext>) => {
    
    return useMutation<UpdateAssetFileMutation, TError, UpdateAssetFileMutationVariables, TContext>(
      {
    mutationKey: ['UpdateAssetFile'],
    mutationFn: (variables?: UpdateAssetFileMutationVariables) => fetcher<UpdateAssetFileMutation, UpdateAssetFileMutationVariables>(UpdateAssetFileDocument, variables)(),
    ...options
  }
    )};

export const UpdateAssetFileTagsDocument = new TypedDocumentString(`
    mutation UpdateAssetFileTags($input: UpdateAssetFileTagsInput!) {
  updateAssetFileTags(input: $input) {
    id
    tags {
      id
      name
    }
  }
}
    `);

export const useUpdateAssetFileTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAssetFileTagsMutation, TError, UpdateAssetFileTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAssetFileTagsMutation, TError, UpdateAssetFileTagsMutationVariables, TContext>(
      {
    mutationKey: ['UpdateAssetFileTags'],
    mutationFn: (variables?: UpdateAssetFileTagsMutationVariables) => fetcher<UpdateAssetFileTagsMutation, UpdateAssetFileTagsMutationVariables>(UpdateAssetFileTagsDocument, variables)(),
    ...options
  }
    )};

export const UpdateAssetFileTextContentDocument = new TypedDocumentString(`
    mutation UpdateAssetFileTextContent($id: ID!, $content: String!) {
  updateAssetFileTextContent(id: $id, content: $content) {
    id
    lastModifiedDate
    sizeBytes
  }
}
    `);

export const useUpdateAssetFileTextContentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAssetFileTextContentMutation, TError, UpdateAssetFileTextContentMutationVariables, TContext>) => {
    
    return useMutation<UpdateAssetFileTextContentMutation, TError, UpdateAssetFileTextContentMutationVariables, TContext>(
      {
    mutationKey: ['UpdateAssetFileTextContent'],
    mutationFn: (variables?: UpdateAssetFileTextContentMutationVariables) => fetcher<UpdateAssetFileTextContentMutation, UpdateAssetFileTextContentMutationVariables>(UpdateAssetFileTextContentDocument, variables)(),
    ...options
  }
    )};

export const A2aProjectWorkflowsByA2aProjectIdDocument = new TypedDocumentString(`
    query a2aProjectWorkflowsByA2aProjectId($a2aProjectId: ID!) {
  a2aProjectWorkflowsByA2aProjectId(a2aProjectId: $a2aProjectId) {
    id
    skillDescription
    skillName
    skillTags
    workflowId
    workflowLabel
  }
}
    `);

export const useA2aProjectWorkflowsByA2aProjectIdQuery = <
      TData = A2aProjectWorkflowsByA2aProjectIdQuery,
      TError = unknown
    >(
      variables: A2aProjectWorkflowsByA2aProjectIdQueryVariables,
      options?: Omit<UseQueryOptions<A2aProjectWorkflowsByA2aProjectIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<A2aProjectWorkflowsByA2aProjectIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<A2aProjectWorkflowsByA2aProjectIdQuery, TError, TData>(
      {
    queryKey: ['a2aProjectWorkflowsByA2aProjectId', variables],
    queryFn: fetcher<A2aProjectWorkflowsByA2aProjectIdQuery, A2aProjectWorkflowsByA2aProjectIdQueryVariables>(A2aProjectWorkflowsByA2aProjectIdDocument, variables),
    ...options
  }
    )};

export const A2aProjectsByServerIdDocument = new TypedDocumentString(`
    query a2aProjectsByServerId($a2aServerId: ID!) {
  a2aProjectsByServerId(a2aServerId: $a2aServerId) {
    a2aServerId
    id
    projectId
    projectVersion
    workflowIds
  }
}
    `);

export const useA2aProjectsByServerIdQuery = <
      TData = A2aProjectsByServerIdQuery,
      TError = unknown
    >(
      variables: A2aProjectsByServerIdQueryVariables,
      options?: Omit<UseQueryOptions<A2aProjectsByServerIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<A2aProjectsByServerIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<A2aProjectsByServerIdQuery, TError, TData>(
      {
    queryKey: ['a2aProjectsByServerId', variables],
    queryFn: fetcher<A2aProjectsByServerIdQuery, A2aProjectsByServerIdQueryVariables>(A2aProjectsByServerIdDocument, variables),
    ...options
  }
    )};

export const A2aServersDocument = new TypedDocumentString(`
    query a2aServers($type: PlatformType!) {
  a2aServers(type: $type) {
    authenticationRequired
    createdDate
    description
    enabled
    environmentId
    id
    lastModifiedDate
    name
    secretKey
    type
    version
  }
}
    `);

export const useA2aServersQuery = <
      TData = A2aServersQuery,
      TError = unknown
    >(
      variables: A2aServersQueryVariables,
      options?: Omit<UseQueryOptions<A2aServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<A2aServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<A2aServersQuery, TError, TData>(
      {
    queryKey: ['a2aServers', variables],
    queryFn: fetcher<A2aServersQuery, A2aServersQueryVariables>(A2aServersDocument, variables),
    ...options
  }
    )};

export const AddWorkspaceUserDocument = new TypedDocumentString(`
    mutation AddWorkspaceUser($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole, $customRoleId: ID) {
  addWorkspaceUser(
    workspaceId: $workspaceId
    userId: $userId
    role: $role
    customRoleId: $customRoleId
  ) {
    id
    workspaceId
    userId
    workspaceRole
    customRoleId
    user {
      email
      firstName
      lastName
    }
  }
}
    `);

export const useAddWorkspaceUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddWorkspaceUserMutation, TError, AddWorkspaceUserMutationVariables, TContext>) => {
    
    return useMutation<AddWorkspaceUserMutation, TError, AddWorkspaceUserMutationVariables, TContext>(
      {
    mutationKey: ['AddWorkspaceUser'],
    mutationFn: (variables?: AddWorkspaceUserMutationVariables) => fetcher<AddWorkspaceUserMutation, AddWorkspaceUserMutationVariables>(AddWorkspaceUserDocument, variables)(),
    ...options
  }
    )};

export const AffectedWorkflowsDocument = new TypedDocumentString(`
    query affectedWorkflows($workspaceId: ID!, $userLogin: String!) {
  affectedWorkflows(workspaceId: $workspaceId, userLogin: $userLogin) {
    workflowId
    workflowName
    connectionIds
  }
}
    `);

export const useAffectedWorkflowsQuery = <
      TData = AffectedWorkflowsQuery,
      TError = unknown
    >(
      variables: AffectedWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<AffectedWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AffectedWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AffectedWorkflowsQuery, TError, TData>(
      {
    queryKey: ['affectedWorkflows', variables],
    queryFn: fetcher<AffectedWorkflowsQuery, AffectedWorkflowsQueryVariables>(AffectedWorkflowsDocument, variables),
    ...options
  }
    )};

export const AssignWorkspaceUserCustomRoleDocument = new TypedDocumentString(`
    mutation AssignWorkspaceUserCustomRole($workspaceId: ID!, $userId: ID!, $customRoleId: ID!) {
  assignWorkspaceUserCustomRole(
    workspaceId: $workspaceId
    userId: $userId
    customRoleId: $customRoleId
  ) {
    id
    userId
    workspaceRole
    customRoleId
  }
}
    `);

export const useAssignWorkspaceUserCustomRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AssignWorkspaceUserCustomRoleMutation, TError, AssignWorkspaceUserCustomRoleMutationVariables, TContext>) => {
    
    return useMutation<AssignWorkspaceUserCustomRoleMutation, TError, AssignWorkspaceUserCustomRoleMutationVariables, TContext>(
      {
    mutationKey: ['AssignWorkspaceUserCustomRole'],
    mutationFn: (variables?: AssignWorkspaceUserCustomRoleMutationVariables) => fetcher<AssignWorkspaceUserCustomRoleMutation, AssignWorkspaceUserCustomRoleMutationVariables>(AssignWorkspaceUserCustomRoleDocument, variables)(),
    ...options
  }
    )};

export const ConnectionGrantsDocument = new TypedDocumentString(`
    query ConnectionGrants($workspaceId: ID!, $connectionId: ID!) {
  connectionGrants(workspaceId: $workspaceId, connectionId: $connectionId)
}
    `);

export const useConnectionGrantsQuery = <
      TData = ConnectionGrantsQuery,
      TError = unknown
    >(
      variables: ConnectionGrantsQueryVariables,
      options?: Omit<UseQueryOptions<ConnectionGrantsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ConnectionGrantsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ConnectionGrantsQuery, TError, TData>(
      {
    queryKey: ['ConnectionGrants', variables],
    queryFn: fetcher<ConnectionGrantsQuery, ConnectionGrantsQueryVariables>(ConnectionGrantsDocument, variables),
    ...options
  }
    )};

export const CreateA2aProjectDocument = new TypedDocumentString(`
    mutation createA2aProject($input: CreateA2aProjectInput!) {
  createA2aProject(input: $input) {
    id
  }
}
    `);

export const useCreateA2aProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateA2aProjectMutation, TError, CreateA2aProjectMutationVariables, TContext>) => {
    
    return useMutation<CreateA2aProjectMutation, TError, CreateA2aProjectMutationVariables, TContext>(
      {
    mutationKey: ['createA2aProject'],
    mutationFn: (variables?: CreateA2aProjectMutationVariables) => fetcher<CreateA2aProjectMutation, CreateA2aProjectMutationVariables>(CreateA2aProjectDocument, variables)(),
    ...options
  }
    )};

export const CreateA2aServerDocument = new TypedDocumentString(`
    mutation createA2aServer($input: CreateA2aServerInput!) {
  createA2aServer(input: $input) {
    id
  }
}
    `);

export const useCreateA2aServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateA2aServerMutation, TError, CreateA2aServerMutationVariables, TContext>) => {
    
    return useMutation<CreateA2aServerMutation, TError, CreateA2aServerMutationVariables, TContext>(
      {
    mutationKey: ['createA2aServer'],
    mutationFn: (variables?: CreateA2aServerMutationVariables) => fetcher<CreateA2aServerMutation, CreateA2aServerMutationVariables>(CreateA2aServerDocument, variables)(),
    ...options
  }
    )};

export const CreateCustomRoleDocument = new TypedDocumentString(`
    mutation CreateCustomRole($input: CreateCustomRoleInput!) {
  createCustomRole(input: $input) {
    id
    name
    description
    scopes
  }
}
    `);

export const useCreateCustomRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateCustomRoleMutation, TError, CreateCustomRoleMutationVariables, TContext>) => {
    
    return useMutation<CreateCustomRoleMutation, TError, CreateCustomRoleMutationVariables, TContext>(
      {
    mutationKey: ['CreateCustomRole'],
    mutationFn: (variables?: CreateCustomRoleMutationVariables) => fetcher<CreateCustomRoleMutation, CreateCustomRoleMutationVariables>(CreateCustomRoleDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpProjectDocument = new TypedDocumentString(`
    mutation createMcpProject($input: CreateMcpProjectInput!) {
  createMcpProject(input: $input) {
    id
    mcpServerId
    projectDeploymentId
    projectVersion
  }
}
    `);

export const useCreateMcpProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpProjectMutation, TError, CreateMcpProjectMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpProjectMutation, TError, CreateMcpProjectMutationVariables, TContext>(
      {
    mutationKey: ['createMcpProject'],
    mutationFn: (variables?: CreateMcpProjectMutationVariables) => fetcher<CreateMcpProjectMutation, CreateMcpProjectMutationVariables>(CreateMcpProjectDocument, variables)(),
    ...options
  }
    )};

export const CreateOrganizationConnectionDocument = new TypedDocumentString(`
    mutation createOrganizationConnection($input: CreateOrganizationConnectionInput!) {
  createOrganizationConnection(input: $input)
}
    `);

export const useCreateOrganizationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateOrganizationConnectionMutation, TError, CreateOrganizationConnectionMutationVariables, TContext>) => {
    
    return useMutation<CreateOrganizationConnectionMutation, TError, CreateOrganizationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['createOrganizationConnection'],
    mutationFn: (variables?: CreateOrganizationConnectionMutationVariables) => fetcher<CreateOrganizationConnectionMutation, CreateOrganizationConnectionMutationVariables>(CreateOrganizationConnectionDocument, variables)(),
    ...options
  }
    )};

export const CreateWorkspaceApiKeyDocument = new TypedDocumentString(`
    mutation createWorkspaceApiKey($workspaceId: ID!, $name: String!, $environmentId: ID!) {
  createWorkspaceApiKey(
    workspaceId: $workspaceId
    name: $name
    environmentId: $environmentId
  )
}
    `);

export const useCreateWorkspaceApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceApiKeyMutation, TError, CreateWorkspaceApiKeyMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceApiKeyMutation, TError, CreateWorkspaceApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceApiKey'],
    mutationFn: (variables?: CreateWorkspaceApiKeyMutationVariables) => fetcher<CreateWorkspaceApiKeyMutation, CreateWorkspaceApiKeyMutationVariables>(CreateWorkspaceApiKeyDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpServerDocument = new TypedDocumentString(`
    mutation createMcpServer($input: CreateWorkspaceMcpServerInput!) {
  createWorkspaceMcpServer(input: $input) {
    id
    name
    type
    environmentId
    enabled
  }
}
    `);

export const useCreateMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpServerMutation, TError, CreateMcpServerMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpServerMutation, TError, CreateMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['createMcpServer'],
    mutationFn: (variables?: CreateMcpServerMutationVariables) => fetcher<CreateMcpServerMutation, CreateMcpServerMutationVariables>(CreateMcpServerDocument, variables)(),
    ...options
  }
    )};

export const CustomRolesDocument = new TypedDocumentString(`
    query CustomRoles($workspaceId: ID) {
  customRoles(workspaceId: $workspaceId) {
    id
    name
    description
    scopes
  }
}
    `);

export const useCustomRolesQuery = <
      TData = CustomRolesQuery,
      TError = unknown
    >(
      variables?: CustomRolesQueryVariables,
      options?: Omit<UseQueryOptions<CustomRolesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CustomRolesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CustomRolesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['CustomRoles'] : ['CustomRoles', variables],
    queryFn: fetcher<CustomRolesQuery, CustomRolesQueryVariables>(CustomRolesDocument, variables),
    ...options
  }
    )};

export const DataStreamCompatibleConnectionsDocument = new TypedDocumentString(`
    query dataStreamCompatibleConnections($workspaceId: ID!, $environmentId: ID!) {
  dataStreamCompatibleConnections(
    workspaceId: $workspaceId
    environmentId: $environmentId
  ) {
    id
    name
    componentName
    componentVersion
  }
}
    `);

export const useDataStreamCompatibleConnectionsQuery = <
      TData = DataStreamCompatibleConnectionsQuery,
      TError = unknown
    >(
      variables: DataStreamCompatibleConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<DataStreamCompatibleConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataStreamCompatibleConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataStreamCompatibleConnectionsQuery, TError, TData>(
      {
    queryKey: ['dataStreamCompatibleConnections', variables],
    queryFn: fetcher<DataStreamCompatibleConnectionsQuery, DataStreamCompatibleConnectionsQueryVariables>(DataStreamCompatibleConnectionsDocument, variables),
    ...options
  }
    )};

export const DeleteA2aProjectDocument = new TypedDocumentString(`
    mutation deleteA2aProject($id: ID!) {
  deleteA2aProject(id: $id)
}
    `);

export const useDeleteA2aProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteA2aProjectMutation, TError, DeleteA2aProjectMutationVariables, TContext>) => {
    
    return useMutation<DeleteA2aProjectMutation, TError, DeleteA2aProjectMutationVariables, TContext>(
      {
    mutationKey: ['deleteA2aProject'],
    mutationFn: (variables?: DeleteA2aProjectMutationVariables) => fetcher<DeleteA2aProjectMutation, DeleteA2aProjectMutationVariables>(DeleteA2aProjectDocument, variables)(),
    ...options
  }
    )};

export const DeleteA2aServerDocument = new TypedDocumentString(`
    mutation deleteA2aServer($id: ID!) {
  deleteA2aServer(id: $id)
}
    `);

export const useDeleteA2aServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteA2aServerMutation, TError, DeleteA2aServerMutationVariables, TContext>) => {
    
    return useMutation<DeleteA2aServerMutation, TError, DeleteA2aServerMutationVariables, TContext>(
      {
    mutationKey: ['deleteA2aServer'],
    mutationFn: (variables?: DeleteA2aServerMutationVariables) => fetcher<DeleteA2aServerMutation, DeleteA2aServerMutationVariables>(DeleteA2aServerDocument, variables)(),
    ...options
  }
    )};

export const DeleteCustomRoleDocument = new TypedDocumentString(`
    mutation DeleteCustomRole($id: ID!) {
  deleteCustomRole(id: $id)
}
    `);

export const useDeleteCustomRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteCustomRoleMutation, TError, DeleteCustomRoleMutationVariables, TContext>) => {
    
    return useMutation<DeleteCustomRoleMutation, TError, DeleteCustomRoleMutationVariables, TContext>(
      {
    mutationKey: ['DeleteCustomRole'],
    mutationFn: (variables?: DeleteCustomRoleMutationVariables) => fetcher<DeleteCustomRoleMutation, DeleteCustomRoleMutationVariables>(DeleteCustomRoleDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpProjectDocument = new TypedDocumentString(`
    mutation deleteMcpProject($id: ID!) {
  deleteMcpProject(id: $id)
}
    `);

export const useDeleteMcpProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpProjectMutation, TError, DeleteMcpProjectMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpProjectMutation, TError, DeleteMcpProjectMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpProject'],
    mutationFn: (variables?: DeleteMcpProjectMutationVariables) => fetcher<DeleteMcpProjectMutation, DeleteMcpProjectMutationVariables>(DeleteMcpProjectDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpProjectWorkflowDocument = new TypedDocumentString(`
    mutation deleteMcpProjectWorkflow($id: ID!) {
  deleteMcpProjectWorkflow(id: $id)
}
    `);

export const useDeleteMcpProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpProjectWorkflowMutation, TError, DeleteMcpProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpProjectWorkflowMutation, TError, DeleteMcpProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpProjectWorkflow'],
    mutationFn: (variables?: DeleteMcpProjectWorkflowMutationVariables) => fetcher<DeleteMcpProjectWorkflowMutation, DeleteMcpProjectWorkflowMutationVariables>(DeleteMcpProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const DeleteOrganizationConnectionDocument = new TypedDocumentString(`
    mutation deleteOrganizationConnection($connectionId: ID!) {
  deleteOrganizationConnection(connectionId: $connectionId)
}
    `);

export const useDeleteOrganizationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteOrganizationConnectionMutation, TError, DeleteOrganizationConnectionMutationVariables, TContext>) => {
    
    return useMutation<DeleteOrganizationConnectionMutation, TError, DeleteOrganizationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['deleteOrganizationConnection'],
    mutationFn: (variables?: DeleteOrganizationConnectionMutationVariables) => fetcher<DeleteOrganizationConnectionMutation, DeleteOrganizationConnectionMutationVariables>(DeleteOrganizationConnectionDocument, variables)(),
    ...options
  }
    )};

export const DeleteSharedProjectDocument = new TypedDocumentString(`
    mutation deleteSharedProject($id: ID!) {
  deleteSharedProject(id: $id)
}
    `);

export const useDeleteSharedProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteSharedProjectMutation, TError, DeleteSharedProjectMutationVariables, TContext>) => {
    
    return useMutation<DeleteSharedProjectMutation, TError, DeleteSharedProjectMutationVariables, TContext>(
      {
    mutationKey: ['deleteSharedProject'],
    mutationFn: (variables?: DeleteSharedProjectMutationVariables) => fetcher<DeleteSharedProjectMutation, DeleteSharedProjectMutationVariables>(DeleteSharedProjectDocument, variables)(),
    ...options
  }
    )};

export const DeleteSharedWorkflowDocument = new TypedDocumentString(`
    mutation deleteSharedWorkflow($workflowId: String!) {
  deleteSharedWorkflow(workflowId: $workflowId)
}
    `);

export const useDeleteSharedWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteSharedWorkflowMutation, TError, DeleteSharedWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DeleteSharedWorkflowMutation, TError, DeleteSharedWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['deleteSharedWorkflow'],
    mutationFn: (variables?: DeleteSharedWorkflowMutationVariables) => fetcher<DeleteSharedWorkflowMutation, DeleteSharedWorkflowMutationVariables>(DeleteSharedWorkflowDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceApiKeyDocument = new TypedDocumentString(`
    mutation deleteWorkspaceApiKey($apiKeyId: ID!) {
  deleteWorkspaceApiKey(apiKeyId: $apiKeyId)
}
    `);

export const useDeleteWorkspaceApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceApiKeyMutation, TError, DeleteWorkspaceApiKeyMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceApiKeyMutation, TError, DeleteWorkspaceApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceApiKey'],
    mutationFn: (variables?: DeleteWorkspaceApiKeyMutationVariables) => fetcher<DeleteWorkspaceApiKeyMutation, DeleteWorkspaceApiKeyMutationVariables>(DeleteWorkspaceApiKeyDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceMcpServerDocument = new TypedDocumentString(`
    mutation deleteWorkspaceMcpServer($id: ID!) {
  deleteWorkspaceMcpServer(mcpServerId: $id)
}
    `);

export const useDeleteWorkspaceMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceMcpServerMutation, TError, DeleteWorkspaceMcpServerMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceMcpServerMutation, TError, DeleteWorkspaceMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceMcpServer'],
    mutationFn: (variables?: DeleteWorkspaceMcpServerMutationVariables) => fetcher<DeleteWorkspaceMcpServerMutation, DeleteWorkspaceMcpServerMutationVariables>(DeleteWorkspaceMcpServerDocument, variables)(),
    ...options
  }
    )};

export const DisconnectConnectionDocument = new TypedDocumentString(`
    mutation DisconnectConnection($connectionId: ID!) {
  disconnectConnection(connectionId: $connectionId)
}
    `);

export const useDisconnectConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DisconnectConnectionMutation, TError, DisconnectConnectionMutationVariables, TContext>) => {
    
    return useMutation<DisconnectConnectionMutation, TError, DisconnectConnectionMutationVariables, TContext>(
      {
    mutationKey: ['DisconnectConnection'],
    mutationFn: (variables?: DisconnectConnectionMutationVariables) => fetcher<DisconnectConnectionMutation, DisconnectConnectionMutationVariables>(DisconnectConnectionDocument, variables)(),
    ...options
  }
    )};

export const ProjectErrorWorkflowDocument = new TypedDocumentString(`
    query projectErrorWorkflow($id: ID!) {
  project(id: $id) {
    errorProjectWorkflowId
  }
}
    `);

export const useProjectErrorWorkflowQuery = <
      TData = ProjectErrorWorkflowQuery,
      TError = unknown
    >(
      variables: ProjectErrorWorkflowQueryVariables,
      options?: Omit<UseQueryOptions<ProjectErrorWorkflowQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ProjectErrorWorkflowQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ProjectErrorWorkflowQuery, TError, TData>(
      {
    queryKey: ['projectErrorWorkflow', variables],
    queryFn: fetcher<ProjectErrorWorkflowQuery, ProjectErrorWorkflowQueryVariables>(ProjectErrorWorkflowDocument, variables),
    ...options
  }
    )};

export const UpdateProjectErrorWorkflowDocument = new TypedDocumentString(`
    mutation updateProjectErrorWorkflow($projectId: ID!, $errorProjectWorkflowId: ID) {
  updateProjectErrorWorkflow(
    projectId: $projectId
    errorProjectWorkflowId: $errorProjectWorkflowId
  )
}
    `);

export const useUpdateProjectErrorWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateProjectErrorWorkflowMutation, TError, UpdateProjectErrorWorkflowMutationVariables, TContext>) => {
    
    return useMutation<UpdateProjectErrorWorkflowMutation, TError, UpdateProjectErrorWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['updateProjectErrorWorkflow'],
    mutationFn: (variables?: UpdateProjectErrorWorkflowMutationVariables) => fetcher<UpdateProjectErrorWorkflowMutation, UpdateProjectErrorWorkflowMutationVariables>(UpdateProjectErrorWorkflowDocument, variables)(),
    ...options
  }
    )};

export const EligibleErrorWorkflowsDocument = new TypedDocumentString(`
    query eligibleErrorWorkflows($projectId: ID!, $projectVersion: Int!) {
  eligibleErrorWorkflows(projectId: $projectId, projectVersion: $projectVersion) {
    id
    workflowId
    workflow {
      label
    }
  }
}
    `);

export const useEligibleErrorWorkflowsQuery = <
      TData = EligibleErrorWorkflowsQuery,
      TError = unknown
    >(
      variables: EligibleErrorWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<EligibleErrorWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EligibleErrorWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EligibleErrorWorkflowsQuery, TError, TData>(
      {
    queryKey: ['eligibleErrorWorkflows', variables],
    queryFn: fetcher<EligibleErrorWorkflowsQuery, EligibleErrorWorkflowsQueryVariables>(EligibleErrorWorkflowsDocument, variables),
    ...options
  }
    )};

export const ProjectWorkflowErrorConfigDocument = new TypedDocumentString(`
    query projectWorkflowErrorConfig($id: ID!) {
  projectWorkflow(id: $id) {
    errorProjectWorkflowId
    errorWorkflowDisabled
  }
}
    `);

export const useProjectWorkflowErrorConfigQuery = <
      TData = ProjectWorkflowErrorConfigQuery,
      TError = unknown
    >(
      variables: ProjectWorkflowErrorConfigQueryVariables,
      options?: Omit<UseQueryOptions<ProjectWorkflowErrorConfigQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ProjectWorkflowErrorConfigQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ProjectWorkflowErrorConfigQuery, TError, TData>(
      {
    queryKey: ['projectWorkflowErrorConfig', variables],
    queryFn: fetcher<ProjectWorkflowErrorConfigQuery, ProjectWorkflowErrorConfigQueryVariables>(ProjectWorkflowErrorConfigDocument, variables),
    ...options
  }
    )};

export const UpdateProjectWorkflowErrorWorkflowDocument = new TypedDocumentString(`
    mutation updateProjectWorkflowErrorWorkflow($projectId: ID!, $projectWorkflowId: ID!, $errorProjectWorkflowId: ID, $errorWorkflowDisabled: Boolean!) {
  updateProjectWorkflowErrorWorkflow(
    projectId: $projectId
    projectWorkflowId: $projectWorkflowId
    errorProjectWorkflowId: $errorProjectWorkflowId
    errorWorkflowDisabled: $errorWorkflowDisabled
  )
}
    `);

export const useUpdateProjectWorkflowErrorWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateProjectWorkflowErrorWorkflowMutation, TError, UpdateProjectWorkflowErrorWorkflowMutationVariables, TContext>) => {
    
    return useMutation<UpdateProjectWorkflowErrorWorkflowMutation, TError, UpdateProjectWorkflowErrorWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['updateProjectWorkflowErrorWorkflow'],
    mutationFn: (variables?: UpdateProjectWorkflowErrorWorkflowMutationVariables) => fetcher<UpdateProjectWorkflowErrorWorkflowMutation, UpdateProjectWorkflowErrorWorkflowMutationVariables>(UpdateProjectWorkflowErrorWorkflowDocument, variables)(),
    ...options
  }
    )};

export const ExportSharedProjectDocument = new TypedDocumentString(`
    mutation exportSharedProject($id: ID!, $description: String) {
  exportSharedProject(id: $id, description: $description)
}
    `);

export const useExportSharedProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ExportSharedProjectMutation, TError, ExportSharedProjectMutationVariables, TContext>) => {
    
    return useMutation<ExportSharedProjectMutation, TError, ExportSharedProjectMutationVariables, TContext>(
      {
    mutationKey: ['exportSharedProject'],
    mutationFn: (variables?: ExportSharedProjectMutationVariables) => fetcher<ExportSharedProjectMutation, ExportSharedProjectMutationVariables>(ExportSharedProjectDocument, variables)(),
    ...options
  }
    )};

export const ExportSharedWorkflowDocument = new TypedDocumentString(`
    mutation exportSharedWorkflow($workflowId: String!, $description: String) {
  exportSharedWorkflow(workflowId: $workflowId, description: $description)
}
    `);

export const useExportSharedWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ExportSharedWorkflowMutation, TError, ExportSharedWorkflowMutationVariables, TContext>) => {
    
    return useMutation<ExportSharedWorkflowMutation, TError, ExportSharedWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['exportSharedWorkflow'],
    mutationFn: (variables?: ExportSharedWorkflowMutationVariables) => fetcher<ExportSharedWorkflowMutation, ExportSharedWorkflowMutationVariables>(ExportSharedWorkflowDocument, variables)(),
    ...options
  }
    )};

export const GrantConnectionAccessDocument = new TypedDocumentString(`
    mutation GrantConnectionAccess($workspaceId: ID!, $connectionId: ID!, $userId: ID!) {
  grantConnectionAccess(
    workspaceId: $workspaceId
    connectionId: $connectionId
    userId: $userId
  )
}
    `);

export const useGrantConnectionAccessMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<GrantConnectionAccessMutation, TError, GrantConnectionAccessMutationVariables, TContext>) => {
    
    return useMutation<GrantConnectionAccessMutation, TError, GrantConnectionAccessMutationVariables, TContext>(
      {
    mutationKey: ['GrantConnectionAccess'],
    mutationFn: (variables?: GrantConnectionAccessMutationVariables) => fetcher<GrantConnectionAccessMutation, GrantConnectionAccessMutationVariables>(GrantConnectionAccessDocument, variables)(),
    ...options
  }
    )};

export const GrantProjectAccessDocument = new TypedDocumentString(`
    mutation GrantProjectAccess($workspaceId: ID!, $projectId: ID!, $userId: ID!) {
  grantProjectAccess(
    workspaceId: $workspaceId
    projectId: $projectId
    userId: $userId
  )
}
    `);

export const useGrantProjectAccessMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<GrantProjectAccessMutation, TError, GrantProjectAccessMutationVariables, TContext>) => {
    
    return useMutation<GrantProjectAccessMutation, TError, GrantProjectAccessMutationVariables, TContext>(
      {
    mutationKey: ['GrantProjectAccess'],
    mutationFn: (variables?: GrantProjectAccessMutationVariables) => fetcher<GrantProjectAccessMutation, GrantProjectAccessMutationVariables>(GrantProjectAccessDocument, variables)(),
    ...options
  }
    )};

export const ImportProjectTemplateDocument = new TypedDocumentString(`
    mutation importProjectTemplate($id: String!, $workspaceId: ID!, $sharedProject: Boolean!) {
  importProjectTemplate(
    id: $id
    workspaceId: $workspaceId
    sharedProject: $sharedProject
  )
}
    `);

export const useImportProjectTemplateMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportProjectTemplateMutation, TError, ImportProjectTemplateMutationVariables, TContext>) => {
    
    return useMutation<ImportProjectTemplateMutation, TError, ImportProjectTemplateMutationVariables, TContext>(
      {
    mutationKey: ['importProjectTemplate'],
    mutationFn: (variables?: ImportProjectTemplateMutationVariables) => fetcher<ImportProjectTemplateMutation, ImportProjectTemplateMutationVariables>(ImportProjectTemplateDocument, variables)(),
    ...options
  }
    )};

export const ImportWorkflowTemplateDocument = new TypedDocumentString(`
    mutation importWorkflowTemplate($workflowUuid: String!, $projectId: ID!, $sharedWorkflow: Boolean!) {
  importWorkflowTemplate(
    id: $workflowUuid
    projectId: $projectId
    sharedWorkflow: $sharedWorkflow
  )
}
    `);

export const useImportWorkflowTemplateMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportWorkflowTemplateMutation, TError, ImportWorkflowTemplateMutationVariables, TContext>) => {
    
    return useMutation<ImportWorkflowTemplateMutation, TError, ImportWorkflowTemplateMutationVariables, TContext>(
      {
    mutationKey: ['importWorkflowTemplate'],
    mutationFn: (variables?: ImportWorkflowTemplateMutationVariables) => fetcher<ImportWorkflowTemplateMutation, ImportWorkflowTemplateMutationVariables>(ImportWorkflowTemplateDocument, variables)(),
    ...options
  }
    )};

export const InviteWorkspaceUserDocument = new TypedDocumentString(`
    mutation inviteWorkspaceUser($workspaceId: ID!, $email: String!, $role: WorkspaceRole, $customRoleId: ID) {
  inviteWorkspaceUser(
    workspaceId: $workspaceId
    email: $email
    role: $role
    customRoleId: $customRoleId
  ) {
    id
    userId
    workspaceId
    workspaceRole
    customRoleId
  }
}
    `);

export const useInviteWorkspaceUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<InviteWorkspaceUserMutation, TError, InviteWorkspaceUserMutationVariables, TContext>) => {
    
    return useMutation<InviteWorkspaceUserMutation, TError, InviteWorkspaceUserMutationVariables, TContext>(
      {
    mutationKey: ['inviteWorkspaceUser'],
    mutationFn: (variables?: InviteWorkspaceUserMutationVariables) => fetcher<InviteWorkspaceUserMutation, InviteWorkspaceUserMutationVariables>(InviteWorkspaceUserDocument, variables)(),
    ...options
  }
    )};

export const McpProjectWorkflowPropertiesDocument = new TypedDocumentString(`
    query mcpProjectWorkflowProperties($mcpProjectWorkflowId: ID!) {
  mcpProjectWorkflowProperties(mcpProjectWorkflowId: $mcpProjectWorkflowId) {
    advancedOption
    description
    displayCondition
    expressionEnabled
    hidden
    name
    required
    type
    ... on StringProperty {
      controlType
      defaultValue
      label
      placeholder
    }
    ... on IntegerProperty {
      controlType
      integerDefaultValue: defaultValue
      label
      placeholder
    }
    ... on NumberProperty {
      controlType
      label
      numberDefaultValue: defaultValue
      placeholder
    }
    ... on BooleanProperty {
      booleanDefaultValue: defaultValue
      controlType
      label
      placeholder
    }
    ... on ArrayProperty {
      arrayDefaultValue: defaultValue
      controlType
      label
      placeholder
    }
    ... on ObjectProperty {
      controlType
      label
      objectDefaultValue: defaultValue
      placeholder
    }
  }
}
    `);

export const useMcpProjectWorkflowPropertiesQuery = <
      TData = McpProjectWorkflowPropertiesQuery,
      TError = unknown
    >(
      variables: McpProjectWorkflowPropertiesQueryVariables,
      options?: Omit<UseQueryOptions<McpProjectWorkflowPropertiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpProjectWorkflowPropertiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpProjectWorkflowPropertiesQuery, TError, TData>(
      {
    queryKey: ['mcpProjectWorkflowProperties', variables],
    queryFn: fetcher<McpProjectWorkflowPropertiesQuery, McpProjectWorkflowPropertiesQueryVariables>(McpProjectWorkflowPropertiesDocument, variables),
    ...options
  }
    )};

export const McpProjectsDocument = new TypedDocumentString(`
    query mcpProjects($workspaceId: ID!) {
  mcpProjects(workspaceId: $workspaceId) {
    id
    mcpServerId
    project {
      id
      name
    }
  }
}
    `);

export const useMcpProjectsQuery = <
      TData = McpProjectsQuery,
      TError = unknown
    >(
      variables: McpProjectsQueryVariables,
      options?: Omit<UseQueryOptions<McpProjectsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpProjectsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpProjectsQuery, TError, TData>(
      {
    queryKey: ['mcpProjects', variables],
    queryFn: fetcher<McpProjectsQuery, McpProjectsQueryVariables>(McpProjectsDocument, variables),
    ...options
  }
    )};

export const McpProjectsByServerIdDocument = new TypedDocumentString(`
    query mcpProjectsByServerId($mcpServerId: ID!) {
  mcpProjectsByServerId(mcpServerId: $mcpServerId) {
    id
    projectDeploymentId
    mcpServerId
    project {
      id
      name
      category {
        id
        name
      }
      tags {
        id
        name
      }
    }
    mcpProjectWorkflows {
      id
      mcpProjectId
      projectDeploymentWorkflowId
      parameters
      projectDeploymentWorkflow {
        id
        connections {
          connectionId
          workflowConnectionKey
          workflowNodeName
        }
        enabled
        inputs
        projectDeploymentId
        version
        workflowId
      }
      workflow {
        id
        label
      }
      createdBy
      createdDate
      lastModifiedBy
      lastModifiedDate
      version
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
    projectVersion
  }
}
    `);

export const useMcpProjectsByServerIdQuery = <
      TData = McpProjectsByServerIdQuery,
      TError = unknown
    >(
      variables: McpProjectsByServerIdQueryVariables,
      options?: Omit<UseQueryOptions<McpProjectsByServerIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpProjectsByServerIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpProjectsByServerIdQuery, TError, TData>(
      {
    queryKey: ['mcpProjectsByServerId', variables],
    queryFn: fetcher<McpProjectsByServerIdQuery, McpProjectsByServerIdQueryVariables>(McpProjectsByServerIdDocument, variables),
    ...options
  }
    )};

export const MyWorkspaceRoleDocument = new TypedDocumentString(`
    query MyWorkspaceRole($workspaceId: ID!) {
  myWorkspaceRole(workspaceId: $workspaceId)
}
    `);

export const useMyWorkspaceRoleQuery = <
      TData = MyWorkspaceRoleQuery,
      TError = unknown
    >(
      variables: MyWorkspaceRoleQueryVariables,
      options?: Omit<UseQueryOptions<MyWorkspaceRoleQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<MyWorkspaceRoleQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<MyWorkspaceRoleQuery, TError, TData>(
      {
    queryKey: ['MyWorkspaceRole', variables],
    queryFn: fetcher<MyWorkspaceRoleQuery, MyWorkspaceRoleQueryVariables>(MyWorkspaceRoleDocument, variables),
    ...options
  }
    )};

export const MyWorkspaceScopesDocument = new TypedDocumentString(`
    query MyWorkspaceScopes($workspaceId: ID!) {
  myWorkspaceScopes(workspaceId: $workspaceId)
}
    `);

export const useMyWorkspaceScopesQuery = <
      TData = MyWorkspaceScopesQuery,
      TError = unknown
    >(
      variables: MyWorkspaceScopesQueryVariables,
      options?: Omit<UseQueryOptions<MyWorkspaceScopesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<MyWorkspaceScopesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<MyWorkspaceScopesQuery, TError, TData>(
      {
    queryKey: ['MyWorkspaceScopes', variables],
    queryFn: fetcher<MyWorkspaceScopesQuery, MyWorkspaceScopesQueryVariables>(MyWorkspaceScopesDocument, variables),
    ...options
  }
    )};

export const OrganizationConnectionsDocument = new TypedDocumentString(`
    query organizationConnections($environmentId: ID) {
  organizationConnections(environmentId: $environmentId) {
    id
    name
    componentName
    environmentId
    visibility
    createdBy
    createdDate
    lastModifiedDate
  }
}
    `);

export const useOrganizationConnectionsQuery = <
      TData = OrganizationConnectionsQuery,
      TError = unknown
    >(
      variables?: OrganizationConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<OrganizationConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<OrganizationConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<OrganizationConnectionsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['organizationConnections'] : ['organizationConnections', variables],
    queryFn: fetcher<OrganizationConnectionsQuery, OrganizationConnectionsQueryVariables>(OrganizationConnectionsDocument, variables),
    ...options
  }
    )};

export const PermissionScopesDocument = new TypedDocumentString(`
    query PermissionScopes {
  permissionScopes
}
    `);

export const usePermissionScopesQuery = <
      TData = PermissionScopesQuery,
      TError = unknown
    >(
      variables?: PermissionScopesQueryVariables,
      options?: Omit<UseQueryOptions<PermissionScopesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<PermissionScopesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<PermissionScopesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['PermissionScopes'] : ['PermissionScopes', variables],
    queryFn: fetcher<PermissionScopesQuery, PermissionScopesQueryVariables>(PermissionScopesDocument, variables),
    ...options
  }
    )};

export const PreBuiltProjectTemplatesDocument = new TypedDocumentString(`
    query preBuiltProjectTemplates($query: String, $category: String) {
  preBuiltProjectTemplates(query: $query, category: $category) {
    authorName
    categories
    components {
      key
      value {
        connection {
          version
        }
        icon
        name
        title
        version
      }
    }
    description
    id
    project {
      name
      description
    }
    projectVersion
    publicUrl
    workflows {
      id
      label
    }
  }
}
    `);

export const usePreBuiltProjectTemplatesQuery = <
      TData = PreBuiltProjectTemplatesQuery,
      TError = unknown
    >(
      variables?: PreBuiltProjectTemplatesQueryVariables,
      options?: Omit<UseQueryOptions<PreBuiltProjectTemplatesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<PreBuiltProjectTemplatesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<PreBuiltProjectTemplatesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['preBuiltProjectTemplates'] : ['preBuiltProjectTemplates', variables],
    queryFn: fetcher<PreBuiltProjectTemplatesQuery, PreBuiltProjectTemplatesQueryVariables>(PreBuiltProjectTemplatesDocument, variables),
    ...options
  }
    )};

export const PreBuiltWorkflowTemplatesDocument = new TypedDocumentString(`
    query preBuiltWorkflowTemplates($query: String, $category: String) {
  preBuiltWorkflowTemplates(query: $query, category: $category) {
    authorName
    categories
    components {
      connection {
        version
      }
      icon
      name
      title
      version
    }
    description
    id
    projectVersion
    publicUrl
    workflow {
      label
      description
    }
  }
}
    `);

export const usePreBuiltWorkflowTemplatesQuery = <
      TData = PreBuiltWorkflowTemplatesQuery,
      TError = unknown
    >(
      variables?: PreBuiltWorkflowTemplatesQueryVariables,
      options?: Omit<UseQueryOptions<PreBuiltWorkflowTemplatesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<PreBuiltWorkflowTemplatesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<PreBuiltWorkflowTemplatesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['preBuiltWorkflowTemplates'] : ['preBuiltWorkflowTemplates', variables],
    queryFn: fetcher<PreBuiltWorkflowTemplatesQuery, PreBuiltWorkflowTemplatesQueryVariables>(PreBuiltWorkflowTemplatesDocument, variables),
    ...options
  }
    )};

export const ProjectByIdDocument = new TypedDocumentString(`
    query projectById($id: ID!) {
  project(id: $id) {
    id
    name
  }
}
    `);

export const useProjectByIdQuery = <
      TData = ProjectByIdQuery,
      TError = unknown
    >(
      variables: ProjectByIdQueryVariables,
      options?: Omit<UseQueryOptions<ProjectByIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ProjectByIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ProjectByIdQuery, TError, TData>(
      {
    queryKey: ['projectById', variables],
    queryFn: fetcher<ProjectByIdQuery, ProjectByIdQueryVariables>(ProjectByIdDocument, variables),
    ...options
  }
    )};

export const ProjectGrantsDocument = new TypedDocumentString(`
    query ProjectGrants($workspaceId: ID!, $projectId: ID!) {
  projectGrants(workspaceId: $workspaceId, projectId: $projectId)
}
    `);

export const useProjectGrantsQuery = <
      TData = ProjectGrantsQuery,
      TError = unknown
    >(
      variables: ProjectGrantsQueryVariables,
      options?: Omit<UseQueryOptions<ProjectGrantsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ProjectGrantsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ProjectGrantsQuery, TError, TData>(
      {
    queryKey: ['ProjectGrants', variables],
    queryFn: fetcher<ProjectGrantsQuery, ProjectGrantsQueryVariables>(ProjectGrantsDocument, variables),
    ...options
  }
    )};

export const ProjectTemplateDocument = new TypedDocumentString(`
    query projectTemplate($id: String!, $sharedProject: Boolean!) {
  projectTemplate(id: $id, sharedProject: $sharedProject) {
    components {
      key
      value {
        connection {
          componentName
          version
        }
        icon
        name
        title
        version
      }
    }
    description
    project {
      name
    }
    projectVersion
    publicUrl
    workflows {
      id
      label
    }
  }
}
    `);

export const useProjectTemplateQuery = <
      TData = ProjectTemplateQuery,
      TError = unknown
    >(
      variables: ProjectTemplateQueryVariables,
      options?: Omit<UseQueryOptions<ProjectTemplateQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ProjectTemplateQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ProjectTemplateQuery, TError, TData>(
      {
    queryKey: ['projectTemplate', variables],
    queryFn: fetcher<ProjectTemplateQuery, ProjectTemplateQueryVariables>(ProjectTemplateDocument, variables),
    ...options
  }
    )};

export const ReassignAllConnectionsDocument = new TypedDocumentString(`
    mutation reassignAllConnections($workspaceId: ID!, $userLogin: String!, $newOwnerLogin: String!) {
  reassignAllConnections(
    workspaceId: $workspaceId
    userLogin: $userLogin
    newOwnerLogin: $newOwnerLogin
  )
}
    `);

export const useReassignAllConnectionsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ReassignAllConnectionsMutation, TError, ReassignAllConnectionsMutationVariables, TContext>) => {
    
    return useMutation<ReassignAllConnectionsMutation, TError, ReassignAllConnectionsMutationVariables, TContext>(
      {
    mutationKey: ['reassignAllConnections'],
    mutationFn: (variables?: ReassignAllConnectionsMutationVariables) => fetcher<ReassignAllConnectionsMutation, ReassignAllConnectionsMutationVariables>(ReassignAllConnectionsDocument, variables)(),
    ...options
  }
    )};

export const RemoveWorkspaceUserDocument = new TypedDocumentString(`
    mutation RemoveWorkspaceUser($workspaceId: ID!, $userId: ID!) {
  removeWorkspaceUser(workspaceId: $workspaceId, userId: $userId)
}
    `);

export const useRemoveWorkspaceUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveWorkspaceUserMutation, TError, RemoveWorkspaceUserMutationVariables, TContext>) => {
    
    return useMutation<RemoveWorkspaceUserMutation, TError, RemoveWorkspaceUserMutationVariables, TContext>(
      {
    mutationKey: ['RemoveWorkspaceUser'],
    mutationFn: (variables?: RemoveWorkspaceUserMutationVariables) => fetcher<RemoveWorkspaceUserMutation, RemoveWorkspaceUserMutationVariables>(RemoveWorkspaceUserDocument, variables)(),
    ...options
  }
    )};

export const RemoveWorkspaceUserEnvironmentRoleDocument = new TypedDocumentString(`
    mutation RemoveWorkspaceUserEnvironmentRole($workspaceId: ID!, $userId: ID!, $environment: EnvironmentEnum!) {
  removeWorkspaceUserEnvironmentRole(
    workspaceId: $workspaceId
    userId: $userId
    environment: $environment
  )
}
    `);

export const useRemoveWorkspaceUserEnvironmentRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveWorkspaceUserEnvironmentRoleMutation, TError, RemoveWorkspaceUserEnvironmentRoleMutationVariables, TContext>) => {
    
    return useMutation<RemoveWorkspaceUserEnvironmentRoleMutation, TError, RemoveWorkspaceUserEnvironmentRoleMutationVariables, TContext>(
      {
    mutationKey: ['RemoveWorkspaceUserEnvironmentRole'],
    mutationFn: (variables?: RemoveWorkspaceUserEnvironmentRoleMutationVariables) => fetcher<RemoveWorkspaceUserEnvironmentRoleMutation, RemoveWorkspaceUserEnvironmentRoleMutationVariables>(RemoveWorkspaceUserEnvironmentRoleDocument, variables)(),
    ...options
  }
    )};

export const RevokeConnectionAccessDocument = new TypedDocumentString(`
    mutation RevokeConnectionAccess($workspaceId: ID!, $connectionId: ID!, $userId: ID!) {
  revokeConnectionAccess(
    workspaceId: $workspaceId
    connectionId: $connectionId
    userId: $userId
  )
}
    `);

export const useRevokeConnectionAccessMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RevokeConnectionAccessMutation, TError, RevokeConnectionAccessMutationVariables, TContext>) => {
    
    return useMutation<RevokeConnectionAccessMutation, TError, RevokeConnectionAccessMutationVariables, TContext>(
      {
    mutationKey: ['RevokeConnectionAccess'],
    mutationFn: (variables?: RevokeConnectionAccessMutationVariables) => fetcher<RevokeConnectionAccessMutation, RevokeConnectionAccessMutationVariables>(RevokeConnectionAccessDocument, variables)(),
    ...options
  }
    )};

export const RevokeProjectAccessDocument = new TypedDocumentString(`
    mutation RevokeProjectAccess($workspaceId: ID!, $projectId: ID!, $userId: ID!) {
  revokeProjectAccess(
    workspaceId: $workspaceId
    projectId: $projectId
    userId: $userId
  )
}
    `);

export const useRevokeProjectAccessMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RevokeProjectAccessMutation, TError, RevokeProjectAccessMutationVariables, TContext>) => {
    
    return useMutation<RevokeProjectAccessMutation, TError, RevokeProjectAccessMutationVariables, TContext>(
      {
    mutationKey: ['RevokeProjectAccess'],
    mutationFn: (variables?: RevokeProjectAccessMutationVariables) => fetcher<RevokeProjectAccessMutation, RevokeProjectAccessMutationVariables>(RevokeProjectAccessDocument, variables)(),
    ...options
  }
    )};

export const SetConnectionVisibilityDocument = new TypedDocumentString(`
    mutation SetConnectionVisibility($workspaceId: ID!, $connectionId: ID!, $visibility: ResourceVisibility!) {
  setConnectionVisibility(
    workspaceId: $workspaceId
    connectionId: $connectionId
    visibility: $visibility
  )
}
    `);

export const useSetConnectionVisibilityMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetConnectionVisibilityMutation, TError, SetConnectionVisibilityMutationVariables, TContext>) => {
    
    return useMutation<SetConnectionVisibilityMutation, TError, SetConnectionVisibilityMutationVariables, TContext>(
      {
    mutationKey: ['SetConnectionVisibility'],
    mutationFn: (variables?: SetConnectionVisibilityMutationVariables) => fetcher<SetConnectionVisibilityMutation, SetConnectionVisibilityMutationVariables>(SetConnectionVisibilityDocument, variables)(),
    ...options
  }
    )};

export const SetProjectVisibilityDocument = new TypedDocumentString(`
    mutation SetProjectVisibility($workspaceId: ID!, $projectId: ID!, $visibility: ResourceVisibility!) {
  setProjectVisibility(
    workspaceId: $workspaceId
    projectId: $projectId
    visibility: $visibility
  )
}
    `);

export const useSetProjectVisibilityMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetProjectVisibilityMutation, TError, SetProjectVisibilityMutationVariables, TContext>) => {
    
    return useMutation<SetProjectVisibilityMutation, TError, SetProjectVisibilityMutationVariables, TContext>(
      {
    mutationKey: ['SetProjectVisibility'],
    mutationFn: (variables?: SetProjectVisibilityMutationVariables) => fetcher<SetProjectVisibilityMutation, SetProjectVisibilityMutationVariables>(SetProjectVisibilityDocument, variables)(),
    ...options
  }
    )};

export const SetWorkspaceUserEnvironmentRoleDocument = new TypedDocumentString(`
    mutation SetWorkspaceUserEnvironmentRole($workspaceId: ID!, $userId: ID!, $environment: EnvironmentEnum!, $role: WorkspaceRole, $customRoleId: ID) {
  setWorkspaceUserEnvironmentRole(
    workspaceId: $workspaceId
    userId: $userId
    environment: $environment
    role: $role
    customRoleId: $customRoleId
  ) {
    id
    userId
    workspaceRole
    customRoleId
    environment
  }
}
    `);

export const useSetWorkspaceUserEnvironmentRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetWorkspaceUserEnvironmentRoleMutation, TError, SetWorkspaceUserEnvironmentRoleMutationVariables, TContext>) => {
    
    return useMutation<SetWorkspaceUserEnvironmentRoleMutation, TError, SetWorkspaceUserEnvironmentRoleMutationVariables, TContext>(
      {
    mutationKey: ['SetWorkspaceUserEnvironmentRole'],
    mutationFn: (variables?: SetWorkspaceUserEnvironmentRoleMutationVariables) => fetcher<SetWorkspaceUserEnvironmentRoleMutation, SetWorkspaceUserEnvironmentRoleMutationVariables>(SetWorkspaceUserEnvironmentRoleDocument, variables)(),
    ...options
  }
    )};

export const SharedProjectDocument = new TypedDocumentString(`
    query sharedProject($projectUuid: String!) {
  sharedProject(projectUuid: $projectUuid) {
    description
    exported
    projectVersion
    publicUrl
  }
}
    `);

export const useSharedProjectQuery = <
      TData = SharedProjectQuery,
      TError = unknown
    >(
      variables: SharedProjectQueryVariables,
      options?: Omit<UseQueryOptions<SharedProjectQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<SharedProjectQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<SharedProjectQuery, TError, TData>(
      {
    queryKey: ['sharedProject', variables],
    queryFn: fetcher<SharedProjectQuery, SharedProjectQueryVariables>(SharedProjectDocument, variables),
    ...options
  }
    )};

export const SharedWorkflowDocument = new TypedDocumentString(`
    query sharedWorkflow($workflowUuid: String!) {
  sharedWorkflow(workflowUuid: $workflowUuid) {
    description
    exported
    projectVersion
    publicUrl
  }
}
    `);

export const useSharedWorkflowQuery = <
      TData = SharedWorkflowQuery,
      TError = unknown
    >(
      variables: SharedWorkflowQueryVariables,
      options?: Omit<UseQueryOptions<SharedWorkflowQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<SharedWorkflowQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<SharedWorkflowQuery, TError, TData>(
      {
    queryKey: ['sharedWorkflow', variables],
    queryFn: fetcher<SharedWorkflowQuery, SharedWorkflowQueryVariables>(SharedWorkflowDocument, variables),
    ...options
  }
    )};

export const ToolEligibleProjectVersionWorkflowsDocument = new TypedDocumentString(`
    query toolEligibleProjectVersionWorkflows($projectId: ID!, $projectVersion: Int!) {
  toolEligibleProjectVersionWorkflows(
    projectId: $projectId
    projectVersion: $projectVersion
  ) {
    id
    workflow {
      id
      label
    }
  }
}
    `);

export const useToolEligibleProjectVersionWorkflowsQuery = <
      TData = ToolEligibleProjectVersionWorkflowsQuery,
      TError = unknown
    >(
      variables: ToolEligibleProjectVersionWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<ToolEligibleProjectVersionWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ToolEligibleProjectVersionWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ToolEligibleProjectVersionWorkflowsQuery, TError, TData>(
      {
    queryKey: ['toolEligibleProjectVersionWorkflows', variables],
    queryFn: fetcher<ToolEligibleProjectVersionWorkflowsQuery, ToolEligibleProjectVersionWorkflowsQueryVariables>(ToolEligibleProjectVersionWorkflowsDocument, variables),
    ...options
  }
    )};

export const UnresolvedConnectionsDocument = new TypedDocumentString(`
    query unresolvedConnections($workspaceId: ID!, $userLogin: String!) {
  unresolvedConnections(workspaceId: $workspaceId, userLogin: $userLogin) {
    connectionId
    connectionName
    visibility
    environmentId
    dependentWorkflowCount
  }
}
    `);

export const useUnresolvedConnectionsQuery = <
      TData = UnresolvedConnectionsQuery,
      TError = unknown
    >(
      variables: UnresolvedConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<UnresolvedConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<UnresolvedConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<UnresolvedConnectionsQuery, TError, TData>(
      {
    queryKey: ['unresolvedConnections', variables],
    queryFn: fetcher<UnresolvedConnectionsQuery, UnresolvedConnectionsQueryVariables>(UnresolvedConnectionsDocument, variables),
    ...options
  }
    )};

export const UpdateA2aProjectDocument = new TypedDocumentString(`
    mutation updateA2aProject($id: ID!, $input: UpdateA2aProjectInput!) {
  updateA2aProject(id: $id, input: $input) {
    id
  }
}
    `);

export const useUpdateA2aProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateA2aProjectMutation, TError, UpdateA2aProjectMutationVariables, TContext>) => {
    
    return useMutation<UpdateA2aProjectMutation, TError, UpdateA2aProjectMutationVariables, TContext>(
      {
    mutationKey: ['updateA2aProject'],
    mutationFn: (variables?: UpdateA2aProjectMutationVariables) => fetcher<UpdateA2aProjectMutation, UpdateA2aProjectMutationVariables>(UpdateA2aProjectDocument, variables)(),
    ...options
  }
    )};

export const UpdateA2aProjectWorkflowParametersDocument = new TypedDocumentString(`
    mutation updateA2aProjectWorkflowParameters($id: ID!, $input: A2aProjectWorkflowParametersInput!) {
  updateA2aProjectWorkflowParameters(id: $id, input: $input) {
    id
  }
}
    `);

export const useUpdateA2aProjectWorkflowParametersMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateA2aProjectWorkflowParametersMutation, TError, UpdateA2aProjectWorkflowParametersMutationVariables, TContext>) => {
    
    return useMutation<UpdateA2aProjectWorkflowParametersMutation, TError, UpdateA2aProjectWorkflowParametersMutationVariables, TContext>(
      {
    mutationKey: ['updateA2aProjectWorkflowParameters'],
    mutationFn: (variables?: UpdateA2aProjectWorkflowParametersMutationVariables) => fetcher<UpdateA2aProjectWorkflowParametersMutation, UpdateA2aProjectWorkflowParametersMutationVariables>(UpdateA2aProjectWorkflowParametersDocument, variables)(),
    ...options
  }
    )};

export const UpdateA2aServerDocument = new TypedDocumentString(`
    mutation updateA2aServer($id: ID!, $input: UpdateA2aServerInput!) {
  updateA2aServer(id: $id, input: $input) {
    id
  }
}
    `);

export const useUpdateA2aServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateA2aServerMutation, TError, UpdateA2aServerMutationVariables, TContext>) => {
    
    return useMutation<UpdateA2aServerMutation, TError, UpdateA2aServerMutationVariables, TContext>(
      {
    mutationKey: ['updateA2aServer'],
    mutationFn: (variables?: UpdateA2aServerMutationVariables) => fetcher<UpdateA2aServerMutation, UpdateA2aServerMutationVariables>(UpdateA2aServerDocument, variables)(),
    ...options
  }
    )};

export const UpdateConnectionCredentialsDocument = new TypedDocumentString(`
    mutation UpdateConnectionCredentials($input: UpdateConnectionCredentialsInput!) {
  updateConnectionCredentials(input: $input)
}
    `);

export const useUpdateConnectionCredentialsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateConnectionCredentialsMutation, TError, UpdateConnectionCredentialsMutationVariables, TContext>) => {
    
    return useMutation<UpdateConnectionCredentialsMutation, TError, UpdateConnectionCredentialsMutationVariables, TContext>(
      {
    mutationKey: ['UpdateConnectionCredentials'],
    mutationFn: (variables?: UpdateConnectionCredentialsMutationVariables) => fetcher<UpdateConnectionCredentialsMutation, UpdateConnectionCredentialsMutationVariables>(UpdateConnectionCredentialsDocument, variables)(),
    ...options
  }
    )};

export const UpdateCustomRoleDocument = new TypedDocumentString(`
    mutation UpdateCustomRole($id: ID!, $input: UpdateCustomRoleInput!) {
  updateCustomRole(id: $id, input: $input) {
    id
    name
    description
    scopes
  }
}
    `);

export const useUpdateCustomRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateCustomRoleMutation, TError, UpdateCustomRoleMutationVariables, TContext>) => {
    
    return useMutation<UpdateCustomRoleMutation, TError, UpdateCustomRoleMutationVariables, TContext>(
      {
    mutationKey: ['UpdateCustomRole'],
    mutationFn: (variables?: UpdateCustomRoleMutationVariables) => fetcher<UpdateCustomRoleMutation, UpdateCustomRoleMutationVariables>(UpdateCustomRoleDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpProjectDocument = new TypedDocumentString(`
    mutation updateMcpProject($id: ID!, $input: UpdateMcpProjectInput!) {
  updateMcpProject(id: $id, input: $input) {
    id
    mcpServerId
    projectDeploymentId
    projectVersion
  }
}
    `);

export const useUpdateMcpProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpProjectMutation, TError, UpdateMcpProjectMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpProjectMutation, TError, UpdateMcpProjectMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpProject'],
    mutationFn: (variables?: UpdateMcpProjectMutationVariables) => fetcher<UpdateMcpProjectMutation, UpdateMcpProjectMutationVariables>(UpdateMcpProjectDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpProjectWorkflowDocument = new TypedDocumentString(`
    mutation updateMcpProjectWorkflow($id: ID!, $input: McpProjectWorkflowUpdateInput!) {
  updateMcpProjectWorkflow(id: $id, input: $input) {
    id
    mcpProjectId
    projectDeploymentWorkflowId
    parameters
  }
}
    `);

export const useUpdateMcpProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpProjectWorkflowMutation, TError, UpdateMcpProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpProjectWorkflowMutation, TError, UpdateMcpProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpProjectWorkflow'],
    mutationFn: (variables?: UpdateMcpProjectWorkflowMutationVariables) => fetcher<UpdateMcpProjectWorkflowMutation, UpdateMcpProjectWorkflowMutationVariables>(UpdateMcpProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpServerDocument = new TypedDocumentString(`
    mutation updateMcpServer($id: ID!, $input: McpServerUpdateInput!) {
  updateMcpServer(id: $id, input: $input) {
    id
    name
    enabled
    enforceToolAuthorization
    authenticationRequired
  }
}
    `);

export const useUpdateMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpServerMutation, TError, UpdateMcpServerMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpServerMutation, TError, UpdateMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpServer'],
    mutationFn: (variables?: UpdateMcpServerMutationVariables) => fetcher<UpdateMcpServerMutation, UpdateMcpServerMutationVariables>(UpdateMcpServerDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpServerTagsDocument = new TypedDocumentString(`
    mutation updateMcpServerTags($id: ID!, $tags: [TagInput!]!) {
  updateMcpServerTags(id: $id, tags: $tags) {
    id
  }
}
    `);

export const useUpdateMcpServerTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpServerTagsMutation, TError, UpdateMcpServerTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpServerTagsMutation, TError, UpdateMcpServerTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpServerTags'],
    mutationFn: (variables?: UpdateMcpServerTagsMutationVariables) => fetcher<UpdateMcpServerTagsMutation, UpdateMcpServerTagsMutationVariables>(UpdateMcpServerTagsDocument, variables)(),
    ...options
  }
    )};

export const UpdateOrganizationConnectionDocument = new TypedDocumentString(`
    mutation updateOrganizationConnection($connectionId: ID!, $name: String!, $tagIds: [ID!], $version: Int!) {
  updateOrganizationConnection(
    connectionId: $connectionId
    name: $name
    tagIds: $tagIds
    version: $version
  )
}
    `);

export const useUpdateOrganizationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateOrganizationConnectionMutation, TError, UpdateOrganizationConnectionMutationVariables, TContext>) => {
    
    return useMutation<UpdateOrganizationConnectionMutation, TError, UpdateOrganizationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['updateOrganizationConnection'],
    mutationFn: (variables?: UpdateOrganizationConnectionMutationVariables) => fetcher<UpdateOrganizationConnectionMutation, UpdateOrganizationConnectionMutationVariables>(UpdateOrganizationConnectionDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceApiKeyDocument = new TypedDocumentString(`
    mutation updateWorkspaceApiKey($apiKeyId: ID!, $name: String!) {
  updateWorkspaceApiKey(apiKeyId: $apiKeyId, name: $name)
}
    `);

export const useUpdateWorkspaceApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceApiKeyMutation, TError, UpdateWorkspaceApiKeyMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceApiKeyMutation, TError, UpdateWorkspaceApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceApiKey'],
    mutationFn: (variables?: UpdateWorkspaceApiKeyMutationVariables) => fetcher<UpdateWorkspaceApiKeyMutation, UpdateWorkspaceApiKeyMutationVariables>(UpdateWorkspaceApiKeyDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceUserRoleDocument = new TypedDocumentString(`
    mutation UpdateWorkspaceUserRole($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole!) {
  updateWorkspaceUserRole(workspaceId: $workspaceId, userId: $userId, role: $role) {
    id
    workspaceRole
  }
}
    `);

export const useUpdateWorkspaceUserRoleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceUserRoleMutation, TError, UpdateWorkspaceUserRoleMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceUserRoleMutation, TError, UpdateWorkspaceUserRoleMutationVariables, TContext>(
      {
    mutationKey: ['UpdateWorkspaceUserRole'],
    mutationFn: (variables?: UpdateWorkspaceUserRoleMutationVariables) => fetcher<UpdateWorkspaceUserRoleMutation, UpdateWorkspaceUserRoleMutationVariables>(UpdateWorkspaceUserRoleDocument, variables)(),
    ...options
  }
    )};

export const WorkflowChatProjectDeploymentWorkflowDocument = new TypedDocumentString(`
    query workflowChatProjectDeploymentWorkflow($id: String!) {
  projectDeploymentWorkflow(id: $id) {
    projectWorkflow {
      sseStreamResponse
      workflow {
        label
      }
    }
  }
}
    `);

export const useWorkflowChatProjectDeploymentWorkflowQuery = <
      TData = WorkflowChatProjectDeploymentWorkflowQuery,
      TError = unknown
    >(
      variables: WorkflowChatProjectDeploymentWorkflowQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowChatProjectDeploymentWorkflowQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowChatProjectDeploymentWorkflowQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowChatProjectDeploymentWorkflowQuery, TError, TData>(
      {
    queryKey: ['workflowChatProjectDeploymentWorkflow', variables],
    queryFn: fetcher<WorkflowChatProjectDeploymentWorkflowQuery, WorkflowChatProjectDeploymentWorkflowQueryVariables>(WorkflowChatProjectDeploymentWorkflowDocument, variables),
    ...options
  }
    )};

export const WorkflowTemplateDocument = new TypedDocumentString(`
    query workflowTemplate($id: String!, $sharedWorkflow: Boolean!) {
  workflowTemplate(id: $id, sharedWorkflow: $sharedWorkflow) {
    description
    projectVersion
    publicUrl
    workflow {
      label
    }
    components {
      connection {
        componentName
        version
      }
      icon
      name
      title
      version
    }
  }
}
    `);

export const useWorkflowTemplateQuery = <
      TData = WorkflowTemplateQuery,
      TError = unknown
    >(
      variables: WorkflowTemplateQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowTemplateQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowTemplateQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowTemplateQuery, TError, TData>(
      {
    queryKey: ['workflowTemplate', variables],
    queryFn: fetcher<WorkflowTemplateQuery, WorkflowTemplateQueryVariables>(WorkflowTemplateDocument, variables),
    ...options
  }
    )};

export const WorkspaceApiKeysDocument = new TypedDocumentString(`
    query workspaceApiKeys($workspaceId: ID!, $environmentId: ID!) {
  workspaceApiKeys(workspaceId: $workspaceId, environmentId: $environmentId) {
    id
    name
    secretKey
    lastUsedDate
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useWorkspaceApiKeysQuery = <
      TData = WorkspaceApiKeysQuery,
      TError = unknown
    >(
      variables: WorkspaceApiKeysQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceApiKeysQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceApiKeysQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceApiKeysQuery, TError, TData>(
      {
    queryKey: ['workspaceApiKeys', variables],
    queryFn: fetcher<WorkspaceApiKeysQuery, WorkspaceApiKeysQueryVariables>(WorkspaceApiKeysDocument, variables),
    ...options
  }
    )};

export const WorkspaceChatWorkflowsDocument = new TypedDocumentString(`
    query workspaceChatWorkflows($workspaceId: ID!, $environmentId: ID!) {
  workspaceChatWorkflows(workspaceId: $workspaceId, environmentId: $environmentId) {
    projectDeploymentId
    projectId
    projectName
    projectWorkflowId
    workflowExecutionId
    workflowId
    workflowLabel
  }
}
    `);

export const useWorkspaceChatWorkflowsQuery = <
      TData = WorkspaceChatWorkflowsQuery,
      TError = unknown
    >(
      variables: WorkspaceChatWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceChatWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceChatWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceChatWorkflowsQuery, TError, TData>(
      {
    queryKey: ['workspaceChatWorkflows', variables],
    queryFn: fetcher<WorkspaceChatWorkflowsQuery, WorkspaceChatWorkflowsQueryVariables>(WorkspaceChatWorkflowsDocument, variables),
    ...options
  }
    )};

export const WorkspaceMcpServerTagsDocument = new TypedDocumentString(`
    query workspaceMcpServerTags($workspaceId: ID!) {
  workspaceMcpServerTags(workspaceId: $workspaceId) {
    id
    name
  }
}
    `);

export const useWorkspaceMcpServerTagsQuery = <
      TData = WorkspaceMcpServerTagsQuery,
      TError = unknown
    >(
      variables: WorkspaceMcpServerTagsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceMcpServerTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceMcpServerTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceMcpServerTagsQuery, TError, TData>(
      {
    queryKey: ['workspaceMcpServerTags', variables],
    queryFn: fetcher<WorkspaceMcpServerTagsQuery, WorkspaceMcpServerTagsQueryVariables>(WorkspaceMcpServerTagsDocument, variables),
    ...options
  }
    )};

export const WorkspaceMcpServersDocument = new TypedDocumentString(`
    query workspaceMcpServers($workspaceId: ID!) {
  workspaceMcpServers(workspaceId: $workspaceId) {
    id
    name
    type
    environmentId
    enabled
    enforceToolAuthorization
    authenticationRequired
    url
    mcpComponents {
      id
      mcpServerId
      componentName
      componentVersion
      title
    }
    tags {
      id
      name
    }
    lastModifiedDate
  }
}
    `);

export const useWorkspaceMcpServersQuery = <
      TData = WorkspaceMcpServersQuery,
      TError = unknown
    >(
      variables: WorkspaceMcpServersQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceMcpServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceMcpServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceMcpServersQuery, TError, TData>(
      {
    queryKey: ['workspaceMcpServers', variables],
    queryFn: fetcher<WorkspaceMcpServersQuery, WorkspaceMcpServersQueryVariables>(WorkspaceMcpServersDocument, variables),
    ...options
  }
    )};

export const WorkspaceProjectWorkflowsDocument = new TypedDocumentString(`
    query workspaceProjectWorkflows($workspaceId: ID!) {
  workspaceProjectWorkflows(workspaceId: $workspaceId) {
    projectId
    projectName
    projectWorkflowId
    workflowId
    workflowLabel
  }
}
    `);

export const useWorkspaceProjectWorkflowsQuery = <
      TData = WorkspaceProjectWorkflowsQuery,
      TError = unknown
    >(
      variables: WorkspaceProjectWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceProjectWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceProjectWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceProjectWorkflowsQuery, TError, TData>(
      {
    queryKey: ['workspaceProjectWorkflows', variables],
    queryFn: fetcher<WorkspaceProjectWorkflowsQuery, WorkspaceProjectWorkflowsQueryVariables>(WorkspaceProjectWorkflowsDocument, variables),
    ...options
  }
    )};

export const WorkspaceUsersDocument = new TypedDocumentString(`
    query WorkspaceUsers($workspaceId: ID!) {
  workspaceUsers(workspaceId: $workspaceId) {
    id
    workspaceId
    userId
    workspaceRole
    customRoleId
    inherited
    user {
      email
      firstName
      lastName
    }
    createdDate
    environment
  }
}
    `);

export const useWorkspaceUsersQuery = <
      TData = WorkspaceUsersQuery,
      TError = unknown
    >(
      variables: WorkspaceUsersQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceUsersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceUsersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceUsersQuery, TError, TData>(
      {
    queryKey: ['WorkspaceUsers', variables],
    queryFn: fetcher<WorkspaceUsersQuery, WorkspaceUsersQueryVariables>(WorkspaceUsersDocument, variables),
    ...options
  }
    )};

export const ContextStoreSourceDocument = new TypedDocumentString(`
    query contextStoreSource($id: ID!) {
  contextStoreSource(id: $id) {
    id
    name
    entityName
    description
    idField
    storedFields
    indexedFields
    semanticIndexFields
    parameters
    sourceComponentName
    sourceComponentVersion
    sourceClusterElementName
    connectionId
    cadence
    status
    enabled
    lastSyncRunAt
    lastSyncJobExecutionId
    workflowId
    fullReplaceCadence
    tombstoneStrategy
  }
}
    `);

export const useContextStoreSourceQuery = <
      TData = ContextStoreSourceQuery,
      TError = unknown
    >(
      variables: ContextStoreSourceQueryVariables,
      options?: Omit<UseQueryOptions<ContextStoreSourceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ContextStoreSourceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ContextStoreSourceQuery, TError, TData>(
      {
    queryKey: ['contextStoreSource', variables],
    queryFn: fetcher<ContextStoreSourceQuery, ContextStoreSourceQueryVariables>(ContextStoreSourceDocument, variables),
    ...options
  }
    )};

export const ContextStoreSourcesDocument = new TypedDocumentString(`
    query contextStoreSources($workspaceId: ID!, $environmentId: ID!, $filter: ContextStoreSourceFilter) {
  contextStoreSources(
    workspaceId: $workspaceId
    environmentId: $environmentId
    filter: $filter
  ) {
    id
    contextStoreId
    name
    entityName
    description
    idField
    indexedFields
    sourceComponentName
    sourceComponentVersion
    sourceClusterElementName
    connectionId
    cadence
    status
    enabled
    lastSyncRunAt
    lastSyncJobExecutionId
    workflowId
  }
}
    `);

export const useContextStoreSourcesQuery = <
      TData = ContextStoreSourcesQuery,
      TError = unknown
    >(
      variables: ContextStoreSourcesQueryVariables,
      options?: Omit<UseQueryOptions<ContextStoreSourcesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ContextStoreSourcesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ContextStoreSourcesQuery, TError, TData>(
      {
    queryKey: ['contextStoreSources', variables],
    queryFn: fetcher<ContextStoreSourcesQuery, ContextStoreSourcesQueryVariables>(ContextStoreSourcesDocument, variables),
    ...options
  }
    )};

export const ContextStoreTagsDocument = new TypedDocumentString(`
    query contextStoreTags($workspaceId: ID!) {
  contextStoreTags(workspaceId: $workspaceId) {
    id
    name
  }
}
    `);

export const useContextStoreTagsQuery = <
      TData = ContextStoreTagsQuery,
      TError = unknown
    >(
      variables: ContextStoreTagsQueryVariables,
      options?: Omit<UseQueryOptions<ContextStoreTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ContextStoreTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ContextStoreTagsQuery, TError, TData>(
      {
    queryKey: ['contextStoreTags', variables],
    queryFn: fetcher<ContextStoreTagsQuery, ContextStoreTagsQueryVariables>(ContextStoreTagsDocument, variables),
    ...options
  }
    )};

export const ContextStoresDocument = new TypedDocumentString(`
    query contextStores($workspaceId: ID!, $environmentId: ID!) {
  contextStores(workspaceId: $workspaceId, environmentId: $environmentId) {
    id
    name
    description
    environment
    tagIds
    tags {
      id
      name
    }
    version
  }
}
    `);

export const useContextStoresQuery = <
      TData = ContextStoresQuery,
      TError = unknown
    >(
      variables: ContextStoresQueryVariables,
      options?: Omit<UseQueryOptions<ContextStoresQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ContextStoresQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ContextStoresQuery, TError, TData>(
      {
    queryKey: ['contextStores', variables],
    queryFn: fetcher<ContextStoresQuery, ContextStoresQueryVariables>(ContextStoresDocument, variables),
    ...options
  }
    )};

export const CreateContextStoreDocument = new TypedDocumentString(`
    mutation createContextStore($workspaceId: ID!, $environmentId: ID!, $input: CreateContextStoreInput!) {
  createContextStore(
    workspaceId: $workspaceId
    environmentId: $environmentId
    input: $input
  ) {
    id
    name
    description
    environment
    tagIds
    version
  }
}
    `);

export const useCreateContextStoreMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateContextStoreMutation, TError, CreateContextStoreMutationVariables, TContext>) => {
    
    return useMutation<CreateContextStoreMutation, TError, CreateContextStoreMutationVariables, TContext>(
      {
    mutationKey: ['createContextStore'],
    mutationFn: (variables?: CreateContextStoreMutationVariables) => fetcher<CreateContextStoreMutation, CreateContextStoreMutationVariables>(CreateContextStoreDocument, variables)(),
    ...options
  }
    )};

export const CreateContextStoreSourceDocument = new TypedDocumentString(`
    mutation createContextStoreSource($input: CreateContextStoreSourceInput!) {
  createContextStoreSource(input: $input) {
    id
    name
    status
    enabled
    cadence
  }
}
    `);

export const useCreateContextStoreSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateContextStoreSourceMutation, TError, CreateContextStoreSourceMutationVariables, TContext>) => {
    
    return useMutation<CreateContextStoreSourceMutation, TError, CreateContextStoreSourceMutationVariables, TContext>(
      {
    mutationKey: ['createContextStoreSource'],
    mutationFn: (variables?: CreateContextStoreSourceMutationVariables) => fetcher<CreateContextStoreSourceMutation, CreateContextStoreSourceMutationVariables>(CreateContextStoreSourceDocument, variables)(),
    ...options
  }
    )};

export const DeleteContextStoreDocument = new TypedDocumentString(`
    mutation deleteContextStore($workspaceId: ID!, $id: ID!) {
  deleteContextStore(workspaceId: $workspaceId, id: $id)
}
    `);

export const useDeleteContextStoreMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteContextStoreMutation, TError, DeleteContextStoreMutationVariables, TContext>) => {
    
    return useMutation<DeleteContextStoreMutation, TError, DeleteContextStoreMutationVariables, TContext>(
      {
    mutationKey: ['deleteContextStore'],
    mutationFn: (variables?: DeleteContextStoreMutationVariables) => fetcher<DeleteContextStoreMutation, DeleteContextStoreMutationVariables>(DeleteContextStoreDocument, variables)(),
    ...options
  }
    )};

export const DeleteContextStoreSourceDocument = new TypedDocumentString(`
    mutation deleteContextStoreSource($id: ID!) {
  deleteContextStoreSource(id: $id)
}
    `);

export const useDeleteContextStoreSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteContextStoreSourceMutation, TError, DeleteContextStoreSourceMutationVariables, TContext>) => {
    
    return useMutation<DeleteContextStoreSourceMutation, TError, DeleteContextStoreSourceMutationVariables, TContext>(
      {
    mutationKey: ['deleteContextStoreSource'],
    mutationFn: (variables?: DeleteContextStoreSourceMutationVariables) => fetcher<DeleteContextStoreSourceMutation, DeleteContextStoreSourceMutationVariables>(DeleteContextStoreSourceDocument, variables)(),
    ...options
  }
    )};

export const RefreshContextStoreSourceDocument = new TypedDocumentString(`
    mutation refreshContextStoreSource($id: ID!) {
  refreshContextStoreSource(id: $id)
}
    `);

export const useRefreshContextStoreSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RefreshContextStoreSourceMutation, TError, RefreshContextStoreSourceMutationVariables, TContext>) => {
    
    return useMutation<RefreshContextStoreSourceMutation, TError, RefreshContextStoreSourceMutationVariables, TContext>(
      {
    mutationKey: ['refreshContextStoreSource'],
    mutationFn: (variables?: RefreshContextStoreSourceMutationVariables) => fetcher<RefreshContextStoreSourceMutation, RefreshContextStoreSourceMutationVariables>(RefreshContextStoreSourceDocument, variables)(),
    ...options
  }
    )};

export const SetContextStoreSourceEnabledDocument = new TypedDocumentString(`
    mutation setContextStoreSourceEnabled($id: ID!, $enabled: Boolean!) {
  setContextStoreSourceEnabled(id: $id, enabled: $enabled) {
    id
    enabled
    status
  }
}
    `);

export const useSetContextStoreSourceEnabledMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetContextStoreSourceEnabledMutation, TError, SetContextStoreSourceEnabledMutationVariables, TContext>) => {
    
    return useMutation<SetContextStoreSourceEnabledMutation, TError, SetContextStoreSourceEnabledMutationVariables, TContext>(
      {
    mutationKey: ['setContextStoreSourceEnabled'],
    mutationFn: (variables?: SetContextStoreSourceEnabledMutationVariables) => fetcher<SetContextStoreSourceEnabledMutation, SetContextStoreSourceEnabledMutationVariables>(SetContextStoreSourceEnabledDocument, variables)(),
    ...options
  }
    )};

export const UpdateContextStoreDocument = new TypedDocumentString(`
    mutation updateContextStore($workspaceId: ID!, $id: ID!, $input: UpdateContextStoreInput!) {
  updateContextStore(workspaceId: $workspaceId, id: $id, input: $input) {
    id
    name
    description
    environment
    tagIds
    version
  }
}
    `);

export const useUpdateContextStoreMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateContextStoreMutation, TError, UpdateContextStoreMutationVariables, TContext>) => {
    
    return useMutation<UpdateContextStoreMutation, TError, UpdateContextStoreMutationVariables, TContext>(
      {
    mutationKey: ['updateContextStore'],
    mutationFn: (variables?: UpdateContextStoreMutationVariables) => fetcher<UpdateContextStoreMutation, UpdateContextStoreMutationVariables>(UpdateContextStoreDocument, variables)(),
    ...options
  }
    )};

export const UpdateContextStoreSourceDocument = new TypedDocumentString(`
    mutation updateContextStoreSource($id: ID!, $input: UpdateContextStoreSourceInput!) {
  updateContextStoreSource(id: $id, input: $input) {
    id
    name
    cadence
    enabled
    status
  }
}
    `);

export const useUpdateContextStoreSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateContextStoreSourceMutation, TError, UpdateContextStoreSourceMutationVariables, TContext>) => {
    
    return useMutation<UpdateContextStoreSourceMutation, TError, UpdateContextStoreSourceMutationVariables, TContext>(
      {
    mutationKey: ['updateContextStoreSource'],
    mutationFn: (variables?: UpdateContextStoreSourceMutationVariables) => fetcher<UpdateContextStoreSourceMutation, UpdateContextStoreSourceMutationVariables>(UpdateContextStoreSourceDocument, variables)(),
    ...options
  }
    )};

export const UpdateContextStoreTagsDocument = new TypedDocumentString(`
    mutation updateContextStoreTags($workspaceId: ID!, $id: ID!, $tags: [TagInput!]!) {
  updateContextStoreTags(workspaceId: $workspaceId, id: $id, tags: $tags) {
    id
    name
  }
}
    `);

export const useUpdateContextStoreTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateContextStoreTagsMutation, TError, UpdateContextStoreTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateContextStoreTagsMutation, TError, UpdateContextStoreTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateContextStoreTags'],
    mutationFn: (variables?: UpdateContextStoreTagsMutationVariables) => fetcher<UpdateContextStoreTagsMutation, UpdateContextStoreTagsMutationVariables>(UpdateContextStoreTagsDocument, variables)(),
    ...options
  }
    )};

export const AddDataTableColumnDocument = new TypedDocumentString(`
    mutation addDataTableColumn($input: AddColumnInput!) {
  addDataTableColumn(input: $input)
}
    `);

export const useAddDataTableColumnMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<AddDataTableColumnMutation, TError, AddDataTableColumnMutationVariables, TContext>) => {
    
    return useMutation<AddDataTableColumnMutation, TError, AddDataTableColumnMutationVariables, TContext>(
      {
    mutationKey: ['addDataTableColumn'],
    mutationFn: (variables?: AddDataTableColumnMutationVariables) => fetcher<AddDataTableColumnMutation, AddDataTableColumnMutationVariables>(AddDataTableColumnDocument, variables)(),
    ...options
  }
    )};

export const CreateDataTableDocument = new TypedDocumentString(`
    mutation createDataTable($input: CreateDataTableInput!) {
  createDataTable(input: $input)
}
    `);

export const useCreateDataTableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateDataTableMutation, TError, CreateDataTableMutationVariables, TContext>) => {
    
    return useMutation<CreateDataTableMutation, TError, CreateDataTableMutationVariables, TContext>(
      {
    mutationKey: ['createDataTable'],
    mutationFn: (variables?: CreateDataTableMutationVariables) => fetcher<CreateDataTableMutation, CreateDataTableMutationVariables>(CreateDataTableDocument, variables)(),
    ...options
  }
    )};

export const DataTableRowsDocument = new TypedDocumentString(`
    query dataTableRows($environmentId: ID!, $tableId: ID!) {
  dataTableRows(environmentId: $environmentId, tableId: $tableId) {
    id
    values
  }
}
    `);

export const useDataTableRowsQuery = <
      TData = DataTableRowsQuery,
      TError = unknown
    >(
      variables: DataTableRowsQueryVariables,
      options?: Omit<UseQueryOptions<DataTableRowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableRowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableRowsQuery, TError, TData>(
      {
    queryKey: ['dataTableRows', variables],
    queryFn: fetcher<DataTableRowsQuery, DataTableRowsQueryVariables>(DataTableRowsDocument, variables),
    ...options
  }
    )};

export const DataTableRowsPageDocument = new TypedDocumentString(`
    query dataTableRowsPage($environmentId: ID!, $tableId: ID!, $limit: Int, $offset: Int) {
  dataTableRowsPage(
    environmentId: $environmentId
    tableId: $tableId
    limit: $limit
    offset: $offset
  ) {
    items {
      id
      values
    }
    hasMore
    nextOffset
  }
}
    `);

export const useDataTableRowsPageQuery = <
      TData = DataTableRowsPageQuery,
      TError = unknown
    >(
      variables: DataTableRowsPageQueryVariables,
      options?: Omit<UseQueryOptions<DataTableRowsPageQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableRowsPageQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableRowsPageQuery, TError, TData>(
      {
    queryKey: ['dataTableRowsPage', variables],
    queryFn: fetcher<DataTableRowsPageQuery, DataTableRowsPageQueryVariables>(DataTableRowsPageDocument, variables),
    ...options
  }
    )};

export const DataTableStorageUsageDocument = new TypedDocumentString(`
    query DataTableStorageUsage {
  dataTableStorageUsage {
    limitBytes
    percentage
    unlimited
    usedBytes
  }
}
    `);

export const useDataTableStorageUsageQuery = <
      TData = DataTableStorageUsageQuery,
      TError = unknown
    >(
      variables?: DataTableStorageUsageQueryVariables,
      options?: Omit<UseQueryOptions<DataTableStorageUsageQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableStorageUsageQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableStorageUsageQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['DataTableStorageUsage'] : ['DataTableStorageUsage', variables],
    queryFn: fetcher<DataTableStorageUsageQuery, DataTableStorageUsageQueryVariables>(DataTableStorageUsageDocument, variables),
    ...options
  }
    )};

export const DataTableTagsDocument = new TypedDocumentString(`
    query dataTableTags($workspaceId: ID!) {
  dataTableTags(workspaceId: $workspaceId) {
    id
    name
  }
}
    `);

export const useDataTableTagsQuery = <
      TData = DataTableTagsQuery,
      TError = unknown
    >(
      variables: DataTableTagsQueryVariables,
      options?: Omit<UseQueryOptions<DataTableTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableTagsQuery, TError, TData>(
      {
    queryKey: ['dataTableTags', variables],
    queryFn: fetcher<DataTableTagsQuery, DataTableTagsQueryVariables>(DataTableTagsDocument, variables),
    ...options
  }
    )};

export const DataTableTagsByTableDocument = new TypedDocumentString(`
    query dataTableTagsByTable {
  dataTableTagsByTable {
    tableId
    tags {
      id
      name
    }
  }
}
    `);

export const useDataTableTagsByTableQuery = <
      TData = DataTableTagsByTableQuery,
      TError = unknown
    >(
      variables?: DataTableTagsByTableQueryVariables,
      options?: Omit<UseQueryOptions<DataTableTagsByTableQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableTagsByTableQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableTagsByTableQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['dataTableTagsByTable'] : ['dataTableTagsByTable', variables],
    queryFn: fetcher<DataTableTagsByTableQuery, DataTableTagsByTableQueryVariables>(DataTableTagsByTableDocument, variables),
    ...options
  }
    )};

export const DataTablesDocument = new TypedDocumentString(`
    query dataTables($environmentId: ID!, $workspaceId: ID!) {
  dataTables(environmentId: $environmentId, workspaceId: $workspaceId) {
    id
    baseName
    lastModifiedDate
    columns {
      id
      name
      type
    }
  }
}
    `);

export const useDataTablesQuery = <
      TData = DataTablesQuery,
      TError = unknown
    >(
      variables: DataTablesQueryVariables,
      options?: Omit<UseQueryOptions<DataTablesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTablesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTablesQuery, TError, TData>(
      {
    queryKey: ['dataTables', variables],
    queryFn: fetcher<DataTablesQuery, DataTablesQueryVariables>(DataTablesDocument, variables),
    ...options
  }
    )};

export const DeleteDataTableRowDocument = new TypedDocumentString(`
    mutation deleteDataTableRow($input: DeleteRowInput!) {
  deleteDataTableRow(input: $input)
}
    `);

export const useDeleteDataTableRowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteDataTableRowMutation, TError, DeleteDataTableRowMutationVariables, TContext>) => {
    
    return useMutation<DeleteDataTableRowMutation, TError, DeleteDataTableRowMutationVariables, TContext>(
      {
    mutationKey: ['deleteDataTableRow'],
    mutationFn: (variables?: DeleteDataTableRowMutationVariables) => fetcher<DeleteDataTableRowMutation, DeleteDataTableRowMutationVariables>(DeleteDataTableRowDocument, variables)(),
    ...options
  }
    )};

export const DropDataTableDocument = new TypedDocumentString(`
    mutation dropDataTable($input: RemoveTableInput!) {
  dropDataTable(input: $input)
}
    `);

export const useDropDataTableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DropDataTableMutation, TError, DropDataTableMutationVariables, TContext>) => {
    
    return useMutation<DropDataTableMutation, TError, DropDataTableMutationVariables, TContext>(
      {
    mutationKey: ['dropDataTable'],
    mutationFn: (variables?: DropDataTableMutationVariables) => fetcher<DropDataTableMutation, DropDataTableMutationVariables>(DropDataTableDocument, variables)(),
    ...options
  }
    )};

export const DuplicateDataTableDocument = new TypedDocumentString(`
    mutation duplicateDataTable($input: DuplicateDataTableInput!) {
  duplicateDataTable(input: $input)
}
    `);

export const useDuplicateDataTableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DuplicateDataTableMutation, TError, DuplicateDataTableMutationVariables, TContext>) => {
    
    return useMutation<DuplicateDataTableMutation, TError, DuplicateDataTableMutationVariables, TContext>(
      {
    mutationKey: ['duplicateDataTable'],
    mutationFn: (variables?: DuplicateDataTableMutationVariables) => fetcher<DuplicateDataTableMutation, DuplicateDataTableMutationVariables>(DuplicateDataTableDocument, variables)(),
    ...options
  }
    )};

export const ExportDataTableCsvDocument = new TypedDocumentString(`
    query exportDataTableCsv($environmentId: ID!, $tableId: ID!) {
  exportDataTableCsv(environmentId: $environmentId, tableId: $tableId)
}
    `);

export const useExportDataTableCsvQuery = <
      TData = ExportDataTableCsvQuery,
      TError = unknown
    >(
      variables: ExportDataTableCsvQueryVariables,
      options?: Omit<UseQueryOptions<ExportDataTableCsvQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ExportDataTableCsvQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ExportDataTableCsvQuery, TError, TData>(
      {
    queryKey: ['exportDataTableCsv', variables],
    queryFn: fetcher<ExportDataTableCsvQuery, ExportDataTableCsvQueryVariables>(ExportDataTableCsvDocument, variables),
    ...options
  }
    )};

export const ImportDataTableCsvDocument = new TypedDocumentString(`
    mutation importDataTableCsv($input: ImportCsvInput!) {
  importDataTableCsv(input: $input)
}
    `);

export const useImportDataTableCsvMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportDataTableCsvMutation, TError, ImportDataTableCsvMutationVariables, TContext>) => {
    
    return useMutation<ImportDataTableCsvMutation, TError, ImportDataTableCsvMutationVariables, TContext>(
      {
    mutationKey: ['importDataTableCsv'],
    mutationFn: (variables?: ImportDataTableCsvMutationVariables) => fetcher<ImportDataTableCsvMutation, ImportDataTableCsvMutationVariables>(ImportDataTableCsvDocument, variables)(),
    ...options
  }
    )};

export const InsertDataTableRowDocument = new TypedDocumentString(`
    mutation insertDataTableRow($input: InsertRowInput!) {
  insertDataTableRow(input: $input) {
    id
    values
  }
}
    `);

export const useInsertDataTableRowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<InsertDataTableRowMutation, TError, InsertDataTableRowMutationVariables, TContext>) => {
    
    return useMutation<InsertDataTableRowMutation, TError, InsertDataTableRowMutationVariables, TContext>(
      {
    mutationKey: ['insertDataTableRow'],
    mutationFn: (variables?: InsertDataTableRowMutationVariables) => fetcher<InsertDataTableRowMutation, InsertDataTableRowMutationVariables>(InsertDataTableRowDocument, variables)(),
    ...options
  }
    )};

export const RemoveDataTableColumnDocument = new TypedDocumentString(`
    mutation removeDataTableColumn($input: RemoveColumnInput!) {
  removeDataTableColumn(input: $input)
}
    `);

export const useRemoveDataTableColumnMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RemoveDataTableColumnMutation, TError, RemoveDataTableColumnMutationVariables, TContext>) => {
    
    return useMutation<RemoveDataTableColumnMutation, TError, RemoveDataTableColumnMutationVariables, TContext>(
      {
    mutationKey: ['removeDataTableColumn'],
    mutationFn: (variables?: RemoveDataTableColumnMutationVariables) => fetcher<RemoveDataTableColumnMutation, RemoveDataTableColumnMutationVariables>(RemoveDataTableColumnDocument, variables)(),
    ...options
  }
    )};

export const RenameDataTableDocument = new TypedDocumentString(`
    mutation renameDataTable($input: RenameDataTableInput!) {
  renameDataTable(input: $input)
}
    `);

export const useRenameDataTableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RenameDataTableMutation, TError, RenameDataTableMutationVariables, TContext>) => {
    
    return useMutation<RenameDataTableMutation, TError, RenameDataTableMutationVariables, TContext>(
      {
    mutationKey: ['renameDataTable'],
    mutationFn: (variables?: RenameDataTableMutationVariables) => fetcher<RenameDataTableMutation, RenameDataTableMutationVariables>(RenameDataTableDocument, variables)(),
    ...options
  }
    )};

export const RenameDataTableColumnDocument = new TypedDocumentString(`
    mutation renameDataTableColumn($input: RenameColumnInput!) {
  renameDataTableColumn(input: $input)
}
    `);

export const useRenameDataTableColumnMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RenameDataTableColumnMutation, TError, RenameDataTableColumnMutationVariables, TContext>) => {
    
    return useMutation<RenameDataTableColumnMutation, TError, RenameDataTableColumnMutationVariables, TContext>(
      {
    mutationKey: ['renameDataTableColumn'],
    mutationFn: (variables?: RenameDataTableColumnMutationVariables) => fetcher<RenameDataTableColumnMutation, RenameDataTableColumnMutationVariables>(RenameDataTableColumnDocument, variables)(),
    ...options
  }
    )};

export const UpdateDataTableRowDocument = new TypedDocumentString(`
    mutation updateDataTableRow($input: UpdateRowInput!) {
  updateDataTableRow(input: $input) {
    id
    values
  }
}
    `);

export const useUpdateDataTableRowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateDataTableRowMutation, TError, UpdateDataTableRowMutationVariables, TContext>) => {
    
    return useMutation<UpdateDataTableRowMutation, TError, UpdateDataTableRowMutationVariables, TContext>(
      {
    mutationKey: ['updateDataTableRow'],
    mutationFn: (variables?: UpdateDataTableRowMutationVariables) => fetcher<UpdateDataTableRowMutation, UpdateDataTableRowMutationVariables>(UpdateDataTableRowDocument, variables)(),
    ...options
  }
    )};

export const UpdateDataTableTagsDocument = new TypedDocumentString(`
    mutation updateDataTableTags($input: UpdateDataTableTagsInput!) {
  updateDataTableTags(input: $input)
}
    `);

export const useUpdateDataTableTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateDataTableTagsMutation, TError, UpdateDataTableTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateDataTableTagsMutation, TError, UpdateDataTableTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateDataTableTags'],
    mutationFn: (variables?: UpdateDataTableTagsMutationVariables) => fetcher<UpdateDataTableTagsMutation, UpdateDataTableTagsMutationVariables>(UpdateDataTableTagsDocument, variables)(),
    ...options
  }
    )};

export const CreateKnowledgeBaseDocument = new TypedDocumentString(`
    mutation createKnowledgeBase($knowledgeBase: KnowledgeBaseInput!, $environmentId: ID!, $workspaceId: ID!) {
  createKnowledgeBase(
    knowledgeBase: $knowledgeBase
    environmentId: $environmentId
    workspaceId: $workspaceId
  ) {
    id
    name
  }
}
    `);

export const useCreateKnowledgeBaseMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateKnowledgeBaseMutation, TError, CreateKnowledgeBaseMutationVariables, TContext>) => {
    
    return useMutation<CreateKnowledgeBaseMutation, TError, CreateKnowledgeBaseMutationVariables, TContext>(
      {
    mutationKey: ['createKnowledgeBase'],
    mutationFn: (variables?: CreateKnowledgeBaseMutationVariables) => fetcher<CreateKnowledgeBaseMutation, CreateKnowledgeBaseMutationVariables>(CreateKnowledgeBaseDocument, variables)(),
    ...options
  }
    )};

export const CreateKnowledgeBaseSourceDocument = new TypedDocumentString(`
    mutation createKnowledgeBaseSource($input: CreateKnowledgeBaseSourceInput!) {
  createKnowledgeBaseSource(input: $input) {
    id
    name
    status
    enabled
    cadence
  }
}
    `);

export const useCreateKnowledgeBaseSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateKnowledgeBaseSourceMutation, TError, CreateKnowledgeBaseSourceMutationVariables, TContext>) => {
    
    return useMutation<CreateKnowledgeBaseSourceMutation, TError, CreateKnowledgeBaseSourceMutationVariables, TContext>(
      {
    mutationKey: ['createKnowledgeBaseSource'],
    mutationFn: (variables?: CreateKnowledgeBaseSourceMutationVariables) => fetcher<CreateKnowledgeBaseSourceMutation, CreateKnowledgeBaseSourceMutationVariables>(CreateKnowledgeBaseSourceDocument, variables)(),
    ...options
  }
    )};

export const DeleteKnowledgeBaseDocument = new TypedDocumentString(`
    mutation deleteKnowledgeBase($id: ID!) {
  deleteKnowledgeBase(id: $id)
}
    `);

export const useDeleteKnowledgeBaseMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteKnowledgeBaseMutation, TError, DeleteKnowledgeBaseMutationVariables, TContext>) => {
    
    return useMutation<DeleteKnowledgeBaseMutation, TError, DeleteKnowledgeBaseMutationVariables, TContext>(
      {
    mutationKey: ['deleteKnowledgeBase'],
    mutationFn: (variables?: DeleteKnowledgeBaseMutationVariables) => fetcher<DeleteKnowledgeBaseMutation, DeleteKnowledgeBaseMutationVariables>(DeleteKnowledgeBaseDocument, variables)(),
    ...options
  }
    )};

export const DeleteKnowledgeBaseDocumentDocument = new TypedDocumentString(`
    mutation deleteKnowledgeBaseDocument($id: ID!) {
  deleteKnowledgeBaseDocument(id: $id)
}
    `);

export const useDeleteKnowledgeBaseDocumentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteKnowledgeBaseDocumentMutation, TError, DeleteKnowledgeBaseDocumentMutationVariables, TContext>) => {
    
    return useMutation<DeleteKnowledgeBaseDocumentMutation, TError, DeleteKnowledgeBaseDocumentMutationVariables, TContext>(
      {
    mutationKey: ['deleteKnowledgeBaseDocument'],
    mutationFn: (variables?: DeleteKnowledgeBaseDocumentMutationVariables) => fetcher<DeleteKnowledgeBaseDocumentMutation, DeleteKnowledgeBaseDocumentMutationVariables>(DeleteKnowledgeBaseDocumentDocument, variables)(),
    ...options
  }
    )};

export const DeleteKnowledgeBaseDocumentChunkDocument = new TypedDocumentString(`
    mutation deleteKnowledgeBaseDocumentChunk($id: ID!) {
  deleteKnowledgeBaseDocumentChunk(id: $id)
}
    `);

export const useDeleteKnowledgeBaseDocumentChunkMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteKnowledgeBaseDocumentChunkMutation, TError, DeleteKnowledgeBaseDocumentChunkMutationVariables, TContext>) => {
    
    return useMutation<DeleteKnowledgeBaseDocumentChunkMutation, TError, DeleteKnowledgeBaseDocumentChunkMutationVariables, TContext>(
      {
    mutationKey: ['deleteKnowledgeBaseDocumentChunk'],
    mutationFn: (variables?: DeleteKnowledgeBaseDocumentChunkMutationVariables) => fetcher<DeleteKnowledgeBaseDocumentChunkMutation, DeleteKnowledgeBaseDocumentChunkMutationVariables>(DeleteKnowledgeBaseDocumentChunkDocument, variables)(),
    ...options
  }
    )};

export const DeleteKnowledgeBaseSourceDocument = new TypedDocumentString(`
    mutation deleteKnowledgeBaseSource($id: ID!) {
  deleteKnowledgeBaseSource(id: $id)
}
    `);

export const useDeleteKnowledgeBaseSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteKnowledgeBaseSourceMutation, TError, DeleteKnowledgeBaseSourceMutationVariables, TContext>) => {
    
    return useMutation<DeleteKnowledgeBaseSourceMutation, TError, DeleteKnowledgeBaseSourceMutationVariables, TContext>(
      {
    mutationKey: ['deleteKnowledgeBaseSource'],
    mutationFn: (variables?: DeleteKnowledgeBaseSourceMutationVariables) => fetcher<DeleteKnowledgeBaseSourceMutation, DeleteKnowledgeBaseSourceMutationVariables>(DeleteKnowledgeBaseSourceDocument, variables)(),
    ...options
  }
    )};

export const KnowledgeBaseDocument = new TypedDocumentString(`
    query knowledgeBase($id: ID!) {
  knowledgeBase(id: $id) {
    id
    name
    description
    maxChunkSize
    minChunkSizeChars
    overlap
    documents {
      id
      name
      document {
        name
        extension
        mimeType
        url
      }
      status
      tags
      createdDate
      sourceId
      sourceRecordId
      chunks {
        id
        knowledgeBaseDocumentId
      }
    }
    createdDate
    lastModifiedDate
  }
}
    `);

export const useKnowledgeBaseQuery = <
      TData = KnowledgeBaseQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseQuery, TError, TData>(
      {
    queryKey: ['knowledgeBase', variables],
    queryFn: fetcher<KnowledgeBaseQuery, KnowledgeBaseQueryVariables>(KnowledgeBaseDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseDocumentChunksDocument = new TypedDocumentString(`
    query knowledgeBaseDocumentChunks($id: ID!) {
  knowledgeBaseDocumentChunks(id: $id) {
    id
    knowledgeBaseDocumentId
    content
    metadata
  }
}
    `);

export const useKnowledgeBaseDocumentChunksQuery = <
      TData = KnowledgeBaseDocumentChunksQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseDocumentChunksQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseDocumentChunksQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseDocumentChunksQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseDocumentChunksQuery, TError, TData>(
      {
    queryKey: ['knowledgeBaseDocumentChunks', variables],
    queryFn: fetcher<KnowledgeBaseDocumentChunksQuery, KnowledgeBaseDocumentChunksQueryVariables>(KnowledgeBaseDocumentChunksDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseDocumentStatusDocument = new TypedDocumentString(`
    query knowledgeBaseDocumentStatus($id: ID!) {
  knowledgeBaseDocumentStatus(id: $id) {
    documentId
    status
    timestamp
    message
  }
}
    `);

export const useKnowledgeBaseDocumentStatusQuery = <
      TData = KnowledgeBaseDocumentStatusQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseDocumentStatusQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseDocumentStatusQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseDocumentStatusQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseDocumentStatusQuery, TError, TData>(
      {
    queryKey: ['knowledgeBaseDocumentStatus', variables],
    queryFn: fetcher<KnowledgeBaseDocumentStatusQuery, KnowledgeBaseDocumentStatusQueryVariables>(KnowledgeBaseDocumentStatusDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseDocumentTagsDocument = new TypedDocumentString(`
    query knowledgeBaseDocumentTags {
  knowledgeBaseDocumentTags
}
    `);

export const useKnowledgeBaseDocumentTagsQuery = <
      TData = KnowledgeBaseDocumentTagsQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseDocumentTagsQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseDocumentTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseDocumentTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseDocumentTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['knowledgeBaseDocumentTags'] : ['knowledgeBaseDocumentTags', variables],
    queryFn: fetcher<KnowledgeBaseDocumentTagsQuery, KnowledgeBaseDocumentTagsQueryVariables>(KnowledgeBaseDocumentTagsDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseDocumentTagsByDocumentDocument = new TypedDocumentString(`
    query knowledgeBaseDocumentTagsByDocument {
  knowledgeBaseDocumentTagsByDocument {
    knowledgeBaseDocumentId
    tags
  }
}
    `);

export const useKnowledgeBaseDocumentTagsByDocumentQuery = <
      TData = KnowledgeBaseDocumentTagsByDocumentQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseDocumentTagsByDocumentQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseDocumentTagsByDocumentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseDocumentTagsByDocumentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseDocumentTagsByDocumentQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['knowledgeBaseDocumentTagsByDocument'] : ['knowledgeBaseDocumentTagsByDocument', variables],
    queryFn: fetcher<KnowledgeBaseDocumentTagsByDocumentQuery, KnowledgeBaseDocumentTagsByDocumentQueryVariables>(KnowledgeBaseDocumentTagsByDocumentDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseEmbeddingActiveDocument = new TypedDocumentString(`
    query knowledgeBaseEmbeddingActive($environment: Int!) {
  knowledgeBaseEmbeddingActive(environment: $environment)
}
    `);

export const useKnowledgeBaseEmbeddingActiveQuery = <
      TData = KnowledgeBaseEmbeddingActiveQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseEmbeddingActiveQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseEmbeddingActiveQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseEmbeddingActiveQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseEmbeddingActiveQuery, TError, TData>(
      {
    queryKey: ['knowledgeBaseEmbeddingActive', variables],
    queryFn: fetcher<KnowledgeBaseEmbeddingActiveQuery, KnowledgeBaseEmbeddingActiveQueryVariables>(KnowledgeBaseEmbeddingActiveDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseSourceDocument = new TypedDocumentString(`
    query knowledgeBaseSource($id: ID!) {
  knowledgeBaseSource(id: $id) {
    id
    name
    knowledgeBaseId
    sourceComponentName
    sourceComponentVersion
    sourceClusterElementName
    connectionId
    cadence
    status
    enabled
    lastSyncRunAt
    lastSyncJobExecutionId
    workflowId
  }
}
    `);

export const useKnowledgeBaseSourceQuery = <
      TData = KnowledgeBaseSourceQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseSourceQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseSourceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseSourceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseSourceQuery, TError, TData>(
      {
    queryKey: ['knowledgeBaseSource', variables],
    queryFn: fetcher<KnowledgeBaseSourceQuery, KnowledgeBaseSourceQueryVariables>(KnowledgeBaseSourceDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseSourcesDocument = new TypedDocumentString(`
    query knowledgeBaseSources($workspaceId: ID!, $environmentId: ID!, $filter: KnowledgeBaseSourceFilter) {
  knowledgeBaseSources(
    workspaceId: $workspaceId
    environmentId: $environmentId
    filter: $filter
  ) {
    id
    name
    knowledgeBaseId
    sourceComponentName
    sourceComponentVersion
    sourceClusterElementName
    connectionId
    cadence
    status
    enabled
    lastSyncRunAt
    lastSyncJobExecutionId
    workflowId
  }
}
    `);

export const useKnowledgeBaseSourcesQuery = <
      TData = KnowledgeBaseSourcesQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseSourcesQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseSourcesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseSourcesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseSourcesQuery, TError, TData>(
      {
    queryKey: ['knowledgeBaseSources', variables],
    queryFn: fetcher<KnowledgeBaseSourcesQuery, KnowledgeBaseSourcesQueryVariables>(KnowledgeBaseSourcesDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseStorageUsageDocument = new TypedDocumentString(`
    query KnowledgeBaseStorageUsage {
  knowledgeBaseStorageUsage {
    limitBytes
    percentage
    unlimited
    usedBytes
  }
}
    `);

export const useKnowledgeBaseStorageUsageQuery = <
      TData = KnowledgeBaseStorageUsageQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseStorageUsageQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseStorageUsageQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseStorageUsageQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseStorageUsageQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['KnowledgeBaseStorageUsage'] : ['KnowledgeBaseStorageUsage', variables],
    queryFn: fetcher<KnowledgeBaseStorageUsageQuery, KnowledgeBaseStorageUsageQueryVariables>(KnowledgeBaseStorageUsageDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseTagsDocument = new TypedDocumentString(`
    query knowledgeBaseTags($workspaceId: ID!) {
  knowledgeBaseTags(workspaceId: $workspaceId) {
    id
    name
  }
}
    `);

export const useKnowledgeBaseTagsQuery = <
      TData = KnowledgeBaseTagsQuery,
      TError = unknown
    >(
      variables: KnowledgeBaseTagsQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseTagsQuery, TError, TData>(
      {
    queryKey: ['knowledgeBaseTags', variables],
    queryFn: fetcher<KnowledgeBaseTagsQuery, KnowledgeBaseTagsQueryVariables>(KnowledgeBaseTagsDocument, variables),
    ...options
  }
    )};

export const KnowledgeBaseTagsByKnowledgeBaseDocument = new TypedDocumentString(`
    query knowledgeBaseTagsByKnowledgeBase {
  knowledgeBaseTagsByKnowledgeBase {
    knowledgeBaseId
    tags {
      id
      name
    }
  }
}
    `);

export const useKnowledgeBaseTagsByKnowledgeBaseQuery = <
      TData = KnowledgeBaseTagsByKnowledgeBaseQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseTagsByKnowledgeBaseQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseTagsByKnowledgeBaseQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseTagsByKnowledgeBaseQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseTagsByKnowledgeBaseQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['knowledgeBaseTagsByKnowledgeBase'] : ['knowledgeBaseTagsByKnowledgeBase', variables],
    queryFn: fetcher<KnowledgeBaseTagsByKnowledgeBaseQuery, KnowledgeBaseTagsByKnowledgeBaseQueryVariables>(KnowledgeBaseTagsByKnowledgeBaseDocument, variables),
    ...options
  }
    )};

export const KnowledgeBasesDocument = new TypedDocumentString(`
    query knowledgeBases($environmentId: ID!, $workspaceId: ID!) {
  knowledgeBases(environmentId: $environmentId, workspaceId: $workspaceId) {
    id
    name
    description
    maxChunkSize
    minChunkSizeChars
    overlap
    createdDate
    lastModifiedDate
  }
}
    `);

export const useKnowledgeBasesQuery = <
      TData = KnowledgeBasesQuery,
      TError = unknown
    >(
      variables: KnowledgeBasesQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBasesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBasesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBasesQuery, TError, TData>(
      {
    queryKey: ['knowledgeBases', variables],
    queryFn: fetcher<KnowledgeBasesQuery, KnowledgeBasesQueryVariables>(KnowledgeBasesDocument, variables),
    ...options
  }
    )};

export const RefreshKnowledgeBaseSourceDocument = new TypedDocumentString(`
    mutation refreshKnowledgeBaseSource($id: ID!) {
  refreshKnowledgeBaseSource(id: $id)
}
    `);

export const useRefreshKnowledgeBaseSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RefreshKnowledgeBaseSourceMutation, TError, RefreshKnowledgeBaseSourceMutationVariables, TContext>) => {
    
    return useMutation<RefreshKnowledgeBaseSourceMutation, TError, RefreshKnowledgeBaseSourceMutationVariables, TContext>(
      {
    mutationKey: ['refreshKnowledgeBaseSource'],
    mutationFn: (variables?: RefreshKnowledgeBaseSourceMutationVariables) => fetcher<RefreshKnowledgeBaseSourceMutation, RefreshKnowledgeBaseSourceMutationVariables>(RefreshKnowledgeBaseSourceDocument, variables)(),
    ...options
  }
    )};

export const SearchKnowledgeBaseDocument = new TypedDocumentString(`
    query searchKnowledgeBase($id: ID!, $query: String!, $metadataFilters: String) {
  searchKnowledgeBase(id: $id, query: $query, metadataFilters: $metadataFilters) {
    id
    knowledgeBaseDocumentId
    content
    metadata
    score
  }
}
    `);

export const useSearchKnowledgeBaseQuery = <
      TData = SearchKnowledgeBaseQuery,
      TError = unknown
    >(
      variables: SearchKnowledgeBaseQueryVariables,
      options?: Omit<UseQueryOptions<SearchKnowledgeBaseQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<SearchKnowledgeBaseQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<SearchKnowledgeBaseQuery, TError, TData>(
      {
    queryKey: ['searchKnowledgeBase', variables],
    queryFn: fetcher<SearchKnowledgeBaseQuery, SearchKnowledgeBaseQueryVariables>(SearchKnowledgeBaseDocument, variables),
    ...options
  }
    )};

export const SetKnowledgeBaseSourceEnabledDocument = new TypedDocumentString(`
    mutation setKnowledgeBaseSourceEnabled($id: ID!, $enabled: Boolean!) {
  setKnowledgeBaseSourceEnabled(id: $id, enabled: $enabled) {
    id
    enabled
    status
  }
}
    `);

export const useSetKnowledgeBaseSourceEnabledMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SetKnowledgeBaseSourceEnabledMutation, TError, SetKnowledgeBaseSourceEnabledMutationVariables, TContext>) => {
    
    return useMutation<SetKnowledgeBaseSourceEnabledMutation, TError, SetKnowledgeBaseSourceEnabledMutationVariables, TContext>(
      {
    mutationKey: ['setKnowledgeBaseSourceEnabled'],
    mutationFn: (variables?: SetKnowledgeBaseSourceEnabledMutationVariables) => fetcher<SetKnowledgeBaseSourceEnabledMutation, SetKnowledgeBaseSourceEnabledMutationVariables>(SetKnowledgeBaseSourceEnabledDocument, variables)(),
    ...options
  }
    )};

export const UpdateKnowledgeBaseDocument = new TypedDocumentString(`
    mutation updateKnowledgeBase($id: ID!, $knowledgeBase: KnowledgeBaseInput!) {
  updateKnowledgeBase(id: $id, knowledgeBase: $knowledgeBase) {
    id
    name
    description
    maxChunkSize
    minChunkSizeChars
    overlap
  }
}
    `);

export const useUpdateKnowledgeBaseMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseMutation, TError, UpdateKnowledgeBaseMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseMutation, TError, UpdateKnowledgeBaseMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBase'],
    mutationFn: (variables?: UpdateKnowledgeBaseMutationVariables) => fetcher<UpdateKnowledgeBaseMutation, UpdateKnowledgeBaseMutationVariables>(UpdateKnowledgeBaseDocument, variables)(),
    ...options
  }
    )};

export const UpdateKnowledgeBaseDocumentChunkDocument = new TypedDocumentString(`
    mutation updateKnowledgeBaseDocumentChunk($id: ID!, $knowledgeBaseDocumentChunk: KnowledgeBaseDocumentChunkInput!) {
  updateKnowledgeBaseDocumentChunk(
    id: $id
    knowledgeBaseDocumentChunk: $knowledgeBaseDocumentChunk
  ) {
    id
    knowledgeBaseDocumentId
    content
    metadata
  }
}
    `);

export const useUpdateKnowledgeBaseDocumentChunkMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseDocumentChunkMutation, TError, UpdateKnowledgeBaseDocumentChunkMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseDocumentChunkMutation, TError, UpdateKnowledgeBaseDocumentChunkMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBaseDocumentChunk'],
    mutationFn: (variables?: UpdateKnowledgeBaseDocumentChunkMutationVariables) => fetcher<UpdateKnowledgeBaseDocumentChunkMutation, UpdateKnowledgeBaseDocumentChunkMutationVariables>(UpdateKnowledgeBaseDocumentChunkDocument, variables)(),
    ...options
  }
    )};

export const UpdateKnowledgeBaseDocumentTagsDocument = new TypedDocumentString(`
    mutation updateKnowledgeBaseDocumentTags($input: UpdateKnowledgeBaseDocumentTagsInput!) {
  updateKnowledgeBaseDocumentTags(input: $input)
}
    `);

export const useUpdateKnowledgeBaseDocumentTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseDocumentTagsMutation, TError, UpdateKnowledgeBaseDocumentTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseDocumentTagsMutation, TError, UpdateKnowledgeBaseDocumentTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBaseDocumentTags'],
    mutationFn: (variables?: UpdateKnowledgeBaseDocumentTagsMutationVariables) => fetcher<UpdateKnowledgeBaseDocumentTagsMutation, UpdateKnowledgeBaseDocumentTagsMutationVariables>(UpdateKnowledgeBaseDocumentTagsDocument, variables)(),
    ...options
  }
    )};

export const UpdateKnowledgeBaseSourceDocument = new TypedDocumentString(`
    mutation updateKnowledgeBaseSource($id: ID!, $input: UpdateKnowledgeBaseSourceInput!) {
  updateKnowledgeBaseSource(id: $id, input: $input) {
    id
    name
    cadence
    enabled
    status
  }
}
    `);

export const useUpdateKnowledgeBaseSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseSourceMutation, TError, UpdateKnowledgeBaseSourceMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseSourceMutation, TError, UpdateKnowledgeBaseSourceMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBaseSource'],
    mutationFn: (variables?: UpdateKnowledgeBaseSourceMutationVariables) => fetcher<UpdateKnowledgeBaseSourceMutation, UpdateKnowledgeBaseSourceMutationVariables>(UpdateKnowledgeBaseSourceDocument, variables)(),
    ...options
  }
    )};

export const UpdateKnowledgeBaseTagsDocument = new TypedDocumentString(`
    mutation updateKnowledgeBaseTags($input: UpdateKnowledgeBaseTagsInput!) {
  updateKnowledgeBaseTags(input: $input)
}
    `);

export const useUpdateKnowledgeBaseTagsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateKnowledgeBaseTagsMutation, TError, UpdateKnowledgeBaseTagsMutationVariables, TContext>) => {
    
    return useMutation<UpdateKnowledgeBaseTagsMutation, TError, UpdateKnowledgeBaseTagsMutationVariables, TContext>(
      {
    mutationKey: ['updateKnowledgeBaseTags'],
    mutationFn: (variables?: UpdateKnowledgeBaseTagsMutationVariables) => fetcher<UpdateKnowledgeBaseTagsMutation, UpdateKnowledgeBaseTagsMutationVariables>(UpdateKnowledgeBaseTagsDocument, variables)(),
    ...options
  }
    )};

export const AutomationSearchDocument = new TypedDocumentString(`
    query automationSearch($query: String!, $limit: Int) {
  automationSearch(query: $query, limit: $limit) {
    id
    name
    description
    type
    ... on WorkflowSearchResult {
      projectId
      label
    }
    ... on ProjectDeploymentSearchResult {
      projectName
    }
    ... on ApiEndpointSearchResult {
      collectionId
      path
    }
    ... on KnowledgeBaseDocumentSearchResult {
      knowledgeBaseId
    }
  }
}
    `);

export const useAutomationSearchQuery = <
      TData = AutomationSearchQuery,
      TError = unknown
    >(
      variables: AutomationSearchQueryVariables,
      options?: Omit<UseQueryOptions<AutomationSearchQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AutomationSearchQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AutomationSearchQuery, TError, TData>(
      {
    queryKey: ['automationSearch', variables],
    queryFn: fetcher<AutomationSearchQuery, AutomationSearchQueryVariables>(AutomationSearchDocument, variables),
    ...options
  }
    )};

export const WorkflowAlertRulesDocument = new TypedDocumentString(`
    query workflowAlertRules($workspaceId: ID!) {
  workflowAlertRules(workspaceId: $workspaceId) {
    cooldownMinutes
    enabled
    id
    lastTriggeredDate
    name
    notificationIds
    ruleType
    threshold
    windowMinutes
    workflowId
  }
}
    `);

export const useWorkflowAlertRulesQuery = <
      TData = WorkflowAlertRulesQuery,
      TError = unknown
    >(
      variables: WorkflowAlertRulesQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowAlertRulesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowAlertRulesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowAlertRulesQuery, TError, TData>(
      {
    queryKey: ['workflowAlertRules', variables],
    queryFn: fetcher<WorkflowAlertRulesQuery, WorkflowAlertRulesQueryVariables>(WorkflowAlertRulesDocument, variables),
    ...options
  }
    )};

export const WorkflowAlertEventsDocument = new TypedDocumentString(`
    query workflowAlertEvents($workspaceId: ID!) {
  workflowAlertEvents(workspaceId: $workspaceId) {
    createdDate
    id
    jobId
    message
    triggeredValue
    workflowAlertRuleId
  }
}
    `);

export const useWorkflowAlertEventsQuery = <
      TData = WorkflowAlertEventsQuery,
      TError = unknown
    >(
      variables: WorkflowAlertEventsQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowAlertEventsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowAlertEventsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowAlertEventsQuery, TError, TData>(
      {
    queryKey: ['workflowAlertEvents', variables],
    queryFn: fetcher<WorkflowAlertEventsQuery, WorkflowAlertEventsQueryVariables>(WorkflowAlertEventsDocument, variables),
    ...options
  }
    )};

export const CreateWorkflowAlertRuleDocument = new TypedDocumentString(`
    mutation createWorkflowAlertRule($workspaceId: ID!, $input: WorkflowAlertRuleInput!) {
  createWorkflowAlertRule(workspaceId: $workspaceId, input: $input) {
    id
  }
}
    `);

export const useCreateWorkflowAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkflowAlertRuleMutation, TError, CreateWorkflowAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkflowAlertRuleMutation, TError, CreateWorkflowAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['createWorkflowAlertRule'],
    mutationFn: (variables?: CreateWorkflowAlertRuleMutationVariables) => fetcher<CreateWorkflowAlertRuleMutation, CreateWorkflowAlertRuleMutationVariables>(CreateWorkflowAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkflowAlertRuleDocument = new TypedDocumentString(`
    mutation updateWorkflowAlertRule($id: ID!, $input: WorkflowAlertRuleInput!) {
  updateWorkflowAlertRule(id: $id, input: $input) {
    id
  }
}
    `);

export const useUpdateWorkflowAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkflowAlertRuleMutation, TError, UpdateWorkflowAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkflowAlertRuleMutation, TError, UpdateWorkflowAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkflowAlertRule'],
    mutationFn: (variables?: UpdateWorkflowAlertRuleMutationVariables) => fetcher<UpdateWorkflowAlertRuleMutation, UpdateWorkflowAlertRuleMutationVariables>(UpdateWorkflowAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkflowAlertRuleDocument = new TypedDocumentString(`
    mutation deleteWorkflowAlertRule($id: ID!) {
  deleteWorkflowAlertRule(id: $id)
}
    `);

export const useDeleteWorkflowAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkflowAlertRuleMutation, TError, DeleteWorkflowAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkflowAlertRuleMutation, TError, DeleteWorkflowAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkflowAlertRule'],
    mutationFn: (variables?: DeleteWorkflowAlertRuleMutationVariables) => fetcher<DeleteWorkflowAlertRuleMutation, DeleteWorkflowAlertRuleMutationVariables>(DeleteWorkflowAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const SendTestWorkflowAlertDocument = new TypedDocumentString(`
    mutation sendTestWorkflowAlert($id: ID!) {
  sendTestWorkflowAlert(id: $id)
}
    `);

export const useSendTestWorkflowAlertMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SendTestWorkflowAlertMutation, TError, SendTestWorkflowAlertMutationVariables, TContext>) => {
    
    return useMutation<SendTestWorkflowAlertMutation, TError, SendTestWorkflowAlertMutationVariables, TContext>(
      {
    mutationKey: ['sendTestWorkflowAlert'],
    mutationFn: (variables?: SendTestWorkflowAlertMutationVariables) => fetcher<SendTestWorkflowAlertMutation, SendTestWorkflowAlertMutationVariables>(SendTestWorkflowAlertDocument, variables)(),
    ...options
  }
    )};

export const EnableWorkflowAlertRuleDocument = new TypedDocumentString(`
    mutation enableWorkflowAlertRule($id: ID!, $enabled: Boolean!) {
  enableWorkflowAlertRule(id: $id, enabled: $enabled) {
    enabled
    id
  }
}
    `);

export const useEnableWorkflowAlertRuleMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableWorkflowAlertRuleMutation, TError, EnableWorkflowAlertRuleMutationVariables, TContext>) => {
    
    return useMutation<EnableWorkflowAlertRuleMutation, TError, EnableWorkflowAlertRuleMutationVariables, TContext>(
      {
    mutationKey: ['enableWorkflowAlertRule'],
    mutationFn: (variables?: EnableWorkflowAlertRuleMutationVariables) => fetcher<EnableWorkflowAlertRuleMutation, EnableWorkflowAlertRuleMutationVariables>(EnableWorkflowAlertRuleDocument, variables)(),
    ...options
  }
    )};

export const WorkflowExecutionCostDocument = new TypedDocumentString(`
    query workflowExecutionCost($jobId: ID!) {
  workflowExecutionCost(jobId: $jobId) {
    aiCost
    baseRunCharge
    currency
    id
    jobId
    totalCost
  }
}
    `);

export const useWorkflowExecutionCostQuery = <
      TData = WorkflowExecutionCostQuery,
      TError = unknown
    >(
      variables: WorkflowExecutionCostQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowExecutionCostQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowExecutionCostQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowExecutionCostQuery, TError, TData>(
      {
    queryKey: ['workflowExecutionCost', variables],
    queryFn: fetcher<WorkflowExecutionCostQuery, WorkflowExecutionCostQueryVariables>(WorkflowExecutionCostDocument, variables),
    ...options
  }
    )};

export const CodeWorkflowSourceDocument = new TypedDocumentString(`
    query codeWorkflowSource($projectId: ID!) {
  codeWorkflowSource(projectId: $projectId)
}
    `);

export const useCodeWorkflowSourceQuery = <
      TData = CodeWorkflowSourceQuery,
      TError = unknown
    >(
      variables: CodeWorkflowSourceQueryVariables,
      options?: Omit<UseQueryOptions<CodeWorkflowSourceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CodeWorkflowSourceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CodeWorkflowSourceQuery, TError, TData>(
      {
    queryKey: ['codeWorkflowSource', variables],
    queryFn: fetcher<CodeWorkflowSourceQuery, CodeWorkflowSourceQueryVariables>(CodeWorkflowSourceDocument, variables),
    ...options
  }
    )};

export const CreateCodeWorkflowDocument = new TypedDocumentString(`
    mutation createCodeWorkflow($workspaceId: ID!, $name: String!, $language: CodeWorkflowLanguage!, $description: String, $categoryId: ID, $tags: [String!]) {
  createCodeWorkflow(
    workspaceId: $workspaceId
    name: $name
    language: $language
    description: $description
    categoryId: $categoryId
    tags: $tags
  )
}
    `);

export const useCreateCodeWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateCodeWorkflowMutation, TError, CreateCodeWorkflowMutationVariables, TContext>) => {
    
    return useMutation<CreateCodeWorkflowMutation, TError, CreateCodeWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['createCodeWorkflow'],
    mutationFn: (variables?: CreateCodeWorkflowMutationVariables) => fetcher<CreateCodeWorkflowMutation, CreateCodeWorkflowMutationVariables>(CreateCodeWorkflowDocument, variables)(),
    ...options
  }
    )};

export const UpdateCodeWorkflowSourceDocument = new TypedDocumentString(`
    mutation updateCodeWorkflowSource($projectId: ID!, $content: String!) {
  updateCodeWorkflowSource(projectId: $projectId, content: $content)
}
    `);

export const useUpdateCodeWorkflowSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateCodeWorkflowSourceMutation, TError, UpdateCodeWorkflowSourceMutationVariables, TContext>) => {
    
    return useMutation<UpdateCodeWorkflowSourceMutation, TError, UpdateCodeWorkflowSourceMutationVariables, TContext>(
      {
    mutationKey: ['updateCodeWorkflowSource'],
    mutationFn: (variables?: UpdateCodeWorkflowSourceMutationVariables) => fetcher<UpdateCodeWorkflowSourceMutation, UpdateCodeWorkflowSourceMutationVariables>(UpdateCodeWorkflowSourceDocument, variables)(),
    ...options
  }
    )};

export const AutomationWorkflowProjectCategoriesDocument = new TypedDocumentString(`
    query automationWorkflowProjectCategories {
  automationWorkflowProjectCategories {
    id
    name
  }
}
    `);

export const useAutomationWorkflowProjectCategoriesQuery = <
      TData = AutomationWorkflowProjectCategoriesQuery,
      TError = unknown
    >(
      variables?: AutomationWorkflowProjectCategoriesQueryVariables,
      options?: Omit<UseQueryOptions<AutomationWorkflowProjectCategoriesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AutomationWorkflowProjectCategoriesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AutomationWorkflowProjectCategoriesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['automationWorkflowProjectCategories'] : ['automationWorkflowProjectCategories', variables],
    queryFn: fetcher<AutomationWorkflowProjectCategoriesQuery, AutomationWorkflowProjectCategoriesQueryVariables>(AutomationWorkflowProjectCategoriesDocument, variables),
    ...options
  }
    )};

export const AutomationWorkflowProjectTagsDocument = new TypedDocumentString(`
    query automationWorkflowProjectTags {
  automationWorkflowProjectTags {
    id
    name
  }
}
    `);

export const useAutomationWorkflowProjectTagsQuery = <
      TData = AutomationWorkflowProjectTagsQuery,
      TError = unknown
    >(
      variables?: AutomationWorkflowProjectTagsQueryVariables,
      options?: Omit<UseQueryOptions<AutomationWorkflowProjectTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AutomationWorkflowProjectTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AutomationWorkflowProjectTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['automationWorkflowProjectTags'] : ['automationWorkflowProjectTags', variables],
    queryFn: fetcher<AutomationWorkflowProjectTagsQuery, AutomationWorkflowProjectTagsQueryVariables>(AutomationWorkflowProjectTagsDocument, variables),
    ...options
  }
    )};

export const AutomationWorkflowProjectVersionsDocument = new TypedDocumentString(`
    query automationWorkflowProjectVersions($id: ID!) {
  automationWorkflowProjectVersions(id: $id) {
    version
    status
    publishedDate
  }
}
    `);

export const useAutomationWorkflowProjectVersionsQuery = <
      TData = AutomationWorkflowProjectVersionsQuery,
      TError = unknown
    >(
      variables: AutomationWorkflowProjectVersionsQueryVariables,
      options?: Omit<UseQueryOptions<AutomationWorkflowProjectVersionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AutomationWorkflowProjectVersionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AutomationWorkflowProjectVersionsQuery, TError, TData>(
      {
    queryKey: ['automationWorkflowProjectVersions', variables],
    queryFn: fetcher<AutomationWorkflowProjectVersionsQuery, AutomationWorkflowProjectVersionsQueryVariables>(AutomationWorkflowProjectVersionsDocument, variables),
    ...options
  }
    )};

export const AutomationWorkflowProjectsDocument = new TypedDocumentString(`
    query automationWorkflowProjects {
  automationWorkflowProjects {
    id
    name
    description
    categoryId
    tagIds
    published
    version
    lastPublishedVersion
    permissionExpression
    codeWorkflowProject
    workflowTemplates {
      workflowUuid
      label
      description
      permissionExpression
      lastModifiedDate
      triggers {
        name
        title
        icon
      }
      components {
        name
        title
        icon
      }
    }
  }
}
    `);

export const useAutomationWorkflowProjectsQuery = <
      TData = AutomationWorkflowProjectsQuery,
      TError = unknown
    >(
      variables?: AutomationWorkflowProjectsQueryVariables,
      options?: Omit<UseQueryOptions<AutomationWorkflowProjectsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AutomationWorkflowProjectsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AutomationWorkflowProjectsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['automationWorkflowProjects'] : ['automationWorkflowProjects', variables],
    queryFn: fetcher<AutomationWorkflowProjectsQuery, AutomationWorkflowProjectsQueryVariables>(AutomationWorkflowProjectsDocument, variables),
    ...options
  }
    )};

export const CreateAutomationWorkflowProjectDocument = new TypedDocumentString(`
    mutation createAutomationWorkflowProject($name: String!, $description: String, $category: String, $tags: [String!], $permissionExpression: String) {
  createAutomationWorkflowProject(
    name: $name
    description: $description
    category: $category
    tags: $tags
    permissionExpression: $permissionExpression
  )
}
    `);

export const useCreateAutomationWorkflowProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAutomationWorkflowProjectMutation, TError, CreateAutomationWorkflowProjectMutationVariables, TContext>) => {
    
    return useMutation<CreateAutomationWorkflowProjectMutation, TError, CreateAutomationWorkflowProjectMutationVariables, TContext>(
      {
    mutationKey: ['createAutomationWorkflowProject'],
    mutationFn: (variables?: CreateAutomationWorkflowProjectMutationVariables) => fetcher<CreateAutomationWorkflowProjectMutation, CreateAutomationWorkflowProjectMutationVariables>(CreateAutomationWorkflowProjectDocument, variables)(),
    ...options
  }
    )};

export const UpdateAutomationWorkflowProjectDocument = new TypedDocumentString(`
    mutation updateAutomationWorkflowProject($id: ID!, $name: String!, $description: String, $category: String, $tags: [String!], $permissionExpression: String) {
  updateAutomationWorkflowProject(
    id: $id
    name: $name
    description: $description
    category: $category
    tags: $tags
    permissionExpression: $permissionExpression
  )
}
    `);

export const useUpdateAutomationWorkflowProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAutomationWorkflowProjectMutation, TError, UpdateAutomationWorkflowProjectMutationVariables, TContext>) => {
    
    return useMutation<UpdateAutomationWorkflowProjectMutation, TError, UpdateAutomationWorkflowProjectMutationVariables, TContext>(
      {
    mutationKey: ['updateAutomationWorkflowProject'],
    mutationFn: (variables?: UpdateAutomationWorkflowProjectMutationVariables) => fetcher<UpdateAutomationWorkflowProjectMutation, UpdateAutomationWorkflowProjectMutationVariables>(UpdateAutomationWorkflowProjectDocument, variables)(),
    ...options
  }
    )};

export const DeleteAutomationWorkflowProjectDocument = new TypedDocumentString(`
    mutation deleteAutomationWorkflowProject($id: ID!) {
  deleteAutomationWorkflowProject(id: $id)
}
    `);

export const useDeleteAutomationWorkflowProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAutomationWorkflowProjectMutation, TError, DeleteAutomationWorkflowProjectMutationVariables, TContext>) => {
    
    return useMutation<DeleteAutomationWorkflowProjectMutation, TError, DeleteAutomationWorkflowProjectMutationVariables, TContext>(
      {
    mutationKey: ['deleteAutomationWorkflowProject'],
    mutationFn: (variables?: DeleteAutomationWorkflowProjectMutationVariables) => fetcher<DeleteAutomationWorkflowProjectMutation, DeleteAutomationWorkflowProjectMutationVariables>(DeleteAutomationWorkflowProjectDocument, variables)(),
    ...options
  }
    )};

export const CreateAutomationWorkflowProjectWorkflowDocument = new TypedDocumentString(`
    mutation createAutomationWorkflowProjectWorkflow($projectId: ID!, $definition: String, $permissionExpression: String) {
  createAutomationWorkflowProjectWorkflow(
    projectId: $projectId
    definition: $definition
    permissionExpression: $permissionExpression
  )
}
    `);

export const useCreateAutomationWorkflowProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateAutomationWorkflowProjectWorkflowMutation, TError, CreateAutomationWorkflowProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<CreateAutomationWorkflowProjectWorkflowMutation, TError, CreateAutomationWorkflowProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['createAutomationWorkflowProjectWorkflow'],
    mutationFn: (variables?: CreateAutomationWorkflowProjectWorkflowMutationVariables) => fetcher<CreateAutomationWorkflowProjectWorkflowMutation, CreateAutomationWorkflowProjectWorkflowMutationVariables>(CreateAutomationWorkflowProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const UpdateAutomationWorkflowProjectWorkflowDocument = new TypedDocumentString(`
    mutation updateAutomationWorkflowProjectWorkflow($workflowUuid: ID!, $label: String!, $description: String) {
  updateAutomationWorkflowProjectWorkflow(
    workflowUuid: $workflowUuid
    label: $label
    description: $description
  )
}
    `);

export const useUpdateAutomationWorkflowProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAutomationWorkflowProjectWorkflowMutation, TError, UpdateAutomationWorkflowProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<UpdateAutomationWorkflowProjectWorkflowMutation, TError, UpdateAutomationWorkflowProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['updateAutomationWorkflowProjectWorkflow'],
    mutationFn: (variables?: UpdateAutomationWorkflowProjectWorkflowMutationVariables) => fetcher<UpdateAutomationWorkflowProjectWorkflowMutation, UpdateAutomationWorkflowProjectWorkflowMutationVariables>(UpdateAutomationWorkflowProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const UpdateAutomationWorkflowProjectWorkflowPermissionExpressionDocument = new TypedDocumentString(`
    mutation updateAutomationWorkflowProjectWorkflowPermissionExpression($workflowUuid: ID!, $permissionExpression: String) {
  updateAutomationWorkflowProjectWorkflowPermissionExpression(
    workflowUuid: $workflowUuid
    permissionExpression: $permissionExpression
  )
}
    `);

export const useUpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation, TError, UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutationVariables, TContext>) => {
    
    return useMutation<UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation, TError, UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutationVariables, TContext>(
      {
    mutationKey: ['updateAutomationWorkflowProjectWorkflowPermissionExpression'],
    mutationFn: (variables?: UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutationVariables) => fetcher<UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutation, UpdateAutomationWorkflowProjectWorkflowPermissionExpressionMutationVariables>(UpdateAutomationWorkflowProjectWorkflowPermissionExpressionDocument, variables)(),
    ...options
  }
    )};

export const DeleteAutomationWorkflowProjectWorkflowDocument = new TypedDocumentString(`
    mutation deleteAutomationWorkflowProjectWorkflow($workflowUuid: ID!) {
  deleteAutomationWorkflowProjectWorkflow(workflowUuid: $workflowUuid)
}
    `);

export const useDeleteAutomationWorkflowProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteAutomationWorkflowProjectWorkflowMutation, TError, DeleteAutomationWorkflowProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DeleteAutomationWorkflowProjectWorkflowMutation, TError, DeleteAutomationWorkflowProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['deleteAutomationWorkflowProjectWorkflow'],
    mutationFn: (variables?: DeleteAutomationWorkflowProjectWorkflowMutationVariables) => fetcher<DeleteAutomationWorkflowProjectWorkflowMutation, DeleteAutomationWorkflowProjectWorkflowMutationVariables>(DeleteAutomationWorkflowProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const PublishAutomationWorkflowProjectDocument = new TypedDocumentString(`
    mutation publishAutomationWorkflowProject($id: ID!) {
  publishAutomationWorkflowProject(id: $id)
}
    `);

export const usePublishAutomationWorkflowProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<PublishAutomationWorkflowProjectMutation, TError, PublishAutomationWorkflowProjectMutationVariables, TContext>) => {
    
    return useMutation<PublishAutomationWorkflowProjectMutation, TError, PublishAutomationWorkflowProjectMutationVariables, TContext>(
      {
    mutationKey: ['publishAutomationWorkflowProject'],
    mutationFn: (variables?: PublishAutomationWorkflowProjectMutationVariables) => fetcher<PublishAutomationWorkflowProjectMutation, PublishAutomationWorkflowProjectMutationVariables>(PublishAutomationWorkflowProjectDocument, variables)(),
    ...options
  }
    )};

export const ConnectedUserCodeWorkflowReferencesDocument = new TypedDocumentString(`
    query connectedUserCodeWorkflowReferences($catalogWorkflowUuids: [ID!]!) {
  connectedUserCodeWorkflowReferences(catalogWorkflowUuids: $catalogWorkflowUuids) {
    catalogWorkflowUuid
    externalUserId
    environment
    enabled
    dangling
    danglingReason
  }
}
    `);

export const useConnectedUserCodeWorkflowReferencesQuery = <
      TData = ConnectedUserCodeWorkflowReferencesQuery,
      TError = unknown
    >(
      variables: ConnectedUserCodeWorkflowReferencesQueryVariables,
      options?: Omit<UseQueryOptions<ConnectedUserCodeWorkflowReferencesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ConnectedUserCodeWorkflowReferencesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ConnectedUserCodeWorkflowReferencesQuery, TError, TData>(
      {
    queryKey: ['connectedUserCodeWorkflowReferences', variables],
    queryFn: fetcher<ConnectedUserCodeWorkflowReferencesQuery, ConnectedUserCodeWorkflowReferencesQueryVariables>(ConnectedUserCodeWorkflowReferencesDocument, variables),
    ...options
  }
    )};

export const ConnectedUserMcpServersDocument = new TypedDocumentString(`
    query connectedUserMcpServers($connectedUserId: ID!) {
  connectedUserMcpServers(connectedUserId: $connectedUserId) {
    id
    name
    enabled
    environmentId
    lastModifiedDate
    tools {
      id
      componentName
      componentVersion
      integrationInstanceId
      name
      enabled
    }
  }
}
    `);

export const useConnectedUserMcpServersQuery = <
      TData = ConnectedUserMcpServersQuery,
      TError = unknown
    >(
      variables: ConnectedUserMcpServersQueryVariables,
      options?: Omit<UseQueryOptions<ConnectedUserMcpServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ConnectedUserMcpServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ConnectedUserMcpServersQuery, TError, TData>(
      {
    queryKey: ['connectedUserMcpServers', variables],
    queryFn: fetcher<ConnectedUserMcpServersQuery, ConnectedUserMcpServersQueryVariables>(ConnectedUserMcpServersDocument, variables),
    ...options
  }
    )};

export const ConnectedUserProjectsDocument = new TypedDocumentString(`
    query connectedUserProjects($connectedUserId: ID, $environmentId: ID) {
  connectedUserProjects(
    connectedUserId: $connectedUserId
    environmentId: $environmentId
  ) {
    id
    connectedUser {
      id
      environmentId
      externalId
    }
    connectedUserProjectWorkflows {
      id
      connectedUserId
      enabled
      lastExecutionDate
      projectId
      workflowUuid
      workflowVersion
      workflow {
        id
        label
        triggers {
          name
          type
          parameters
        }
      }
    }
    environmentId
    lastExecutionDate
    projectId
    projectVersion
  }
}
    `);

export const useConnectedUserProjectsQuery = <
      TData = ConnectedUserProjectsQuery,
      TError = unknown
    >(
      variables?: ConnectedUserProjectsQueryVariables,
      options?: Omit<UseQueryOptions<ConnectedUserProjectsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ConnectedUserProjectsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ConnectedUserProjectsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['connectedUserProjects'] : ['connectedUserProjects', variables],
    queryFn: fetcher<ConnectedUserProjectsQuery, ConnectedUserProjectsQueryVariables>(ConnectedUserProjectsDocument, variables),
    ...options
  }
    )};

export const CreateEmbeddedMcpServerDocument = new TypedDocumentString(`
    mutation createEmbeddedMcpServer($input: CreateEmbeddedMcpServerInput!) {
  createEmbeddedMcpServer(input: $input) {
    enabled
    environmentId
    id
    name
    type
  }
}
    `);

export const useCreateEmbeddedMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateEmbeddedMcpServerMutation, TError, CreateEmbeddedMcpServerMutationVariables, TContext>) => {
    
    return useMutation<CreateEmbeddedMcpServerMutation, TError, CreateEmbeddedMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['createEmbeddedMcpServer'],
    mutationFn: (variables?: CreateEmbeddedMcpServerMutationVariables) => fetcher<CreateEmbeddedMcpServerMutation, CreateEmbeddedMcpServerMutationVariables>(CreateEmbeddedMcpServerDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpIntegrationInstanceConfigurationDocument = new TypedDocumentString(`
    mutation createMcpIntegrationInstanceConfiguration($input: CreateMcpIntegrationInstanceConfigurationInput!) {
  createMcpIntegrationInstanceConfiguration(input: $input) {
    id
    integrationInstanceConfigurationId
    mcpServerId
  }
}
    `);

export const useCreateMcpIntegrationInstanceConfigurationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpIntegrationInstanceConfigurationMutation, TError, CreateMcpIntegrationInstanceConfigurationMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpIntegrationInstanceConfigurationMutation, TError, CreateMcpIntegrationInstanceConfigurationMutationVariables, TContext>(
      {
    mutationKey: ['createMcpIntegrationInstanceConfiguration'],
    mutationFn: (variables?: CreateMcpIntegrationInstanceConfigurationMutationVariables) => fetcher<CreateMcpIntegrationInstanceConfigurationMutation, CreateMcpIntegrationInstanceConfigurationMutationVariables>(CreateMcpIntegrationInstanceConfigurationDocument, variables)(),
    ...options
  }
    )};

export const DeleteConnectedUserMcpServerDocument = new TypedDocumentString(`
    mutation deleteConnectedUserMcpServer($connectedUserId: ID!, $mcpServerId: ID!) {
  deleteConnectedUserMcpServer(
    connectedUserId: $connectedUserId
    mcpServerId: $mcpServerId
  )
}
    `);

export const useDeleteConnectedUserMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteConnectedUserMcpServerMutation, TError, DeleteConnectedUserMcpServerMutationVariables, TContext>) => {
    
    return useMutation<DeleteConnectedUserMcpServerMutation, TError, DeleteConnectedUserMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['deleteConnectedUserMcpServer'],
    mutationFn: (variables?: DeleteConnectedUserMcpServerMutationVariables) => fetcher<DeleteConnectedUserMcpServerMutation, DeleteConnectedUserMcpServerMutationVariables>(DeleteConnectedUserMcpServerDocument, variables)(),
    ...options
  }
    )};

export const DeleteConnectedUserProjectWorkflowDocument = new TypedDocumentString(`
    mutation deleteConnectedUserProjectWorkflow($id: ID!) {
  deleteConnectedUserProjectWorkflow(id: $id)
}
    `);

export const useDeleteConnectedUserProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteConnectedUserProjectWorkflowMutation, TError, DeleteConnectedUserProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DeleteConnectedUserProjectWorkflowMutation, TError, DeleteConnectedUserProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['deleteConnectedUserProjectWorkflow'],
    mutationFn: (variables?: DeleteConnectedUserProjectWorkflowMutationVariables) => fetcher<DeleteConnectedUserProjectWorkflowMutation, DeleteConnectedUserProjectWorkflowMutationVariables>(DeleteConnectedUserProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const DeleteEmbeddedMcpServerDocument = new TypedDocumentString(`
    mutation deleteEmbeddedMcpServer($mcpServerId: ID!) {
  deleteEmbeddedMcpServer(mcpServerId: $mcpServerId)
}
    `);

export const useDeleteEmbeddedMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteEmbeddedMcpServerMutation, TError, DeleteEmbeddedMcpServerMutationVariables, TContext>) => {
    
    return useMutation<DeleteEmbeddedMcpServerMutation, TError, DeleteEmbeddedMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['deleteEmbeddedMcpServer'],
    mutationFn: (variables?: DeleteEmbeddedMcpServerMutationVariables) => fetcher<DeleteEmbeddedMcpServerMutation, DeleteEmbeddedMcpServerMutationVariables>(DeleteEmbeddedMcpServerDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpIntegrationInstanceConfigurationDocument = new TypedDocumentString(`
    mutation deleteMcpIntegrationInstanceConfiguration($id: ID!) {
  deleteMcpIntegrationInstanceConfiguration(id: $id)
}
    `);

export const useDeleteMcpIntegrationInstanceConfigurationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpIntegrationInstanceConfigurationMutation, TError, DeleteMcpIntegrationInstanceConfigurationMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpIntegrationInstanceConfigurationMutation, TError, DeleteMcpIntegrationInstanceConfigurationMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpIntegrationInstanceConfiguration'],
    mutationFn: (variables?: DeleteMcpIntegrationInstanceConfigurationMutationVariables) => fetcher<DeleteMcpIntegrationInstanceConfigurationMutation, DeleteMcpIntegrationInstanceConfigurationMutationVariables>(DeleteMcpIntegrationInstanceConfigurationDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpIntegrationInstanceConfigurationWorkflowDocument = new TypedDocumentString(`
    mutation deleteMcpIntegrationInstanceConfigurationWorkflow($id: ID!) {
  deleteMcpIntegrationInstanceConfigurationWorkflow(id: $id)
}
    `);

export const useDeleteMcpIntegrationInstanceConfigurationWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpIntegrationInstanceConfigurationWorkflowMutation, TError, DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpIntegrationInstanceConfigurationWorkflowMutation, TError, DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpIntegrationInstanceConfigurationWorkflow'],
    mutationFn: (variables?: DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables) => fetcher<DeleteMcpIntegrationInstanceConfigurationWorkflowMutation, DeleteMcpIntegrationInstanceConfigurationWorkflowMutationVariables>(DeleteMcpIntegrationInstanceConfigurationWorkflowDocument, variables)(),
    ...options
  }
    )};

export const DuplicateAutomationWorkflowProjectDocument = new TypedDocumentString(`
    mutation duplicateAutomationWorkflowProject($id: ID!) {
  duplicateAutomationWorkflowProject(id: $id)
}
    `);

export const useDuplicateAutomationWorkflowProjectMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DuplicateAutomationWorkflowProjectMutation, TError, DuplicateAutomationWorkflowProjectMutationVariables, TContext>) => {
    
    return useMutation<DuplicateAutomationWorkflowProjectMutation, TError, DuplicateAutomationWorkflowProjectMutationVariables, TContext>(
      {
    mutationKey: ['duplicateAutomationWorkflowProject'],
    mutationFn: (variables?: DuplicateAutomationWorkflowProjectMutationVariables) => fetcher<DuplicateAutomationWorkflowProjectMutation, DuplicateAutomationWorkflowProjectMutationVariables>(DuplicateAutomationWorkflowProjectDocument, variables)(),
    ...options
  }
    )};

export const DuplicateAutomationWorkflowProjectWorkflowDocument = new TypedDocumentString(`
    mutation duplicateAutomationWorkflowProjectWorkflow($workflowUuid: ID!) {
  duplicateAutomationWorkflowProjectWorkflow(workflowUuid: $workflowUuid)
}
    `);

export const useDuplicateAutomationWorkflowProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DuplicateAutomationWorkflowProjectWorkflowMutation, TError, DuplicateAutomationWorkflowProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<DuplicateAutomationWorkflowProjectWorkflowMutation, TError, DuplicateAutomationWorkflowProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['duplicateAutomationWorkflowProjectWorkflow'],
    mutationFn: (variables?: DuplicateAutomationWorkflowProjectWorkflowMutationVariables) => fetcher<DuplicateAutomationWorkflowProjectWorkflowMutation, DuplicateAutomationWorkflowProjectWorkflowMutationVariables>(DuplicateAutomationWorkflowProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const EmbeddedMcpServerTagsDocument = new TypedDocumentString(`
    query embeddedMcpServerTags {
  embeddedMcpServerTags {
    id
    name
  }
}
    `);

export const useEmbeddedMcpServerTagsQuery = <
      TData = EmbeddedMcpServerTagsQuery,
      TError = unknown
    >(
      variables?: EmbeddedMcpServerTagsQueryVariables,
      options?: Omit<UseQueryOptions<EmbeddedMcpServerTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EmbeddedMcpServerTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EmbeddedMcpServerTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['embeddedMcpServerTags'] : ['embeddedMcpServerTags', variables],
    queryFn: fetcher<EmbeddedMcpServerTagsQuery, EmbeddedMcpServerTagsQueryVariables>(EmbeddedMcpServerTagsDocument, variables),
    ...options
  }
    )};

export const EmbeddedMcpServersDocument = new TypedDocumentString(`
    query embeddedMcpServers {
  embeddedMcpServers {
    id
    enabled
    enforceToolAuthorization
    authenticationRequired
    environmentId
    lastModifiedDate
    mcpComponents {
      componentName
      componentVersion
      connectionId
      id
      lastModifiedDate
      mcpServerId
      title
      mcpTools {
        id
        enabled
        mcpComponentId
        name
        title
        parameters
      }
    }
    name
    tags {
      id
      name
    }
    type
    url
  }
}
    `);

export const useEmbeddedMcpServersQuery = <
      TData = EmbeddedMcpServersQuery,
      TError = unknown
    >(
      variables?: EmbeddedMcpServersQueryVariables,
      options?: Omit<UseQueryOptions<EmbeddedMcpServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EmbeddedMcpServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EmbeddedMcpServersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['embeddedMcpServers'] : ['embeddedMcpServers', variables],
    queryFn: fetcher<EmbeddedMcpServersQuery, EmbeddedMcpServersQueryVariables>(EmbeddedMcpServersDocument, variables),
    ...options
  }
    )};

export const EnableConnectedUserMcpServerDocument = new TypedDocumentString(`
    mutation enableConnectedUserMcpServer($connectedUserId: ID!, $mcpServerId: ID!, $enable: Boolean!) {
  enableConnectedUserMcpServer(
    connectedUserId: $connectedUserId
    mcpServerId: $mcpServerId
    enable: $enable
  )
}
    `);

export const useEnableConnectedUserMcpServerMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableConnectedUserMcpServerMutation, TError, EnableConnectedUserMcpServerMutationVariables, TContext>) => {
    
    return useMutation<EnableConnectedUserMcpServerMutation, TError, EnableConnectedUserMcpServerMutationVariables, TContext>(
      {
    mutationKey: ['enableConnectedUserMcpServer'],
    mutationFn: (variables?: EnableConnectedUserMcpServerMutationVariables) => fetcher<EnableConnectedUserMcpServerMutation, EnableConnectedUserMcpServerMutationVariables>(EnableConnectedUserMcpServerDocument, variables)(),
    ...options
  }
    )};

export const EnableConnectedUserMcpToolDocument = new TypedDocumentString(`
    mutation enableConnectedUserMcpTool($id: ID!, $enable: Boolean!) {
  enableConnectedUserMcpTool(id: $id, enable: $enable)
}
    `);

export const useEnableConnectedUserMcpToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableConnectedUserMcpToolMutation, TError, EnableConnectedUserMcpToolMutationVariables, TContext>) => {
    
    return useMutation<EnableConnectedUserMcpToolMutation, TError, EnableConnectedUserMcpToolMutationVariables, TContext>(
      {
    mutationKey: ['enableConnectedUserMcpTool'],
    mutationFn: (variables?: EnableConnectedUserMcpToolMutationVariables) => fetcher<EnableConnectedUserMcpToolMutation, EnableConnectedUserMcpToolMutationVariables>(EnableConnectedUserMcpToolDocument, variables)(),
    ...options
  }
    )};

export const EnableConnectedUserProjectWorkflowDocument = new TypedDocumentString(`
    mutation enableConnectedUserProjectWorkflow($id: ID!, $enable: Boolean!) {
  enableConnectedUserProjectWorkflow(id: $id, enable: $enable)
}
    `);

export const useEnableConnectedUserProjectWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableConnectedUserProjectWorkflowMutation, TError, EnableConnectedUserProjectWorkflowMutationVariables, TContext>) => {
    
    return useMutation<EnableConnectedUserProjectWorkflowMutation, TError, EnableConnectedUserProjectWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['enableConnectedUserProjectWorkflow'],
    mutationFn: (variables?: EnableConnectedUserProjectWorkflowMutationVariables) => fetcher<EnableConnectedUserProjectWorkflowMutation, EnableConnectedUserProjectWorkflowMutationVariables>(EnableConnectedUserProjectWorkflowDocument, variables)(),
    ...options
  }
    )};

export const IntegrationWorkflowsDocument = new TypedDocumentString(`
    query integrationWorkflows {
  integrationWorkflows {
    id
    label
    description
    integrationWorkflowId
    workflowUuid
    workflowTaskComponentNames
    workflowTriggerComponentNames
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useIntegrationWorkflowsQuery = <
      TData = IntegrationWorkflowsQuery,
      TError = unknown
    >(
      variables?: IntegrationWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<IntegrationWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IntegrationWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IntegrationWorkflowsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['integrationWorkflows'] : ['integrationWorkflows', variables],
    queryFn: fetcher<IntegrationWorkflowsQuery, IntegrationWorkflowsQueryVariables>(IntegrationWorkflowsDocument, variables),
    ...options
  }
    )};

export const IntegrationWorkflowsByIntegrationIdDocument = new TypedDocumentString(`
    query integrationWorkflowsByIntegrationId($integrationId: ID!) {
  integrationWorkflowsByIntegrationId(integrationId: $integrationId) {
    id
    label
    description
    integrationWorkflowId
    workflowUuid
    permissionExpression
    workflowTaskComponentNames
    workflowTriggerComponentNames
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useIntegrationWorkflowsByIntegrationIdQuery = <
      TData = IntegrationWorkflowsByIntegrationIdQuery,
      TError = unknown
    >(
      variables: IntegrationWorkflowsByIntegrationIdQueryVariables,
      options?: Omit<UseQueryOptions<IntegrationWorkflowsByIntegrationIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IntegrationWorkflowsByIntegrationIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IntegrationWorkflowsByIntegrationIdQuery, TError, TData>(
      {
    queryKey: ['integrationWorkflowsByIntegrationId', variables],
    queryFn: fetcher<IntegrationWorkflowsByIntegrationIdQuery, IntegrationWorkflowsByIntegrationIdQueryVariables>(IntegrationWorkflowsByIntegrationIdDocument, variables),
    ...options
  }
    )};

export const McpComponentDefinitionsDocument = new TypedDocumentString(`
    query mcpComponentDefinitions {
  mcpComponentDefinitions {
    clusterElementsCount
    description
    icon
    name
    title
    version
  }
}
    `);

export const useMcpComponentDefinitionsQuery = <
      TData = McpComponentDefinitionsQuery,
      TError = unknown
    >(
      variables?: McpComponentDefinitionsQueryVariables,
      options?: Omit<UseQueryOptions<McpComponentDefinitionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpComponentDefinitionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpComponentDefinitionsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpComponentDefinitions'] : ['mcpComponentDefinitions', variables],
    queryFn: fetcher<McpComponentDefinitionsQuery, McpComponentDefinitionsQueryVariables>(McpComponentDefinitionsDocument, variables),
    ...options
  }
    )};

export const McpIntegrationInstanceConfigurationWorkflowPropertiesDocument = new TypedDocumentString(`
    query mcpIntegrationInstanceConfigurationWorkflowProperties($mcpIntegrationInstanceConfigurationWorkflowId: ID!) {
  mcpIntegrationInstanceConfigurationWorkflowProperties(
    mcpIntegrationInstanceConfigurationWorkflowId: $mcpIntegrationInstanceConfigurationWorkflowId
  ) {
    advancedOption
    description
    displayCondition
    expressionEnabled
    hidden
    name
    required
    type
    ... on StringProperty {
      controlType
      defaultValue
      label
      placeholder
    }
    ... on IntegerProperty {
      controlType
      integerDefaultValue: defaultValue
      label
      placeholder
    }
    ... on NumberProperty {
      controlType
      label
      numberDefaultValue: defaultValue
      placeholder
    }
    ... on BooleanProperty {
      booleanDefaultValue: defaultValue
      controlType
      label
      placeholder
    }
    ... on ArrayProperty {
      arrayDefaultValue: defaultValue
      controlType
      label
      placeholder
    }
    ... on ObjectProperty {
      controlType
      label
      objectDefaultValue: defaultValue
      placeholder
    }
  }
}
    `);

export const useMcpIntegrationInstanceConfigurationWorkflowPropertiesQuery = <
      TData = McpIntegrationInstanceConfigurationWorkflowPropertiesQuery,
      TError = unknown
    >(
      variables: McpIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables,
      options?: Omit<UseQueryOptions<McpIntegrationInstanceConfigurationWorkflowPropertiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpIntegrationInstanceConfigurationWorkflowPropertiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpIntegrationInstanceConfigurationWorkflowPropertiesQuery, TError, TData>(
      {
    queryKey: ['mcpIntegrationInstanceConfigurationWorkflowProperties', variables],
    queryFn: fetcher<McpIntegrationInstanceConfigurationWorkflowPropertiesQuery, McpIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables>(McpIntegrationInstanceConfigurationWorkflowPropertiesDocument, variables),
    ...options
  }
    )};

export const McpIntegrationInstanceConfigurationsDocument = new TypedDocumentString(`
    query mcpIntegrationInstanceConfigurations {
  mcpIntegrationInstanceConfigurations {
    id
    integrationInstanceConfigurationId
    mcpServerId
    integration {
      id
      name
    }
    mcpIntegrationInstanceConfigurationWorkflows {
      integrationInstanceConfigurationWorkflow {
        workflowId
      }
    }
  }
}
    `);

export const useMcpIntegrationInstanceConfigurationsQuery = <
      TData = McpIntegrationInstanceConfigurationsQuery,
      TError = unknown
    >(
      variables?: McpIntegrationInstanceConfigurationsQueryVariables,
      options?: Omit<UseQueryOptions<McpIntegrationInstanceConfigurationsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpIntegrationInstanceConfigurationsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpIntegrationInstanceConfigurationsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpIntegrationInstanceConfigurations'] : ['mcpIntegrationInstanceConfigurations', variables],
    queryFn: fetcher<McpIntegrationInstanceConfigurationsQuery, McpIntegrationInstanceConfigurationsQueryVariables>(McpIntegrationInstanceConfigurationsDocument, variables),
    ...options
  }
    )};

export const McpIntegrationInstanceConfigurationsByServerIdDocument = new TypedDocumentString(`
    query mcpIntegrationInstanceConfigurationsByServerId($mcpServerId: ID) {
  mcpIntegrationInstanceConfigurationsByServerId(mcpServerId: $mcpServerId) {
    id
    integration {
      componentName
      id
      name
    }
    integrationInstanceConfigurationId
    integrationInstanceConfigurationName
    integrationVersion
    lastModifiedDate
    mcpIntegrationInstanceConfigurationWorkflows {
      id
      integrationInstanceConfigurationWorkflowId
      integrationInstanceConfigurationWorkflow {
        id
        connections {
          connectionId
          workflowConnectionKey
          workflowNodeName
        }
        enabled
        inputs
        integrationInstanceConfigurationId
        version
        workflowId
      }
      mcpIntegrationInstanceConfigurationId
      parameters
      workflow {
        id
        label
      }
    }
    mcpServerId
  }
}
    `);

export const useMcpIntegrationInstanceConfigurationsByServerIdQuery = <
      TData = McpIntegrationInstanceConfigurationsByServerIdQuery,
      TError = unknown
    >(
      variables?: McpIntegrationInstanceConfigurationsByServerIdQueryVariables,
      options?: Omit<UseQueryOptions<McpIntegrationInstanceConfigurationsByServerIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpIntegrationInstanceConfigurationsByServerIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpIntegrationInstanceConfigurationsByServerIdQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpIntegrationInstanceConfigurationsByServerId'] : ['mcpIntegrationInstanceConfigurationsByServerId', variables],
    queryFn: fetcher<McpIntegrationInstanceConfigurationsByServerIdQuery, McpIntegrationInstanceConfigurationsByServerIdQueryVariables>(McpIntegrationInstanceConfigurationsByServerIdDocument, variables),
    ...options
  }
    )};

export const ToolEligibleIntegrationInstanceConfigurationWorkflowsDocument = new TypedDocumentString(`
    query toolEligibleIntegrationInstanceConfigurationWorkflows($integrationInstanceConfigurationId: ID!) {
  toolEligibleIntegrationInstanceConfigurationWorkflows(
    integrationInstanceConfigurationId: $integrationInstanceConfigurationId
  ) {
    id
    integrationWorkflowId
    label
  }
}
    `);

export const useToolEligibleIntegrationInstanceConfigurationWorkflowsQuery = <
      TData = ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery,
      TError = unknown
    >(
      variables: ToolEligibleIntegrationInstanceConfigurationWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery, TError, TData>(
      {
    queryKey: ['toolEligibleIntegrationInstanceConfigurationWorkflows', variables],
    queryFn: fetcher<ToolEligibleIntegrationInstanceConfigurationWorkflowsQuery, ToolEligibleIntegrationInstanceConfigurationWorkflowsQueryVariables>(ToolEligibleIntegrationInstanceConfigurationWorkflowsDocument, variables),
    ...options
  }
    )};

export const ToolEligibleIntegrationVersionWorkflowsDocument = new TypedDocumentString(`
    query toolEligibleIntegrationVersionWorkflows($integrationId: ID!, $integrationVersion: Int!) {
  toolEligibleIntegrationVersionWorkflows(
    integrationId: $integrationId
    integrationVersion: $integrationVersion
  ) {
    id
    integrationWorkflowId
    label
  }
}
    `);

export const useToolEligibleIntegrationVersionWorkflowsQuery = <
      TData = ToolEligibleIntegrationVersionWorkflowsQuery,
      TError = unknown
    >(
      variables: ToolEligibleIntegrationVersionWorkflowsQueryVariables,
      options?: Omit<UseQueryOptions<ToolEligibleIntegrationVersionWorkflowsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ToolEligibleIntegrationVersionWorkflowsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ToolEligibleIntegrationVersionWorkflowsQuery, TError, TData>(
      {
    queryKey: ['toolEligibleIntegrationVersionWorkflows', variables],
    queryFn: fetcher<ToolEligibleIntegrationVersionWorkflowsQuery, ToolEligibleIntegrationVersionWorkflowsQueryVariables>(ToolEligibleIntegrationVersionWorkflowsDocument, variables),
    ...options
  }
    )};

export const UpdateIntegrationWorkflowPermissionExpressionDocument = new TypedDocumentString(`
    mutation updateIntegrationWorkflowPermissionExpression($integrationWorkflowId: ID!, $permissionExpression: String) {
  updateIntegrationWorkflowPermissionExpression(
    integrationWorkflowId: $integrationWorkflowId
    permissionExpression: $permissionExpression
  ) {
    id
    permissionExpression
  }
}
    `);

export const useUpdateIntegrationWorkflowPermissionExpressionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateIntegrationWorkflowPermissionExpressionMutation, TError, UpdateIntegrationWorkflowPermissionExpressionMutationVariables, TContext>) => {
    
    return useMutation<UpdateIntegrationWorkflowPermissionExpressionMutation, TError, UpdateIntegrationWorkflowPermissionExpressionMutationVariables, TContext>(
      {
    mutationKey: ['updateIntegrationWorkflowPermissionExpression'],
    mutationFn: (variables?: UpdateIntegrationWorkflowPermissionExpressionMutationVariables) => fetcher<UpdateIntegrationWorkflowPermissionExpressionMutation, UpdateIntegrationWorkflowPermissionExpressionMutationVariables>(UpdateIntegrationWorkflowPermissionExpressionDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpIntegrationInstanceConfigurationDocument = new TypedDocumentString(`
    mutation updateMcpIntegrationInstanceConfiguration($id: ID!, $input: UpdateMcpIntegrationInstanceConfigurationInput!) {
  updateMcpIntegrationInstanceConfiguration(id: $id, input: $input) {
    id
    integrationInstanceConfigurationId
    mcpServerId
  }
}
    `);

export const useUpdateMcpIntegrationInstanceConfigurationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpIntegrationInstanceConfigurationMutation, TError, UpdateMcpIntegrationInstanceConfigurationMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpIntegrationInstanceConfigurationMutation, TError, UpdateMcpIntegrationInstanceConfigurationMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpIntegrationInstanceConfiguration'],
    mutationFn: (variables?: UpdateMcpIntegrationInstanceConfigurationMutationVariables) => fetcher<UpdateMcpIntegrationInstanceConfigurationMutation, UpdateMcpIntegrationInstanceConfigurationMutationVariables>(UpdateMcpIntegrationInstanceConfigurationDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpIntegrationInstanceConfigurationVersionDocument = new TypedDocumentString(`
    mutation updateMcpIntegrationInstanceConfigurationVersion($id: ID!, $input: UpdateMcpIntegrationInstanceConfigurationVersionInput!) {
  updateMcpIntegrationInstanceConfigurationVersion(id: $id, input: $input)
}
    `);

export const useUpdateMcpIntegrationInstanceConfigurationVersionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpIntegrationInstanceConfigurationVersionMutation, TError, UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpIntegrationInstanceConfigurationVersionMutation, TError, UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpIntegrationInstanceConfigurationVersion'],
    mutationFn: (variables?: UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables) => fetcher<UpdateMcpIntegrationInstanceConfigurationVersionMutation, UpdateMcpIntegrationInstanceConfigurationVersionMutationVariables>(UpdateMcpIntegrationInstanceConfigurationVersionDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpIntegrationInstanceConfigurationWorkflowDocument = new TypedDocumentString(`
    mutation updateMcpIntegrationInstanceConfigurationWorkflow($id: ID!, $input: McpIntegrationInstanceConfigurationWorkflowUpdateInput!) {
  updateMcpIntegrationInstanceConfigurationWorkflow(id: $id, input: $input) {
    id
    mcpIntegrationInstanceConfigurationId
    integrationInstanceConfigurationWorkflowId
    parameters
  }
}
    `);

export const useUpdateMcpIntegrationInstanceConfigurationWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpIntegrationInstanceConfigurationWorkflowMutation, TError, UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpIntegrationInstanceConfigurationWorkflowMutation, TError, UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpIntegrationInstanceConfigurationWorkflow'],
    mutationFn: (variables?: UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables) => fetcher<UpdateMcpIntegrationInstanceConfigurationWorkflowMutation, UpdateMcpIntegrationInstanceConfigurationWorkflowMutationVariables>(UpdateMcpIntegrationInstanceConfigurationWorkflowDocument, variables)(),
    ...options
  }
    )};

export const CreateIntegrationCodeWorkflowDocument = new TypedDocumentString(`
    mutation createIntegrationCodeWorkflow($componentName: String!, $language: CodeWorkflowLanguage!, $name: String, $description: String, $categoryId: ID, $tags: [String!], $permissionExpression: String) {
  createIntegrationCodeWorkflow(
    componentName: $componentName
    language: $language
    name: $name
    description: $description
    categoryId: $categoryId
    tags: $tags
    permissionExpression: $permissionExpression
  )
}
    `);

export const useCreateIntegrationCodeWorkflowMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateIntegrationCodeWorkflowMutation, TError, CreateIntegrationCodeWorkflowMutationVariables, TContext>) => {
    
    return useMutation<CreateIntegrationCodeWorkflowMutation, TError, CreateIntegrationCodeWorkflowMutationVariables, TContext>(
      {
    mutationKey: ['createIntegrationCodeWorkflow'],
    mutationFn: (variables?: CreateIntegrationCodeWorkflowMutationVariables) => fetcher<CreateIntegrationCodeWorkflowMutation, CreateIntegrationCodeWorkflowMutationVariables>(CreateIntegrationCodeWorkflowDocument, variables)(),
    ...options
  }
    )};

export const IntegrationCodeWorkflowSourceDocument = new TypedDocumentString(`
    query integrationCodeWorkflowSource($integrationId: ID!) {
  integrationCodeWorkflowSource(integrationId: $integrationId)
}
    `);

export const useIntegrationCodeWorkflowSourceQuery = <
      TData = IntegrationCodeWorkflowSourceQuery,
      TError = unknown
    >(
      variables: IntegrationCodeWorkflowSourceQueryVariables,
      options?: Omit<UseQueryOptions<IntegrationCodeWorkflowSourceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IntegrationCodeWorkflowSourceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IntegrationCodeWorkflowSourceQuery, TError, TData>(
      {
    queryKey: ['integrationCodeWorkflowSource', variables],
    queryFn: fetcher<IntegrationCodeWorkflowSourceQuery, IntegrationCodeWorkflowSourceQueryVariables>(IntegrationCodeWorkflowSourceDocument, variables),
    ...options
  }
    )};

export const UpdateIntegrationCodeWorkflowSourceDocument = new TypedDocumentString(`
    mutation updateIntegrationCodeWorkflowSource($integrationId: ID!, $content: String!) {
  updateIntegrationCodeWorkflowSource(
    integrationId: $integrationId
    content: $content
  )
}
    `);

export const useUpdateIntegrationCodeWorkflowSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateIntegrationCodeWorkflowSourceMutation, TError, UpdateIntegrationCodeWorkflowSourceMutationVariables, TContext>) => {
    
    return useMutation<UpdateIntegrationCodeWorkflowSourceMutation, TError, UpdateIntegrationCodeWorkflowSourceMutationVariables, TContext>(
      {
    mutationKey: ['updateIntegrationCodeWorkflowSource'],
    mutationFn: (variables?: UpdateIntegrationCodeWorkflowSourceMutationVariables) => fetcher<UpdateIntegrationCodeWorkflowSourceMutation, UpdateIntegrationCodeWorkflowSourceMutationVariables>(UpdateIntegrationCodeWorkflowSourceDocument, variables)(),
    ...options
  }
    )};

export const AiGuardrailsWorkspaceSettingsDocument = new TypedDocumentString(`
    query aiGuardrailsWorkspaceSettings($workspaceId: ID) {
  aiGuardrailsWorkspaceSettings(workspaceId: $workspaceId) {
    blockedTerms
    blockingMode
    injectionDetectionEnabled
    moderationEnabled
    redactPii
    redactSecrets
    scanResponses
    workspaceId
  }
}
    `);

export const useAiGuardrailsWorkspaceSettingsQuery = <
      TData = AiGuardrailsWorkspaceSettingsQuery,
      TError = unknown
    >(
      variables?: AiGuardrailsWorkspaceSettingsQueryVariables,
      options?: Omit<UseQueryOptions<AiGuardrailsWorkspaceSettingsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiGuardrailsWorkspaceSettingsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiGuardrailsWorkspaceSettingsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['aiGuardrailsWorkspaceSettings'] : ['aiGuardrailsWorkspaceSettings', variables],
    queryFn: fetcher<AiGuardrailsWorkspaceSettingsQuery, AiGuardrailsWorkspaceSettingsQueryVariables>(AiGuardrailsWorkspaceSettingsDocument, variables),
    ...options
  }
    )};

export const UpdateAiGuardrailsWorkspaceSettingsDocument = new TypedDocumentString(`
    mutation updateAiGuardrailsWorkspaceSettings($input: AiGuardrailsWorkspaceSettingsInput!) {
  updateAiGuardrailsWorkspaceSettings(input: $input) {
    blockedTerms
    blockingMode
    injectionDetectionEnabled
    moderationEnabled
    redactPii
    redactSecrets
    scanResponses
    workspaceId
  }
}
    `);

export const useUpdateAiGuardrailsWorkspaceSettingsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateAiGuardrailsWorkspaceSettingsMutation, TError, UpdateAiGuardrailsWorkspaceSettingsMutationVariables, TContext>) => {
    
    return useMutation<UpdateAiGuardrailsWorkspaceSettingsMutation, TError, UpdateAiGuardrailsWorkspaceSettingsMutationVariables, TContext>(
      {
    mutationKey: ['updateAiGuardrailsWorkspaceSettings'],
    mutationFn: (variables?: UpdateAiGuardrailsWorkspaceSettingsMutationVariables) => fetcher<UpdateAiGuardrailsWorkspaceSettingsMutation, UpdateAiGuardrailsWorkspaceSettingsMutationVariables>(UpdateAiGuardrailsWorkspaceSettingsDocument, variables)(),
    ...options
  }
    )};

export const AiDefaultModelDocument = new TypedDocumentString(`
    query aiDefaultModel($environment: ID!) {
  aiDefaultModel(environment: $environment) {
    provider
    model
  }
}
    `);

export const useAiDefaultModelQuery = <
      TData = AiDefaultModelQuery,
      TError = unknown
    >(
      variables: AiDefaultModelQueryVariables,
      options?: Omit<UseQueryOptions<AiDefaultModelQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiDefaultModelQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiDefaultModelQuery, TError, TData>(
      {
    queryKey: ['aiDefaultModel', variables],
    queryFn: fetcher<AiDefaultModelQuery, AiDefaultModelQueryVariables>(AiDefaultModelDocument, variables),
    ...options
  }
    )};

export const AiProviderCatalogDocument = new TypedDocumentString(`
    query aiProviderCatalog($environment: ID!) {
  aiProviderCatalog(environment: $environment) {
    key
    name
    icon
    enabled
    supportsModelById
    models {
      name
      label
    }
  }
}
    `);

export const useAiProviderCatalogQuery = <
      TData = AiProviderCatalogQuery,
      TError = unknown
    >(
      variables: AiProviderCatalogQueryVariables,
      options?: Omit<UseQueryOptions<AiProviderCatalogQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AiProviderCatalogQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AiProviderCatalogQuery, TError, TData>(
      {
    queryKey: ['aiProviderCatalog', variables],
    queryFn: fetcher<AiProviderCatalogQuery, AiProviderCatalogQueryVariables>(AiProviderCatalogDocument, variables),
    ...options
  }
    )};

export const ApiConnectorDocument = new TypedDocumentString(`
    query apiConnector($id: ID!) {
  apiConnector(id: $id) {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `);

export const useApiConnectorQuery = <
      TData = ApiConnectorQuery,
      TError = unknown
    >(
      variables: ApiConnectorQueryVariables,
      options?: Omit<UseQueryOptions<ApiConnectorQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApiConnectorQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApiConnectorQuery, TError, TData>(
      {
    queryKey: ['apiConnector', variables],
    queryFn: fetcher<ApiConnectorQuery, ApiConnectorQueryVariables>(ApiConnectorDocument, variables),
    ...options
  }
    )};

export const ApiConnectorsDocument = new TypedDocumentString(`
    query apiConnectors {
  apiConnectors {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `);

export const useApiConnectorsQuery = <
      TData = ApiConnectorsQuery,
      TError = unknown
    >(
      variables?: ApiConnectorsQueryVariables,
      options?: Omit<UseQueryOptions<ApiConnectorsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApiConnectorsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApiConnectorsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['apiConnectors'] : ['apiConnectors', variables],
    queryFn: fetcher<ApiConnectorsQuery, ApiConnectorsQueryVariables>(ApiConnectorsDocument, variables),
    ...options
  }
    )};

export const CancelGenerationJobDocument = new TypedDocumentString(`
    mutation cancelGenerationJob($jobId: String!) {
  cancelGenerationJob(jobId: $jobId)
}
    `);

export const useCancelGenerationJobMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CancelGenerationJobMutation, TError, CancelGenerationJobMutationVariables, TContext>) => {
    
    return useMutation<CancelGenerationJobMutation, TError, CancelGenerationJobMutationVariables, TContext>(
      {
    mutationKey: ['cancelGenerationJob'],
    mutationFn: (variables?: CancelGenerationJobMutationVariables) => fetcher<CancelGenerationJobMutation, CancelGenerationJobMutationVariables>(CancelGenerationJobDocument, variables)(),
    ...options
  }
    )};

export const DeleteApiConnectorDocument = new TypedDocumentString(`
    mutation deleteApiConnector($id: ID!) {
  deleteApiConnector(id: $id)
}
    `);

export const useDeleteApiConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteApiConnectorMutation, TError, DeleteApiConnectorMutationVariables, TContext>) => {
    
    return useMutation<DeleteApiConnectorMutation, TError, DeleteApiConnectorMutationVariables, TContext>(
      {
    mutationKey: ['deleteApiConnector'],
    mutationFn: (variables?: DeleteApiConnectorMutationVariables) => fetcher<DeleteApiConnectorMutation, DeleteApiConnectorMutationVariables>(DeleteApiConnectorDocument, variables)(),
    ...options
  }
    )};

export const EnableApiConnectorDocument = new TypedDocumentString(`
    mutation enableApiConnector($id: ID!, $enable: Boolean!) {
  enableApiConnector(id: $id, enable: $enable)
}
    `);

export const useEnableApiConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableApiConnectorMutation, TError, EnableApiConnectorMutationVariables, TContext>) => {
    
    return useMutation<EnableApiConnectorMutation, TError, EnableApiConnectorMutationVariables, TContext>(
      {
    mutationKey: ['enableApiConnector'],
    mutationFn: (variables?: EnableApiConnectorMutationVariables) => fetcher<EnableApiConnectorMutation, EnableApiConnectorMutationVariables>(EnableApiConnectorDocument, variables)(),
    ...options
  }
    )};

export const GenerateSpecificationDocument = new TypedDocumentString(`
    mutation generateSpecification($input: GenerateSpecificationInput!) {
  generateSpecification(input: $input) {
    specification
  }
}
    `);

export const useGenerateSpecificationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<GenerateSpecificationMutation, TError, GenerateSpecificationMutationVariables, TContext>) => {
    
    return useMutation<GenerateSpecificationMutation, TError, GenerateSpecificationMutationVariables, TContext>(
      {
    mutationKey: ['generateSpecification'],
    mutationFn: (variables?: GenerateSpecificationMutationVariables) => fetcher<GenerateSpecificationMutation, GenerateSpecificationMutationVariables>(GenerateSpecificationDocument, variables)(),
    ...options
  }
    )};

export const GenerationJobStatusDocument = new TypedDocumentString(`
    query generationJobStatus($jobId: String!) {
  generationJobStatus(jobId: $jobId) {
    jobId
    status
    specification
    errorMessage
  }
}
    `);

export const useGenerationJobStatusQuery = <
      TData = GenerationJobStatusQuery,
      TError = unknown
    >(
      variables: GenerationJobStatusQueryVariables,
      options?: Omit<UseQueryOptions<GenerationJobStatusQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<GenerationJobStatusQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<GenerationJobStatusQuery, TError, TData>(
      {
    queryKey: ['generationJobStatus', variables],
    queryFn: fetcher<GenerationJobStatusQuery, GenerationJobStatusQueryVariables>(GenerationJobStatusDocument, variables),
    ...options
  }
    )};

export const ImportOpenApiSpecificationDocument = new TypedDocumentString(`
    mutation importOpenApiSpecification($input: ImportOpenApiSpecificationInput!) {
  importOpenApiSpecification(input: $input) {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `);

export const useImportOpenApiSpecificationMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<ImportOpenApiSpecificationMutation, TError, ImportOpenApiSpecificationMutationVariables, TContext>) => {
    
    return useMutation<ImportOpenApiSpecificationMutation, TError, ImportOpenApiSpecificationMutationVariables, TContext>(
      {
    mutationKey: ['importOpenApiSpecification'],
    mutationFn: (variables?: ImportOpenApiSpecificationMutationVariables) => fetcher<ImportOpenApiSpecificationMutation, ImportOpenApiSpecificationMutationVariables>(ImportOpenApiSpecificationDocument, variables)(),
    ...options
  }
    )};

export const StartGenerateFromDocumentationPreviewDocument = new TypedDocumentString(`
    mutation startGenerateFromDocumentationPreview($input: GenerateFromDocumentationInput!) {
  startGenerateFromDocumentationPreview(input: $input) {
    jobId
    status
    specification
    errorMessage
  }
}
    `);

export const useStartGenerateFromDocumentationPreviewMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<StartGenerateFromDocumentationPreviewMutation, TError, StartGenerateFromDocumentationPreviewMutationVariables, TContext>) => {
    
    return useMutation<StartGenerateFromDocumentationPreviewMutation, TError, StartGenerateFromDocumentationPreviewMutationVariables, TContext>(
      {
    mutationKey: ['startGenerateFromDocumentationPreview'],
    mutationFn: (variables?: StartGenerateFromDocumentationPreviewMutationVariables) => fetcher<StartGenerateFromDocumentationPreviewMutation, StartGenerateFromDocumentationPreviewMutationVariables>(StartGenerateFromDocumentationPreviewDocument, variables)(),
    ...options
  }
    )};

export const UpdateApiConnectorDocument = new TypedDocumentString(`
    mutation updateApiConnector($id: ID!, $input: UpdateApiConnectorInput!) {
  updateApiConnector(id: $id, input: $input) {
    id
    name
    title
    description
    icon
    connectorVersion
    enabled
    specification
    definition
    endpoints {
      id
      name
      description
      path
      httpMethod
    }
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `);

export const useUpdateApiConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateApiConnectorMutation, TError, UpdateApiConnectorMutationVariables, TContext>) => {
    
    return useMutation<UpdateApiConnectorMutation, TError, UpdateApiConnectorMutationVariables, TContext>(
      {
    mutationKey: ['updateApiConnector'],
    mutationFn: (variables?: UpdateApiConnectorMutationVariables) => fetcher<UpdateApiConnectorMutation, UpdateApiConnectorMutationVariables>(UpdateApiConnectorDocument, variables)(),
    ...options
  }
    )};

export const EditorJobFileLogsDocument = new TypedDocumentString(`
    query editorJobFileLogs($jobId: ID!, $filter: LogFilterInput, $page: Int, $size: Int) {
  editorJobFileLogs(jobId: $jobId, filter: $filter, page: $page, size: $size) {
    content {
      timestamp
      level
      componentName
      componentOperationName
      taskExecutionId
      message
      exceptionType
      exceptionMessage
      stackTrace
    }
    totalElements
    totalPages
    pageNumber
    pageSize
    hasNext
    hasPrevious
  }
}
    `);

export const useEditorJobFileLogsQuery = <
      TData = EditorJobFileLogsQuery,
      TError = unknown
    >(
      variables: EditorJobFileLogsQueryVariables,
      options?: Omit<UseQueryOptions<EditorJobFileLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EditorJobFileLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EditorJobFileLogsQuery, TError, TData>(
      {
    queryKey: ['editorJobFileLogs', variables],
    queryFn: fetcher<EditorJobFileLogsQuery, EditorJobFileLogsQueryVariables>(EditorJobFileLogsDocument, variables),
    ...options
  }
    )};

export const EditorJobFileLogsExistDocument = new TypedDocumentString(`
    query editorJobFileLogsExist($jobId: ID!) {
  editorJobFileLogsExist(jobId: $jobId)
}
    `);

export const useEditorJobFileLogsExistQuery = <
      TData = EditorJobFileLogsExistQuery,
      TError = unknown
    >(
      variables: EditorJobFileLogsExistQueryVariables,
      options?: Omit<UseQueryOptions<EditorJobFileLogsExistQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EditorJobFileLogsExistQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EditorJobFileLogsExistQuery, TError, TData>(
      {
    queryKey: ['editorJobFileLogsExist', variables],
    queryFn: fetcher<EditorJobFileLogsExistQuery, EditorJobFileLogsExistQueryVariables>(EditorJobFileLogsExistDocument, variables),
    ...options
  }
    )};

export const EditorTaskExecutionFileLogsDocument = new TypedDocumentString(`
    query editorTaskExecutionFileLogs($jobId: ID!, $taskExecutionId: ID!) {
  editorTaskExecutionFileLogs(jobId: $jobId, taskExecutionId: $taskExecutionId) {
    timestamp
    level
    componentName
    componentOperationName
    taskExecutionId
    message
    exceptionType
    exceptionMessage
    stackTrace
  }
}
    `);

export const useEditorTaskExecutionFileLogsQuery = <
      TData = EditorTaskExecutionFileLogsQuery,
      TError = unknown
    >(
      variables: EditorTaskExecutionFileLogsQueryVariables,
      options?: Omit<UseQueryOptions<EditorTaskExecutionFileLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EditorTaskExecutionFileLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EditorTaskExecutionFileLogsQuery, TError, TData>(
      {
    queryKey: ['editorTaskExecutionFileLogs', variables],
    queryFn: fetcher<EditorTaskExecutionFileLogsQuery, EditorTaskExecutionFileLogsQueryVariables>(EditorTaskExecutionFileLogsDocument, variables),
    ...options
  }
    )};

export const JobFileLogsDocument = new TypedDocumentString(`
    query jobFileLogs($jobId: ID!, $filter: LogFilterInput, $page: Int, $size: Int) {
  jobFileLogs(jobId: $jobId, filter: $filter, page: $page, size: $size) {
    content {
      timestamp
      level
      componentName
      componentOperationName
      taskExecutionId
      message
      exceptionType
      exceptionMessage
      stackTrace
    }
    totalElements
    totalPages
    pageNumber
    pageSize
    hasNext
    hasPrevious
  }
}
    `);

export const useJobFileLogsQuery = <
      TData = JobFileLogsQuery,
      TError = unknown
    >(
      variables: JobFileLogsQueryVariables,
      options?: Omit<UseQueryOptions<JobFileLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<JobFileLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<JobFileLogsQuery, TError, TData>(
      {
    queryKey: ['jobFileLogs', variables],
    queryFn: fetcher<JobFileLogsQuery, JobFileLogsQueryVariables>(JobFileLogsDocument, variables),
    ...options
  }
    )};

export const JobFileLogsExistDocument = new TypedDocumentString(`
    query jobFileLogsExist($jobId: ID!) {
  jobFileLogsExist(jobId: $jobId)
}
    `);

export const useJobFileLogsExistQuery = <
      TData = JobFileLogsExistQuery,
      TError = unknown
    >(
      variables: JobFileLogsExistQueryVariables,
      options?: Omit<UseQueryOptions<JobFileLogsExistQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<JobFileLogsExistQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<JobFileLogsExistQuery, TError, TData>(
      {
    queryKey: ['jobFileLogsExist', variables],
    queryFn: fetcher<JobFileLogsExistQuery, JobFileLogsExistQueryVariables>(JobFileLogsExistDocument, variables),
    ...options
  }
    )};

export const TaskExecutionFileLogsDocument = new TypedDocumentString(`
    query taskExecutionFileLogs($jobId: ID!, $taskExecutionId: ID!) {
  taskExecutionFileLogs(jobId: $jobId, taskExecutionId: $taskExecutionId) {
    timestamp
    level
    componentName
    componentOperationName
    taskExecutionId
    message
    exceptionType
    exceptionMessage
    stackTrace
  }
}
    `);

export const useTaskExecutionFileLogsQuery = <
      TData = TaskExecutionFileLogsQuery,
      TError = unknown
    >(
      variables: TaskExecutionFileLogsQueryVariables,
      options?: Omit<UseQueryOptions<TaskExecutionFileLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<TaskExecutionFileLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<TaskExecutionFileLogsQuery, TError, TData>(
      {
    queryKey: ['taskExecutionFileLogs', variables],
    queryFn: fetcher<TaskExecutionFileLogsQuery, TaskExecutionFileLogsQueryVariables>(TaskExecutionFileLogsDocument, variables),
    ...options
  }
    )};

export const ComponentOperationPoliciesDocument = new TypedDocumentString(`
    query ComponentOperationPolicies($componentName: String!) {
  componentOperationPolicies(componentName: $componentName) {
    componentName
    operationType
    operationName
  }
}
    `);

export const useComponentOperationPoliciesQuery = <
      TData = ComponentOperationPoliciesQuery,
      TError = unknown
    >(
      variables: ComponentOperationPoliciesQueryVariables,
      options?: Omit<UseQueryOptions<ComponentOperationPoliciesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ComponentOperationPoliciesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ComponentOperationPoliciesQuery, TError, TData>(
      {
    queryKey: ['ComponentOperationPolicies', variables],
    queryFn: fetcher<ComponentOperationPoliciesQuery, ComponentOperationPoliciesQueryVariables>(ComponentOperationPoliciesDocument, variables),
    ...options
  }
    )};

export const ComponentPoliciesDocument = new TypedDocumentString(`
    query ComponentPolicies {
  componentPolicies {
    name
    title
    description
    icon
    version
    enabled
  }
}
    `);

export const useComponentPoliciesQuery = <
      TData = ComponentPoliciesQuery,
      TError = unknown
    >(
      variables?: ComponentPoliciesQueryVariables,
      options?: Omit<UseQueryOptions<ComponentPoliciesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ComponentPoliciesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ComponentPoliciesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['ComponentPolicies'] : ['ComponentPolicies', variables],
    queryFn: fetcher<ComponentPoliciesQuery, ComponentPoliciesQueryVariables>(ComponentPoliciesDocument, variables),
    ...options
  }
    )};

export const UpdateComponentOperationPolicyDocument = new TypedDocumentString(`
    mutation UpdateComponentOperationPolicy($componentName: String!, $operationType: ComponentOperationType!, $operationName: String!, $enabled: Boolean!) {
  updateComponentOperationPolicy(
    componentName: $componentName
    operationType: $operationType
    operationName: $operationName
    enabled: $enabled
  )
}
    `);

export const useUpdateComponentOperationPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateComponentOperationPolicyMutation, TError, UpdateComponentOperationPolicyMutationVariables, TContext>) => {
    
    return useMutation<UpdateComponentOperationPolicyMutation, TError, UpdateComponentOperationPolicyMutationVariables, TContext>(
      {
    mutationKey: ['UpdateComponentOperationPolicy'],
    mutationFn: (variables?: UpdateComponentOperationPolicyMutationVariables) => fetcher<UpdateComponentOperationPolicyMutation, UpdateComponentOperationPolicyMutationVariables>(UpdateComponentOperationPolicyDocument, variables)(),
    ...options
  }
    )};

export const UpdateComponentPolicyDocument = new TypedDocumentString(`
    mutation UpdateComponentPolicy($name: String!, $enabled: Boolean!) {
  updateComponentPolicy(name: $name, enabled: $enabled) {
    name
    title
    icon
    version
    enabled
  }
}
    `);

export const useUpdateComponentPolicyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateComponentPolicyMutation, TError, UpdateComponentPolicyMutationVariables, TContext>) => {
    
    return useMutation<UpdateComponentPolicyMutation, TError, UpdateComponentPolicyMutationVariables, TContext>(
      {
    mutationKey: ['UpdateComponentPolicy'],
    mutationFn: (variables?: UpdateComponentPolicyMutationVariables) => fetcher<UpdateComponentPolicyMutation, UpdateComponentPolicyMutationVariables>(UpdateComponentPolicyDocument, variables)(),
    ...options
  }
    )};

export const AdminApiKeysDocument = new TypedDocumentString(`
    query adminApiKeys($environmentId: ID!) {
  adminApiKeys(environmentId: $environmentId) {
    id
    name
    secretKey
    lastUsedDate
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useAdminApiKeysQuery = <
      TData = AdminApiKeysQuery,
      TError = unknown
    >(
      variables: AdminApiKeysQueryVariables,
      options?: Omit<UseQueryOptions<AdminApiKeysQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AdminApiKeysQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AdminApiKeysQuery, TError, TData>(
      {
    queryKey: ['adminApiKeys', variables],
    queryFn: fetcher<AdminApiKeysQuery, AdminApiKeysQueryVariables>(AdminApiKeysDocument, variables),
    ...options
  }
    )};

export const ApiKeysDocument = new TypedDocumentString(`
    query apiKeys($environmentId: ID!, $type: PlatformType!) {
  apiKeys(environmentId: $environmentId, type: $type) {
    id
    name
    secretKey
    lastUsedDate
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useApiKeysQuery = <
      TData = ApiKeysQuery,
      TError = unknown
    >(
      variables: ApiKeysQueryVariables,
      options?: Omit<UseQueryOptions<ApiKeysQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ApiKeysQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ApiKeysQuery, TError, TData>(
      {
    queryKey: ['apiKeys', variables],
    queryFn: fetcher<ApiKeysQuery, ApiKeysQueryVariables>(ApiKeysDocument, variables),
    ...options
  }
    )};

export const ClusterElementComponentConnectionsDocument = new TypedDocumentString(`
    query clusterElementComponentConnections($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!) {
  clusterElementComponentConnections(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
  ) {
    componentName
    componentVersion
    key
    required
    workflowNodeName
  }
}
    `);

export const useClusterElementComponentConnectionsQuery = <
      TData = ClusterElementComponentConnectionsQuery,
      TError = unknown
    >(
      variables: ClusterElementComponentConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementComponentConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementComponentConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementComponentConnectionsQuery, TError, TData>(
      {
    queryKey: ['clusterElementComponentConnections', variables],
    queryFn: fetcher<ClusterElementComponentConnectionsQuery, ClusterElementComponentConnectionsQueryVariables>(ClusterElementComponentConnectionsDocument, variables),
    ...options
  }
    )};

export const ClusterElementDefinitionDocument = new TypedDocumentString(`
    query clusterElementDefinition($componentName: String!, $componentVersion: Int!, $clusterElementName: String!) {
  clusterElementDefinition(
    componentName: $componentName
    componentVersion: $componentVersion
    clusterElementName: $clusterElementName
  ) {
    componentName
    componentVersion
    description
    name
    title
    properties {
      advancedOption
      description
      displayCondition
      expressionEnabled
      hidden
      name
      required
      type
      ... on StringProperty {
        controlType
        defaultValue
        label
        placeholder
        options {
          description
          label
          value
        }
        optionsDataSource {
          optionsLookupDependsOn
        }
      }
      ... on IntegerProperty {
        controlType
        integerDefaultValue: defaultValue
        label
        placeholder
        options {
          description
          label
          value
        }
        optionsDataSource {
          optionsLookupDependsOn
        }
      }
      ... on NumberProperty {
        controlType
        label
        numberDefaultValue: defaultValue
        placeholder
        options {
          description
          label
          value
        }
        optionsDataSource {
          optionsLookupDependsOn
        }
      }
      ... on BooleanProperty {
        booleanDefaultValue: defaultValue
        controlType
        label
        placeholder
      }
      ... on ArrayProperty {
        arrayDefaultValue: defaultValue
        controlType
        label
        placeholder
        optionsDataSource {
          optionsLookupDependsOn
        }
        items {
          advancedOption
          description
          displayCondition
          expressionEnabled
          hidden
          name
          required
          type
          ... on StringProperty {
            controlType
            defaultValue
            label
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on IntegerProperty {
            controlType
            integerDefaultValue: defaultValue
            label
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on NumberProperty {
            controlType
            label
            numberDefaultValue: defaultValue
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on BooleanProperty {
            booleanDefaultValue: defaultValue
            controlType
            label
            placeholder
          }
          ... on ArrayProperty {
            arrayDefaultValue: defaultValue
            controlType
            label
            placeholder
          }
          ... on ObjectProperty {
            controlType
            label
            objectDefaultValue: defaultValue
            placeholder
          }
          ... on DateProperty {
            controlType
            dateDefaultValue: defaultValue
            label
            placeholder
          }
          ... on DateTimeProperty {
            controlType
            dateTimeDefaultValue: defaultValue
            label
            placeholder
          }
          ... on TimeProperty {
            controlType
            label
            placeholder
            timeDefaultValue: defaultValue
          }
          ... on NullProperty {
            controlType
            label
            placeholder
          }
          ... on DynamicPropertiesProperty {
            propertiesDataSource {
              propertiesLookupDependsOn
            }
          }
        }
      }
      ... on ObjectProperty {
        controlType
        label
        objectDefaultValue: defaultValue
        placeholder
        optionsDataSource {
          optionsLookupDependsOn
        }
        properties {
          advancedOption
          description
          displayCondition
          expressionEnabled
          hidden
          name
          required
          type
          ... on StringProperty {
            controlType
            defaultValue
            label
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on IntegerProperty {
            controlType
            integerDefaultValue: defaultValue
            label
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on NumberProperty {
            controlType
            label
            numberDefaultValue: defaultValue
            placeholder
            options {
              description
              label
              value
            }
            optionsDataSource {
              optionsLookupDependsOn
            }
          }
          ... on BooleanProperty {
            booleanDefaultValue: defaultValue
            controlType
            label
            placeholder
          }
          ... on ArrayProperty {
            arrayDefaultValue: defaultValue
            controlType
            label
            placeholder
          }
          ... on ObjectProperty {
            controlType
            label
            objectDefaultValue: defaultValue
            placeholder
          }
          ... on DateProperty {
            controlType
            dateDefaultValue: defaultValue
            label
            placeholder
          }
          ... on DateTimeProperty {
            controlType
            dateTimeDefaultValue: defaultValue
            label
            placeholder
          }
          ... on TimeProperty {
            controlType
            label
            placeholder
            timeDefaultValue: defaultValue
          }
          ... on NullProperty {
            controlType
            label
            placeholder
          }
          ... on DynamicPropertiesProperty {
            propertiesDataSource {
              propertiesLookupDependsOn
            }
          }
        }
      }
      ... on DateProperty {
        controlType
        dateDefaultValue: defaultValue
        label
        placeholder
      }
      ... on DateTimeProperty {
        controlType
        dateTimeDefaultValue: defaultValue
        label
        placeholder
      }
      ... on TimeProperty {
        controlType
        label
        placeholder
        timeDefaultValue: defaultValue
      }
      ... on NullProperty {
        controlType
        label
        placeholder
      }
      ... on DynamicPropertiesProperty {
        propertiesDataSource {
          propertiesLookupDependsOn
        }
      }
    }
  }
}
    `);

export const useClusterElementDefinitionQuery = <
      TData = ClusterElementDefinitionQuery,
      TError = unknown
    >(
      variables: ClusterElementDefinitionQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementDefinitionQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementDefinitionQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementDefinitionQuery, TError, TData>(
      {
    queryKey: ['clusterElementDefinition', variables],
    queryFn: fetcher<ClusterElementDefinitionQuery, ClusterElementDefinitionQueryVariables>(ClusterElementDefinitionDocument, variables),
    ...options
  }
    )};

export const ClusterElementFieldsDocument = new TypedDocumentString(`
    query clusterElementFields($componentName: String!, $componentVersion: Int!, $clusterElementName: String!, $connectionId: Long, $inputParameters: Map) {
  clusterElementFields(
    componentName: $componentName
    componentVersion: $componentVersion
    clusterElementName: $clusterElementName
    connectionId: $connectionId
    inputParameters: $inputParameters
  ) {
    name
    label
    type
  }
}
    `);

export const useClusterElementFieldsQuery = <
      TData = ClusterElementFieldsQuery,
      TError = unknown
    >(
      variables: ClusterElementFieldsQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementFieldsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementFieldsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementFieldsQuery, TError, TData>(
      {
    queryKey: ['clusterElementFields', variables],
    queryFn: fetcher<ClusterElementFieldsQuery, ClusterElementFieldsQueryVariables>(ClusterElementFieldsDocument, variables),
    ...options
  }
    )};

export const ClusterElementMissingRequiredPropertiesDocument = new TypedDocumentString(`
    query ClusterElementMissingRequiredProperties($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!) {
  clusterElementMissingRequiredProperties(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
  )
}
    `);

export const useClusterElementMissingRequiredPropertiesQuery = <
      TData = ClusterElementMissingRequiredPropertiesQuery,
      TError = unknown
    >(
      variables: ClusterElementMissingRequiredPropertiesQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementMissingRequiredPropertiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementMissingRequiredPropertiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementMissingRequiredPropertiesQuery, TError, TData>(
      {
    queryKey: ['ClusterElementMissingRequiredProperties', variables],
    queryFn: fetcher<ClusterElementMissingRequiredPropertiesQuery, ClusterElementMissingRequiredPropertiesQueryVariables>(ClusterElementMissingRequiredPropertiesDocument, variables),
    ...options
  }
    )};

export const ClusterElementDynamicPropertiesDocument = new TypedDocumentString(`
    query clusterElementDynamicProperties($componentName: String!, $componentVersion: Int!, $clusterElementName: String!, $propertyName: String!, $connectionId: Long, $inputParameters: Map, $lookupDependsOnPaths: [String!]) {
  clusterElementDynamicProperties(
    componentName: $componentName
    componentVersion: $componentVersion
    clusterElementName: $clusterElementName
    propertyName: $propertyName
    connectionId: $connectionId
    inputParameters: $inputParameters
    lookupDependsOnPaths: $lookupDependsOnPaths
  ) {
    advancedOption
    description
    displayCondition
    expressionEnabled
    hidden
    name
    required
    type
    ... on StringProperty {
      controlType
      label
      placeholder
      options {
        description
        label
        value
      }
      optionsDataSource {
        optionsLookupDependsOn
      }
    }
    ... on IntegerProperty {
      controlType
      label
      placeholder
      options {
        description
        label
        value
      }
      optionsDataSource {
        optionsLookupDependsOn
      }
    }
    ... on NumberProperty {
      controlType
      label
      placeholder
      options {
        description
        label
        value
      }
      optionsDataSource {
        optionsLookupDependsOn
      }
    }
    ... on BooleanProperty {
      controlType
      label
      placeholder
    }
    ... on ArrayProperty {
      controlType
      label
      placeholder
      optionsDataSource {
        optionsLookupDependsOn
      }
      items {
        advancedOption
        description
        displayCondition
        expressionEnabled
        hidden
        name
        required
        type
        ... on StringProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on IntegerProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on NumberProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on BooleanProperty {
          controlType
          label
          placeholder
        }
        ... on ArrayProperty {
          controlType
          label
          placeholder
        }
        ... on ObjectProperty {
          controlType
          label
          placeholder
        }
        ... on DateProperty {
          controlType
          label
          placeholder
        }
        ... on DateTimeProperty {
          controlType
          label
          placeholder
        }
        ... on TimeProperty {
          controlType
          label
          placeholder
        }
        ... on NullProperty {
          controlType
          label
          placeholder
        }
        ... on DynamicPropertiesProperty {
          propertiesDataSource {
            propertiesLookupDependsOn
          }
        }
      }
    }
    ... on ObjectProperty {
      controlType
      label
      placeholder
      optionsDataSource {
        optionsLookupDependsOn
      }
      properties {
        advancedOption
        description
        displayCondition
        expressionEnabled
        hidden
        name
        required
        type
        ... on StringProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on IntegerProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on NumberProperty {
          controlType
          label
          placeholder
          options {
            description
            label
            value
          }
          optionsDataSource {
            optionsLookupDependsOn
          }
        }
        ... on BooleanProperty {
          controlType
          label
          placeholder
        }
        ... on ArrayProperty {
          controlType
          label
          placeholder
        }
        ... on ObjectProperty {
          controlType
          label
          placeholder
        }
        ... on DateProperty {
          controlType
          label
          placeholder
        }
        ... on DateTimeProperty {
          controlType
          label
          placeholder
        }
        ... on TimeProperty {
          controlType
          label
          placeholder
        }
        ... on NullProperty {
          controlType
          label
          placeholder
        }
        ... on DynamicPropertiesProperty {
          propertiesDataSource {
            propertiesLookupDependsOn
          }
        }
      }
    }
    ... on DateProperty {
      controlType
      label
      placeholder
    }
    ... on DateTimeProperty {
      controlType
      label
      placeholder
    }
    ... on TimeProperty {
      controlType
      label
      placeholder
    }
    ... on NullProperty {
      controlType
      label
      placeholder
    }
    ... on DynamicPropertiesProperty {
      propertiesDataSource {
        propertiesLookupDependsOn
      }
    }
  }
}
    `);

export const useClusterElementDynamicPropertiesQuery = <
      TData = ClusterElementDynamicPropertiesQuery,
      TError = unknown
    >(
      variables: ClusterElementDynamicPropertiesQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementDynamicPropertiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementDynamicPropertiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementDynamicPropertiesQuery, TError, TData>(
      {
    queryKey: ['clusterElementDynamicProperties', variables],
    queryFn: fetcher<ClusterElementDynamicPropertiesQuery, ClusterElementDynamicPropertiesQueryVariables>(ClusterElementDynamicPropertiesDocument, variables),
    ...options
  }
    )};

export const ClusterElementOptionsDocument = new TypedDocumentString(`
    query clusterElementOptions($componentName: String!, $componentVersion: Int!, $clusterElementName: String!, $propertyName: String!, $connectionId: Long, $inputParameters: Map, $lookupDependsOnPaths: [String!]) {
  clusterElementOptions(
    componentName: $componentName
    componentVersion: $componentVersion
    clusterElementName: $clusterElementName
    propertyName: $propertyName
    connectionId: $connectionId
    inputParameters: $inputParameters
    lookupDependsOnPaths: $lookupDependsOnPaths
  ) {
    description
    label
    value
  }
}
    `);

export const useClusterElementOptionsQuery = <
      TData = ClusterElementOptionsQuery,
      TError = unknown
    >(
      variables: ClusterElementOptionsQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementOptionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementOptionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementOptionsQuery, TError, TData>(
      {
    queryKey: ['clusterElementOptions', variables],
    queryFn: fetcher<ClusterElementOptionsQuery, ClusterElementOptionsQueryVariables>(ClusterElementOptionsDocument, variables),
    ...options
  }
    )};

export const ClusterElementScriptInputDocument = new TypedDocumentString(`
    query clusterElementScriptInput($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $environmentId: Long!) {
  clusterElementScriptInput(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    environmentId: $environmentId
  )
}
    `);

export const useClusterElementScriptInputQuery = <
      TData = ClusterElementScriptInputQuery,
      TError = unknown
    >(
      variables: ClusterElementScriptInputQueryVariables,
      options?: Omit<UseQueryOptions<ClusterElementScriptInputQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ClusterElementScriptInputQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ClusterElementScriptInputQuery, TError, TData>(
      {
    queryKey: ['clusterElementScriptInput', variables],
    queryFn: fetcher<ClusterElementScriptInputQuery, ClusterElementScriptInputQueryVariables>(ClusterElementScriptInputDocument, variables),
    ...options
  }
    )};

export const ComponentDefinitionSearchDocument = new TypedDocumentString(`
    query ComponentDefinitionSearch($query: String!) {
  componentDefinitionSearch(query: $query) {
    name
    title
    icon
    description
    version
    actionsCount
    triggersCount
    clusterElementsCount
    clusterRoot
    componentCategories {
      name
      label
    }
    actions {
      name
      title
      description
    }
    triggers {
      name
      title
      description
    }
    clusterElements {
      type {
        name
        label
      }
    }
  }
}
    `);

export const useComponentDefinitionSearchQuery = <
      TData = ComponentDefinitionSearchQuery,
      TError = unknown
    >(
      variables: ComponentDefinitionSearchQueryVariables,
      options?: Omit<UseQueryOptions<ComponentDefinitionSearchQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ComponentDefinitionSearchQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ComponentDefinitionSearchQuery, TError, TData>(
      {
    queryKey: ['ComponentDefinitionSearch', variables],
    queryFn: fetcher<ComponentDefinitionSearchQuery, ComponentDefinitionSearchQueryVariables>(ComponentDefinitionSearchDocument, variables),
    ...options
  }
    )};

export const ComponentPropertyDisplayConditionsDocument = new TypedDocumentString(`
    query componentPropertyDisplayConditions($componentName: String!, $componentVersion: Int!, $operationName: String!, $operationType: String!, $parameters: Map) {
  componentPropertyDisplayConditions(
    componentName: $componentName
    componentVersion: $componentVersion
    operationName: $operationName
    operationType: $operationType
    parameters: $parameters
  )
}
    `);

export const useComponentPropertyDisplayConditionsQuery = <
      TData = ComponentPropertyDisplayConditionsQuery,
      TError = unknown
    >(
      variables: ComponentPropertyDisplayConditionsQueryVariables,
      options?: Omit<UseQueryOptions<ComponentPropertyDisplayConditionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ComponentPropertyDisplayConditionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ComponentPropertyDisplayConditionsQuery, TError, TData>(
      {
    queryKey: ['componentPropertyDisplayConditions', variables],
    queryFn: fetcher<ComponentPropertyDisplayConditionsQuery, ComponentPropertyDisplayConditionsQueryVariables>(ComponentPropertyDisplayConditionsDocument, variables),
    ...options
  }
    )};

export const CreateApiKeyDocument = new TypedDocumentString(`
    mutation createApiKey($name: String!, $environmentId: ID!, $type: PlatformType) {
  createApiKey(name: $name, environmentId: $environmentId, type: $type)
}
    `);

export const useCreateApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateApiKeyMutation, TError, CreateApiKeyMutationVariables, TContext>) => {
    
    return useMutation<CreateApiKeyMutation, TError, CreateApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['createApiKey'],
    mutationFn: (variables?: CreateApiKeyMutationVariables) => fetcher<CreateApiKeyMutation, CreateApiKeyMutationVariables>(CreateApiKeyDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpComponentDocument = new TypedDocumentString(`
    mutation createMcpComponent($input: McpComponentInput!) {
  createMcpComponent(input: $input) {
    id
    componentName
    componentVersion
    title
    mcpServerId
    connectionId
  }
}
    `);

export const useCreateMcpComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpComponentMutation, TError, CreateMcpComponentMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpComponentMutation, TError, CreateMcpComponentMutationVariables, TContext>(
      {
    mutationKey: ['createMcpComponent'],
    mutationFn: (variables?: CreateMcpComponentMutationVariables) => fetcher<CreateMcpComponentMutation, CreateMcpComponentMutationVariables>(CreateMcpComponentDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpComponentWithToolsDocument = new TypedDocumentString(`
    mutation createMcpComponentWithTools($input: McpComponentWithToolsInput!) {
  createMcpComponentWithTools(input: $input) {
    id
    componentName
    componentVersion
    title
    mcpServerId
    connectionId
    requiredAuthorities
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `);

export const useCreateMcpComponentWithToolsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpComponentWithToolsMutation, TError, CreateMcpComponentWithToolsMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpComponentWithToolsMutation, TError, CreateMcpComponentWithToolsMutationVariables, TContext>(
      {
    mutationKey: ['createMcpComponentWithTools'],
    mutationFn: (variables?: CreateMcpComponentWithToolsMutationVariables) => fetcher<CreateMcpComponentWithToolsMutation, CreateMcpComponentWithToolsMutationVariables>(CreateMcpComponentWithToolsDocument, variables)(),
    ...options
  }
    )};

export const CreateMcpToolDocument = new TypedDocumentString(`
    mutation createMcpTool($input: McpToolInput!) {
  createMcpTool(input: $input) {
    id
    name
    mcpComponentId
    parameters
  }
}
    `);

export const useCreateMcpToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateMcpToolMutation, TError, CreateMcpToolMutationVariables, TContext>) => {
    
    return useMutation<CreateMcpToolMutation, TError, CreateMcpToolMutationVariables, TContext>(
      {
    mutationKey: ['createMcpTool'],
    mutationFn: (variables?: CreateMcpToolMutationVariables) => fetcher<CreateMcpToolMutation, CreateMcpToolMutationVariables>(CreateMcpToolDocument, variables)(),
    ...options
  }
    )};

export const DeleteApiKeyDocument = new TypedDocumentString(`
    mutation deleteApiKey($id: ID!) {
  deleteApiKey(id: $id)
}
    `);

export const useDeleteApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteApiKeyMutation, TError, DeleteApiKeyMutationVariables, TContext>) => {
    
    return useMutation<DeleteApiKeyMutation, TError, DeleteApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['deleteApiKey'],
    mutationFn: (variables?: DeleteApiKeyMutationVariables) => fetcher<DeleteApiKeyMutation, DeleteApiKeyMutationVariables>(DeleteApiKeyDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpComponentDocument = new TypedDocumentString(`
    mutation deleteMcpComponent($id: ID!) {
  deleteMcpComponent(id: $id)
}
    `);

export const useDeleteMcpComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpComponentMutation, TError, DeleteMcpComponentMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpComponentMutation, TError, DeleteMcpComponentMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpComponent'],
    mutationFn: (variables?: DeleteMcpComponentMutationVariables) => fetcher<DeleteMcpComponentMutation, DeleteMcpComponentMutationVariables>(DeleteMcpComponentDocument, variables)(),
    ...options
  }
    )};

export const DeleteMcpToolDocument = new TypedDocumentString(`
    mutation deleteMcpTool($id: ID!) {
  deleteMcpTool(id: $id)
}
    `);

export const useDeleteMcpToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteMcpToolMutation, TError, DeleteMcpToolMutationVariables, TContext>) => {
    
    return useMutation<DeleteMcpToolMutation, TError, DeleteMcpToolMutationVariables, TContext>(
      {
    mutationKey: ['deleteMcpTool'],
    mutationFn: (variables?: DeleteMcpToolMutationVariables) => fetcher<DeleteMcpToolMutation, DeleteMcpToolMutationVariables>(DeleteMcpToolDocument, variables)(),
    ...options
  }
    )};

export const EnvironmentsDocument = new TypedDocumentString(`
    query environments {
  environments {
    id
    name
  }
}
    `);

export const useEnvironmentsQuery = <
      TData = EnvironmentsQuery,
      TError = unknown
    >(
      variables?: EnvironmentsQueryVariables,
      options?: Omit<UseQueryOptions<EnvironmentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EnvironmentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EnvironmentsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['environments'] : ['environments', variables],
    queryFn: fetcher<EnvironmentsQuery, EnvironmentsQueryVariables>(EnvironmentsDocument, variables),
    ...options
  }
    )};

export const EvaluatorFunctionDefinitionsDocument = new TypedDocumentString(`
    query evaluatorFunctionDefinitions {
  evaluatorFunctionDefinitions {
    name
    title
    description
    category
    returnType
    example
    parameters {
      name
      description
      type
      required
    }
  }
}
    `);

export const useEvaluatorFunctionDefinitionsQuery = <
      TData = EvaluatorFunctionDefinitionsQuery,
      TError = unknown
    >(
      variables?: EvaluatorFunctionDefinitionsQueryVariables,
      options?: Omit<UseQueryOptions<EvaluatorFunctionDefinitionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EvaluatorFunctionDefinitionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EvaluatorFunctionDefinitionsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['evaluatorFunctionDefinitions'] : ['evaluatorFunctionDefinitions', variables],
    queryFn: fetcher<EvaluatorFunctionDefinitionsQuery, EvaluatorFunctionDefinitionsQueryVariables>(EvaluatorFunctionDefinitionsDocument, variables),
    ...options
  }
    )};

export const ManagementMcpServerAuthenticationRequiredDocument = new TypedDocumentString(`
    query managementMcpServerAuthenticationRequired {
  managementMcpServerAuthenticationRequired
}
    `);

export const useManagementMcpServerAuthenticationRequiredQuery = <
      TData = ManagementMcpServerAuthenticationRequiredQuery,
      TError = unknown
    >(
      variables?: ManagementMcpServerAuthenticationRequiredQueryVariables,
      options?: Omit<UseQueryOptions<ManagementMcpServerAuthenticationRequiredQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ManagementMcpServerAuthenticationRequiredQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ManagementMcpServerAuthenticationRequiredQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['managementMcpServerAuthenticationRequired'] : ['managementMcpServerAuthenticationRequired', variables],
    queryFn: fetcher<ManagementMcpServerAuthenticationRequiredQuery, ManagementMcpServerAuthenticationRequiredQueryVariables>(ManagementMcpServerAuthenticationRequiredDocument, variables),
    ...options
  }
    )};

export const ManagementMcpServerUrlDocument = new TypedDocumentString(`
    query managementMcpServerUrl {
  managementMcpServerUrl
}
    `);

export const useManagementMcpServerUrlQuery = <
      TData = ManagementMcpServerUrlQuery,
      TError = unknown
    >(
      variables?: ManagementMcpServerUrlQueryVariables,
      options?: Omit<UseQueryOptions<ManagementMcpServerUrlQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ManagementMcpServerUrlQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ManagementMcpServerUrlQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['managementMcpServerUrl'] : ['managementMcpServerUrl', variables],
    queryFn: fetcher<ManagementMcpServerUrlQuery, ManagementMcpServerUrlQueryVariables>(ManagementMcpServerUrlDocument, variables),
    ...options
  }
    )};

export const McpComponentsByServerIdDocument = new TypedDocumentString(`
    query mcpComponentsByServerId($mcpServerId: ID!) {
  mcpComponentsByServerId(mcpServerId: $mcpServerId) {
    id
    componentName
    componentVersion
    title
    connectionId
    lastModifiedDate
    mcpServerId
    mcpTools {
      id
      enabled
      mcpComponentId
      name
      parameters
      title
      version
    }
    requiredAuthorities
    version
  }
}
    `);

export const useMcpComponentsByServerIdQuery = <
      TData = McpComponentsByServerIdQuery,
      TError = unknown
    >(
      variables: McpComponentsByServerIdQueryVariables,
      options?: Omit<UseQueryOptions<McpComponentsByServerIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpComponentsByServerIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpComponentsByServerIdQuery, TError, TData>(
      {
    queryKey: ['mcpComponentsByServerId', variables],
    queryFn: fetcher<McpComponentsByServerIdQuery, McpComponentsByServerIdQueryVariables>(McpComponentsByServerIdDocument, variables),
    ...options
  }
    )};

export const McpToolsByComponentIdDocument = new TypedDocumentString(`
    query mcpToolsByComponentId($mcpComponentId: ID!) {
  mcpToolsByComponentId(mcpComponentId: $mcpComponentId) {
    id
    enabled
    name
    title
    mcpComponentId
    parameters
    version
  }
}
    `);

export const useMcpToolsByComponentIdQuery = <
      TData = McpToolsByComponentIdQuery,
      TError = unknown
    >(
      variables: McpToolsByComponentIdQueryVariables,
      options?: Omit<UseQueryOptions<McpToolsByComponentIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpToolsByComponentIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpToolsByComponentIdQuery, TError, TData>(
      {
    queryKey: ['mcpToolsByComponentId', variables],
    queryFn: fetcher<McpToolsByComponentIdQuery, McpToolsByComponentIdQueryVariables>(McpToolsByComponentIdDocument, variables),
    ...options
  }
    )};

export const SaveClusterElementTestConfigurationConnectionDocument = new TypedDocumentString(`
    mutation saveClusterElementTestConfigurationConnection($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $workflowConnectionKey: String!, $connectionId: Long!, $environmentId: Long!) {
  saveClusterElementTestConfigurationConnection(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    workflowConnectionKey: $workflowConnectionKey
    connectionId: $connectionId
    environmentId: $environmentId
  )
}
    `);

export const useSaveClusterElementTestConfigurationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SaveClusterElementTestConfigurationConnectionMutation, TError, SaveClusterElementTestConfigurationConnectionMutationVariables, TContext>) => {
    
    return useMutation<SaveClusterElementTestConfigurationConnectionMutation, TError, SaveClusterElementTestConfigurationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['saveClusterElementTestConfigurationConnection'],
    mutationFn: (variables?: SaveClusterElementTestConfigurationConnectionMutationVariables) => fetcher<SaveClusterElementTestConfigurationConnectionMutation, SaveClusterElementTestConfigurationConnectionMutationVariables>(SaveClusterElementTestConfigurationConnectionDocument, variables)(),
    ...options
  }
    )};

export const SaveClusterElementTestOutputDocument = new TypedDocumentString(`
    mutation saveClusterElementTestOutput($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $environmentId: Long!, $inputParameters: Map) {
  saveClusterElementTestOutput(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    environmentId: $environmentId
    inputParameters: $inputParameters
  ) {
    id
    workflowId
    workflowNodeName
  }
}
    `);

export const useSaveClusterElementTestOutputMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SaveClusterElementTestOutputMutation, TError, SaveClusterElementTestOutputMutationVariables, TContext>) => {
    
    return useMutation<SaveClusterElementTestOutputMutation, TError, SaveClusterElementTestOutputMutationVariables, TContext>(
      {
    mutationKey: ['saveClusterElementTestOutput'],
    mutationFn: (variables?: SaveClusterElementTestOutputMutationVariables) => fetcher<SaveClusterElementTestOutputMutation, SaveClusterElementTestOutputMutationVariables>(SaveClusterElementTestOutputDocument, variables)(),
    ...options
  }
    )};

export const SaveWorkflowTestConfigurationConnectionDocument = new TypedDocumentString(`
    mutation saveWorkflowTestConfigurationConnection($workflowId: String!, $workflowNodeName: String!, $workflowConnectionKey: String!, $connectionId: Long!, $environmentId: Long!) {
  saveWorkflowTestConfigurationConnection(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    workflowConnectionKey: $workflowConnectionKey
    connectionId: $connectionId
    environmentId: $environmentId
  )
}
    `);

export const useSaveWorkflowTestConfigurationConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<SaveWorkflowTestConfigurationConnectionMutation, TError, SaveWorkflowTestConfigurationConnectionMutationVariables, TContext>) => {
    
    return useMutation<SaveWorkflowTestConfigurationConnectionMutation, TError, SaveWorkflowTestConfigurationConnectionMutationVariables, TContext>(
      {
    mutationKey: ['saveWorkflowTestConfigurationConnection'],
    mutationFn: (variables?: SaveWorkflowTestConfigurationConnectionMutationVariables) => fetcher<SaveWorkflowTestConfigurationConnectionMutation, SaveWorkflowTestConfigurationConnectionMutationVariables>(SaveWorkflowTestConfigurationConnectionDocument, variables)(),
    ...options
  }
    )};

export const TestClusterElementScriptDocument = new TypedDocumentString(`
    mutation testClusterElementScript($workflowId: String!, $workflowNodeName: String!, $clusterElementType: String!, $clusterElementWorkflowNodeName: String!, $environmentId: Long!, $inputParameters: Map) {
  testClusterElementScript(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    clusterElementType: $clusterElementType
    clusterElementWorkflowNodeName: $clusterElementWorkflowNodeName
    environmentId: $environmentId
    inputParameters: $inputParameters
  ) {
    error {
      message
      stackTrace
    }
    output
  }
}
    `);

export const useTestClusterElementScriptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestClusterElementScriptMutation, TError, TestClusterElementScriptMutationVariables, TContext>) => {
    
    return useMutation<TestClusterElementScriptMutation, TError, TestClusterElementScriptMutationVariables, TContext>(
      {
    mutationKey: ['testClusterElementScript'],
    mutationFn: (variables?: TestClusterElementScriptMutationVariables) => fetcher<TestClusterElementScriptMutation, TestClusterElementScriptMutationVariables>(TestClusterElementScriptDocument, variables)(),
    ...options
  }
    )};

export const TestWorkflowNodeScriptDocument = new TypedDocumentString(`
    mutation testWorkflowNodeScript($workflowId: String!, $workflowNodeName: String!, $environmentId: Long!, $inputParameters: Map) {
  testWorkflowNodeScript(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    environmentId: $environmentId
    inputParameters: $inputParameters
  ) {
    error {
      message
      stackTrace
    }
    output
  }
}
    `);

export const useTestWorkflowNodeScriptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<TestWorkflowNodeScriptMutation, TError, TestWorkflowNodeScriptMutationVariables, TContext>) => {
    
    return useMutation<TestWorkflowNodeScriptMutation, TError, TestWorkflowNodeScriptMutationVariables, TContext>(
      {
    mutationKey: ['testWorkflowNodeScript'],
    mutationFn: (variables?: TestWorkflowNodeScriptMutationVariables) => fetcher<TestWorkflowNodeScriptMutation, TestWorkflowNodeScriptMutationVariables>(TestWorkflowNodeScriptDocument, variables)(),
    ...options
  }
    )};

export const UpdateApiKeyDocument = new TypedDocumentString(`
    mutation updateApiKey($id: ID!, $name: String!) {
  updateApiKey(id: $id, name: $name)
}
    `);

export const useUpdateApiKeyMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateApiKeyMutation, TError, UpdateApiKeyMutationVariables, TContext>) => {
    
    return useMutation<UpdateApiKeyMutation, TError, UpdateApiKeyMutationVariables, TContext>(
      {
    mutationKey: ['updateApiKey'],
    mutationFn: (variables?: UpdateApiKeyMutationVariables) => fetcher<UpdateApiKeyMutation, UpdateApiKeyMutationVariables>(UpdateApiKeyDocument, variables)(),
    ...options
  }
    )};

export const UpdateManagementMcpServerAuthenticationRequiredDocument = new TypedDocumentString(`
    mutation updateManagementMcpServerAuthenticationRequired($authenticationRequired: Boolean!) {
  updateManagementMcpServerAuthenticationRequired(
    authenticationRequired: $authenticationRequired
  )
}
    `);

export const useUpdateManagementMcpServerAuthenticationRequiredMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateManagementMcpServerAuthenticationRequiredMutation, TError, UpdateManagementMcpServerAuthenticationRequiredMutationVariables, TContext>) => {
    
    return useMutation<UpdateManagementMcpServerAuthenticationRequiredMutation, TError, UpdateManagementMcpServerAuthenticationRequiredMutationVariables, TContext>(
      {
    mutationKey: ['updateManagementMcpServerAuthenticationRequired'],
    mutationFn: (variables?: UpdateManagementMcpServerAuthenticationRequiredMutationVariables) => fetcher<UpdateManagementMcpServerAuthenticationRequiredMutation, UpdateManagementMcpServerAuthenticationRequiredMutationVariables>(UpdateManagementMcpServerAuthenticationRequiredDocument, variables)(),
    ...options
  }
    )};

export const UpdateManagementMcpServerUrlDocument = new TypedDocumentString(`
    mutation updateManagementMcpServerUrl {
  updateManagementMcpServerUrl
}
    `);

export const useUpdateManagementMcpServerUrlMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateManagementMcpServerUrlMutation, TError, UpdateManagementMcpServerUrlMutationVariables, TContext>) => {
    
    return useMutation<UpdateManagementMcpServerUrlMutation, TError, UpdateManagementMcpServerUrlMutationVariables, TContext>(
      {
    mutationKey: ['updateManagementMcpServerUrl'],
    mutationFn: (variables?: UpdateManagementMcpServerUrlMutationVariables) => fetcher<UpdateManagementMcpServerUrlMutation, UpdateManagementMcpServerUrlMutationVariables>(UpdateManagementMcpServerUrlDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpComponentWithToolsDocument = new TypedDocumentString(`
    mutation updateMcpComponentWithTools($id: ID!, $input: McpComponentWithToolsInput!) {
  updateMcpComponentWithTools(id: $id, input: $input) {
    id
    componentName
    componentVersion
    title
    mcpServerId
    connectionId
    requiredAuthorities
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    version
  }
}
    `);

export const useUpdateMcpComponentWithToolsMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpComponentWithToolsMutation, TError, UpdateMcpComponentWithToolsMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpComponentWithToolsMutation, TError, UpdateMcpComponentWithToolsMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpComponentWithTools'],
    mutationFn: (variables?: UpdateMcpComponentWithToolsMutationVariables) => fetcher<UpdateMcpComponentWithToolsMutation, UpdateMcpComponentWithToolsMutationVariables>(UpdateMcpComponentWithToolsDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpServerUrlDocument = new TypedDocumentString(`
    mutation updateMcpServerUrl($id: ID!) {
  updateMcpServerUrl(id: $id)
}
    `);

export const useUpdateMcpServerUrlMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpServerUrlMutation, TError, UpdateMcpServerUrlMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpServerUrlMutation, TError, UpdateMcpServerUrlMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpServerUrl'],
    mutationFn: (variables?: UpdateMcpServerUrlMutationVariables) => fetcher<UpdateMcpServerUrlMutation, UpdateMcpServerUrlMutationVariables>(UpdateMcpServerUrlDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpToolDocument = new TypedDocumentString(`
    mutation updateMcpTool($id: ID!, $input: McpToolInput!) {
  updateMcpTool(id: $id, input: $input) {
    id
    name
    mcpComponentId
    parameters
    version
  }
}
    `);

export const useUpdateMcpToolMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpToolMutation, TError, UpdateMcpToolMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpToolMutation, TError, UpdateMcpToolMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpTool'],
    mutationFn: (variables?: UpdateMcpToolMutationVariables) => fetcher<UpdateMcpToolMutation, UpdateMcpToolMutationVariables>(UpdateMcpToolDocument, variables)(),
    ...options
  }
    )};

export const UpdateMcpToolEnabledDocument = new TypedDocumentString(`
    mutation updateMcpToolEnabled($id: ID!, $enabled: Boolean!) {
  updateMcpToolEnabled(id: $id, enabled: $enabled) {
    id
    enabled
  }
}
    `);

export const useUpdateMcpToolEnabledMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateMcpToolEnabledMutation, TError, UpdateMcpToolEnabledMutationVariables, TContext>) => {
    
    return useMutation<UpdateMcpToolEnabledMutation, TError, UpdateMcpToolEnabledMutationVariables, TContext>(
      {
    mutationKey: ['updateMcpToolEnabled'],
    mutationFn: (variables?: UpdateMcpToolEnabledMutationVariables) => fetcher<UpdateMcpToolEnabledMutation, UpdateMcpToolEnabledMutationVariables>(UpdateMcpToolEnabledDocument, variables)(),
    ...options
  }
    )};

export const ValidateWorkflowDocument = new TypedDocumentString(`
    query ValidateWorkflow($workflowDefinition: String!) {
  validateWorkflow(workflow: $workflowDefinition) {
    errors
    warnings
  }
}
    `);

export const useValidateWorkflowQuery = <
      TData = ValidateWorkflowQuery,
      TError = unknown
    >(
      variables: ValidateWorkflowQueryVariables,
      options?: Omit<UseQueryOptions<ValidateWorkflowQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ValidateWorkflowQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ValidateWorkflowQuery, TError, TData>(
      {
    queryKey: ['ValidateWorkflow', variables],
    queryFn: fetcher<ValidateWorkflowQuery, ValidateWorkflowQueryVariables>(ValidateWorkflowDocument, variables),
    ...options
  }
    )};

export const ValidateWorkflowByIdDocument = new TypedDocumentString(`
    query ValidateWorkflowById($workflowId: String!) {
  validateWorkflowById(workflowId: $workflowId) {
    errors
    warnings
  }
}
    `);

export const useValidateWorkflowByIdQuery = <
      TData = ValidateWorkflowByIdQuery,
      TError = unknown
    >(
      variables: ValidateWorkflowByIdQueryVariables,
      options?: Omit<UseQueryOptions<ValidateWorkflowByIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ValidateWorkflowByIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ValidateWorkflowByIdQuery, TError, TData>(
      {
    queryKey: ['ValidateWorkflowById', variables],
    queryFn: fetcher<ValidateWorkflowByIdQuery, ValidateWorkflowByIdQueryVariables>(ValidateWorkflowByIdDocument, variables),
    ...options
  }
    )};

export const WorkflowNodeComponentConnectionsDocument = new TypedDocumentString(`
    query workflowNodeComponentConnections($workflowId: String!, $workflowNodeName: String!) {
  workflowNodeComponentConnections(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
  ) {
    componentName
    componentVersion
    key
    required
    workflowNodeName
  }
}
    `);

export const useWorkflowNodeComponentConnectionsQuery = <
      TData = WorkflowNodeComponentConnectionsQuery,
      TError = unknown
    >(
      variables: WorkflowNodeComponentConnectionsQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowNodeComponentConnectionsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowNodeComponentConnectionsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowNodeComponentConnectionsQuery, TError, TData>(
      {
    queryKey: ['workflowNodeComponentConnections', variables],
    queryFn: fetcher<WorkflowNodeComponentConnectionsQuery, WorkflowNodeComponentConnectionsQueryVariables>(WorkflowNodeComponentConnectionsDocument, variables),
    ...options
  }
    )};

export const WorkflowNodeMissingRequiredPropertiesDocument = new TypedDocumentString(`
    query WorkflowNodeMissingRequiredProperties($workflowId: String!, $workflowNodeName: String!) {
  workflowNodeMissingRequiredProperties(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
  )
}
    `);

export const useWorkflowNodeMissingRequiredPropertiesQuery = <
      TData = WorkflowNodeMissingRequiredPropertiesQuery,
      TError = unknown
    >(
      variables: WorkflowNodeMissingRequiredPropertiesQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowNodeMissingRequiredPropertiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowNodeMissingRequiredPropertiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowNodeMissingRequiredPropertiesQuery, TError, TData>(
      {
    queryKey: ['WorkflowNodeMissingRequiredProperties', variables],
    queryFn: fetcher<WorkflowNodeMissingRequiredPropertiesQuery, WorkflowNodeMissingRequiredPropertiesQueryVariables>(WorkflowNodeMissingRequiredPropertiesDocument, variables),
    ...options
  }
    )};

export const WorkflowNodeScriptInputDocument = new TypedDocumentString(`
    query workflowNodeScriptInput($workflowId: String!, $workflowNodeName: String!, $environmentId: Long!) {
  workflowNodeScriptInput(
    workflowId: $workflowId
    workflowNodeName: $workflowNodeName
    environmentId: $environmentId
  )
}
    `);

export const useWorkflowNodeScriptInputQuery = <
      TData = WorkflowNodeScriptInputQuery,
      TError = unknown
    >(
      variables: WorkflowNodeScriptInputQueryVariables,
      options?: Omit<UseQueryOptions<WorkflowNodeScriptInputQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkflowNodeScriptInputQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkflowNodeScriptInputQuery, TError, TData>(
      {
    queryKey: ['workflowNodeScriptInput', variables],
    queryFn: fetcher<WorkflowNodeScriptInputQuery, WorkflowNodeScriptInputQueryVariables>(WorkflowNodeScriptInputDocument, variables),
    ...options
  }
    )};

export const ConnectionCredentialStoresDocument = new TypedDocumentString(`
    query ConnectionCredentialStores {
  connectionCredentialStores {
    type
    readOnly
  }
}
    `);

export const useConnectionCredentialStoresQuery = <
      TData = ConnectionCredentialStoresQuery,
      TError = unknown
    >(
      variables?: ConnectionCredentialStoresQueryVariables,
      options?: Omit<UseQueryOptions<ConnectionCredentialStoresQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ConnectionCredentialStoresQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ConnectionCredentialStoresQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['ConnectionCredentialStores'] : ['ConnectionCredentialStores', variables],
    queryFn: fetcher<ConnectionCredentialStoresQuery, ConnectionCredentialStoresQueryVariables>(ConnectionCredentialStoresDocument, variables),
    ...options
  }
    )};

export const RegisterExistingConnectionDocument = new TypedDocumentString(`
    mutation RegisterExistingConnection($input: RegisterExistingConnectionInput!) {
  registerExistingConnection(input: $input)
}
    `);

export const useRegisterExistingConnectionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<RegisterExistingConnectionMutation, TError, RegisterExistingConnectionMutationVariables, TContext>) => {
    
    return useMutation<RegisterExistingConnectionMutation, TError, RegisterExistingConnectionMutationVariables, TContext>(
      {
    mutationKey: ['RegisterExistingConnection'],
    mutationFn: (variables?: RegisterExistingConnectionMutationVariables) => fetcher<RegisterExistingConnectionMutation, RegisterExistingConnectionMutationVariables>(RegisterExistingConnectionDocument, variables)(),
    ...options
  }
    )};

export const GeneratePropertyValueDocument = new TypedDocumentString(`
    mutation generatePropertyValue($input: GeneratePropertyValueInput!) {
  generatePropertyValue(input: $input) {
    value
    valid
    message
  }
}
    `);

export const useGeneratePropertyValueMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<GeneratePropertyValueMutation, TError, GeneratePropertyValueMutationVariables, TContext>) => {
    
    return useMutation<GeneratePropertyValueMutation, TError, GeneratePropertyValueMutationVariables, TContext>(
      {
    mutationKey: ['generatePropertyValue'],
    mutationFn: (variables?: GeneratePropertyValueMutationVariables) => fetcher<GeneratePropertyValueMutation, GeneratePropertyValueMutationVariables>(GeneratePropertyValueDocument, variables)(),
    ...options
  }
    )};

export const GenerateWorkflowDescriptionDocument = new TypedDocumentString(`
    mutation generateWorkflowDescription($input: GenerateWorkflowDescriptionInput!) {
  generateWorkflowDescription(input: $input) {
    value
  }
}
    `);

export const useGenerateWorkflowDescriptionMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<GenerateWorkflowDescriptionMutation, TError, GenerateWorkflowDescriptionMutationVariables, TContext>) => {
    
    return useMutation<GenerateWorkflowDescriptionMutation, TError, GenerateWorkflowDescriptionMutationVariables, TContext>(
      {
    mutationKey: ['generateWorkflowDescription'],
    mutationFn: (variables?: GenerateWorkflowDescriptionMutationVariables) => fetcher<GenerateWorkflowDescriptionMutation, GenerateWorkflowDescriptionMutationVariables>(GenerateWorkflowDescriptionDocument, variables)(),
    ...options
  }
    )};

export const CreateCustomComponentDocument = new TypedDocumentString(`
    mutation createCustomComponent($name: String!, $language: CustomComponentLanguage!) {
  createCustomComponent(name: $name, language: $language) {
    id
    name
    language
  }
}
    `);

export const useCreateCustomComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateCustomComponentMutation, TError, CreateCustomComponentMutationVariables, TContext>) => {
    
    return useMutation<CreateCustomComponentMutation, TError, CreateCustomComponentMutationVariables, TContext>(
      {
    mutationKey: ['createCustomComponent'],
    mutationFn: (variables?: CreateCustomComponentMutationVariables) => fetcher<CreateCustomComponentMutation, CreateCustomComponentMutationVariables>(CreateCustomComponentDocument, variables)(),
    ...options
  }
    )};

export const CustomComponentDocument = new TypedDocumentString(`
    query customComponent($id: ID!) {
  customComponent(id: $id) {
    id
    name
    title
    description
    icon
    componentVersion
    enabled
    language
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    status
    publishedDate
    version
  }
}
    `);

export const useCustomComponentQuery = <
      TData = CustomComponentQuery,
      TError = unknown
    >(
      variables: CustomComponentQueryVariables,
      options?: Omit<UseQueryOptions<CustomComponentQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CustomComponentQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CustomComponentQuery, TError, TData>(
      {
    queryKey: ['customComponent', variables],
    queryFn: fetcher<CustomComponentQuery, CustomComponentQueryVariables>(CustomComponentDocument, variables),
    ...options
  }
    )};

export const CustomComponentDefinitionDocument = new TypedDocumentString(`
    query customComponentDefinition($id: ID!) {
  customComponentDefinition(id: $id) {
    actions {
      name
      title
      description
    }
    triggers {
      name
      title
      description
    }
  }
}
    `);

export const useCustomComponentDefinitionQuery = <
      TData = CustomComponentDefinitionQuery,
      TError = unknown
    >(
      variables: CustomComponentDefinitionQueryVariables,
      options?: Omit<UseQueryOptions<CustomComponentDefinitionQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CustomComponentDefinitionQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CustomComponentDefinitionQuery, TError, TData>(
      {
    queryKey: ['customComponentDefinition', variables],
    queryFn: fetcher<CustomComponentDefinitionQuery, CustomComponentDefinitionQueryVariables>(CustomComponentDefinitionDocument, variables),
    ...options
  }
    )};

export const CustomComponentSourceDocument = new TypedDocumentString(`
    query customComponentSource($id: ID!) {
  customComponentSource(id: $id)
}
    `);

export const useCustomComponentSourceQuery = <
      TData = CustomComponentSourceQuery,
      TError = unknown
    >(
      variables: CustomComponentSourceQueryVariables,
      options?: Omit<UseQueryOptions<CustomComponentSourceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CustomComponentSourceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CustomComponentSourceQuery, TError, TData>(
      {
    queryKey: ['customComponentSource', variables],
    queryFn: fetcher<CustomComponentSourceQuery, CustomComponentSourceQueryVariables>(CustomComponentSourceDocument, variables),
    ...options
  }
    )};

export const CustomComponentsDocument = new TypedDocumentString(`
    query customComponents {
  customComponents {
    id
    name
    title
    description
    icon
    componentVersion
    enabled
    language
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
    status
    publishedDate
    version
  }
}
    `);

export const useCustomComponentsQuery = <
      TData = CustomComponentsQuery,
      TError = unknown
    >(
      variables?: CustomComponentsQueryVariables,
      options?: Omit<UseQueryOptions<CustomComponentsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<CustomComponentsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<CustomComponentsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['customComponents'] : ['customComponents', variables],
    queryFn: fetcher<CustomComponentsQuery, CustomComponentsQueryVariables>(CustomComponentsDocument, variables),
    ...options
  }
    )};

export const DeleteCustomComponentDocument = new TypedDocumentString(`
    mutation deleteCustomComponent($id: ID!) {
  deleteCustomComponent(id: $id)
}
    `);

export const useDeleteCustomComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteCustomComponentMutation, TError, DeleteCustomComponentMutationVariables, TContext>) => {
    
    return useMutation<DeleteCustomComponentMutation, TError, DeleteCustomComponentMutationVariables, TContext>(
      {
    mutationKey: ['deleteCustomComponent'],
    mutationFn: (variables?: DeleteCustomComponentMutationVariables) => fetcher<DeleteCustomComponentMutation, DeleteCustomComponentMutationVariables>(DeleteCustomComponentDocument, variables)(),
    ...options
  }
    )};

export const EnableCustomComponentDocument = new TypedDocumentString(`
    mutation enableCustomComponent($id: ID!, $enable: Boolean!) {
  enableCustomComponent(id: $id, enable: $enable)
}
    `);

export const useEnableCustomComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<EnableCustomComponentMutation, TError, EnableCustomComponentMutationVariables, TContext>) => {
    
    return useMutation<EnableCustomComponentMutation, TError, EnableCustomComponentMutationVariables, TContext>(
      {
    mutationKey: ['enableCustomComponent'],
    mutationFn: (variables?: EnableCustomComponentMutationVariables) => fetcher<EnableCustomComponentMutation, EnableCustomComponentMutationVariables>(EnableCustomComponentDocument, variables)(),
    ...options
  }
    )};

export const PublishCustomComponentDocument = new TypedDocumentString(`
    mutation publishCustomComponent($id: ID!) {
  publishCustomComponent(id: $id) {
    id
    publishedDate
    status
  }
}
    `);

export const usePublishCustomComponentMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<PublishCustomComponentMutation, TError, PublishCustomComponentMutationVariables, TContext>) => {
    
    return useMutation<PublishCustomComponentMutation, TError, PublishCustomComponentMutationVariables, TContext>(
      {
    mutationKey: ['publishCustomComponent'],
    mutationFn: (variables?: PublishCustomComponentMutationVariables) => fetcher<PublishCustomComponentMutation, PublishCustomComponentMutationVariables>(PublishCustomComponentDocument, variables)(),
    ...options
  }
    )};

export const UpdateCustomComponentSourceDocument = new TypedDocumentString(`
    mutation updateCustomComponentSource($id: ID!, $content: String!) {
  updateCustomComponentSource(id: $id, content: $content) {
    id
    componentVersion
    status
  }
}
    `);

export const useUpdateCustomComponentSourceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateCustomComponentSourceMutation, TError, UpdateCustomComponentSourceMutationVariables, TContext>) => {
    
    return useMutation<UpdateCustomComponentSourceMutation, TError, UpdateCustomComponentSourceMutationVariables, TContext>(
      {
    mutationKey: ['updateCustomComponentSource'],
    mutationFn: (variables?: UpdateCustomComponentSourceMutationVariables) => fetcher<UpdateCustomComponentSourceMutation, UpdateCustomComponentSourceMutationVariables>(UpdateCustomComponentSourceDocument, variables)(),
    ...options
  }
    )};

export const DeleteLicenceDocument = new TypedDocumentString(`
    mutation deleteLicence {
  deleteLicence
}
    `);

export const useDeleteLicenceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteLicenceMutation, TError, DeleteLicenceMutationVariables, TContext>) => {
    
    return useMutation<DeleteLicenceMutation, TError, DeleteLicenceMutationVariables, TContext>(
      {
    mutationKey: ['deleteLicence'],
    mutationFn: (variables?: DeleteLicenceMutationVariables) => fetcher<DeleteLicenceMutation, DeleteLicenceMutationVariables>(DeleteLicenceDocument, variables)(),
    ...options
  }
    )};

export const LicenceDocument = new TypedDocumentString(`
    query licence {
  licence {
    allowedJobs
    currentMonthJobUsage
    expiresAt
    features
    holderEmail
    holderName
    id
    issuedAt
    maxUsers
    status
  }
}
    `);

export const useLicenceQuery = <
      TData = LicenceQuery,
      TError = unknown
    >(
      variables?: LicenceQueryVariables,
      options?: Omit<UseQueryOptions<LicenceQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<LicenceQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<LicenceQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['licence'] : ['licence', variables],
    queryFn: fetcher<LicenceQuery, LicenceQueryVariables>(LicenceDocument, variables),
    ...options
  }
    )};

export const UploadLicenceDocument = new TypedDocumentString(`
    mutation uploadLicence($contents: String!) {
  uploadLicence(contents: $contents) {
    allowedJobs
    currentMonthJobUsage
    expiresAt
    features
    holderEmail
    holderName
    id
    issuedAt
    maxUsers
    status
  }
}
    `);

export const useUploadLicenceMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UploadLicenceMutation, TError, UploadLicenceMutationVariables, TContext>) => {
    
    return useMutation<UploadLicenceMutation, TError, UploadLicenceMutationVariables, TContext>(
      {
    mutationKey: ['uploadLicence'],
    mutationFn: (variables?: UploadLicenceMutationVariables) => fetcher<UploadLicenceMutation, UploadLicenceMutationVariables>(UploadLicenceDocument, variables)(),
    ...options
  }
    )};

export const DeleteRegisteredClientDocument = new TypedDocumentString(`
    mutation deleteRegisteredClient($id: ID!) {
  deleteRegisteredClient(id: $id)
}
    `);

export const useDeleteRegisteredClientMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteRegisteredClientMutation, TError, DeleteRegisteredClientMutationVariables, TContext>) => {
    
    return useMutation<DeleteRegisteredClientMutation, TError, DeleteRegisteredClientMutationVariables, TContext>(
      {
    mutationKey: ['deleteRegisteredClient'],
    mutationFn: (variables?: DeleteRegisteredClientMutationVariables) => fetcher<DeleteRegisteredClientMutation, DeleteRegisteredClientMutationVariables>(DeleteRegisteredClientDocument, variables)(),
    ...options
  }
    )};

export const RegisteredClientsDocument = new TypedDocumentString(`
    query registeredClients {
  registeredClients {
    id
    clientId
    clientName
    clientIdIssuedAt
    scopes
    authorizationGrantTypes
    redirectUris
  }
}
    `);

export const useRegisteredClientsQuery = <
      TData = RegisteredClientsQuery,
      TError = unknown
    >(
      variables?: RegisteredClientsQueryVariables,
      options?: Omit<UseQueryOptions<RegisteredClientsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<RegisteredClientsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<RegisteredClientsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['registeredClients'] : ['registeredClients', variables],
    queryFn: fetcher<RegisteredClientsQuery, RegisteredClientsQueryVariables>(RegisteredClientsDocument, variables),
    ...options
  }
    )};

export const AuthoritiesDocument = new TypedDocumentString(`
    query authorities {
  authorities
}
    `);

export const useAuthoritiesQuery = <
      TData = AuthoritiesQuery,
      TError = unknown
    >(
      variables?: AuthoritiesQueryVariables,
      options?: Omit<UseQueryOptions<AuthoritiesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<AuthoritiesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<AuthoritiesQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['authorities'] : ['authorities', variables],
    queryFn: fetcher<AuthoritiesQuery, AuthoritiesQueryVariables>(AuthoritiesDocument, variables),
    ...options
  }
    )};

export const CreateIdentityProviderDocument = new TypedDocumentString(`
    mutation createIdentityProvider($input: IdentityProviderInput!) {
  createIdentityProvider(input: $input) {
    authoritiesClaim
    authorityMappings {
      authority
      externalGroup
    }
    autoProvision
    clientId
    createdBy
    createdDate
    defaultAuthority
    domains
    enabled
    enforced
    id
    issuerUri
    lastModifiedBy
    lastModifiedDate
    mcpEmbedded
    mcpAutomation
    mcpManagement
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
    validateMcpAudience
  }
}
    `);

export const useCreateIdentityProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateIdentityProviderMutation, TError, CreateIdentityProviderMutationVariables, TContext>) => {
    
    return useMutation<CreateIdentityProviderMutation, TError, CreateIdentityProviderMutationVariables, TContext>(
      {
    mutationKey: ['createIdentityProvider'],
    mutationFn: (variables?: CreateIdentityProviderMutationVariables) => fetcher<CreateIdentityProviderMutation, CreateIdentityProviderMutationVariables>(CreateIdentityProviderDocument, variables)(),
    ...options
  }
    )};

export const DeleteIdentityProviderDocument = new TypedDocumentString(`
    mutation deleteIdentityProvider($id: ID!) {
  deleteIdentityProvider(id: $id)
}
    `);

export const useDeleteIdentityProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteIdentityProviderMutation, TError, DeleteIdentityProviderMutationVariables, TContext>) => {
    
    return useMutation<DeleteIdentityProviderMutation, TError, DeleteIdentityProviderMutationVariables, TContext>(
      {
    mutationKey: ['deleteIdentityProvider'],
    mutationFn: (variables?: DeleteIdentityProviderMutationVariables) => fetcher<DeleteIdentityProviderMutation, DeleteIdentityProviderMutationVariables>(DeleteIdentityProviderDocument, variables)(),
    ...options
  }
    )};

export const DeleteUserDocument = new TypedDocumentString(`
    mutation deleteUser($login: String!) {
  deleteUser(login: $login)
}
    `);

export const useDeleteUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteUserMutation, TError, DeleteUserMutationVariables, TContext>) => {
    
    return useMutation<DeleteUserMutation, TError, DeleteUserMutationVariables, TContext>(
      {
    mutationKey: ['deleteUser'],
    mutationFn: (variables?: DeleteUserMutationVariables) => fetcher<DeleteUserMutation, DeleteUserMutationVariables>(DeleteUserDocument, variables)(),
    ...options
  }
    )};

export const IdentityProviderDocument = new TypedDocumentString(`
    query identityProvider($id: ID!) {
  identityProvider(id: $id) {
    authoritiesClaim
    authorityMappings {
      authority
      externalGroup
    }
    autoProvision
    clientId
    createdBy
    createdDate
    defaultAuthority
    domains
    enabled
    enforced
    id
    issuerUri
    lastModifiedBy
    lastModifiedDate
    mcpEmbedded
    mcpAutomation
    mcpManagement
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
    validateMcpAudience
  }
}
    `);

export const useIdentityProviderQuery = <
      TData = IdentityProviderQuery,
      TError = unknown
    >(
      variables: IdentityProviderQueryVariables,
      options?: Omit<UseQueryOptions<IdentityProviderQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IdentityProviderQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IdentityProviderQuery, TError, TData>(
      {
    queryKey: ['identityProvider', variables],
    queryFn: fetcher<IdentityProviderQuery, IdentityProviderQueryVariables>(IdentityProviderDocument, variables),
    ...options
  }
    )};

export const IdentityProvidersDocument = new TypedDocumentString(`
    query identityProviders {
  identityProviders {
    authoritiesClaim
    authorityMappings {
      authority
      externalGroup
    }
    autoProvision
    clientId
    createdBy
    createdDate
    defaultAuthority
    domains
    enabled
    enforced
    id
    issuerUri
    lastModifiedBy
    lastModifiedDate
    mcpEmbedded
    mcpAutomation
    mcpManagement
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
    validateMcpAudience
  }
}
    `);

export const useIdentityProvidersQuery = <
      TData = IdentityProvidersQuery,
      TError = unknown
    >(
      variables?: IdentityProvidersQueryVariables,
      options?: Omit<UseQueryOptions<IdentityProvidersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IdentityProvidersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IdentityProvidersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['identityProviders'] : ['identityProviders', variables],
    queryFn: fetcher<IdentityProvidersQuery, IdentityProvidersQueryVariables>(IdentityProvidersDocument, variables),
    ...options
  }
    )};

export const InviteUserDocument = new TypedDocumentString(`
    mutation inviteUser($email: String!, $role: String!, $workspaces: [WorkspaceAssignmentInput!]) {
  inviteUser(email: $email, role: $role, workspaces: $workspaces)
}
    `);

export const useInviteUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<InviteUserMutation, TError, InviteUserMutationVariables, TContext>) => {
    
    return useMutation<InviteUserMutation, TError, InviteUserMutationVariables, TContext>(
      {
    mutationKey: ['inviteUser'],
    mutationFn: (variables?: InviteUserMutationVariables) => fetcher<InviteUserMutation, InviteUserMutationVariables>(InviteUserDocument, variables)(),
    ...options
  }
    )};

export const UpdateIdentityProviderDocument = new TypedDocumentString(`
    mutation updateIdentityProvider($id: ID!, $input: IdentityProviderInput!) {
  updateIdentityProvider(id: $id, input: $input) {
    authoritiesClaim
    authorityMappings {
      authority
      externalGroup
    }
    autoProvision
    clientId
    createdBy
    createdDate
    defaultAuthority
    domains
    enabled
    enforced
    id
    issuerUri
    lastModifiedBy
    lastModifiedDate
    mcpEmbedded
    mcpAutomation
    mcpManagement
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
    validateMcpAudience
  }
}
    `);

export const useUpdateIdentityProviderMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateIdentityProviderMutation, TError, UpdateIdentityProviderMutationVariables, TContext>) => {
    
    return useMutation<UpdateIdentityProviderMutation, TError, UpdateIdentityProviderMutationVariables, TContext>(
      {
    mutationKey: ['updateIdentityProvider'],
    mutationFn: (variables?: UpdateIdentityProviderMutationVariables) => fetcher<UpdateIdentityProviderMutation, UpdateIdentityProviderMutationVariables>(UpdateIdentityProviderDocument, variables)(),
    ...options
  }
    )};

export const UpdateUserDocument = new TypedDocumentString(`
    mutation updateUser($login: String!, $role: String!) {
  updateUser(login: $login, role: $role) {
    id
    login
    email
    firstName
    lastName
    activated
    authorities
  }
}
    `);

export const useUpdateUserMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateUserMutation, TError, UpdateUserMutationVariables, TContext>) => {
    
    return useMutation<UpdateUserMutation, TError, UpdateUserMutationVariables, TContext>(
      {
    mutationKey: ['updateUser'],
    mutationFn: (variables?: UpdateUserMutationVariables) => fetcher<UpdateUserMutation, UpdateUserMutationVariables>(UpdateUserDocument, variables)(),
    ...options
  }
    )};

export const UsersDocument = new TypedDocumentString(`
    query users($pageNumber: Int, $pageSize: Int) {
  users(pageNumber: $pageNumber, pageSize: $pageSize) {
    content {
      id
      login
      email
      firstName
      lastName
      activated
      authorities
    }
    number
    size
    totalElements
    totalPages
  }
}
    `);

export const useUsersQuery = <
      TData = UsersQuery,
      TError = unknown
    >(
      variables?: UsersQueryVariables,
      options?: Omit<UseQueryOptions<UsersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<UsersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<UsersQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['users'] : ['users', variables],
    queryFn: fetcher<UsersQuery, UsersQueryVariables>(UsersDocument, variables),
    ...options
  }
    )};

export const CreateEmbeddedVariableDocument = new TypedDocumentString(`
    mutation createEmbeddedVariable($environmentId: ID!, $input: VariableInput!) {
  createEmbeddedVariable(environmentId: $environmentId, input: $input) {
    id
    name
    value
  }
}
    `);

export const useCreateEmbeddedVariableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateEmbeddedVariableMutation, TError, CreateEmbeddedVariableMutationVariables, TContext>) => {
    
    return useMutation<CreateEmbeddedVariableMutation, TError, CreateEmbeddedVariableMutationVariables, TContext>(
      {
    mutationKey: ['createEmbeddedVariable'],
    mutationFn: (variables?: CreateEmbeddedVariableMutationVariables) => fetcher<CreateEmbeddedVariableMutation, CreateEmbeddedVariableMutationVariables>(CreateEmbeddedVariableDocument, variables)(),
    ...options
  }
    )};

export const CreateWorkspaceVariableDocument = new TypedDocumentString(`
    mutation createWorkspaceVariable($workspaceId: ID!, $environmentId: ID!, $input: VariableInput!) {
  createWorkspaceVariable(
    workspaceId: $workspaceId
    environmentId: $environmentId
    input: $input
  ) {
    id
    name
    value
  }
}
    `);

export const useCreateWorkspaceVariableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateWorkspaceVariableMutation, TError, CreateWorkspaceVariableMutationVariables, TContext>) => {
    
    return useMutation<CreateWorkspaceVariableMutation, TError, CreateWorkspaceVariableMutationVariables, TContext>(
      {
    mutationKey: ['createWorkspaceVariable'],
    mutationFn: (variables?: CreateWorkspaceVariableMutationVariables) => fetcher<CreateWorkspaceVariableMutation, CreateWorkspaceVariableMutationVariables>(CreateWorkspaceVariableDocument, variables)(),
    ...options
  }
    )};

export const DeleteEmbeddedVariableDocument = new TypedDocumentString(`
    mutation deleteEmbeddedVariable($environmentId: ID!, $id: ID!) {
  deleteEmbeddedVariable(environmentId: $environmentId, id: $id)
}
    `);

export const useDeleteEmbeddedVariableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteEmbeddedVariableMutation, TError, DeleteEmbeddedVariableMutationVariables, TContext>) => {
    
    return useMutation<DeleteEmbeddedVariableMutation, TError, DeleteEmbeddedVariableMutationVariables, TContext>(
      {
    mutationKey: ['deleteEmbeddedVariable'],
    mutationFn: (variables?: DeleteEmbeddedVariableMutationVariables) => fetcher<DeleteEmbeddedVariableMutation, DeleteEmbeddedVariableMutationVariables>(DeleteEmbeddedVariableDocument, variables)(),
    ...options
  }
    )};

export const DeleteWorkspaceVariableDocument = new TypedDocumentString(`
    mutation deleteWorkspaceVariable($workspaceId: ID!, $environmentId: ID!, $id: ID!) {
  deleteWorkspaceVariable(
    workspaceId: $workspaceId
    environmentId: $environmentId
    id: $id
  )
}
    `);

export const useDeleteWorkspaceVariableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<DeleteWorkspaceVariableMutation, TError, DeleteWorkspaceVariableMutationVariables, TContext>) => {
    
    return useMutation<DeleteWorkspaceVariableMutation, TError, DeleteWorkspaceVariableMutationVariables, TContext>(
      {
    mutationKey: ['deleteWorkspaceVariable'],
    mutationFn: (variables?: DeleteWorkspaceVariableMutationVariables) => fetcher<DeleteWorkspaceVariableMutation, DeleteWorkspaceVariableMutationVariables>(DeleteWorkspaceVariableDocument, variables)(),
    ...options
  }
    )};

export const EmbeddedVariablesDocument = new TypedDocumentString(`
    query embeddedVariables($environmentId: ID!) {
  embeddedVariables(environmentId: $environmentId) {
    id
    name
    value
    environmentId
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useEmbeddedVariablesQuery = <
      TData = EmbeddedVariablesQuery,
      TError = unknown
    >(
      variables: EmbeddedVariablesQueryVariables,
      options?: Omit<UseQueryOptions<EmbeddedVariablesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<EmbeddedVariablesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<EmbeddedVariablesQuery, TError, TData>(
      {
    queryKey: ['embeddedVariables', variables],
    queryFn: fetcher<EmbeddedVariablesQuery, EmbeddedVariablesQueryVariables>(EmbeddedVariablesDocument, variables),
    ...options
  }
    )};

export const UpdateEmbeddedVariableDocument = new TypedDocumentString(`
    mutation updateEmbeddedVariable($environmentId: ID!, $id: ID!, $input: VariableInput!) {
  updateEmbeddedVariable(environmentId: $environmentId, id: $id, input: $input) {
    id
    name
    value
  }
}
    `);

export const useUpdateEmbeddedVariableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateEmbeddedVariableMutation, TError, UpdateEmbeddedVariableMutationVariables, TContext>) => {
    
    return useMutation<UpdateEmbeddedVariableMutation, TError, UpdateEmbeddedVariableMutationVariables, TContext>(
      {
    mutationKey: ['updateEmbeddedVariable'],
    mutationFn: (variables?: UpdateEmbeddedVariableMutationVariables) => fetcher<UpdateEmbeddedVariableMutation, UpdateEmbeddedVariableMutationVariables>(UpdateEmbeddedVariableDocument, variables)(),
    ...options
  }
    )};

export const UpdateWorkspaceVariableDocument = new TypedDocumentString(`
    mutation updateWorkspaceVariable($workspaceId: ID!, $environmentId: ID!, $id: ID!, $input: VariableInput!) {
  updateWorkspaceVariable(
    workspaceId: $workspaceId
    environmentId: $environmentId
    id: $id
    input: $input
  ) {
    id
    name
    value
  }
}
    `);

export const useUpdateWorkspaceVariableMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceVariableMutation, TError, UpdateWorkspaceVariableMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceVariableMutation, TError, UpdateWorkspaceVariableMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceVariable'],
    mutationFn: (variables?: UpdateWorkspaceVariableMutationVariables) => fetcher<UpdateWorkspaceVariableMutation, UpdateWorkspaceVariableMutationVariables>(UpdateWorkspaceVariableDocument, variables)(),
    ...options
  }
    )};

export const WorkspaceVariablesDocument = new TypedDocumentString(`
    query workspaceVariables($workspaceId: ID!, $environmentId: ID!) {
  workspaceVariables(workspaceId: $workspaceId, environmentId: $environmentId) {
    id
    name
    value
    environmentId
    createdBy
    createdDate
    lastModifiedBy
    lastModifiedDate
  }
}
    `);

export const useWorkspaceVariablesQuery = <
      TData = WorkspaceVariablesQuery,
      TError = unknown
    >(
      variables: WorkspaceVariablesQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceVariablesQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceVariablesQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceVariablesQuery, TError, TData>(
      {
    queryKey: ['workspaceVariables', variables],
    queryFn: fetcher<WorkspaceVariablesQuery, WorkspaceVariablesQueryVariables>(WorkspaceVariablesDocument, variables),
    ...options
  }
    )};

export const WorkspaceNotificationsDocument = new TypedDocumentString(`
    query workspaceNotifications($workspaceId: ID!) {
  workspaceNotifications(workspaceId: $workspaceId) {
    id
    name
    type
  }
}
    `);

export const useWorkspaceNotificationsQuery = <
      TData = WorkspaceNotificationsQuery,
      TError = unknown
    >(
      variables: WorkspaceNotificationsQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceNotificationsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceNotificationsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceNotificationsQuery, TError, TData>(
      {
    queryKey: ['workspaceNotifications', variables],
    queryFn: fetcher<WorkspaceNotificationsQuery, WorkspaceNotificationsQueryVariables>(WorkspaceNotificationsDocument, variables),
    ...options
  }
    )};

export const WorkspaceSystemPromptDocument = new TypedDocumentString(`
    query workspaceSystemPrompt($workspaceId: ID!) {
  workspaceSystemPrompt(workspaceId: $workspaceId) {
    prompt
    workspaceId
  }
}
    `);

export const useWorkspaceSystemPromptQuery = <
      TData = WorkspaceSystemPromptQuery,
      TError = unknown
    >(
      variables: WorkspaceSystemPromptQueryVariables,
      options?: Omit<UseQueryOptions<WorkspaceSystemPromptQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<WorkspaceSystemPromptQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<WorkspaceSystemPromptQuery, TError, TData>(
      {
    queryKey: ['workspaceSystemPrompt', variables],
    queryFn: fetcher<WorkspaceSystemPromptQuery, WorkspaceSystemPromptQueryVariables>(WorkspaceSystemPromptDocument, variables),
    ...options
  }
    )};

export const UpdateWorkspaceSystemPromptDocument = new TypedDocumentString(`
    mutation updateWorkspaceSystemPrompt($input: WorkspaceSystemPromptInput!) {
  updateWorkspaceSystemPrompt(input: $input) {
    prompt
    workspaceId
  }
}
    `);

export const useUpdateWorkspaceSystemPromptMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<UpdateWorkspaceSystemPromptMutation, TError, UpdateWorkspaceSystemPromptMutationVariables, TContext>) => {
    
    return useMutation<UpdateWorkspaceSystemPromptMutation, TError, UpdateWorkspaceSystemPromptMutationVariables, TContext>(
      {
    mutationKey: ['updateWorkspaceSystemPrompt'],
    mutationFn: (variables?: UpdateWorkspaceSystemPromptMutationVariables) => fetcher<UpdateWorkspaceSystemPromptMutation, UpdateWorkspaceSystemPromptMutationVariables>(UpdateWorkspaceSystemPromptDocument, variables)(),
    ...options
  }
    )};

export const ToolInvocationLogsDocument = new TypedDocumentString(`
    query ToolInvocationLogs($surface: String, $outcome: String, $mcpServerId: Long, $connectedUserId: Long, $integrationInstanceId: Long, $fromDate: Long, $toDate: Long, $page: Int) {
  toolInvocationLogs(
    surface: $surface
    outcome: $outcome
    mcpServerId: $mcpServerId
    connectedUserId: $connectedUserId
    integrationInstanceId: $integrationInstanceId
    fromDate: $fromDate
    toDate: $toDate
    page: $page
  ) {
    content {
      id
      surface
      kind
      toolName
      componentName
      componentVersion
      operationName
      connectionId
      environment
      externalUserId
      connectedUserId
      integrationInstanceId
      mcpServerId
      jobId
      outcome
      errorType
      errorMessage
      durationMs
      createdDate
    }
    number
    size
    totalElements
    totalPages
  }
}
    `);

export const useToolInvocationLogsQuery = <
      TData = ToolInvocationLogsQuery,
      TError = unknown
    >(
      variables?: ToolInvocationLogsQueryVariables,
      options?: Omit<UseQueryOptions<ToolInvocationLogsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<ToolInvocationLogsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<ToolInvocationLogsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['ToolInvocationLogs'] : ['ToolInvocationLogs', variables],
    queryFn: fetcher<ToolInvocationLogsQuery, ToolInvocationLogsQueryVariables>(ToolInvocationLogsDocument, variables),
    ...options
  }
    )};
