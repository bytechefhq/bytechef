import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {CommandItem} from '@/components/ui/command';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ReferencedResourceKindType} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {
    AiHubPersonalAgentResourceI,
    AiHubPersonalAgentToolI,
    useAddAiHubPersonalAgentResourceMutation,
    useAddAiHubPersonalAgentToolMutation,
    useRemoveAiHubPersonalAgentResourceMutation,
    useRemoveAiHubPersonalAgentToolMutation,
} from '@/ee/pages/automation/ai-hub/personal-agents/hooks/useAiHubPersonalAgents';
import ResourcePickerMenu, {
    ResourcePickerSelectionI,
} from '@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu';
import WorkflowNodesTabsItem from '@/pages/platform/workflow-editor/components/workflow-nodes-tabs/WorkflowNodesTabsItem';
import {useFilteredComponentDefinitions} from '@/pages/platform/workflow-editor/hooks/useFilteredComponentDefinitions';
import {AiHubPersonalAgentResourceKind} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {
    ArrowLeftIcon,
    ClockIcon,
    ComponentIcon,
    DatabaseIcon,
    EllipsisVerticalIcon,
    FileTextIcon,
    HistoryIcon,
    InfoIcon,
    LinkIcon,
    type LucideIcon,
    PlusIcon,
    ServerIcon,
    SettingsIcon,
    TrashIcon,
    VectorSquareIcon,
    WorkflowIcon,
    WrenchIcon,
} from 'lucide-react';
import {ReactNode, useCallback, useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import AiHubPersonalAgentToolConfigDialog from './AiHubPersonalAgentToolConfigDialog';

/**
 * Local-state shape used during agent creation. Mirrors the persisted {@link AiHubPersonalAgentToolI} minus the
 * server-assigned {@code id} and {@code aiHubPersonalAgentId} fields, since neither exists yet — the parent form
 * collects these in memory and bulk-attaches them after the agent itself has been created.
 */
export interface AiHubPersonalAgentPendingToolI {
    componentName: string;
    componentVersion: number;
    operationName: string;
}

/**
 * Local-state shape used during agent creation for a reference-kind resource. Mirrors the persisted
 * {@link AiHubPersonalAgentResourceI} minus the server-assigned {@code id} and {@code aiHubPersonalAgentId}
 * fields. {@code kind} holds the SCREAMING_SNAKE {@code AiHubPersonalAgentResourceKind} value — the composer's
 * lowercase kind is normalized at the {@code ResourcePickerMenu.onSelect} boundary.
 */
export interface AiHubPersonalAgentPendingResourceI {
    kind: string;
    resourceId: string;
    resourceName: string;
}

/**
 * Maps the composer's lowercase {@link ReferencedResourceKindType} (emitted by {@code ResourcePickerMenu.onSelect})
 * to the SCREAMING_SNAKE {@code AiHubPersonalAgentResourceKind} the GraphQL mutation and persisted rows use.
 * Normalize at the onSelect boundary so pending resources, mutation input, persisted resources, and
 * {@link ResourceRow} all uniformly use the SCREAMING_SNAKE form.
 */
const COMPOSER_KIND_TO_RESOURCE_KIND: Record<ReferencedResourceKindType, string> = {
    apiCollection: 'API_COLLECTION',
    dataTable: 'DATA_TABLE',
    file: 'FILE',
    knowledgeBase: 'KNOWLEDGE_BASE',
    mcpServer: 'MCP_SERVER',
    task: 'TASK',
    workflow: 'WORKFLOW',
    workflowExecution: 'WORKFLOW_EXECUTION',
};

// Lucide icon per SCREAMING_SNAKE resource kind, matching what the composer uses for that kind. Falls back to
// WrenchIcon for an unrecognised kind so a stale client never crashes on a server that added a new kind.
const RESOURCE_KIND_ICONS: Record<string, LucideIcon> = {
    API_COLLECTION: LinkIcon,
    DATA_TABLE: DatabaseIcon,
    FILE: FileTextIcon,
    KNOWLEDGE_BASE: VectorSquareIcon,
    MCP_SERVER: ServerIcon,
    TASK: ClockIcon,
    WORKFLOW: WorkflowIcon,
    WORKFLOW_EXECUTION: HistoryIcon,
};

interface AiHubPersonalAgentResourcesCardEditPropsI {
    /** Persisted agent id. Add/remove fire server-side mutations against the row. */
    aiHubPersonalAgentId: number;
    /** Environment context for the resource picker's per-kind queries. */
    environmentId: number;
    /** Current persisted reference-kind resource rows. */
    resources: AiHubPersonalAgentResourceI[];
    /** Current persisted tool template rows. */
    tools: AiHubPersonalAgentToolI[];
    /** Workspace ownership context for the mutations and the resource picker. */
    workspaceId: number;
}

interface AiHubPersonalAgentResourcesCardCreatePropsI {
    /** Environment context for the resource picker's per-kind queries. */
    environmentId: number;
    /** Local-state add for a reference-kind resource: parent form bulk-attaches after agent create. */
    onAddPendingResource: (resource: AiHubPersonalAgentPendingResourceI) => void;
    /** Local-state add: parent form holds the in-memory list and bulk-attaches after agent create. */
    onAddPendingTool: (tool: AiHubPersonalAgentPendingToolI) => void;
    /** Local-state remove for a reference-kind resource: by index since pending resources have no server id. */
    onRemovePendingResource: (index: number) => void;
    /** Local-state remove: by index since pending tools have no server id yet. */
    onRemovePendingTool: (index: number) => void;
    /** Reference-kind resources queued for bulk-attach after the agent row exists. */
    pendingResources: AiHubPersonalAgentPendingResourceI[];
    /** Tools queued for bulk-attach after the agent row exists. */
    pendingTools: AiHubPersonalAgentPendingToolI[];
    /** Workspace context for the resource picker. */
    workspaceId: number;
}

type AiHubPersonalAgentResourcesCardPropsType =
    AiHubPersonalAgentResourcesCardEditPropsI | AiHubPersonalAgentResourcesCardCreatePropsI;

const isEditModeProps = (
    props: AiHubPersonalAgentResourcesCardPropsType
): props is AiHubPersonalAgentResourcesCardEditPropsI => 'aiHubPersonalAgentId' in props;

interface ToolRowPropsI {
    componentName: string;
    componentVersion: number;
    /**
     * When true, the Configure dropdown item is hidden — used in create-mode where pending tools don't yet
     * have a persisted id to update. The Remove item stays available so the user can still curate the list
     * before saving.
     */
    configureDisabled?: boolean;
    onConfigure: () => void;
    onRemove: () => void;
    operationName: string;
    removeDisabled?: boolean;
}

/**
 * Single tool row, styled to match {@code AiAgentTool} from the AI Agent simple-mode panel: muted
 * background, the component's SVG icon on the left, "{componentLabel} - {operationName}" in the middle,
 * and a 3-dot dropdown on the right with Configure + Remove. Fetches the component definition per row to
 * pick up the live title + icon — same pattern as {@code AiAgentTool}, which trades a per-row query for
 * richer metadata than what's persisted on the personal-agent-tool row itself (the persisted row only has
 * component name + version + operation name).
 */
const ToolRow = ({
    componentName,
    componentVersion,
    configureDisabled,
    onConfigure,
    onRemove,
    operationName,
    removeDisabled,
}: ToolRowPropsI) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {componentName, componentVersion},
        !!componentName
    );

    return (
        <div className="flex items-center gap-2 rounded bg-muted/50 p-2">
            {componentDefinition?.icon ? (
                <InlineSVG
                    className="size-5 flex-none text-content-neutral-secondary"
                    loader={<ComponentIcon className="size-5 flex-none text-content-neutral-secondary" />}
                    src={componentDefinition.icon}
                />
            ) : (
                <ComponentIcon className="size-5 flex-none text-content-neutral-secondary" />
            )}

            <div className="flex flex-1 items-center justify-start gap-1 truncate">
                <span className="text-xs font-medium">{componentDefinition?.title || componentName}</span>

                <span className="px-1 text-xs font-medium">-</span>

                <span className="flex-1 text-xs font-medium">{operationName}</span>
            </div>

            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button className="h-6" disabled={removeDisabled} size="sm" variant="ghost">
                        <EllipsisVerticalIcon className="size-3 text-content-neutral-tertiary" />
                    </Button>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    {!configureDisabled && (
                        <DropdownMenuItem onClick={onConfigure}>
                            <SettingsIcon />
                            Configure
                        </DropdownMenuItem>
                    )}

                    <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={onRemove}>
                        <TrashIcon />
                        Remove
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
};

interface ResourceRowPropsI {
    /** SCREAMING_SNAKE resource kind — drives the leading icon. */
    kind: string;
    onRemove: () => void;
    removeDisabled?: boolean;
    resourceName: string;
}

/**
 * Single reference-kind resource row, styled like {@link ToolRow}: muted background, a kind icon on the
 * left, the resource name in the middle, and a 3-dot dropdown on the right. Resources have no per-row
 * configuration, so the dropdown exposes only Remove.
 */
const ResourceRow = ({kind, onRemove, removeDisabled, resourceName}: ResourceRowPropsI) => {
    const KindIcon = RESOURCE_KIND_ICONS[kind] ?? WrenchIcon;

    return (
        <div className="flex items-center gap-2 rounded bg-muted/50 p-2">
            <KindIcon className="size-5 flex-none text-content-neutral-secondary" />

            <div className="flex flex-1 items-center justify-start gap-1 truncate">
                <span className="flex-1 truncate text-xs font-medium">{resourceName}</span>
            </div>

            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button className="h-6" disabled={removeDisabled} size="sm" variant="ghost">
                        <EllipsisVerticalIcon className="size-3 text-content-neutral-tertiary" />
                    </Button>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem className="text-destructive focus:text-destructive" onClick={onRemove}>
                        <TrashIcon />
                        Remove
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
};

interface ToolsBranchBodyPropsI {
    /** When true, action picks are disabled while the add-tool mutation is in flight (edit mode). */
    addPending: boolean;
    /** Returns to the picker's root menu. */
    onBack: () => void;
    /** Closes the whole picker popover — called once a tool is finally picked. */
    onClose: () => void;
    /** Fires for the picked action; the card decides edit-mutation vs. pending-add. */
    onPickAction: (componentName: string, componentVersion: number, operationName: string) => void;
}

/**
 * The component→action drill-down body rendered inside {@code ResourcePickerMenu}'s {@code toolsBranch} slot.
 * This is the same self-contained picker {@code AiHubPersonalAgentToolsCard} previously hosted in its own
 * popover — relocated under the shared resource picker so Tools and the 8 reference kinds share one "Add"
 * affordance. {@link WorkflowNodesTabsItem} is used directly (rather than {@code WorkflowNodesPopoverMenu})
 * because the latter pulls workflow state from {@code useWorkflowDataStore}, which only exists inside the
 * workflow editor provider.
 */
const ToolsBranchBody = ({addPending, onBack, onClose, onPickAction}: ToolsBranchBodyPropsI) => {
    // null = component-list step. Non-null = action-list step for that component.
    const [pickedComponentName, setPickedComponentName] = useState<string | null>(null);

    const {data: componentDefinitions, isLoading: isLoadingComponents} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
    });

    // Reuse the workflow editor's filter+debounce hook so the search behaviour (debounce delay, server-side
    // refetch on long queries) matches the workflow editor exactly. Pure-function: takes the basic list as
    // input, returns a filtered version + setter — no store dependency.
    const {componentsWithActions, filter, isSearchFetching, setFilter} = useFilteredComponentDefinitions(
        componentDefinitions ?? []
    );

    // The basic-list endpoint omits the per-component action list to keep the payload small. We refetch the
    // full definition only when a component is picked.
    const pickedComponentVersion =
        componentDefinitions?.find((component) => component.name === pickedComponentName)?.version ?? 1;

    const {data: pickedComponent, isLoading: isLoadingActions} = useGetComponentDefinitionQuery(
        {componentName: pickedComponentName ?? '', componentVersion: pickedComponentVersion},
        pickedComponentName !== null
    );

    const handleAddAction = (operationName: string) => {
        if (!pickedComponent) {
            return;
        }

        onPickAction(pickedComponent.name, pickedComponent.version, operationName);

        // Close the whole picker popover once a tool is finally picked — keeps parity with the old
        // self-contained popover, which dismissed on selection.
        onClose();
    };

    if (pickedComponentName === null) {
        return (
            <div className="flex flex-col">
                {/*
                 * Header mirrors WorkflowNodesPopoverMenuComponentList: filter input with an inline
                 * LoadingIcon when the server-side search query is in flight. The filter hook handles
                 * debounce + remote search; setting `pr-8` on the input during search reserves space for
                 * the spinner overlay.
                 */}

                <header className="flex items-center gap-2 border-b p-3">
                    <Button
                        aria-label="Back to resource list"
                        icon={<ArrowLeftIcon />}
                        onClick={onBack}
                        size="icon"
                        variant="ghost"
                    />

                    <div className="relative w-full">
                        <Input
                            autoFocus
                            className={twMerge('bg-surface-neutral-primary shadow-none', isSearchFetching && 'pr-8')}
                            id="personal-agent-tools-filter"
                            onChange={(event) => setFilter(event.target.value)}
                            placeholder="Filter components"
                            value={filter}
                        />

                        {isSearchFetching && (
                            <span
                                aria-label="Loading"
                                className="absolute top-1/2 right-2 -translate-y-1/2"
                                role="status"
                            >
                                <LoadingIcon className="text-content-neutral-secondary" />
                            </span>
                        )}
                    </div>
                </header>

                <div className="h-96 overflow-y-auto rounded-b-lg p-3">
                    {isLoadingComponents ? (
                        // Initial-load spinner: shown the first time the branch opens before the basic-list
                        // response lands. Visually centred so the body doesn't show empty content that
                        // might be mistaken for "no results".
                        <div className="flex h-full items-center justify-center">
                            <LoadingIcon className="text-content-neutral-secondary" />
                        </div>
                    ) : componentsWithActions.length === 0 ? (
                        <p className="px-2 py-3 text-xs text-muted-foreground">No components match your filter.</p>
                    ) : (
                        <ul className="space-y-1">
                            {componentsWithActions.map((component) => (
                                <WorkflowNodesTabsItem
                                    handleClick={() => setPickedComponentName(component.name)}
                                    key={component.name}
                                    node={{
                                        ...component,
                                        // WorkflowNodesTabsItem's node type expects these discriminator
                                        // booleans even though we don't use them. Pinning to false matches
                                        // how the workflow-editor populates "tool-style" cards.
                                        clusterElement: false,
                                        taskDispatcher: false,
                                        trigger: false,
                                    }}
                                />
                            ))}
                        </ul>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col">
            <header className="flex items-center gap-2 border-b p-3">
                <Button
                    aria-label="Back to component list"
                    icon={<ArrowLeftIcon />}
                    onClick={() => setPickedComponentName(null)}
                    size="icon"
                    variant="ghost"
                />

                <span className="flex size-5 flex-none items-center justify-center">
                    {pickedComponent?.icon ? (
                        <InlineSVG
                            className="size-5"
                            loader={<ComponentIcon className="size-5" />}
                            src={pickedComponent.icon}
                        />
                    ) : (
                        <ComponentIcon className="size-5" />
                    )}
                </span>

                <span className="truncate text-sm font-medium">{pickedComponent?.title || pickedComponentName}</span>
            </header>

            <div className="h-96 overflow-y-auto p-3">
                {isLoadingActions ? (
                    <div className="flex h-full items-center justify-center">
                        <LoadingIcon className="text-content-neutral-secondary" />
                    </div>
                ) : !pickedComponent || (pickedComponent.actions ?? []).length === 0 ? (
                    <p className="px-2 py-3 text-xs text-muted-foreground">
                        This component has no actions to expose as tools.
                    </p>
                ) : (
                    <ul className="space-y-1">
                        {pickedComponent.actions?.map((action) => (
                            <li key={action.name}>
                                <button
                                    className="flex w-full flex-col items-start gap-0.5 rounded-md p-2 text-left hover:bg-accent disabled:opacity-50"
                                    disabled={addPending}
                                    onClick={() => handleAddAction(action.name)}
                                    type="button"
                                >
                                    <span className="text-sm font-medium">{action.title || action.name}</span>

                                    {action.description && (
                                        <span className="line-clamp-2 text-xs text-muted-foreground">
                                            {action.description}
                                        </span>
                                    )}
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
};

/**
 * Unified resources picker for the Personal Agent form. Replaces the tools-only
 * {@code AiHubPersonalAgentToolsCard}: a single "Add" button opens the shared {@link ResourcePickerMenu}
 * (the 8 reference kinds) with a {@code toolsBranch} slot supplying the agent form's existing
 * component→action Tools picker.
 *
 * <ul>
 * <li><b>Tools</b> keep the per-row Configure dialog ({@link AiHubPersonalAgentToolConfigDialog}) — unchanged
 *     behaviour, just relocated under the picker's tools branch.</li>
 * <li><b>Reference-kind resources</b> are simpler: a kind icon + name + Remove control, no Configure.</li>
 * <li>Attached items render as one list: Tools first, then the 8 reference kinds.</li>
 * </ul>
 */
const AiHubPersonalAgentResourcesCard = (props: AiHubPersonalAgentResourcesCardPropsType) => {
    const editMode = isEditModeProps(props);

    // The full tool row drives the configure dialog so it can pre-fill connectionId + parameters from the
    // persisted template. Pending tools don't have a persisted id yet (the agent itself hasn't been saved),
    // so configure isn't available for those — they go through the picker once and live in the parent
    // form's local state until the agent is created.
    const [configureTool, setConfigureTool] = useState<AiHubPersonalAgentToolI | null>(null);

    const addToolMutation = useAddAiHubPersonalAgentToolMutation();
    const removeToolMutation = useRemoveAiHubPersonalAgentToolMutation();
    const addResourceMutation = useAddAiHubPersonalAgentResourceMutation();
    const removeResourceMutation = useRemoveAiHubPersonalAgentResourceMutation();

    const handlePickAction = useCallback(
        async (componentName: string, componentVersion: number, operationName: string) => {
            if (editMode) {
                await addToolMutation.mutateAsync({
                    input: {
                        aiHubPersonalAgentId: String(props.aiHubPersonalAgentId),
                        componentName,
                        componentVersion,
                        operationName,
                        workspaceId: String(props.workspaceId),
                    },
                });
            } else {
                props.onAddPendingTool({componentName, componentVersion, operationName});
            }
        },
        [addToolMutation, editMode, props]
    );

    const handleSelectResource = async (selection: ResourcePickerSelectionI) => {
        // Normalize the composer's lowercase kind to the SCREAMING_SNAKE form used everywhere downstream.
        const resourceKind = COMPOSER_KIND_TO_RESOURCE_KIND[selection.kind];

        if (editMode) {
            await addResourceMutation.mutateAsync({
                input: {
                    aiHubPersonalAgentId: String(props.aiHubPersonalAgentId),
                    kind: resourceKind as AiHubPersonalAgentResourceKind,
                    resourceId: selection.id,
                    resourceName: selection.name,
                    workspaceId: String(props.workspaceId),
                },
            });
        } else {
            props.onAddPendingResource({
                kind: resourceKind,
                resourceId: selection.id,
                resourceName: selection.name,
            });
        }
    };

    const handleRemoveTool = (toolId: number) => {
        if (!editMode) {
            return;
        }

        removeToolMutation.mutate({
            toolId: String(toolId),
            workspaceId: String(props.workspaceId),
        });
    };

    const handleRemovePendingTool = (index: number) => {
        if (editMode) {
            return;
        }

        props.onRemovePendingTool(index);
    };

    const handleRemoveResource = (resourceId: number) => {
        if (!editMode) {
            return;
        }

        removeResourceMutation.mutate({
            id: String(resourceId),
            workspaceId: String(props.workspaceId),
        });
    };

    const handleRemovePendingResource = (index: number) => {
        if (editMode) {
            return;
        }

        props.onRemovePendingResource(index);
    };

    const renderedTools = editMode ? props.tools : props.pendingTools;
    const renderedResources = editMode ? props.resources : props.pendingResources;
    const isEmpty = renderedTools.length === 0 && renderedResources.length === 0;

    const renderToolsRootItem = useCallback(
        (onEnter: () => void): ReactNode => (
            <CommandItem onSelect={onEnter} value="root-tools">
                <WrenchIcon className="mr-2 size-3.5" />

                <span className="flex-1">Tools</span>
            </CommandItem>
        ),
        []
    );

    const renderToolsBranch = useCallback(
        (onBack: () => void, onClose: () => void): ReactNode => (
            <ToolsBranchBody
                addPending={editMode && addToolMutation.isPending}
                onBack={onBack}
                onClose={onClose}
                onPickAction={handlePickAction}
            />
        ),
        [addToolMutation.isPending, editMode, handlePickAction]
    );

    // Memoized so the `toolsBranch` object identity is stable across renders unless one of the render
    // callbacks actually changes — avoids handing ResourcePickerMenu a fresh object on every render.
    const toolsBranch = useMemo(
        () => ({renderBranch: renderToolsBranch, renderRootItem: renderToolsRootItem}),
        [renderToolsBranch, renderToolsRootItem]
    );

    return (
        <div className="space-y-2">
            <div className="mb-3 flex items-center justify-between">
                <div className="flex items-center gap-1">
                    <h2 className="flex-1 text-sm font-medium">Resources this agent can use:</h2>

                    <Tooltip>
                        <TooltipTrigger>
                            <InfoIcon className="size-4 text-muted-foreground" />
                        </TooltipTrigger>

                        <TooltipContent className="max-w-xs">
                            The agent draws on these tools and resources whenever they&apos;re helpful during a chat.
                            Each new task starts with the same set attached automatically.
                        </TooltipContent>
                    </Tooltip>
                </div>

                <ResourcePickerMenu
                    environmentId={props.environmentId}
                    onSelect={handleSelectResource}
                    toolsBranch={toolsBranch}
                    trigger={<Button icon={<PlusIcon />} label="Add" size="sm" variant="outline" />}
                    workspaceId={props.workspaceId}
                />
            </div>

            {isEmpty && (
                <p className="text-xs text-muted-foreground">
                    No resources added yet. Click &quot;Add&quot; to give your agent tools and resources.
                </p>
            )}

            {editMode
                ? props.tools.map((tool) => (
                      <ToolRow
                          componentName={tool.componentName}
                          componentVersion={tool.componentVersion}
                          key={`tool-${tool.id}`}
                          onConfigure={() => setConfigureTool(tool)}
                          onRemove={() => handleRemoveTool(tool.id)}
                          operationName={tool.operationName}
                          removeDisabled={removeToolMutation.isPending}
                      />
                  ))
                : props.pendingTools.map((tool, index) => (
                      <ToolRow
                          componentName={tool.componentName}
                          componentVersion={tool.componentVersion}
                          // Pending tools (pre-create) don't expose a configure entry — without a persisted
                          // row there's nothing to update. The agent must be created first; configure
                          // becomes available on the edit screen after save.
                          configureDisabled={true}
                          key={`tool-${tool.componentName}:${tool.operationName}:${index}`}
                          onConfigure={() => undefined}
                          onRemove={() => handleRemovePendingTool(index)}
                          operationName={tool.operationName}
                      />
                  ))}

            {editMode
                ? props.resources.map((resource) => (
                      <ResourceRow
                          key={`resource-${resource.id}`}
                          kind={resource.kind}
                          onRemove={() => handleRemoveResource(resource.id)}
                          removeDisabled={removeResourceMutation.isPending}
                          resourceName={resource.resourceName}
                      />
                  ))
                : props.pendingResources.map((resource, index) => (
                      <ResourceRow
                          key={`resource-${resource.kind}:${resource.resourceId}:${index}`}
                          kind={resource.kind}
                          onRemove={() => handleRemovePendingResource(index)}
                          resourceName={resource.resourceName}
                      />
                  ))}

            {/*
             * Edit-mode only — pending tools (pre-save) can't be configured because their id doesn't yet
             * exist server-side. The configure dialog short-circuits when tool is null + open is false, so
             * there's no fetch overhead while it's closed.
             */}

            {editMode && (
                <AiHubPersonalAgentToolConfigDialog
                    onClose={() => setConfigureTool(null)}
                    open={configureTool !== null}
                    tool={configureTool}
                    workspaceId={props.workspaceId}
                />
            )}
        </div>
    );
};

export default AiHubPersonalAgentResourcesCard;
