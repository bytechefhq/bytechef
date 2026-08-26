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

// Mirrors AiAgentSettings (automation-ai-agent-service) — absence of the `builtInTools` key, of any
// individual key inside it, or of a top-level key means that key's default below applies. There is no
// separate "explicitly unset" state, so every commit sends every key explicitly (replace-whole semantics
// on updateAiAgentSettings — see AiAgentFacadeImpl.updateAgentSettings).
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

// Mirrors AiAgentSettings.WebSearchProvider (automation-ai-agent-service). A null componentName marks the one
// provider that is not a tool of its own: NATIVE runs inside the model call and so has no connection to pick.
const WEB_SEARCH_PROVIDERS: Record<WebSearchProviderType, {componentName: string | null; title: string}> = {
    BRAVE: {componentName: 'brave', title: 'Brave'},
    FIRECRAWL: {componentName: 'firecrawl', title: 'Firecrawl'},
    NATIVE: {componentName: null, title: 'Model provider'},
};

// Mirrors AiAgentSettings.NATIVE_WEB_SEARCH_MODEL_PROVIDERS. Spring AI 2.0.1 exposes a provider-side web search
// on anthropic alone, so publishing NATIVE against any other model provider is rejected server-side — this list
// exists to say so before the user hits publish, not to enforce it.
const NATIVE_WEB_SEARCH_MODEL_PROVIDERS = ['anthropic'];

// Mirrors AiAgentSettings.THINKING_MODEL_PROVIDERS. Only a provider whose model cluster element declares the
// `thinking` property can act on it, so publishing against any other is rejected server-side — this list exists
// to say so before the user hits publish, not to enforce it.
const THINKING_MODEL_PROVIDERS = ['anthropic', 'openAi'];

const REASONING_EFFORTS: ReasoningEffortType[] = ['LOW', 'MEDIUM', 'HIGH'];

// Spring AI's DefaultToolCallingManager.DEFAULT_MAX_TOTAL_TOOL_CALLS — what applies when maxToolCalls is unset.
// Shown rather than sent: the field stays empty, so the agent keeps following the platform default even if that
// default later changes.
const DEFAULT_MAX_TOOL_CALLS = 150;

type ReasoningEffortType = 'HIGH' | 'LOW' | 'MEDIUM';

// The keys that sit beside `builtInTools` rather than inside it, because none of them is a tool: `streamResponse`
// picks which aiAgent action the generated workflow runs (streamChat when on, chat when off — the same choice the
// workflow editor's AI Agent panel offers), `thinking`/`reasoningEffort` are written onto the MODEL cluster
// element, and `maxToolCalls` onto the aiAgent node itself.
interface TopLevelSettingsI {
    maxToolCalls: number | null;
    reasoningEffort: ReasoningEffortType;
    streamResponse: boolean;
    thinking: boolean;
}

// maxToolCalls alone has no default to pin: absent means the platform's own cap applies, and inventing a number
// here would silently pin every agent to it.
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
    /** Passed through to the approval settings, which name the channels approvals are delivered over. */
    channels: AiAgentChannel[];
    elements: AiAgentElement[];
    settings: unknown;
}

const AgentSettingsCard = ({agentId, channels, elements, settings}: AgentSettingsCardProps) => {
    const [builtInTools, setBuiltInTools] = useState<BuiltInToolsSettingsI>(() => resolveBuiltInTools(settings));
    const [topLevelSettings, setTopLevelSettings] = useState<TopLevelSettingsI>(() =>
        resolveTopLevelSettings(settings)
    );

    // Chat memory sits in this toggle list beside the built-in tools, but it is not one of them: it is a
    // CHAT_MEMORY AiAgentElement row, added and deleted rather than flipped in the settings map. The two
    // backends stay separate — only the presentation is shared.
    const chatMemoryElement = elements.find((element) => element.kind === 'CHAT_MEMORY');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const webSearchProvider = WEB_SEARCH_PROVIDERS[builtInTools.webSearchProvider];

    // The MODEL element carries the agent's provider, which is what decides whether NATIVE can work at all.
    const modelProvider = elements.find((element) => element.kind === 'MODEL')?.parameters?.provider as
        string | undefined;

    const {data: connections = []} = useGetWorkspaceConnectionsQuery(
        {
            componentName: webSearchProvider.componentName ?? '',
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId,
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

    // Only the element-backed toggles below need a busy flag. The settings-map switches read from local
    // state that `commit` updates before it fires the mutation, so their displayed value is already correct
    // and disabling them would just grey out the entire list every time one of them is flipped.
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

    // nextTopLevel defaults to the current value so the built-in-tool call sites stay one argument — but its
    // keys are still SENT on every commit, because updateAiAgentSettings replaces the whole map and an omitted
    // key would silently reset that setting to its default.
    const commit = (next: BuiltInToolsSettingsI, nextTopLevel: TopLevelSettingsI = topLevelSettings) => {
        setBuiltInTools(next);
        setTopLevelSettings(nextTopLevel);

        // The four booleans and the provider are always sent explicitly (replace-whole semantics — see
        // this file's top comment); webSearchConnectionId and maxToolCalls are included only when set, since
        // neither has a default to pin.
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

    // Committed on blur rather than per keystroke: every commit is a mutation, and a three-digit cap typed one
    // digit at a time would fire three of them, the first two carrying a number the user never meant (1, then 15,
    // then 150). An empty field clears the cap back to the platform default.
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
                // Turning webSearch off drops the connection id along with it — a stale id sitting on a
                // disabled built-in would otherwise silently linger in the map with no way to see it.
                ...(key === 'webSearch' && !checked ? {webSearchConnectionId: null} : {}),
            });
        };

    // A connection belongs to one component, so it cannot survive a provider change — a Brave connection id left
    // behind on FIRECRAWL would resolve to a connection of the wrong component at deployment time.
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

                {/* Element-backed rather than settings-backed, like Chat memory above — see the component's
                    own docs. Rendered inline so the HITL switches sit in this one toggle list instead of a
                    separate section of their own. */}

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
