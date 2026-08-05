import {reportMutationError, useReportQueryError} from '@/shared/error/useReportQueryError';
import {
    AddAiHubPersonalAgentResourceInput,
    AddAiHubPersonalAgentToolInput,
    AiHubPersonalAgentQuery,
    AiHubPersonalAgentScheduleFieldsFragment,
    AiHubPersonalAgentsQuery,
    CreateAiHubPersonalAgentInput,
    UpdateAiHubPersonalAgentInput,
    UpdateAiHubPersonalAgentToolConfigInput,
    useAddAiHubPersonalAgentResourceMutation as useGeneratedAddAiHubPersonalAgentResourceMutation,
    useAddAiHubPersonalAgentToolMutation as useGeneratedAddAiHubPersonalAgentToolMutation,
    useAiHubPersonalAgentQuery as useGeneratedAiHubPersonalAgentQuery,
    useAiHubPersonalAgentsQuery as useGeneratedAiHubPersonalAgentsQuery,
    useCreateAiHubPersonalAgentMutation as useGeneratedCreateAiHubPersonalAgentMutation,
    useDeleteAiHubPersonalAgentMutation as useGeneratedDeleteAiHubPersonalAgentMutation,
    useRemoveAiHubPersonalAgentResourceMutation as useGeneratedRemoveAiHubPersonalAgentResourceMutation,
    useRemoveAiHubPersonalAgentToolMutation as useGeneratedRemoveAiHubPersonalAgentToolMutation,
    useUpdateAiHubPersonalAgentMutation as useGeneratedUpdateAiHubPersonalAgentMutation,
    useUpdateAiHubPersonalAgentToolConfigMutation as useGeneratedUpdateAiHubPersonalAgentToolConfigMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

// Public consumer-facing AiHubPersonalAgent shape. The codegen output uses Long branded types and id-as-string;
// flatten to plain numbers + ISO strings so React components can render without per-call coercion.
export interface AiHubPersonalAgentI {
    createdAt: string;
    description: string | null;
    environmentId: number;
    id: number;
    instructions: string | null;
    // Per-agent LLM override (provider, model). Both set or both null — server validation enforces.
    // When both are null, the agent uses the workspace's default LLM. Provider is the lowercase
    // AiGatewayProviderType name ("openai", "anthropic", …); model is the provider-specific id.
    llmModel: string | null;
    llmProvider: string | null;
    name: string;
    resources: AiHubPersonalAgentResourceI[];
    // Single schedule row, if one has been set via setAiHubPersonalAgentSchedule. Null when no schedule is
    // configured. The fragment type is the codegen shape; consumers use fromExistingSchedule to map to the form
    // value type.
    schedule: AiHubPersonalAgentScheduleFieldsFragment | null;
    title: string | null;
    tools: AiHubPersonalAgentToolI[];
    updatedAt: string;
    userId: number;
    workspaceId: number;
}

// Per-agent tool template row. Each entry corresponds to a (componentName, componentVersion, operationName) triple
// the agent has declared as available; the backend auto-attaches these to every new task the agent spawns.
//
// `connectionId` and `parameters` were added in 20260506000001 — they let the user pre-bind a specific connection
// and pre-set invocation parameters on the agent's template, mirroring the AI Agent simple-mode tools picker. Both
// are optional: a null `connectionId` means "let the user pick at first invocation" and an empty `parameters` map
// means "every argument is LLM-supplied". The `parameters` map shape matches `TaskTool.parameters` so the
// task copy path forwards both forwards verbatim into `ai_hub_task_tool`.
export interface AiHubPersonalAgentToolI {
    componentName: string;
    componentVersion: number;
    connectionId: number | null;
    id: number;
    operationName: string;
    parameters: Record<string, unknown>;
    aiHubPersonalAgentId: number;
}

// Per-agent resource template row. Each entry is a (kind, resourceId, resourceName) reference the agent has
// pre-declared; the backend copies these onto every task the agent spawns and the routing agent also injects them
// into the LLM context each turn. `kind` is the GraphQL AiHubPersonalAgentResourceKind enum value.
export interface AiHubPersonalAgentResourceI {
    aiHubPersonalAgentId: number;
    createdAt: string;
    id: number;
    kind: string;
    resourceId: string;
    resourceName: string;
}

export interface AiHubPersonalAgentPatchI {
    description?: string;
    instructions?: string;
    title?: string;
}

type GraphQlAgentType = NonNullable<AiHubPersonalAgentQuery['aiHubPersonalAgent']>;

function toAgent(agent: GraphQlAgentType): AiHubPersonalAgentI {
    return {
        createdAt: agent.createdAt != null ? new Date(Number(agent.createdAt)).toISOString() : '',
        description: agent.description ?? null,
        environmentId: Number(agent.environmentId),
        id: Number(agent.id),
        instructions: agent.instructions ?? null,
        llmModel: agent.llmModel ?? null,
        llmProvider: agent.llmProvider ?? null,
        name: agent.name,
        resources: (agent.resources ?? []).map((resource) => ({
            aiHubPersonalAgentId: Number(resource.aiHubPersonalAgentId),
            createdAt: resource.createdAt != null ? new Date(Number(resource.createdAt)).toISOString() : '',
            id: Number(resource.id),
            kind: resource.kind,
            resourceId: resource.resourceId,
            resourceName: resource.resourceName,
        })),
        schedule: agent.schedule ?? null,
        title: agent.title ?? null,
        // Defensive against a server response that omits the field — codegen marks it required, but a stale
        // client / proxy that drops it should still render the agent rather than blow up.
        tools: (agent.tools ?? []).map((tool) => ({
            aiHubPersonalAgentId: Number(tool.aiHubPersonalAgentId),
            componentName: tool.componentName,
            componentVersion: tool.componentVersion,
            // Coerce the Long-branded GraphQL id to a plain number; null when the tool template doesn't pin
            // a connection (matches the entity's nullable column).
            connectionId: tool.connectionId != null ? Number(tool.connectionId) : null,
            id: Number(tool.id),
            operationName: tool.operationName,
            // Cast through unknown because the codegen `Any` scalar resolves to `unknown` while consumers want
            // to read named keys. The shape is opaque here (LLM-invocation parameter map); per-component shape
            // checks happen in the property renderer.
            parameters: (tool.parameters ?? {}) as Record<string, unknown>,
        })),
        updatedAt: agent.updatedAt != null ? new Date(Number(agent.updatedAt)).toISOString() : '',
        userId: Number(agent.userId),
        workspaceId: Number(agent.workspaceId),
    };
}

export const AiHubPersonalAgentsKeys = {
    all: ['aiHubPersonalAgents'] as const,
    detail: (agentId: number, workspaceId: number) =>
        [...AiHubPersonalAgentsKeys.all, 'detail', agentId, workspaceId] as const,
    list: (workspaceId: number, environmentId: number) =>
        [...AiHubPersonalAgentsKeys.all, 'list', workspaceId, environmentId] as const,
};

export function useAiHubPersonalAgentsQuery(workspaceId: number, environmentId: number) {
    const query = useGeneratedAiHubPersonalAgentsQuery<AiHubPersonalAgentI[], Error>(
        {environment: environmentId, workspaceId: String(workspaceId)},
        {
            enabled: workspaceId > 0,
            queryKey: AiHubPersonalAgentsKeys.list(workspaceId, environmentId),
            select: (data: AiHubPersonalAgentsQuery) =>
                data.aiHubPersonalAgents.map((agent) => toAgent(agent as GraphQlAgentType)),
            staleTime: 60_000,
        }
    );

    useReportQueryError('List personal agents', query.error);

    return query;
}

export function useAiHubPersonalAgentQuery(agentId: number | undefined, workspaceId: number, enabled = true) {
    const query = useGeneratedAiHubPersonalAgentQuery<AiHubPersonalAgentI | null, Error>(
        {id: String(agentId ?? -1), workspaceId: String(workspaceId)},
        {
            enabled: enabled && agentId !== undefined && workspaceId > 0,
            queryKey: AiHubPersonalAgentsKeys.detail(agentId ?? -1, workspaceId),
            select: (data: AiHubPersonalAgentQuery) =>
                data.aiHubPersonalAgent ? toAgent(data.aiHubPersonalAgent) : null,
        }
    );

    useReportQueryError('Load personal agent', query.error);

    return query;
}

export function useCreateAiHubPersonalAgentMutation() {
    const queryClient = useQueryClient();

    return useGeneratedCreateAiHubPersonalAgentMutation({
        onError: (error) => reportMutationError('Create personal agent', error as Error),
        // Invalidate the list query for the new agent's workspace so the sidebar picks up the row immediately.
        // Optimistic UI update isn't worth the complexity here — the create mutation is fast and the query
        // refetches inside a few hundred ms.
        onSuccess: (_data, variables) => {
            const createInput = (variables as {input: CreateAiHubPersonalAgentInput}).input;

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', Number(createInput.workspaceId)],
            });
        },
    });
}

export function useUpdateAiHubPersonalAgentMutation() {
    const queryClient = useQueryClient();

    return useGeneratedUpdateAiHubPersonalAgentMutation({
        onError: (error) => reportMutationError('Update personal agent', error as Error),
        onSuccess: (_data, variables) => {
            const updateInput = (variables as {input: UpdateAiHubPersonalAgentInput}).input;

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', Number(updateInput.workspaceId)],
            });

            queryClient.invalidateQueries({
                queryKey: AiHubPersonalAgentsKeys.detail(Number(updateInput.id), Number(updateInput.workspaceId)),
            });
        },
    });
}

export function useDeleteAiHubPersonalAgentMutation() {
    const queryClient = useQueryClient();

    return useGeneratedDeleteAiHubPersonalAgentMutation({
        onError: (error) => reportMutationError('Delete personal agent', error as Error),
        onSuccess: (_data, variables) => {
            const deleteVars = variables as {id: string; workspaceId: string};

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', Number(deleteVars.workspaceId)],
            });
        },
    });
}

export function useAddAiHubPersonalAgentToolMutation() {
    const queryClient = useQueryClient();

    return useGeneratedAddAiHubPersonalAgentToolMutation({
        onError: (error) => reportMutationError('Add personal agent tool', error as Error),
        // Invalidate the list query so the agent's tools array on the cached row reflects the addition. The
        // detail query is also invalidated for any parallel render that's pulling a single agent.
        onSuccess: (_data, variables) => {
            const addInput = (variables as {input: AddAiHubPersonalAgentToolInput}).input;
            const workspaceId = Number(addInput.workspaceId);

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', workspaceId],
            });

            queryClient.invalidateQueries({
                queryKey: AiHubPersonalAgentsKeys.detail(Number(addInput.aiHubPersonalAgentId), workspaceId),
            });
        },
    });
}

export function useRemoveAiHubPersonalAgentToolMutation() {
    const queryClient = useQueryClient();

    return useGeneratedRemoveAiHubPersonalAgentToolMutation({
        onError: (error) => reportMutationError('Remove personal agent tool', error as Error),
        // Invalidate every list/detail query for the workspace — the removed tool could have been on any
        // agent, and we don't have the agent id in the mutation variables. Bulk invalidation by workspace
        // is cheap (the query refetches a small list) and keeps the cache honest.
        onSuccess: (_data, variables) => {
            const removeVars = variables as {toolId: string; workspaceId: string};
            const workspaceId = Number(removeVars.workspaceId);

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', workspaceId],
            });

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'detail'],
            });
        },
    });
}

export function useAddAiHubPersonalAgentResourceMutation() {
    const queryClient = useQueryClient();

    return useGeneratedAddAiHubPersonalAgentResourceMutation({
        onError: (error) => reportMutationError('Add personal agent resource', error as Error),
        onSuccess: (_data, variables) => {
            const addInput = (variables as {input: AddAiHubPersonalAgentResourceInput}).input;
            const workspaceId = Number(addInput.workspaceId);

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', workspaceId],
            });

            queryClient.invalidateQueries({
                queryKey: AiHubPersonalAgentsKeys.detail(Number(addInput.aiHubPersonalAgentId), workspaceId),
            });
        },
    });
}

export function useRemoveAiHubPersonalAgentResourceMutation() {
    const queryClient = useQueryClient();

    return useGeneratedRemoveAiHubPersonalAgentResourceMutation({
        onError: (error) => reportMutationError('Remove personal agent resource', error as Error),
        onSuccess: (_data, variables) => {
            const removeVars = variables as {id: string; workspaceId: string};
            const workspaceId = Number(removeVars.workspaceId);

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', workspaceId],
            });

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'detail'],
            });
        },
    });
}

export function useUpdateAiHubPersonalAgentToolConfigMutation() {
    const queryClient = useQueryClient();

    return useGeneratedUpdateAiHubPersonalAgentToolConfigMutation({
        onError: (error) => reportMutationError('Update personal agent tool config', error as Error),
        // Same bulk-invalidation rationale as removeAiHubPersonalAgentTool — the mutation variables don't carry the
        // parent agent id, so we can't target a single detail key. Workspace-wide list invalidation triggers a
        // refetch of a small list; the detail-* invalidation catches any actively-rendered single-agent view
        // (the tool list nested inside it picks up the new config without a manual refresh).
        onSuccess: (_data, variables) => {
            const input = (variables as {input: UpdateAiHubPersonalAgentToolConfigInput}).input;
            const workspaceId = Number(input.workspaceId);

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'list', workspaceId],
            });

            queryClient.invalidateQueries({
                queryKey: [...AiHubPersonalAgentsKeys.all, 'detail'],
            });
        },
    });
}
