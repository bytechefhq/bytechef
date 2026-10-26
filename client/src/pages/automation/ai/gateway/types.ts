import {
    AiGatewayProjectsQuery,
    WorkspaceAiGatewayProvidersQuery,
    WorkspaceAiGatewayRoutingPoliciesQuery,
    WorkspaceAiModelsQuery,
} from '@/shared/middleware/graphql';

export type AiGatewayModelType = NonNullable<NonNullable<WorkspaceAiModelsQuery['workspaceAiModels']>[number]>;

export type AiGatewayProjectType = NonNullable<AiGatewayProjectsQuery['aiGatewayProjects'][number]>;

export type AiGatewayProviderType = NonNullable<
    NonNullable<WorkspaceAiGatewayProvidersQuery['workspaceAiGatewayProviders']>[number]
>;

export type AiGatewayRoutingPolicyType = NonNullable<
    NonNullable<WorkspaceAiGatewayRoutingPoliciesQuery['workspaceAiGatewayRoutingPolicies']>[number]
>;
