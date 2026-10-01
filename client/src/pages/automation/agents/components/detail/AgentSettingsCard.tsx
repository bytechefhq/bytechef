import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import Switch from '@/components/Switch/Switch';
import AgentApprovalSettings from '@/pages/automation/agents/components/detail/AgentApprovalSettings';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
import AgentSettingRow from '@/pages/automation/agents/components/detail/AgentSettingRow';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiAgentChannel,
    AiAgentElement,
    useAddAiAgentElementMutation,
    useDeleteAiAgentElementMutation,
    useUpdateAiAgentSettingsMutation,
} from '@/shared/middleware/graphql';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {toast} from 'sonner';

type WebSearchProviderType = 'BRAVE' | 'FIRECRAWL' | 'NATIVE';

interface BuiltInToolsSettingsI {
    askUserQuestion: boolean;
    autoMemory: boolean;
    skillManagement: boolean;
    webSearch: boolean;
    webSearchConnectionId: number | null;
    webSearchProvider: WebSearchProviderType;
}

const DEFAULT_BUILT_IN_TOOLS: BuiltInToolsSettingsI = {
    askUserQuestion: true,
    autoMemory: true,
    skillManagement: true,
    webSearch: false,
    webSearchConnectionId: null,
    webSearchProvider: 'BRAVE',
};

const WEB_SEARCH_PROVIDERS: Record<WebSearchProviderType, {componentName: string | null; title: string}> = {
    BRAVE: {componentName: 'brave', title: 'Brave'},
    FIRECRAWL: {componentName: 'firecrawl', title: 'Firecrawl'},
    NATIVE: {componentName: null, title: 'Model provider'},
};

const NATIVE_WEB_SEARCH_MODEL_PROVIDERS = ['anthropic'];

const THINKING_MODEL_PROVIDERS = [
    'anthropic',
    'azureOpenAi',
    'gemini',
    'groq',
    'mistral',
    'nvidia',
    'ollama',
    'openAi',
    'perplexity',
];

const REASONING_EFFORTS: ReasoningEffortType[] = ['LOW', 'MEDIUM', 'HIGH'];

const DEFAULT_MAX_TOOL_CALLS = 150;

type ReasoningEffortType = 'HIGH' | 'LOW' | 'MEDIUM';

interface TopLevelSettingsI {
    maxToolCalls: number | null;
    reasoningEffort: ReasoningEffortType;
    streamResponse: boolean;
    thinking: boolean;
}

const DEFAULT_TOP_LEVEL_SETTINGS: TopLevelSettingsI = {
    maxToolCalls: null,
    reasoningEffort: 'MEDIUM',
    streamResponse: true,
    thinking: false,
};

const resolveTopLevelSettings = (settings: unknown): TopLevelSettingsI => {
    const record = ((settings as Record<string, unknown> | null | undefined) ?? {}) as Record<string, unknown>;

    const reasoningEffort = String(record.reasoningEffort ?? '').toUpperCase() as ReasoningEffortType;

    return {
        maxToolCalls: record.maxToolCalls != null ? Number(record.maxToolCalls) : null,
        reasoningEffort: REASONING_EFFORTS.includes(reasoningEffort)
            ? reasoningEffort
            : DEFAULT_TOP_LEVEL_SETTINGS.reasoningEffort,
        streamResponse: (record.streamResponse as boolean | undefined) ?? DEFAULT_TOP_LEVEL_SETTINGS.streamResponse,
        thinking: (record.thinking as boolean | undefined) ?? DEFAULT_TOP_LEVEL_SETTINGS.thinking,
    };
};

const resolveBuiltInTools = (settings: unknown): BuiltInToolsSettingsI => {
    const builtInTools = ((settings as {builtInTools?: Record<string, unknown>} | null | undefined)?.builtInTools ??
        {}) as Record<string, unknown>;

    return {
        askUserQuestion:
            (builtInTools.askUserQuestion as boolean | undefined) ?? DEFAULT_BUILT_IN_TOOLS.askUserQuestion,
        autoMemory: (builtInTools.autoMemory as boolean | undefined) ?? DEFAULT_BUILT_IN_TOOLS.autoMemory,
        skillManagement:
            (builtInTools.skillManagement as boolean | undefined) ?? DEFAULT_BUILT_IN_TOOLS.skillManagement,
        webSearch: (builtInTools.webSearch as boolean | undefined) ?? DEFAULT_BUILT_IN_TOOLS.webSearch,
        webSearchConnectionId:
            builtInTools.webSearchConnectionId != null ? Number(builtInTools.webSearchConnectionId) : null,
        webSearchProvider:
            builtInTools.webSearchProvider != null &&
            Object.hasOwn(WEB_SEARCH_PROVIDERS, String(builtInTools.webSearchProvider))
                ? (String(builtInTools.webSearchProvider) as WebSearchProviderType)
                : DEFAULT_BUILT_IN_TOOLS.webSearchProvider,
    };
};

interface AgentSettingsCardProps {
    agentId: string;
    channels: AiAgentChannel[];
    elements: AiAgentElement[];
    settings: unknown;
}

const AgentSettingsCard = ({agentId, channels, elements, settings}: AgentSettingsCardProps) => {
    const [builtInTools, setBuiltInTools] = useState<BuiltInToolsSettingsI>(() => resolveBuiltInTools(settings));
    const [topLevelSettings, setTopLevelSettings] = useState<TopLevelSettingsI>(() =>
        resolveTopLevelSettings(settings)
    );

    const chatMemoryElement = elements.find((element) => element.kind === 'CHAT_MEMORY');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const webSearchProvider = WEB_SEARCH_PROVIDERS[builtInTools.webSearchProvider];

    const modelProvider = elements.find((element) => element.kind === 'MODEL')?.parameters?.provider as
        string | undefined;

    const {data: connections = []} = useGetWorkspaceConnectionsQuery(
        {
            componentName: webSearchProvider.componentName ?? '',
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId!,
        },
        builtInTools.webSearch && webSearchProvider.componentName != null
    );

    const queryClient = useQueryClient();

    const updateAiAgentSettingsMutation = useUpdateAiAgentSettingsMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to update settings.'),
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const addAiAgentElementMutation = useAddAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to update memory.'),
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const deleteAiAgentElementMutation = useDeleteAiAgentElementMutation({
        onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to update memory.'),
        onSuccess: () => invalidateAgentQueries(queryClient),
    });

    const isChatMemoryBusy = addAiAgentElementMutation.isPending || deleteAiAgentElementMutation.isPending;

    const isWebSearchConnectionBusy = updateAiAgentSettingsMutation.isPending;

    const handleChatMemoryToggle = (checked: boolean) => {
        if (checked && !chatMemoryElement) {
            addAiAgentElementMutation.mutate({input: {agentId, kind: 'CHAT_MEMORY'}});

            return;
        }

        if (!checked && chatMemoryElement) {
            deleteAiAgentElementMutation.mutate({id: chatMemoryElement.id});
        }
    };

    const commit = (next: BuiltInToolsSettingsI, nextTopLevel: TopLevelSettingsI = topLevelSettings) => {
        setBuiltInTools(next);
        setTopLevelSettings(nextTopLevel);

        updateAiAgentSettingsMutation.mutate({
            id: agentId,
            settings: {
                builtInTools: {
                    askUserQuestion: next.askUserQuestion,
                    autoMemory: next.autoMemory,
                    skillManagement: next.skillManagement,
                    webSearch: next.webSearch,
                    webSearchProvider: next.webSearchProvider,
                    ...(next.webSearchConnectionId != null ? {webSearchConnectionId: next.webSearchConnectionId} : {}),
                },
                reasoningEffort: nextTopLevel.reasoningEffort,
                streamResponse: nextTopLevel.streamResponse,
                thinking: nextTopLevel.thinking,
                ...(nextTopLevel.maxToolCalls != null ? {maxToolCalls: nextTopLevel.maxToolCalls} : {}),
            },
        });
    };

    const handleStreamResponseToggle = (checked: boolean) => {
        commit(builtInTools, {...topLevelSettings, streamResponse: checked});
    };

    const handleThinkingToggle = (checked: boolean) => {
        commit(builtInTools, {...topLevelSettings, thinking: checked});
    };

    const handleReasoningEffortChange = (value: string) => {
        commit(builtInTools, {...topLevelSettings, reasoningEffort: value as ReasoningEffortType});
    };

    const handleMaxToolCallsBlur = (value: string) => {
        const trimmed = value.trim();
        const parsed = trimmed === '' ? null : Number(trimmed);
        const maxToolCalls = parsed != null && Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : null;

        if (maxToolCalls === topLevelSettings.maxToolCalls) {
            return;
        }

        commit(builtInTools, {...topLevelSettings, maxToolCalls});
    };

    const handleToggle =
        (key: keyof Omit<BuiltInToolsSettingsI, 'webSearchConnectionId' | 'webSearchProvider'>) =>
        (checked: boolean) => {
            commit({
                ...builtInTools,
                [key]: checked,
                ...(key === 'webSearch' && !checked ? {webSearchConnectionId: null} : {}),
            });
        };

    const handleProviderChange = (value: string) => {
        commit({...builtInTools, webSearchConnectionId: null, webSearchProvider: value as WebSearchProviderType});
    };

    const handleConnectionChange = (value: string) => {
        commit({...builtInTools, webSearchConnectionId: value === 'no-connection' ? null : Number(value)});
    };

    return (
        <AgentSection title="Settings">
            <fieldset className="space-y-4 border-0 p-0">
                <AgentSettingRow
                    control={
                        <Switch
                            aria-label="Stream response"
                            checked={topLevelSettings.streamResponse}
                            id="agent-settings-stream-response"
                            onCheckedChange={handleStreamResponseToggle}
                        />
                    }
                    controlId="agent-settings-stream-response"
                    description="Send the response back token by token instead of once it is complete."
                    label="Stream response"
                />

                <AgentSettingRow
                    control={
                        <Switch
                            aria-label="Ask user question"
                            checked={builtInTools.askUserQuestion}
                            id="agent-settings-ask-user-question"
                            onCheckedChange={handleToggle('askUserQuestion')}
                        />
                    }
                    controlId="agent-settings-ask-user-question"
                    description="Lets the agent pause a run to ask the user a clarifying question."
                    label="Ask user question"
                />

                <AgentSettingRow
                    control={
                        <Switch
                            aria-label="Chat memory"
                            checked={!!chatMemoryElement}
                            disabled={isChatMemoryBusy}
                            id="agent-settings-chat-memory"
                            onCheckedChange={handleChatMemoryToggle}
                        />
                    }
                    controlId="agent-settings-chat-memory"
                    description="Remembers prior turns within a session."
                    label="Chat memory"
                />

                <AgentSettingRow
                    control={
                        <Switch
                            aria-label="Auto memory"
                            checked={builtInTools.autoMemory}
                            id="agent-settings-auto-memory"
                            onCheckedChange={handleToggle('autoMemory')}
                        />
                    }
                    controlId="agent-settings-auto-memory"
                    description="Lets the agent persist facts across conversations automatically."
                    label="Auto memory"
                />

                <AgentSettingRow
                    control={
                        <Switch
                            aria-label="Skill management"
                            checked={builtInTools.skillManagement}
                            id="agent-settings-skill-management"
                            onCheckedChange={handleToggle('skillManagement')}
                        />
                    }
                    controlId="agent-settings-skill-management"
                    description="Lets the agent create, update, and delete its own skills."
                    label="Skill management"
                />

                <AgentApprovalSettings agentId={agentId} channels={channels} elements={elements} />

                <AgentSettingRow
                    control={
                        <Switch
                            aria-label="Web search"
                            checked={builtInTools.webSearch}
                            id="agent-settings-web-search"
                            onCheckedChange={handleToggle('webSearch')}
                        />
                    }
                    controlId="agent-settings-web-search"
                    description="Lets the agent search the web."
                    label="Web search"
                />

                {builtInTools.webSearch && (
                    <div className="ml-1 space-y-4 border-l border-stroke-neutral-secondary pl-4">
                        <AgentSettingRow
                            control={
                                <Select
                                    disabled={isWebSearchConnectionBusy}
                                    onValueChange={handleProviderChange}
                                    value={builtInTools.webSearchProvider}
                                >
                                    <SelectTrigger className="w-56" id="agent-settings-web-search-provider">
                                        <SelectValue />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value="BRAVE">Brave</SelectItem>

                                        <SelectItem value="FIRECRAWL">Firecrawl</SelectItem>

                                        <SelectItem value="NATIVE">Model provider (native)</SelectItem>
                                    </SelectContent>
                                </Select>
                            }
                            controlId="agent-settings-web-search-provider"
                            label="Search provider"
                        />

                        {webSearchProvider.componentName ? (
                            <AgentSettingRow
                                control={
                                    <Select
                                        disabled={isWebSearchConnectionBusy}
                                        onValueChange={handleConnectionChange}
                                        value={builtInTools.webSearchConnectionId?.toString() ?? 'no-connection'}
                                    >
                                        <SelectTrigger className="w-56" id="agent-settings-web-search-connection">
                                            <SelectValue placeholder="Choose a connection…" />
                                        </SelectTrigger>

                                        <SelectContent>
                                            <SelectItem value="no-connection">No connection</SelectItem>

                                            {connections.map((connection) => (
                                                <SelectItem key={connection.id} value={String(connection.id)}>
                                                    {connection.name}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                }
                                controlId="agent-settings-web-search-connection"
                                label={`${webSearchProvider.title} connection`}
                            />
                        ) : (
                            <p className="text-sm text-muted-foreground">
                                {modelProvider && !NATIVE_WEB_SEARCH_MODEL_PROVIDERS.includes(modelProvider)
                                    ? `${modelProvider} has no built-in web search. Supported: ${NATIVE_WEB_SEARCH_MODEL_PROVIDERS.join(', ')}.`
                                    : 'The model searches the web itself. No connection needed.'}
                            </p>
                        )}
                    </div>
                )}

                <AgentSettingRow
                    control={
                        <Switch
                            aria-label="Thinking"
                            checked={topLevelSettings.thinking}
                            id="agent-settings-thinking"
                            onCheckedChange={handleThinkingToggle}
                        />
                    }
                    controlId="agent-settings-thinking"
                    description="Let the model reason before responding."
                    label="Thinking"
                />

                {topLevelSettings.thinking && (
                    <div className="ml-1 space-y-4 border-l border-stroke-neutral-secondary pl-4">
                        <AgentSettingRow
                            control={
                                <Select
                                    onValueChange={handleReasoningEffortChange}
                                    value={topLevelSettings.reasoningEffort}
                                >
                                    <SelectTrigger className="w-56" id="agent-settings-reasoning-effort">
                                        <SelectValue />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectItem value="LOW">Low</SelectItem>

                                        <SelectItem value="MEDIUM">Medium</SelectItem>

                                        <SelectItem value="HIGH">High</SelectItem>
                                    </SelectContent>
                                </Select>
                            }
                            controlId="agent-settings-reasoning-effort"
                            label="Reasoning effort"
                        />

                        {modelProvider && !THINKING_MODEL_PROVIDERS.includes(modelProvider) && (
                            <p className="text-sm text-muted-foreground">
                                {`${modelProvider} has no extended reasoning. Supported: ${THINKING_MODEL_PROVIDERS.join(', ')}.`}
                            </p>
                        )}
                    </div>
                )}

                <AgentSettingRow
                    control={
                        <Input
                            className="w-56"
                            defaultValue={topLevelSettings.maxToolCalls ?? ''}
                            id="agent-settings-max-tool-calls"
                            max={1000}
                            min={1}
                            onBlur={(event) => handleMaxToolCallsBlur(event.target.value)}
                            placeholder={String(DEFAULT_MAX_TOOL_CALLS)}
                            type="number"
                        />
                    }
                    controlId="agent-settings-max-tool-calls"
                    description={`Across all tools in one run. Leave empty for ${DEFAULT_MAX_TOOL_CALLS}.`}
                    label="Max tool calls"
                />
            </fieldset>
        </AgentSection>
    );
};

export default AgentSettingsCard;
