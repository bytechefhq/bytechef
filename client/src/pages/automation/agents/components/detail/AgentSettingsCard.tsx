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
interface BuiltInToolsSettingsI {
    askUserQuestion: boolean;
    autoMemory: boolean;
    skillManagement: boolean;
    webSearch: boolean;
    webSearchConnectionId: number | null;
}

const DEFAULT_BUILT_IN_TOOLS: BuiltInToolsSettingsI = {
    askUserQuestion: true,
    autoMemory: true,
    skillManagement: true,
    webSearch: false,
    webSearchConnectionId: null,
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

    const {data: connections = []} = useGetWorkspaceConnectionsQuery(
        {componentName: 'brave', environmentId: currentEnvironmentId, id: currentWorkspaceId},
        builtInTools.webSearch
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

        // The five booleans are always sent explicitly (replace-whole semantics — see this file's
        // top comment); webSearchConnectionId is included only when set, since it has no boolean
        // default to pin.
        updateAiAgentSettingsMutation.mutate({
            id: agentId,
            settings: {
                builtInTools: {
                    askUserQuestion: next.askUserQuestion,
                    autoMemory: next.autoMemory,
                    skillManagement: next.skillManagement,
                    webSearch: next.webSearch,
                    ...(next.webSearchConnectionId != null ? {webSearchConnectionId: next.webSearchConnectionId} : {}),
                },
            },
        });
    };

    const handleToggle = (key: keyof Omit<BuiltInToolsSettingsI, 'webSearchConnectionId'>) => (checked: boolean) => {
        commit({
            ...builtInTools,
            [key]: checked,
            // Turning webSearch off drops the connection id along with it — a stale id sitting on a
            // disabled built-in would otherwise silently linger in the map with no way to see it.
            ...(key === 'webSearch' && !checked ? {webSearchConnectionId: null} : {}),
        });
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
                    description="Lets the agent search the web via Brave."
                    label="Web search"
                    onCheckedChange={handleToggle('webSearch')}
                />

                {builtInTools.webSearch && (
                    <div className="ml-0 space-y-1 sm:ml-8">
                        <Label htmlFor="agent-settings-web-search-connection">Brave connection</Label>

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
                )}
            </fieldset>
        </AgentSection>
    );
};

export default AgentSettingsCard;
