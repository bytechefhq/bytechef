/** Internal type. DO NOT USE DIRECTLY. */
type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
/** Internal type. DO NOT USE DIRECTLY. */
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
import type * as Types from './graphql-types';

import { useQuery, useMutation, UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import { fetcher } from './graphqlFetcher';
export * from './graphql-types';
import type {KnowledgeBaseDocument as KnowledgeBaseDocumentSchemaType} from './graphql-types';
export type KnowledgeBaseDocument = KnowledgeBaseDocumentSchemaType;
export class TypedDocumentString<TResult, TVariables> extends String {
  __apiType?: { result: TResult; variables: TVariables };
  __meta__?: Record<string, unknown>;

  constructor(private value: string, __meta__?: Record<string, unknown>) {
    super(value);
    this.__meta__ = __meta__;
  }

  override toString(): string {
    return this.value;
  }
}
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

export type AiSkillsQueryVariables = Exact<{ [key: string]: never; }>;


export type AiSkillsQuery = { aiSkills: Array<{ id: string, name: string, description: string | null, createdDate: any, lastModifiedDate: any }> };

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

export type UpdateApprovalTaskMutationVariables = Exact<{
  approvalTask: Types.ApprovalTaskInput;
}>;


export type UpdateApprovalTaskMutation = { updateApprovalTask: { assigneeId: string | null, description: string | null, dueDate: string | null, id: string, name: string, priority: Types.ApprovalTaskPriority, status: Types.ApprovalTaskStatus, version: number } | null };

export type CreateMcpProjectMutationVariables = Exact<{
  input: Types.CreateMcpProjectInput;
}>;


export type CreateMcpProjectMutation = { createMcpProject: { id: string, mcpServerId: string, projectDeploymentId: string, projectVersion: number | null } | null };

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

export type DeleteMcpProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteMcpProjectMutation = { deleteMcpProject: boolean | null };

export type DeleteMcpProjectWorkflowMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteMcpProjectWorkflowMutation = { deleteMcpProjectWorkflow: boolean | null };

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

export type McpProjectsQueryVariables = Exact<{ [key: string]: never; }>;


export type McpProjectsQuery = { mcpProjects: Array<{ id: string, mcpServerId: string, project: { id: string, name: string } | null } | null> | null };

export type McpProjectsByServerIdQueryVariables = Exact<{
  mcpServerId: string | number;
}>;


export type McpProjectsByServerIdQuery = { mcpProjectsByServerId: Array<{ id: string, projectDeploymentId: string, mcpServerId: string, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, projectVersion: number | null, project: { id: string, name: string, category: { id: string | null, name: string | null } | null, tags: Array<{ id: string, name: string } | null> | null } | null, mcpProjectWorkflows: Array<{ id: string, mcpProjectId: any, projectDeploymentWorkflowId: any, parameters: any, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, projectDeploymentWorkflow: { id: string, enabled: boolean, inputs: any, projectDeploymentId: string, version: number, workflowId: string, connections: Array<{ connectionId: string | null, workflowConnectionKey: string, workflowNodeName: string }> } | null, workflow: { id: string, label: string } | null } | null> | null } | null> | null };

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

export type ProjectTemplateQueryVariables = Exact<{
  id: string;
  sharedProject: boolean;
}>;


export type ProjectTemplateQuery = { projectTemplate: { description: string | null, projectVersion: number | null, publicUrl: string | null, components: Array<{ key: string | null, value: Array<{ icon: string | null, name: string, title: string | null, version: number | null, connection: { componentName: string, version: number } | null } | null> }>, project: { name: string } | null, workflows: Array<{ id: string, label: string }> } | null };

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


export type UpdateMcpServerMutation = { updateMcpServer: { id: string, name: string, enabled: boolean } | null };

export type UpdateMcpServerTagsMutationVariables = Exact<{
  id: string | number;
  tags: Array<Types.TagInput> | Types.TagInput;
}>;


export type UpdateMcpServerTagsMutation = { updateMcpServerTags: Array<{ id: string } | null> | null };

export type UpdateWorkspaceApiKeyMutationVariables = Exact<{
  apiKeyId: string | number;
  name: string;
}>;


export type UpdateWorkspaceApiKeyMutation = { updateWorkspaceApiKey: boolean };

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


export type WorkspaceChatWorkflowsQuery = { workspaceChatWorkflows: Array<{ projectDeploymentId: string, projectId: string, projectName: string, workflowExecutionId: string, workflowLabel: string }> };

export type WorkspaceMcpServersQueryVariables = Exact<{
  workspaceId: string | number;
}>;


export type WorkspaceMcpServersQuery = { workspaceMcpServers: Array<{ id: string, name: string, type: Types.PlatformType, environmentId: string, enabled: boolean, url: string, lastModifiedDate: any, mcpComponents: Array<{ id: string, mcpServerId: string, componentName: string, componentVersion: number, title: string | null } | null> | null, tags: Array<{ id: string, name: string } | null> | null } | null> | null };

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

export type DataTableTagsQueryVariables = Exact<{ [key: string]: never; }>;


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

export type KnowledgeBaseQueryVariables = Exact<{
  id: string | number;
}>;


export type KnowledgeBaseQuery = { knowledgeBase: { id: string, name: string, description: string | null, maxChunkSize: number | null, minChunkSizeChars: number | null, overlap: number | null, createdDate: any, lastModifiedDate: any, documents: Array<{ id: string, name: string, status: number, tags: Array<string> | null, createdDate: any, document: { name: string, extension: string | null, mimeType: string | null, url: string } | null, chunks: Array<{ id: string, knowledgeBaseDocumentId: string, content: string | null, metadata: any } | null> | null } | null> | null } | null };

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

export type KnowledgeBaseTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseTagsQuery = { knowledgeBaseTags: Array<{ id: string, name: string }> | null };

export type KnowledgeBaseTagsByKnowledgeBaseQueryVariables = Exact<{ [key: string]: never; }>;


export type KnowledgeBaseTagsByKnowledgeBaseQuery = { knowledgeBaseTagsByKnowledgeBase: Array<{ knowledgeBaseId: string, tags: Array<{ id: string, name: string }> }> | null };

export type KnowledgeBasesQueryVariables = Exact<{
  environmentId: string | number;
  workspaceId: string | number;
}>;


export type KnowledgeBasesQuery = { knowledgeBases: Array<{ id: string, name: string, description: string | null, maxChunkSize: number | null, minChunkSizeChars: number | null, overlap: number | null, createdDate: any, lastModifiedDate: any } | null> | null };

export type SearchKnowledgeBaseQueryVariables = Exact<{
  id: string | number;
  query: string;
  metadataFilters?: string | null | undefined;
}>;


export type SearchKnowledgeBaseQuery = { searchKnowledgeBase: Array<{ id: string, knowledgeBaseDocumentId: string, content: string | null, metadata: any, score: number | null } | null> | null };

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

export type AutomationWorkflowProjectCategoriesQueryVariables = Exact<{ [key: string]: never; }>;


export type AutomationWorkflowProjectCategoriesQuery = { automationWorkflowProjectCategories: Array<{ id: string, name: string }> };

export type AutomationWorkflowProjectTagsQueryVariables = Exact<{ [key: string]: never; }>;


export type AutomationWorkflowProjectTagsQuery = { automationWorkflowProjectTags: Array<{ id: string, name: string }> };

export type AutomationWorkflowProjectVersionsQueryVariables = Exact<{
  id: string | number;
}>;


export type AutomationWorkflowProjectVersionsQuery = { automationWorkflowProjectVersions: Array<{ version: number, status: string, publishedDate: string | null }> };

export type AutomationWorkflowProjectsQueryVariables = Exact<{ [key: string]: never; }>;


export type AutomationWorkflowProjectsQuery = { automationWorkflowProjects: Array<{ id: string, name: string, description: string | null, categoryId: string | null, tagIds: Array<string>, published: boolean, version: number, lastPublishedVersion: number | null, workflowTemplates: Array<{ workflowUuid: string, label: string | null, description: string | null, lastModifiedDate: string | null, triggers: Array<{ name: string, title: string | null, icon: string | null }>, components: Array<{ name: string, title: string | null, icon: string | null }> }> }> };

export type CreateAutomationWorkflowProjectMutationVariables = Exact<{
  name: string;
  description?: string | null | undefined;
  category?: string | null | undefined;
  tags?: Array<string> | string | null | undefined;
}>;


export type CreateAutomationWorkflowProjectMutation = { createAutomationWorkflowProject: string };

export type UpdateAutomationWorkflowProjectMutationVariables = Exact<{
  id: string | number;
  name: string;
  description?: string | null | undefined;
  category?: string | null | undefined;
  tags?: Array<string> | string | null | undefined;
}>;


export type UpdateAutomationWorkflowProjectMutation = { updateAutomationWorkflowProject: boolean };

export type DeleteAutomationWorkflowProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteAutomationWorkflowProjectMutation = { deleteAutomationWorkflowProject: boolean };

export type CreateAutomationWorkflowProjectWorkflowMutationVariables = Exact<{
  projectId: string | number;
  definition?: string | null | undefined;
}>;


export type CreateAutomationWorkflowProjectWorkflowMutation = { createAutomationWorkflowProjectWorkflow: string };

export type DeleteAutomationWorkflowProjectWorkflowMutationVariables = Exact<{
  workflowUuid: string | number;
}>;


export type DeleteAutomationWorkflowProjectWorkflowMutation = { deleteAutomationWorkflowProjectWorkflow: boolean };

export type PublishAutomationWorkflowProjectMutationVariables = Exact<{
  id: string | number;
}>;


export type PublishAutomationWorkflowProjectMutation = { publishAutomationWorkflowProject: boolean };

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

export type EmbeddedMcpServersQueryVariables = Exact<{ [key: string]: never; }>;


export type EmbeddedMcpServersQuery = { embeddedMcpServers: Array<{ id: string, enabled: boolean, environmentId: string, lastModifiedDate: any, name: string, type: Types.PlatformType, url: string, mcpComponents: Array<{ componentName: string, componentVersion: number, connectionId: string | null, id: string, lastModifiedDate: any, mcpServerId: string, title: string | null, mcpTools: Array<{ id: string, mcpComponentId: string, name: string, title: string | null, parameters: any } | null> | null } | null> | null, tags: Array<{ id: string, name: string } | null> | null } | null> | null };

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

export type IntegrationByIdQueryVariables = Exact<{
  id: string | number;
}>;


export type IntegrationByIdQuery = { integration: { id: string, name: string } | null };

export type IntegrationWorkflowsQueryVariables = Exact<{ [key: string]: never; }>;


export type IntegrationWorkflowsQuery = { integrationWorkflows: Array<{ id: string, label: string, description: string | null, integrationWorkflowId: string, workflowUuid: string | null, workflowTaskComponentNames: Array<string>, workflowTriggerComponentNames: Array<string>, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any }> };

export type IntegrationWorkflowsByIntegrationIdQueryVariables = Exact<{
  integrationId: string | number;
}>;


export type IntegrationWorkflowsByIntegrationIdQuery = { integrationWorkflowsByIntegrationId: Array<{ id: string, label: string, description: string | null, integrationWorkflowId: string, workflowUuid: string | null, workflowTaskComponentNames: Array<string>, workflowTriggerComponentNames: Array<string>, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any }> };

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

export type CreateApiConnectorMutationVariables = Exact<{
  input: Types.CreateApiConnectorInput;
}>;


export type CreateApiConnectorMutation = { createApiConnector: { id: string, name: string, title: string | null, description: string | null, icon: string | null, connectorVersion: number, enabled: boolean | null, specification: string | null, definition: string | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null, endpoints: Array<{ id: string, name: string, description: string | null, path: string | null, httpMethod: Types.HttpMethod | null }> | null } };

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


export type ComponentDefinitionSearchQuery = { componentDefinitionSearch: Array<{ name: string, title: string | null, icon: string | null, description: string | null, version: number | null, actionsCount: number | null, triggersCount: number | null, clusterElementsCount: any, componentCategories: Array<{ name: string, label: string | null }> | null, actions: Array<{ name: string, title: string | null, description: string | null }> | null, triggers: Array<{ name: string, title: string | null, description: string | null }> | null, clusterElements: Array<{ type: { name: string | null, label: string | null } | null }> | null }> };

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


export type CreateMcpComponentWithToolsMutation = { createMcpComponentWithTools: { id: string, componentName: string, componentVersion: number, title: string | null, mcpServerId: string, connectionId: string | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null } | null };

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

export type ManagementMcpServerUrlQueryVariables = Exact<{ [key: string]: never; }>;


export type ManagementMcpServerUrlQuery = { managementMcpServerUrl: string | null };

export type McpComponentsByServerIdQueryVariables = Exact<{
  mcpServerId: string | number;
}>;


export type McpComponentsByServerIdQuery = { mcpComponentsByServerId: Array<{ id: string, componentName: string, componentVersion: number, title: string | null, connectionId: string | null, lastModifiedDate: any, mcpServerId: string, version: number | null, mcpTools: Array<{ id: string, mcpComponentId: string, name: string, parameters: any, title: string | null, version: number | null } | null> | null } | null> | null };

export type McpServerTagsQueryVariables = Exact<{
  type: Types.PlatformType;
}>;


export type McpServerTagsQuery = { mcpServerTags: Array<{ id: string, name: string } | null> | null };

export type McpServersQueryVariables = Exact<{
  type: Types.PlatformType;
}>;


export type McpServersQuery = { mcpServers: Array<{ id: string, name: string, type: Types.PlatformType, environmentId: string, enabled: boolean, secretKey: string, lastModifiedDate: any, mcpComponents: Array<{ id: string, mcpServerId: string, componentName: string, componentVersion: number, title: string | null } | null> | null, tags: Array<{ id: string, name: string } | null> | null } | null> | null };

export type McpToolsByComponentIdQueryVariables = Exact<{
  mcpComponentId: string | number;
}>;


export type McpToolsByComponentIdQuery = { mcpToolsByComponentId: Array<{ id: string, name: string, title: string | null, mcpComponentId: string, parameters: any, version: number | null } | null> | null };

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

export type UpdateManagementMcpServerUrlMutationVariables = Exact<{ [key: string]: never; }>;


export type UpdateManagementMcpServerUrlMutation = { updateManagementMcpServerUrl: string };

export type UpdateMcpComponentWithToolsMutationVariables = Exact<{
  id: string | number;
  input: Types.McpComponentWithToolsInput;
}>;


export type UpdateMcpComponentWithToolsMutation = { updateMcpComponentWithTools: { id: string, componentName: string, componentVersion: number, title: string | null, mcpServerId: string, connectionId: string | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null } | null };

export type UpdateMcpServerUrlMutationVariables = Exact<{
  id: string | number;
}>;


export type UpdateMcpServerUrlMutation = { updateMcpServerUrl: string };

export type UpdateMcpToolMutationVariables = Exact<{
  id: string | number;
  input: Types.McpToolInput;
}>;


export type UpdateMcpToolMutation = { updateMcpTool: { id: string, name: string, mcpComponentId: string, parameters: any, version: number | null } | null };

export type ValidateWorkflowQueryVariables = Exact<{
  workflowDefinition: string;
}>;


export type ValidateWorkflowQuery = { validateWorkflow: { errors: Array<string>, warnings: Array<string> } };

export type ValidateWorkflowByIdQueryVariables = Exact<{
  workflowId: string;
}>;


export type ValidateWorkflowByIdQuery = { validateWorkflowById: { errors: Array<string>, warnings: Array<string> } };

export type WorkflowNodeMissingRequiredPropertiesQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
}>;


export type WorkflowNodeMissingRequiredPropertiesQuery = { workflowNodeMissingRequiredProperties: Array<string> };

export type ClusterElementMissingRequiredPropertiesQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  clusterElementType: string;
  clusterElementWorkflowNodeName: string;
}>;


export type ClusterElementMissingRequiredPropertiesQuery = { clusterElementMissingRequiredProperties: Array<string> };

export type WorkflowNodeComponentConnectionsQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
}>;


export type WorkflowNodeComponentConnectionsQuery = { workflowNodeComponentConnections: Array<{ componentName: string, componentVersion: number, key: string, required: boolean, workflowNodeName: string }> };

export type WorkflowNodeScriptInputQueryVariables = Exact<{
  workflowId: string;
  workflowNodeName: string;
  environmentId: any;
}>;


export type WorkflowNodeScriptInputQuery = { workflowNodeScriptInput: any };

export type CustomComponentQueryVariables = Exact<{
  id: string | number;
}>;


export type CustomComponentQuery = { customComponent: { id: string, name: string, title: string | null, description: string | null, icon: string | null, componentVersion: number | null, enabled: boolean | null, language: Types.CustomComponentLanguage | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null } | null };

export type CustomComponentDefinitionQueryVariables = Exact<{
  id: string | number;
}>;


export type CustomComponentDefinitionQuery = { customComponentDefinition: { actions: Array<{ name: string, title: string | null, description: string | null }>, triggers: Array<{ name: string, title: string | null, description: string | null }> } | null };

export type CustomComponentsQueryVariables = Exact<{ [key: string]: never; }>;


export type CustomComponentsQuery = { customComponents: Array<{ id: string, name: string, title: string | null, description: string | null, icon: string | null, componentVersion: number | null, enabled: boolean | null, language: Types.CustomComponentLanguage | null, createdBy: string | null, createdDate: any, lastModifiedBy: string | null, lastModifiedDate: any, version: number | null }> };

export type DeleteCustomComponentMutationVariables = Exact<{
  id: string | number;
}>;


export type DeleteCustomComponentMutation = { deleteCustomComponent: boolean };

export type EnableCustomComponentMutationVariables = Exact<{
  id: string | number;
  enable: boolean;
}>;


export type EnableCustomComponentMutation = { enableCustomComponent: boolean };

export type AuthoritiesQueryVariables = Exact<{ [key: string]: never; }>;


export type AuthoritiesQuery = { authorities: Array<string> };

export type CreateIdentityProviderMutationVariables = Exact<{
  input: Types.IdentityProviderInput;
}>;


export type CreateIdentityProviderMutation = { createIdentityProvider: { autoProvision: boolean, clientId: string | null, createdBy: string | null, createdDate: any, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string | null, lastModifiedBy: string | null, lastModifiedDate: any, metadataUri: string | null, mfaMethod: string | null, mfaRequired: boolean, name: string, nameIdFormat: string | null, scopes: string | null, signingCertificate: string | null, type: string } };

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


export type IdentityProviderQuery = { identityProvider: { autoProvision: boolean, clientId: string | null, createdBy: string | null, createdDate: any, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string | null, lastModifiedBy: string | null, lastModifiedDate: any, metadataUri: string | null, mfaMethod: string | null, mfaRequired: boolean, name: string, nameIdFormat: string | null, scopes: string | null, signingCertificate: string | null, type: string } | null };

export type IdentityProvidersQueryVariables = Exact<{ [key: string]: never; }>;


export type IdentityProvidersQuery = { identityProviders: Array<{ autoProvision: boolean, clientId: string | null, createdBy: string | null, createdDate: any, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string | null, lastModifiedBy: string | null, lastModifiedDate: any, metadataUri: string | null, mfaMethod: string | null, mfaRequired: boolean, name: string, nameIdFormat: string | null, scopes: string | null, signingCertificate: string | null, type: string } | null> };

export type InviteUserMutationVariables = Exact<{
  email: string;
  password: string;
  role: string;
}>;


export type InviteUserMutation = { inviteUser: boolean };

export type UpdateIdentityProviderMutationVariables = Exact<{
  id: string | number;
  input: Types.IdentityProviderInput;
}>;


export type UpdateIdentityProviderMutation = { updateIdentityProvider: { autoProvision: boolean, clientId: string | null, createdBy: string | null, createdDate: any, defaultAuthority: string, domains: Array<string>, enabled: boolean, enforced: boolean, id: string, issuerUri: string | null, lastModifiedBy: string | null, lastModifiedDate: any, metadataUri: string | null, mfaMethod: string | null, mfaRequired: boolean, name: string, nameIdFormat: string | null, scopes: string | null, signingCertificate: string | null, type: string } };

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

export const AiSkillsDocument = new TypedDocumentString(`
    query aiSkills {
  aiSkills {
    id
    name
    description
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
    query mcpProjects {
  mcpProjects {
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
      variables?: McpProjectsQueryVariables,
      options?: Omit<UseQueryOptions<McpProjectsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpProjectsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpProjectsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['mcpProjects'] : ['mcpProjects', variables],
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
    workflowExecutionId
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

export const WorkspaceMcpServersDocument = new TypedDocumentString(`
    query workspaceMcpServers($workspaceId: ID!) {
  workspaceMcpServers(workspaceId: $workspaceId) {
    id
    name
    type
    environmentId
    enabled
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

export const DataTableTagsDocument = new TypedDocumentString(`
    query dataTableTags {
  dataTableTags {
    id
    name
  }
}
    `);

export const useDataTableTagsQuery = <
      TData = DataTableTagsQuery,
      TError = unknown
    >(
      variables?: DataTableTagsQueryVariables,
      options?: Omit<UseQueryOptions<DataTableTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<DataTableTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<DataTableTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['dataTableTags'] : ['dataTableTags', variables],
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
      chunks {
        id
        knowledgeBaseDocumentId
        content
        metadata
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

export const KnowledgeBaseTagsDocument = new TypedDocumentString(`
    query knowledgeBaseTags {
  knowledgeBaseTags {
    id
    name
  }
}
    `);

export const useKnowledgeBaseTagsQuery = <
      TData = KnowledgeBaseTagsQuery,
      TError = unknown
    >(
      variables?: KnowledgeBaseTagsQueryVariables,
      options?: Omit<UseQueryOptions<KnowledgeBaseTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<KnowledgeBaseTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<KnowledgeBaseTagsQuery, TError, TData>(
      {
    queryKey: variables === undefined ? ['knowledgeBaseTags'] : ['knowledgeBaseTags', variables],
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
    workflowTemplates {
      workflowUuid
      label
      description
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
    mutation createAutomationWorkflowProject($name: String!, $description: String, $category: String, $tags: [String!]) {
  createAutomationWorkflowProject(
    name: $name
    description: $description
    category: $category
    tags: $tags
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
    mutation updateAutomationWorkflowProject($id: ID!, $name: String!, $description: String, $category: String, $tags: [String!]) {
  updateAutomationWorkflowProject(
    id: $id
    name: $name
    description: $description
    category: $category
    tags: $tags
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
    mutation createAutomationWorkflowProjectWorkflow($projectId: ID!, $definition: String) {
  createAutomationWorkflowProjectWorkflow(
    projectId: $projectId
    definition: $definition
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

export const EmbeddedMcpServersDocument = new TypedDocumentString(`
    query embeddedMcpServers {
  embeddedMcpServers {
    id
    enabled
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

export const IntegrationByIdDocument = new TypedDocumentString(`
    query integrationById($id: ID!) {
  integration(id: $id) {
    id
    name
  }
}
    `);

export const useIntegrationByIdQuery = <
      TData = IntegrationByIdQuery,
      TError = unknown
    >(
      variables: IntegrationByIdQueryVariables,
      options?: Omit<UseQueryOptions<IntegrationByIdQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<IntegrationByIdQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<IntegrationByIdQuery, TError, TData>(
      {
    queryKey: ['integrationById', variables],
    queryFn: fetcher<IntegrationByIdQuery, IntegrationByIdQueryVariables>(IntegrationByIdDocument, variables),
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

export const CreateApiConnectorDocument = new TypedDocumentString(`
    mutation createApiConnector($input: CreateApiConnectorInput!) {
  createApiConnector(input: $input) {
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

export const useCreateApiConnectorMutation = <
      TError = unknown,
      TContext = unknown
    >(options?: UseMutationOptions<CreateApiConnectorMutation, TError, CreateApiConnectorMutationVariables, TContext>) => {
    
    return useMutation<CreateApiConnectorMutation, TError, CreateApiConnectorMutationVariables, TContext>(
      {
    mutationKey: ['createApiConnector'],
    mutationFn: (variables?: CreateApiConnectorMutationVariables) => fetcher<CreateApiConnectorMutation, CreateApiConnectorMutationVariables>(CreateApiConnectorDocument, variables)(),
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
      mcpComponentId
      name
      parameters
      title
      version
    }
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

export const McpServerTagsDocument = new TypedDocumentString(`
    query mcpServerTags($type: PlatformType!) {
  mcpServerTags(type: $type) {
    id
    name
  }
}
    `);

export const useMcpServerTagsQuery = <
      TData = McpServerTagsQuery,
      TError = unknown
    >(
      variables: McpServerTagsQueryVariables,
      options?: Omit<UseQueryOptions<McpServerTagsQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpServerTagsQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpServerTagsQuery, TError, TData>(
      {
    queryKey: ['mcpServerTags', variables],
    queryFn: fetcher<McpServerTagsQuery, McpServerTagsQueryVariables>(McpServerTagsDocument, variables),
    ...options
  }
    )};

export const McpServersDocument = new TypedDocumentString(`
    query mcpServers($type: PlatformType!) {
  mcpServers(type: $type, orderBy: NAME_ASC) {
    id
    name
    type
    environmentId
    enabled
    secretKey
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

export const useMcpServersQuery = <
      TData = McpServersQuery,
      TError = unknown
    >(
      variables: McpServersQueryVariables,
      options?: Omit<UseQueryOptions<McpServersQuery, TError, TData>, 'queryKey'> & { queryKey?: UseQueryOptions<McpServersQuery, TError, TData>['queryKey'] }
    ) => {
    
    return useQuery<McpServersQuery, TError, TData>(
      {
    queryKey: ['mcpServers', variables],
    queryFn: fetcher<McpServersQuery, McpServersQueryVariables>(McpServersDocument, variables),
    ...options
  }
    )};

export const McpToolsByComponentIdDocument = new TypedDocumentString(`
    query mcpToolsByComponentId($mcpComponentId: ID!) {
  mcpToolsByComponentId(mcpComponentId: $mcpComponentId) {
    id
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

export const WorkflowNodeMissingRequiredPropertiesDocument = new TypedDocumentString(`
    query WorkflowNodeMissingRequiredProperties($workflowId: String!, $workflowNodeName: String!) {
  workflowNodeMissingRequiredProperties(workflowId: $workflowId, workflowNodeName: $workflowNodeName)
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
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
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
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
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
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
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
    mutation inviteUser($email: String!, $password: String!, $role: String!) {
  inviteUser(email: $email, password: $password, role: $role)
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
    metadataUri
    mfaMethod
    mfaRequired
    name
    nameIdFormat
    scopes
    signingCertificate
    type
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
