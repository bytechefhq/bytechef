import {
    AiEvalRulesQuery,
    AiEvalScoreConfigsQuery,
    AiEvalScoresByTraceQuery,
    AiEvalScoresQuery,
    AiGatewayProjectsQuery,
    AiGatewayTagsQuery,
    AiObservabilityAlertEventsQuery,
    AiObservabilityAlertRulesQuery,
    AiObservabilityExportJobQuery,
    AiObservabilityExportJobsQuery,
    AiObservabilityNotificationChannelsQuery,
    AiObservabilitySessionQuery,
    AiObservabilitySessionsQuery,
    AiObservabilityTraceQuery,
    AiObservabilityTracesQuery,
    AiObservabilityWebhookSubscriptionQuery,
    AiObservabilityWebhookSubscriptionsQuery,
    AiPromptQuery,
    AiPromptsQuery,
    PlaygroundChatCompletionMutation,
    WorkspaceAiGatewayModelsQuery,
    WorkspaceAiGatewayProvidersQuery,
    WorkspaceAiGatewayRoutingPoliciesQuery,
} from '@/shared/middleware/graphql';

export type AiEvalRuleType = NonNullable<NonNullable<AiEvalRulesQuery['aiEvalRules']>[number]>;

export type AiEvalScoreConfigType = NonNullable<NonNullable<AiEvalScoreConfigsQuery['aiEvalScoreConfigs']>[number]>;

export type AiEvalScoreType = NonNullable<NonNullable<AiEvalScoresQuery['aiEvalScores']>[number]>;

export type AiEvalTraceScoreType = NonNullable<NonNullable<AiEvalScoresByTraceQuery['aiEvalScoresByTrace']>[number]>;

export type AiObservabilityExportJobDetailType = NonNullable<AiObservabilityExportJobQuery['aiObservabilityExportJob']>;

export type AiObservabilityExportJobType = NonNullable<
    NonNullable<AiObservabilityExportJobsQuery['aiObservabilityExportJobs']>[number]
>;

export type AiObservabilityAlertEventType = NonNullable<
    NonNullable<AiObservabilityAlertEventsQuery['aiObservabilityAlertEvents']>[number]
>;

export type AiObservabilityAlertRuleType = NonNullable<
    NonNullable<AiObservabilityAlertRulesQuery['aiObservabilityAlertRules']>[number]
>;

export type AiObservabilityNotificationChannelType = NonNullable<
    NonNullable<AiObservabilityNotificationChannelsQuery['aiObservabilityNotificationChannels']>[number]
>;

export type AiGatewayModelType = NonNullable<
    NonNullable<WorkspaceAiGatewayModelsQuery['workspaceAiGatewayModels']>[number]
>;

export type AiGatewayProjectType = NonNullable<AiGatewayProjectsQuery['aiGatewayProjects'][number]>;

export type AiGatewayProviderType = NonNullable<
    NonNullable<WorkspaceAiGatewayProvidersQuery['workspaceAiGatewayProviders']>[number]
>;

export type AiGatewayTagType = NonNullable<NonNullable<AiGatewayTagsQuery['aiGatewayTags']>[number]>;

export type AiGatewayRoutingPolicyType = NonNullable<
    NonNullable<WorkspaceAiGatewayRoutingPoliciesQuery['workspaceAiGatewayRoutingPolicies']>[number]
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

export type AiPromptDetailType = NonNullable<AiPromptQuery['aiPrompt']>;

export type AiPromptType = NonNullable<NonNullable<AiPromptsQuery['aiPrompts']>[number]>;

export type AiPromptVersionType = NonNullable<NonNullable<NonNullable<AiPromptQuery['aiPrompt']>['versions']>[number]>;

export type AiObservabilityWebhookSubscriptionDetailType = NonNullable<
    AiObservabilityWebhookSubscriptionQuery['aiObservabilityWebhookSubscription']
>;

export type AiObservabilityWebhookSubscriptionType = NonNullable<
    NonNullable<AiObservabilityWebhookSubscriptionsQuery['aiObservabilityWebhookSubscriptions']>[number]
>;

export type PlaygroundChatCompletionResponseType = NonNullable<
    PlaygroundChatCompletionMutation['playgroundChatCompletion']
>;
