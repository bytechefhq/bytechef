import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Popover, PopoverAnchor} from '@/components/ui/popover';
import AiHubAddConnectorDialog from '@/ee/pages/automation/ai-hub/context/AiHubAddConnectorDialog';
import AiHubAddMcpServerDialog from '@/ee/pages/automation/ai-hub/context/AiHubAddMcpServerDialog';
import AiHubConnectConnectionDialog from '@/ee/pages/automation/ai-hub/context/AiHubConnectConnectionDialog';
import AiHubConnectorToolPropertiesPopover from '@/ee/pages/automation/ai-hub/context/AiHubConnectorToolPropertiesPopover';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    AiHubMcpServersQuery,
    AiHubUserConnectorsQuery,
    useAiHubMcpServerToolsQuery,
    useAiHubMcpServersQuery,
    useAiHubUserConnectorsQuery,
    useRemoveAiHubMcpServerMutation,
    useRemoveAiHubUserConnectorMutation,
    useSetAiHubMcpServerEnabledMutation,
    useSetAiHubMcpServerToolEnabledMutation,
    useSetAiHubUserConnectorEnabledMutation,
    useSetAiHubUserConnectorToolEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {
    BoltIcon,
    ChevronDownIcon,
    ChevronRightIcon,
    EllipsisVerticalIcon,
    Link2Icon,
    Loader2Icon,
    PencilIcon,
    PlugIcon,
    PlusIcon,
    ServerIcon,
    Trash2Icon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

type UserConnectorType = AiHubUserConnectorsQuery['aiHubUserConnectors'][number];

// How many added connectors to show before the "Show N more" expander.
const PREVIEW_COUNT = 8;

interface ConnectorRowProps {
    connector: UserConnectorType;
    onConnect: () => void;
    onRemove: () => void;
    onToggle: (enabled: boolean) => void;
    onToggleTool: (toolName: string, enabled: boolean) => void;
    workspaceId: string;
}

/**
 * One added-connector row: chevron (reveals the tools, each with its own Configure popover + toggle), icon,
 * title/description, and either a Connect action (component needs a connection but has none) or the
 * component-level enabled toggle + remove. The chevron is its own trigger so expanding doesn't collide with the
 * controls on the same row.
 */
const ConnectorRow = ({connector, onConnect, onRemove, onToggle, onToggleTool, workspaceId}: ConnectorRowProps) => {
    const [expanded, setExpanded] = useState(false);
    const [configuringTool, setConfiguringTool] = useState<string | null>(null);

    const needsConnection = connector.connectionRequired && connector.connectionId == null;

    return (
        <Collapsible className="group rounded-md border border-border" onOpenChange={setExpanded} open={expanded}>
            <div className="flex items-center gap-2.5 px-3 py-2.5">
                <CollapsibleTrigger asChild>
                    <button
                        aria-label={expanded ? 'Hide tools' : 'Show tools'}
                        className="shrink-0 text-muted-foreground hover:text-foreground"
                        type="button"
                    >
                        {expanded ? <ChevronDownIcon className="size-4" /> : <ChevronRightIcon className="size-4" />}
                    </button>
                </CollapsibleTrigger>

                {connector.icon ? (
                    <InlineSVG className="size-6 shrink-0" src={connector.icon} />
                ) : (
                    <PlugIcon className="size-6 shrink-0 text-muted-foreground" />
                )}

                <div className="flex min-w-0 flex-1 flex-col">
                    <span className="truncate text-sm font-medium">{connector.title ?? connector.componentName}</span>
                </div>

                {needsConnection ? (
                    <Button label="Connect" onClick={onConnect} size="sm" variant="outline" />
                ) : (
                    <Switch checked={connector.enabled} onCheckedChange={onToggle} />
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button icon={<EllipsisVerticalIcon />} size="iconSm" variant="ghost" />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={onConnect}>
                            <PencilIcon />
                            Edit connection
                        </DropdownMenuItem>

                        <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={onRemove}>
                            <Trash2Icon />
                            Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            <CollapsibleContent>
                <div className="flex flex-col gap-1 border-t border-border px-3 py-2 pl-10">
                    {connector.tools.length === 0 ? (
                        <span className="text-xs text-muted-foreground">No tools.</span>
                    ) : (
                        connector.tools.map((tool) => (
                            <div className="flex items-center gap-2 py-0.5" key={tool.name}>
                                <div className="flex min-w-0 flex-1 flex-col">
                                    <span className="text-sm font-medium">{tool.title ?? tool.name}</span>

                                    {tool.description && (
                                        <span className="text-xs text-muted-foreground">{tool.description}</span>
                                    )}
                                </div>

                                <Switch
                                    checked={tool.enabled}
                                    onCheckedChange={(checked) => onToggleTool(tool.name, checked)}
                                />

                                <Popover
                                    onOpenChange={(open) => !open && setConfiguringTool(null)}
                                    open={configuringTool === tool.name}
                                >
                                    <PopoverAnchor asChild>
                                        <button
                                            aria-label={`Configure ${tool.title ?? tool.name}`}
                                            className="shrink-0 rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                            onClick={() => setConfiguringTool(tool.name)}
                                            title="Configure"
                                            type="button"
                                        >
                                            <BoltIcon className="size-4" />
                                        </button>
                                    </PopoverAnchor>

                                    {configuringTool === tool.name && (
                                        <AiHubConnectorToolPropertiesPopover
                                            componentName={connector.componentName}
                                            componentVersion={connector.componentVersion}
                                            connectionId={connector.connectionId}
                                            connectorId={connector.id}
                                            onClose={() => setConfiguringTool(null)}
                                            tool={tool}
                                            workspaceId={workspaceId}
                                        />
                                    )}
                                </Popover>
                            </div>
                        ))
                    )}
                </div>
            </CollapsibleContent>
        </Collapsible>
    );
};

type McpServerType = AiHubMcpServersQuery['aiHubMcpServers'][number];

interface McpServerRowProps {
    mcpServer: McpServerType;
    onRemove: () => void;
    onToggle: (enabled: boolean) => void;
    workspaceId: string;
}

/**
 * One custom MCP server row: chevron (reveals the server's tools, discovered live on expand, each with its own
 * toggle), icon, name + URL (with an auth hint when a token is set), an enabled toggle, and a remove button.
 * Toggling a tool off filters it out of the agent's tool list.
 */
const McpServerRow = ({mcpServer, onRemove, onToggle, workspaceId}: McpServerRowProps) => {
    const [expanded, setExpanded] = useState(false);

    const queryClient = useQueryClient();

    const {
        data: toolsData,
        isError,
        isLoading,
    } = useAiHubMcpServerToolsQuery({mcpServerId: mcpServer.id, workspaceId}, {enabled: expanded});

    const setToolEnabledMutation = useSetAiHubMcpServerToolEnabledMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiHubMcpServerTools']}),
    });

    const tools = toolsData?.aiHubMcpServerTools ?? [];

    return (
        <Collapsible className="group rounded-md border border-border" onOpenChange={setExpanded} open={expanded}>
            <div className="flex items-center gap-2.5 px-3 py-2.5">
                <CollapsibleTrigger asChild>
                    <button
                        aria-label={expanded ? 'Hide tools' : 'Show tools'}
                        className="shrink-0 text-muted-foreground hover:text-foreground"
                        type="button"
                    >
                        {expanded ? <ChevronDownIcon className="size-4" /> : <ChevronRightIcon className="size-4" />}
                    </button>
                </CollapsibleTrigger>

                <ServerIcon className="size-6 shrink-0 text-muted-foreground" />

                <div className="flex min-w-0 flex-1 flex-col">
                    <span className="truncate text-sm font-medium">{mcpServer.name}</span>

                    <span className="truncate text-xs text-muted-foreground">
                        {mcpServer.url}

                        {mcpServer.hasAuthToken && ' · authenticated'}
                    </span>
                </div>

                <Switch checked={mcpServer.enabled} onCheckedChange={onToggle} />

                <button
                    aria-label="Remove MCP server"
                    className="shrink-0 rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                    onClick={onRemove}
                    type="button"
                >
                    <Trash2Icon className="size-4" />
                </button>
            </div>

            <CollapsibleContent>
                <div className="flex flex-col gap-1 border-t border-border px-3 py-2 pl-10">
                    {isLoading && (
                        <span className="flex items-center gap-2 text-xs text-muted-foreground">
                            <Loader2Icon className="size-3.5 animate-spin" />
                            Loading tools…
                        </span>
                    )}

                    {isError && (
                        <span className="text-xs text-destructive">
                            Could not connect to this server to list its tools.
                        </span>
                    )}

                    {!isLoading && !isError && tools.length === 0 && (
                        <span className="text-xs text-muted-foreground">No tools.</span>
                    )}

                    {tools.map((tool) => (
                        <div className="flex items-center gap-2 py-0.5" key={tool.name}>
                            <div className="flex min-w-0 flex-1 flex-col">
                                <span className="text-sm font-medium">{tool.name}</span>

                                {tool.description && (
                                    <span className="text-xs text-muted-foreground">{tool.description}</span>
                                )}
                            </div>

                            <Switch
                                checked={tool.enabled}
                                onCheckedChange={(checked) =>
                                    setToolEnabledMutation.mutate({
                                        enabled: checked,
                                        mcpServerId: mcpServer.id,
                                        toolName: tool.name,
                                        workspaceId,
                                    })
                                }
                            />
                        </div>
                    ))}
                </div>
            </CollapsibleContent>
        </Collapsible>
    );
};

/**
 * AI Hub > Context > Connectors.
 *
 * Two sections: Pre-built Connectors (the user's globally-added connectors, with per-component and per-tool
 * toggles, Connect for components lacking a connection, and an Add Connector picker) and Custom MCP (the user's
 * registered external MCP servers, with an Add server dialog and enable/remove; tool bridging lands in a
 * follow-up).
 */
const AiHubConnectors = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [showAll, setShowAll] = useState(false);
    const [addOpen, setAddOpen] = useState(false);
    const [addServerOpen, setAddServerOpen] = useState(false);
    const [connectTarget, setConnectTarget] = useState<{componentName: string; componentVersion: number} | null>(null);

    const queryClient = useQueryClient();

    const {data, isLoading} = useAiHubUserConnectorsQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const {data: mcpServersData, isLoading: mcpServersLoading} = useAiHubMcpServersQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const invalidateConnectors = () => queryClient.invalidateQueries({queryKey: ['aiHubUserConnectors']});
    const invalidateMcpServers = () => queryClient.invalidateQueries({queryKey: ['aiHubMcpServers']});

    const setEnabledMutation = useSetAiHubUserConnectorEnabledMutation({onSuccess: invalidateConnectors});
    const setToolEnabledMutation = useSetAiHubUserConnectorToolEnabledMutation({onSuccess: invalidateConnectors});
    const removeMutation = useRemoveAiHubUserConnectorMutation({onSuccess: invalidateConnectors});
    const setMcpServerEnabledMutation = useSetAiHubMcpServerEnabledMutation({onSuccess: invalidateMcpServers});
    const removeMcpServerMutation = useRemoveAiHubMcpServerMutation({onSuccess: invalidateMcpServers});

    const connectors = useMemo(
        () =>
            [...(data?.aiHubUserConnectors ?? [])].sort((first, second) =>
                (first.title ?? first.componentName).localeCompare(second.title ?? second.componentName)
            ),
        [data]
    );

    const addedComponentNames = useMemo(
        () => new Set((data?.aiHubUserConnectors ?? []).map((connector) => connector.componentName)),
        [data]
    );

    const visibleConnectors = showAll ? connectors : connectors.slice(0, PREVIEW_COUNT);
    const hiddenCount = connectors.length - visibleConnectors.length;

    const mcpServers = mcpServersData?.aiHubMcpServers ?? [];

    const workspaceIdString = String(currentWorkspaceId ?? '');

    return (
        <LayoutContainer header={<Header centerTitle position="main" title="Connectors" />} leftSidebarOpen={false}>
            <div className="flex w-full flex-1 flex-col gap-10 px-4 py-6 3xl:mx-auto 3xl:w-4/5">
                {/* Pre-built Connectors */}

                <section className="flex flex-col gap-3">
                    <div className="flex items-center justify-between gap-4">
                        <div className="flex items-start gap-2">
                            <Link2Icon className="mt-0.5 size-5 shrink-0" />

                            <div className="flex flex-col">
                                <h3 className="text-base font-semibold">Pre-built Connectors</h3>

                                <p className="text-sm text-muted-foreground">
                                    Email, calendar, docs, chat — and anything else your agent needs to reach.
                                </p>
                            </div>
                        </div>

                        <Button
                            className="shrink-0"
                            icon={<PlusIcon />}
                            label="Add Connector"
                            onClick={() => setAddOpen(true)}
                        />
                    </div>

                    <div className="flex flex-col gap-1.5">
                        {isLoading && (
                            <div className="flex items-center justify-center gap-2 py-10 text-sm text-muted-foreground">
                                <Loader2Icon className="size-4 animate-spin" />

                                <span>Loading connectors…</span>
                            </div>
                        )}

                        {!isLoading && connectors.length === 0 && (
                            <div className="rounded-md border border-dashed border-border py-8 text-center text-sm text-muted-foreground">
                                No connectors yet. Add one to let the agent use its tools.
                            </div>
                        )}

                        {visibleConnectors.map((connector) => (
                            <ConnectorRow
                                connector={connector}
                                key={connector.id}
                                onConnect={() =>
                                    setConnectTarget({
                                        componentName: connector.componentName,
                                        componentVersion: connector.componentVersion,
                                    })
                                }
                                onRemove={() =>
                                    removeMutation.mutate({connectorId: connector.id, workspaceId: workspaceIdString})
                                }
                                onToggle={(enabled) =>
                                    setEnabledMutation.mutate({
                                        connectorId: connector.id,
                                        enabled,
                                        workspaceId: workspaceIdString,
                                    })
                                }
                                onToggleTool={(toolName, enabled) =>
                                    setToolEnabledMutation.mutate({
                                        connectorId: connector.id,
                                        enabled,
                                        toolName,
                                        workspaceId: workspaceIdString,
                                    })
                                }
                                workspaceId={workspaceIdString}
                            />
                        ))}

                        {hiddenCount > 0 && (
                            <button
                                className="mt-1 flex items-center justify-center gap-1 py-1 text-sm text-muted-foreground hover:text-foreground"
                                onClick={() => setShowAll(true)}
                                type="button"
                            >
                                Show {hiddenCount}
                                more
                                <ChevronDownIcon className="size-4" />
                            </button>
                        )}
                    </div>
                </section>

                {/* Custom MCP */}

                <section className="flex flex-col gap-3">
                    <div className="flex items-center justify-between gap-4">
                        <div className="flex items-start gap-2">
                            <ServerIcon className="mt-0.5 size-5 shrink-0" />

                            <div className="flex flex-col">
                                <h3 className="text-base font-semibold">Custom MCP</h3>

                                <p className="text-sm text-muted-foreground">
                                    Connect any Model Context Protocol (MCP) server by URL. Its tools become available
                                    to your agent in chat.
                                </p>
                            </div>
                        </div>

                        <Button icon={<PlusIcon />} label="Add server" onClick={() => setAddServerOpen(true)} />
                    </div>

                    <div className="flex flex-col gap-1.5">
                        {mcpServersLoading && (
                            <div className="flex items-center justify-center gap-2 py-10 text-sm text-muted-foreground">
                                <Loader2Icon className="size-4 animate-spin" />

                                <span>Loading servers…</span>
                            </div>
                        )}

                        {!mcpServersLoading && mcpServers.length === 0 && (
                            <div className="rounded-md border border-dashed border-border py-8 text-center text-sm text-muted-foreground">
                                No custom MCP servers yet.
                            </div>
                        )}

                        {mcpServers.map((mcpServer) => (
                            <McpServerRow
                                key={mcpServer.id}
                                mcpServer={mcpServer}
                                onRemove={() =>
                                    removeMcpServerMutation.mutate({
                                        mcpServerId: mcpServer.id,
                                        workspaceId: workspaceIdString,
                                    })
                                }
                                onToggle={(enabled) =>
                                    setMcpServerEnabledMutation.mutate({
                                        enabled,
                                        mcpServerId: mcpServer.id,
                                        workspaceId: workspaceIdString,
                                    })
                                }
                                workspaceId={workspaceIdString}
                            />
                        ))}
                    </div>
                </section>
            </div>

            {addOpen && (
                <AiHubAddConnectorDialog addedComponentNames={addedComponentNames} onClose={() => setAddOpen(false)} />
            )}

            {addServerOpen && <AiHubAddMcpServerDialog onClose={() => setAddServerOpen(false)} />}

            {connectTarget && (
                <AiHubConnectConnectionDialog
                    componentName={connectTarget.componentName}
                    componentVersion={connectTarget.componentVersion}
                    onClose={() => setConnectTarget(null)}
                />
            )}
        </LayoutContainer>
    );
};

export default AiHubConnectors;
