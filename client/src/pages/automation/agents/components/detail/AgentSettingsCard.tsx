import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import AgentApprovalSettings from '@/pages/automation/agents/components/detail/AgentApprovalSettings';
import AgentSection from '@/pages/automation/agents/components/detail/AgentSection';
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

// Mirrors AiAgentSettings (automation-ai-agent-service) — absence of the `builtInTools` key, or of
// any individual key inside it, means that key's default below applies. There is no separate
// "explicitly unset" state, so every commit sends every key explicitly (replace-whole semantics on
// updateAiAgentSettings — see AiAgentFacadeImpl.updateAgentSettings).
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

    const commit = (next: BuiltInToolsSettingsI) => {
        setBuiltInTools(next);

        // The four booleans and the provider are always sent explicitly (replace-whole semantics — see
        // this file's top comment); webSearchConnectionId is included only when set, since it has no
        // default to pin.
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
            },
        });
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
                <Switch
                    checked={builtInTools.askUserQuestion}
                    description="Lets the agent pause a run to ask the user a clarifying question."
                    label="Ask user question"
                    onCheckedChange={handleToggle('askUserQuestion')}
                />

                <Switch
                    checked={!!chatMemoryElement}
                    description="Remembers prior turns within a session."
                    disabled={isChatMemoryBusy}
                    label="Chat memory"
                    onCheckedChange={handleChatMemoryToggle}
                />

                <Switch
                    checked={builtInTools.autoMemory}
                    description="Lets the agent persist facts across conversations automatically."
                    label="Auto memory"
                    onCheckedChange={handleToggle('autoMemory')}
                />

                <Switch
                    checked={builtInTools.skillManagement}
                    description="Lets the agent create, update, and delete its own skills."
                    label="Skill management"
                    onCheckedChange={handleToggle('skillManagement')}
                />

                {/* Element-backed rather than settings-backed, like Chat memory above — see the component's
                    own docs. Rendered inline so the HITL switches sit in this one toggle list instead of a
                    separate section of their own. */}

                <AgentApprovalSettings agentId={agentId} channels={channels} elements={elements} />

                <Switch
                    checked={builtInTools.webSearch}
                    description="Lets the agent search the web."
                    label="Web search"
                    onCheckedChange={handleToggle('webSearch')}
                />

                {builtInTools.webSearch && (
                    <div className="ml-0 space-y-4 sm:ml-11">
                        <div className="space-y-1">
                            <Label htmlFor="agent-settings-web-search-provider">Search provider</Label>

                            <Select
                                disabled={isWebSearchConnectionBusy}
                                onValueChange={handleProviderChange}
                                value={builtInTools.webSearchProvider}
                            >
                                <SelectTrigger className="sm:w-64" id="agent-settings-web-search-provider">
                                    <SelectValue />
                                </SelectTrigger>

                                <SelectContent>
                                    <SelectItem value="BRAVE">Brave</SelectItem>

                                    <SelectItem value="FIRECRAWL">Firecrawl</SelectItem>

                                    <SelectItem value="NATIVE">Model provider (native)</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        {webSearchProvider.componentName ? (
                            <div className="space-y-1">
                                <Label htmlFor="agent-settings-web-search-connection">
                                    {webSearchProvider.title} connection
                                </Label>

                                <Select
                                    disabled={isWebSearchConnectionBusy}
                                    onValueChange={handleConnectionChange}
                                    value={builtInTools.webSearchConnectionId?.toString() ?? 'no-connection'}
                                >
                                    <SelectTrigger className="sm:w-64" id="agent-settings-web-search-connection">
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
                            </div>
                        ) : (
                            <p className="text-sm text-muted-foreground">
                                {modelProvider && !NATIVE_WEB_SEARCH_MODEL_PROVIDERS.includes(modelProvider)
                                    ? `The model provider ${modelProvider} has no built-in web search, so this agent cannot be published with it. Supported: ${NATIVE_WEB_SEARCH_MODEL_PROVIDERS.join(', ')}.`
                                    : 'The model searches the web itself. No connection needed.'}
                            </p>
                        )}
                    </div>
                )}
            </fieldset>
        </AgentSection>
    );
};

export default AgentSettingsCard;
