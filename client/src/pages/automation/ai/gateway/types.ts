import {
    AiEvalRulesQuery,
    AiEvalScoreConfigsQuery,
    AiGatewayProjectsQuery,
    AiPromptQuery,
    AiPromptsQuery,
    WorkspaceAiGatewayProvidersQuery,
    WorkspaceAiGatewayRoutingPoliciesQuery,
    WorkspaceAiModelsQuery,
} from '@/shared/middleware/graphql';

export type AiEvalRuleType = NonNullable<NonNullable<AiEvalRulesQuery['aiEvalRules']>[number]>;

export type AiEvalScoreConfigType = NonNullable<NonNullable<AiEvalScoreConfigsQuery['aiEvalScoreConfigs']>[number]>;

export type AiGatewayModelType = NonNullable<NonNullable<WorkspaceAiModelsQuery['workspaceAiModels']>[number]>;

export type AiGatewayProjectType = NonNullable<AiGatewayProjectsQuery['aiGatewayProjects'][number]>;

export type AiGatewayProviderType = NonNullable<
    NonNullable<WorkspaceAiGatewayProvidersQuery['workspaceAiGatewayProviders']>[number]
>;

export type AiGatewayRoutingPolicyType = NonNullable<
    NonNullable<WorkspaceAiGatewayRoutingPoliciesQuery['workspaceAiGatewayRoutingPolicies']>[number]
>;

export type AiPromptType = NonNullable<NonNullable<AiPromptsQuery['aiPrompts']>[number]>;

export type AiPromptVersionType = NonNullable<NonNullable<NonNullable<AiPromptQuery['aiPrompt']>['versions']>[number]>;
