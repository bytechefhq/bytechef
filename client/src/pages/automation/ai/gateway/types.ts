import {
    AiGatewayProjectsQuery,
    AiObservabilityAlertEventsQuery,
    AiObservabilityAlertRulesQuery,
    AiObservabilityExportJobQuery,
    AiObservabilityExportJobsQuery,
    AiObservabilitySessionQuery,
    AiObservabilitySessionsQuery,
    AiObservabilityTraceQuery,
    AiObservabilityTracesQuery,
    AiObservabilityWebhookSubscriptionQuery,
    AiObservabilityWebhookSubscriptionsQuery,
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

export type AiObservabilityAlertEventType = NonNullable<
    NonNullable<AiObservabilityAlertEventsQuery['aiObservabilityAlertEvents']>[number]
>;

export type AiObservabilityAlertRuleType = NonNullable<
    NonNullable<AiObservabilityAlertRulesQuery['aiObservabilityAlertRules']>[number]
>;

export type AiObservabilityExportJobDetailType = NonNullable<AiObservabilityExportJobQuery['aiObservabilityExportJob']>;

export type AiObservabilityExportJobType = NonNullable<
    NonNullable<AiObservabilityExportJobsQuery['aiObservabilityExportJobs']>[number]
>;

export type AiObservabilitySessionDetailType = NonNullable<AiObservabilitySessionQuery['aiObservabilitySession']>;

export type AiObservabilitySessionType = NonNullable<
    NonNullable<AiObservabilitySessionsQuery['aiObservabilitySessions']>[number]
>;

export type AiObservabilitySpanType = NonNullable<
    NonNullable<NonNullable<AiObservabilityTraceQuery['aiObservabilityTrace']>['spans']>[number]
>;

export type AiObservabilityTraceDetailType = NonNullable<AiObservabilityTraceQuery['aiObservabilityTrace']>;

export type AiObservabilityTraceType = NonNullable<
    NonNullable<AiObservabilityTracesQuery['aiObservabilityTraces']>[number]
>;

export type AiObservabilityWebhookSubscriptionDetailType = NonNullable<
    AiObservabilityWebhookSubscriptionQuery['aiObservabilityWebhookSubscription']
>;

export type AiObservabilityWebhookSubscriptionType = NonNullable<
    NonNullable<AiObservabilityWebhookSubscriptionsQuery['aiObservabilityWebhookSubscriptions']>[number]
>;
