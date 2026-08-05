import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ReferencedResourceKindType} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {groupWorkflowsByProject} from '@/ee/pages/automation/ai-hub/resource-picker/groupWorkflowsByProject';
import {useGetApiCollectionsQuery} from '@/ee/shared/mutations/automation/apiCollections.queries';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {Workflow, WorkflowApi} from '@/shared/middleware/automation/configuration';
import {
    AiHubTaskStatus,
    useAiHubTasksQuery,
    useDataTablesQuery,
    useGetAssetFilesQuery,
    useKnowledgeBasesQuery,
    useWorkspaceMcpServersQuery,
} from '@/shared/middleware/graphql';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useInfiniteWorkspaceProjectWorkflowExecutionsQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useQueries} from '@tanstack/react-query';
import {
    ChevronLeftIcon,
    ChevronRightIcon,
    ClockIcon,
    DatabaseIcon,
    FileTextIcon,
    HistoryIcon,
    LinkIcon,
    ServerIcon,
    VectorSquareIcon,
    WorkflowIcon,
} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';
import {useDebouncedCallback} from 'use-debounce';

export interface ResourcePickerSelectionI {
    id: string;
    kind: ReferencedResourceKindType;
    name: string;
    // Workflow-only: present so a caller can open a workflow tab. Undefined for the other 7 kinds.
    projectId?: string;
    projectWorkflowId?: number;
}

// A caller-supplied Tools branch. ResourcePickerMenu covers only the 8 reference kinds; Tools differs per
// context, so each caller plugs its own. When omitted, the menu shows only 8 kinds.
export interface ResourcePickerToolsBranchI {
    // Rendered as the "Tools" CommandItem in the root menu. `onEnter` drills the menu into the tools branch.
    renderRootItem: (onEnter: () => void) => ReactNode;
    // Rendered as the full menu body when the tools branch is active. `onBack` returns to the root menu;
    // `onClose` closes the whole picker popover (used when a tool pick opens a dialog that must not sit
    // behind an open popover).
    renderBranch: (onBack: () => void, onClose: () => void) => ReactNode;
}

export interface ResourcePickerMenuPropsI {
    workspaceId: number;
    environmentId: number;
    // The Popover trigger (the composer's "+" button, the agent form's "Add" button).
    trigger: ReactNode;
    // Fired whenever the picker popover opens or closes (including an outside-click dismiss). A consumer
    // can use this to reset consumer-owned branch state — e.g. a Tools sub-flow's selected component or a
    // lazy-query `enabled` flag — that ResourcePickerMenu cannot reach because it lives in the consumer.
    onOpenChange?: (open: boolean) => void;
    // Fired when the user picks one of the 8 reference-kind resources. The menu closes itself after.
    onSelect: (selection: ResourcePickerSelectionI) => void;
    toolsBranch?: ResourcePickerToolsBranchI;
}

interface WorkflowItemI {
    id: string;
    name: string;
    // projectId / projectName / projectWorkflowId are included so the caller can also push the workflow as
    // a tab in the right panel via {@code aiHubTabsStore.openWorkflowTab(...)}, and so the menu can
    // group workflows under their parent project name. Without `projectId` and `projectWorkflowId`, picking a
    // workflow as a reference can only register it in the composer store; the resource panel
    // would never display it. Mirrors the same shape used by `AiHubFilePicker.useAllWorkspaceWorkflows`.
    projectId: string;
    projectName: string;
    projectWorkflowId: number;
}

const SECTION_INITIAL_CAP = 20;
const SECTION_EXPAND_INCREMENT = 50;

function useAllWorkspaceWorkflows(workspaceId: number | undefined): WorkflowItemI[] {
    const {data: projects} = useGetWorkspaceProjectsQuery({id: workspaceId ?? 0}, Boolean(workspaceId));

    // All workspace projects (no cap) so workflows from every project are reachable. Project counts are
    // bounded (tens); each getProjectWorkflows is cheap and react-query-cached.
    const allProjects = useMemo(() => projects ?? [], [projects]);

    const workflowQueries = useQueries({
        queries: allProjects.map((project) => ({
            enabled: Boolean(project.id),
            queryFn: () => new WorkflowApi().getProjectWorkflows({id: project.id!}),
            queryKey: ProjectWorkflowKeys.projectWorkflows(project.id!),
        })),
    });

    // useQueries returns a fresh array reference every render, so depending on it directly defeats useMemo.
    // Each query's `.data` is stable when the cache hasn't moved, so depend on the data refs indirectly via a stable
    // fingerprint string. Spreading the array directly into the deps would give useMemo a variable-length deps array
    // (length grows as projects load), which is undefined behavior in React 19 and hard-errors in dev.
    const workflowDataList = workflowQueries.map((queryResult) => queryResult.data as Workflow[] | undefined);

    const workflowDataFingerprint = workflowDataList.map((workflows) => (workflows ? workflows.length : -1)).join(',');

    return useMemo(() => {
        const allWorkflows: WorkflowItemI[] = [];

        allProjects.forEach((project, projectIndex) => {
            const workflows = workflowDataList[projectIndex];

            if (!workflows || project.id == null) {
                return;
            }

            for (const workflow of workflows) {
                allWorkflows.push({
                    id: String(workflow.id ?? ''),
                    name: workflow.label ?? workflow.id ?? '',
                    projectId: String(project.id),
                    projectName: project.name ?? `Project ${project.id}`,
                    // `workflow.id` is the workflow's UUID string; the numeric join-entity id the workflow
                    // editor needs lives on `projectWorkflowId`. Deriving it from Number(id) yields NaN for
                    // real UUIDs, which renders as "Workflow reference unavailable." in the viewer.
                    projectWorkflowId: workflow.projectWorkflowId ?? 0,
                });
            }
        });

        return allWorkflows;
        // workflowDataList is intentionally read inside the memo body but excluded from deps; the fingerprint above
        // captures the only mutation we care about (per-project data length / presence) with constant-length deps.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowDataFingerprint, allProjects]);
}

// The 'workflows' branch uses a two-level drilldown: pick a parent (project), then pick the child
// (workflow). Keeps each visible list short — a workspace can have hundreds of workflows across projects —
// and it matches the user's mental model ("find the project, then the workflow inside it"). The 'tools'
// branch is caller-owned: ResourcePickerMenu only tracks the ['tools'] path and delegates rendering.
type MenuPathType =
    | []
    | ['workflows']
    | ['workflows', string]
    | ['files']
    | ['dataTables']
    | ['knowledgeBases']
    | ['tools']
    | ['workflowExecutions']
    | ['mcpServers']
    | ['apiCollections']
    | ['tasks'];

const ResourcePickerMenu = ({
    environmentId,
    onOpenChange,
    onSelect,
    toolsBranch,
    trigger,
    workspaceId,
}: ResourcePickerMenuPropsI) => {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [menuPath, setMenuPath] = useState<MenuPathType>([]);
    const [filesShowCount, setFilesShowCount] = useState(SECTION_INITIAL_CAP);
    const [workflowsShowCount, setWorkflowsShowCount] = useState(SECTION_INITIAL_CAP);
    const [dataTablesShowCount, setDataTablesShowCount] = useState(SECTION_INITIAL_CAP);
    const [knowledgeBasesShowCount, setKnowledgeBasesShowCount] = useState(SECTION_INITIAL_CAP);
    const [mcpServersShowCount, setMcpServersShowCount] = useState(SECTION_INITIAL_CAP);
    const [apiCollectionsShowCount, setApiCollectionsShowCount] = useState(SECTION_INITIAL_CAP);
    const [tasksShowCount, setTasksShowCount] = useState(SECTION_INITIAL_CAP);

    const {data: filesData} = useGetAssetFilesQuery({
        mimeTypePrefix: null,
        workspaceId: String(workspaceId),
    });

    const {data: dataTablesData} = useDataTablesQuery({
        environmentId: String(environmentId ?? DEVELOPMENT_ENVIRONMENT),
        workspaceId: String(workspaceId),
    });

    const {data: knowledgeBasesData} = useKnowledgeBasesQuery({
        environmentId: String(environmentId ?? DEVELOPMENT_ENVIRONMENT),
        workspaceId: String(workspaceId),
    });

    // Lazy-load the four extra resource kinds: only fetch when the user enters that branch (or types
    // into search). Workflow-executions and previous-tasks lists can be heavy; api-collections and
    // mcp-servers are usually small but still gated for symmetry. The `enabled` flag keeps first-paint
    // cheap when the user only wants to attach files / data tables / workflows.
    const isPickerActive = open;

    const {
        data: workflowExecutionsData,
        fetchNextPage: fetchNextWorkflowExecutionsPage,
        hasNextPage: hasMoreWorkflowExecutions,
        isFetchingNextPage: isFetchingMoreWorkflowExecutions,
    } = useInfiniteWorkspaceProjectWorkflowExecutionsQuery({
        environmentId: environmentId ?? DEVELOPMENT_ENVIRONMENT,
        id: workspaceId ?? 0,
    });

    const {data: mcpServersData} = useWorkspaceMcpServersQuery(
        {workspaceId: String(workspaceId ?? '')},
        {enabled: isPickerActive && workspaceId != null}
    );

    const {data: apiCollectionsData} = useGetApiCollectionsQuery({
        environmentId: environmentId ?? DEVELOPMENT_ENVIRONMENT,
        id: workspaceId ?? 0,
    });

    const {data: tasksData} = useAiHubTasksQuery(
        {
            environment: environmentId ?? DEVELOPMENT_ENVIRONMENT,
            status: AiHubTaskStatus.Active,
            workspaceId: String(workspaceId ?? ''),
        },
        {enabled: isPickerActive && workspaceId != null}
    );

    const workflowItems = useAllWorkspaceWorkflows(workspaceId);

    const handleSearchChange = useDebouncedCallback((value: string) => {
        setDebouncedSearch(value);
    }, 300);

    const lowerSearch = debouncedSearch.toLowerCase();

    const filteredFiles = useMemo(() => {
        const files = filesData?.assetFiles ?? [];

        return files.filter((file) => file.name.toLowerCase().includes(lowerSearch));
    }, [filesData, lowerSearch]);

    const filteredDataTables = useMemo(() => {
        const dataTables = dataTablesData?.dataTables ?? [];

        return dataTables.filter((table) => table.baseName.toLowerCase().includes(lowerSearch));
    }, [dataTablesData, lowerSearch]);

    const filteredKnowledgeBases = useMemo(() => {
        const knowledgeBases = knowledgeBasesData?.knowledgeBases?.filter(Boolean) ?? [];

        return knowledgeBases.filter(
            (knowledgeBase) => knowledgeBase != null && knowledgeBase.name.toLowerCase().includes(lowerSearch)
        );
    }, [knowledgeBasesData, lowerSearch]);

    const filteredWorkflows = useMemo(
        () => workflowItems.filter((workflow) => workflow.name.toLowerCase().includes(lowerSearch)),
        [workflowItems, lowerSearch]
    );

    const filteredWorkflowExecutions = useMemo(() => {
        // The workflow-executions REST endpoint returns a Page where `content` is typed as `Array<object>`
        // — we know the runtime shape is `WorkflowExecution`, so we narrow here so downstream renderers can
        // read `id`, `workflow.label`, `project.name`, and `job.startDate` without each call site casting.
        const executions = (workflowExecutionsData?.pages.flatMap((page) => page.content) ?? []) as Array<{
            id: number;
            job?: {startDate?: string; status?: string};
            project?: {name?: string};
            workflow?: {label?: string};
        }>;

        return executions.filter((execution) => {
            const label = (execution.workflow?.label ?? '').toLowerCase();
            const projectName = (execution.project?.name ?? '').toLowerCase();

            return label.includes(lowerSearch) || projectName.includes(lowerSearch);
        });
    }, [workflowExecutionsData, lowerSearch]);

    const filteredMcpServers = useMemo(() => {
        const servers = mcpServersData?.workspaceMcpServers?.filter(Boolean) ?? [];

        return servers.filter((server) => server != null && server.name.toLowerCase().includes(lowerSearch));
    }, [mcpServersData, lowerSearch]);

    const filteredApiCollections = useMemo(() => {
        const collections = apiCollectionsData ?? [];

        return collections.filter((collection) => collection.name.toLowerCase().includes(lowerSearch));
    }, [apiCollectionsData, lowerSearch]);

    const filteredTasks = useMemo(() => {
        const tasks = tasksData?.aiHubTasks ?? [];

        return tasks.filter((task) => {
            const title = (task.title ?? '').toLowerCase();
            const preview = (task.lastPreview ?? '').toLowerCase();

            return title.includes(lowerSearch) || preview.includes(lowerSearch);
        });
    }, [tasksData, lowerSearch]);

    // Workflows drill down by project so the menu mirrors the natural mental model — "find the project,
    // then the workflow inside it" — instead of dumping all workflows into a flat list where two workflows
    // with similar names from different projects look indistinguishable. Preserves source-order of
    // projects so the project list stays predictable across re-renders.
    const filteredWorkflowProjects = useMemo(() => groupWorkflowsByProject(filteredWorkflows), [filteredWorkflows]);

    // When in workflow-project drilldown (menuPath = ['workflows', projectId]), find the matching project
    // group so we can render its workflow list. Returns null when the path doesn't match — the renderer
    // treats a null as "show empty state" rather than crashing.
    const selectedWorkflowProject = useMemo(() => {
        if (menuPath.length !== 2 || menuPath[0] !== 'workflows') {
            return null;
        }

        return filteredWorkflowProjects.find((project) => project.projectId === menuPath[1]) ?? null;
    }, [menuPath, filteredWorkflowProjects]);

    const hasResults =
        filteredFiles.length > 0 ||
        filteredDataTables.length > 0 ||
        filteredKnowledgeBases.length > 0 ||
        filteredWorkflows.length > 0 ||
        filteredWorkflowExecutions.length > 0 ||
        filteredMcpServers.length > 0 ||
        filteredApiCollections.length > 0 ||
        filteredTasks.length > 0;

    const handleSelect = (id: string, kind: ReferencedResourceKindType, name: string) => {
        onSelect({id, kind, name});

        setOpen(false);
        setSearch('');
        setDebouncedSearch('');
    };

    const handleSelectWorkflow = (workflow: WorkflowItemI) => {
        onSelect({
            id: workflow.id,
            kind: 'workflow',
            name: workflow.name,
            projectId: workflow.projectId,
            projectWorkflowId: workflow.projectWorkflowId,
        });

        setOpen(false);
        setSearch('');
        setDebouncedSearch('');
    };

    const handleOpenChange = (nextOpen: boolean) => {
        setOpen(nextOpen);

        if (!nextOpen) {
            setMenuPath([]);
            setSearch('');
            setDebouncedSearch('');
            setFilesShowCount(SECTION_INITIAL_CAP);
            setWorkflowsShowCount(SECTION_INITIAL_CAP);
            setDataTablesShowCount(SECTION_INITIAL_CAP);
            setKnowledgeBasesShowCount(SECTION_INITIAL_CAP);
            setMcpServersShowCount(SECTION_INITIAL_CAP);
            setApiCollectionsShowCount(SECTION_INITIAL_CAP);
            setTasksShowCount(SECTION_INITIAL_CAP);
        }

        // Notify the consumer after ResourcePickerMenu resets its own state so a consumer-owned branch
        // (e.g. the composer's Tools sub-flow) can clear its lingering state on close too.
        onOpenChange?.(nextOpen);
    };

    const visibleFiles = filteredFiles.slice(0, filesShowCount);
    // Workflows aren't sliced here — the search-mode renderer slices `filteredWorkflows` directly, and
    // the drilldown renderer slices the selected project's workflow array. Both apply the same
    // `workflowsShowCount` cap, so the existing show-more affordance still works after the drilldown
    // refactor.
    const visibleDataTables = filteredDataTables.slice(0, dataTablesShowCount);
    const visibleKnowledgeBases = filteredKnowledgeBases.slice(0, knowledgeBasesShowCount);
    const visibleMcpServers = filteredMcpServers.slice(0, mcpServersShowCount);
    const visibleApiCollections = filteredApiCollections.slice(0, apiCollectionsShowCount);
    const visibleTasks = filteredTasks.slice(0, tasksShowCount);

    return (
        <Popover onOpenChange={handleOpenChange} open={open}>
            <PopoverTrigger asChild>{trigger}</PopoverTrigger>

            <PopoverContent align="start" className="w-72 p-0">
                <Command shouldFilter={false}>
                    <div className="mt-1 pb-3">
                        <CommandInput
                            className="!border-0 !ring-0 !outline-none focus:!border-0 focus:!ring-0 focus:!outline-none"
                            onValueChange={(value) => {
                                setSearch(value);
                                handleSearchChange(value);
                            }}
                            placeholder="Search resources…"
                            value={search}
                        />
                    </div>

                    <CommandList>
                        {debouncedSearch ? (
                            <>
                                {!hasResults && <CommandEmpty>No resources found.</CommandEmpty>}

                                {filteredFiles.length > 0 && (
                                    <CommandGroup heading="Files">
                                        {visibleFiles.map((file) => (
                                            <CommandItem
                                                key={`file-${file.id}`}
                                                onSelect={() => handleSelect(String(file.id), 'file', file.name)}
                                                value={`file-${file.id}-${file.name}`}
                                            >
                                                <FileTextIcon className="mr-2 size-3.5" />

                                                {file.name}
                                            </CommandItem>
                                        ))}

                                        {filteredFiles.length > filesShowCount && (
                                            <CommandItem
                                                key="files-show-more"
                                                onSelect={() =>
                                                    setFilesShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="files-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredFiles.length - filesShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {filteredWorkflows.length > 0 && (
                                    // In search mode workflows render flat under one heading — search transcends
                                    // the menu's level structure, so a query like "checkout" should match across
                                    // every project at once. The project name follows the workflow name as a muted
                                    // suffix so two similarly-named workflows from different projects stay
                                    // distinguishable.
                                    <CommandGroup heading="Workflows">
                                        {filteredWorkflows.slice(0, workflowsShowCount).map((workflow) => (
                                            <CommandItem
                                                key={`workflow-${workflow.id}`}
                                                onSelect={() => handleSelectWorkflow(workflow)}
                                                value={`workflow-${workflow.id}-${workflow.name}`}
                                            >
                                                <WorkflowIcon className="mr-2 size-3.5" />

                                                <span className="flex-1 truncate">{workflow.name}</span>

                                                <span className="ml-2 text-xs text-muted-foreground">
                                                    {workflow.projectName}
                                                </span>
                                            </CommandItem>
                                        ))}

                                        {filteredWorkflows.length > workflowsShowCount && (
                                            <CommandItem
                                                key="workflows-show-more"
                                                onSelect={() =>
                                                    setWorkflowsShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="workflows-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredWorkflows.length - workflowsShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {filteredDataTables.length > 0 && (
                                    <CommandGroup heading="Data Tables">
                                        {visibleDataTables.map((table) => (
                                            <CommandItem
                                                key={`dataTable-${table.id}`}
                                                onSelect={() =>
                                                    handleSelect(String(table.id), 'dataTable', table.baseName)
                                                }
                                                value={`dataTable-${table.id}-${table.baseName}`}
                                            >
                                                <DatabaseIcon className="mr-2 size-3.5" />

                                                {table.baseName}
                                            </CommandItem>
                                        ))}

                                        {filteredDataTables.length > dataTablesShowCount && (
                                            <CommandItem
                                                key="data-tables-show-more"
                                                onSelect={() =>
                                                    setDataTablesShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="data-tables-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredDataTables.length - dataTablesShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {filteredKnowledgeBases.length > 0 && (
                                    <CommandGroup heading="Knowledge Bases">
                                        {visibleKnowledgeBases.map((knowledgeBase) => {
                                            if (knowledgeBase == null) {
                                                return null;
                                            }

                                            return (
                                                <CommandItem
                                                    key={`knowledgeBase-${knowledgeBase.id}`}
                                                    onSelect={() =>
                                                        handleSelect(
                                                            String(knowledgeBase.id),
                                                            'knowledgeBase',
                                                            knowledgeBase.name
                                                        )
                                                    }
                                                    value={`knowledgeBase-${knowledgeBase.id}-${knowledgeBase.name}`}
                                                >
                                                    <VectorSquareIcon className="mr-2 size-3.5" />

                                                    {knowledgeBase.name}
                                                </CommandItem>
                                            );
                                        })}

                                        {filteredKnowledgeBases.length > knowledgeBasesShowCount && (
                                            <CommandItem
                                                key="knowledge-bases-show-more"
                                                onSelect={() =>
                                                    setKnowledgeBasesShowCount(
                                                        (count) => count + SECTION_EXPAND_INCREMENT
                                                    )
                                                }
                                                value="knowledge-bases-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredKnowledgeBases.length - knowledgeBasesShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {filteredWorkflowExecutions.length > 0 && (
                                    <CommandGroup heading="Workflow Executions">
                                        {filteredWorkflowExecutions.map((execution) => {
                                            const label = execution.workflow?.label ?? `Execution ${execution.id}`;
                                            const projectName = execution.project?.name;
                                            const status = execution.job?.status;

                                            return (
                                                <CommandItem
                                                    key={`workflowExecution-${execution.id}`}
                                                    onSelect={() =>
                                                        handleSelect(String(execution.id), 'workflowExecution', label)
                                                    }
                                                    value={`workflowExecution-${execution.id}-${label}`}
                                                >
                                                    <HistoryIcon className="mr-2 size-3.5" />

                                                    <div className="flex min-w-0 flex-1 flex-col">
                                                        <span className="truncate">{label}</span>

                                                        <span className="truncate text-xs text-muted-foreground">
                                                            {[projectName, status].filter(Boolean).join(' · ')}
                                                        </span>
                                                    </div>
                                                </CommandItem>
                                            );
                                        })}

                                        {hasMoreWorkflowExecutions && (
                                            <CommandItem
                                                key="workflow-executions-show-more"
                                                onSelect={() => {
                                                    void fetchNextWorkflowExecutionsPage();
                                                }}
                                                value="workflow-executions-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {isFetchingMoreWorkflowExecutions ? 'Loading…' : 'Show more…'}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {filteredMcpServers.length > 0 && (
                                    <CommandGroup heading="MCP Servers">
                                        {visibleMcpServers.map((server) => {
                                            if (server == null) {
                                                return null;
                                            }

                                            return (
                                                <CommandItem
                                                    key={`mcpServer-${server.id}`}
                                                    onSelect={() =>
                                                        handleSelect(String(server.id), 'mcpServer', server.name)
                                                    }
                                                    value={`mcpServer-${server.id}-${server.name}`}
                                                >
                                                    <ServerIcon className="mr-2 size-3.5" />

                                                    <span className="flex-1 truncate">{server.name}</span>

                                                    {!server.enabled && (
                                                        <span className="ml-2 text-xs text-muted-foreground">
                                                            disabled
                                                        </span>
                                                    )}
                                                </CommandItem>
                                            );
                                        })}

                                        {filteredMcpServers.length > mcpServersShowCount && (
                                            <CommandItem
                                                key="mcp-servers-show-more"
                                                onSelect={() =>
                                                    setMcpServersShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="mcp-servers-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredMcpServers.length - mcpServersShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {filteredApiCollections.length > 0 && (
                                    <CommandGroup heading="API Collections">
                                        {visibleApiCollections.map((collection) => {
                                            const collectionId = collection.id;

                                            if (collectionId == null) {
                                                return null;
                                            }

                                            return (
                                                <CommandItem
                                                    key={`apiCollection-${collectionId}`}
                                                    onSelect={() =>
                                                        handleSelect(
                                                            String(collectionId),
                                                            'apiCollection',
                                                            collection.name
                                                        )
                                                    }
                                                    value={`apiCollection-${collectionId}-${collection.name}`}
                                                >
                                                    <LinkIcon className="mr-2 size-3.5" />

                                                    <span className="flex-1 truncate">{collection.name}</span>

                                                    {!collection.enabled && (
                                                        <span className="ml-2 text-xs text-muted-foreground">
                                                            disabled
                                                        </span>
                                                    )}
                                                </CommandItem>
                                            );
                                        })}

                                        {filteredApiCollections.length > apiCollectionsShowCount && (
                                            <CommandItem
                                                key="api-collections-show-more"
                                                onSelect={() =>
                                                    setApiCollectionsShowCount(
                                                        (count) => count + SECTION_EXPAND_INCREMENT
                                                    )
                                                }
                                                value="api-collections-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredApiCollections.length - apiCollectionsShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {filteredTasks.length > 0 && (
                                    <CommandGroup heading="Previous Tasks">
                                        {visibleTasks.map((task) => {
                                            // Tasks may have an empty title before the LLM-driven title generator
                                            // runs. Falling back to the lastPreview keeps the row meaningful in
                                            // that window.
                                            const label = task.title || task.lastPreview || `Task ${task.id}`;

                                            return (
                                                <CommandItem
                                                    key={`task-${task.id}`}
                                                    onSelect={() => handleSelect(String(task.id), 'task', label)}
                                                    value={`task-${task.id}-${label}`}
                                                >
                                                    <ClockIcon className="mr-2 size-3.5" />

                                                    <span className="flex-1 truncate">{label}</span>
                                                </CommandItem>
                                            );
                                        })}

                                        {filteredTasks.length > tasksShowCount && (
                                            <CommandItem
                                                key="tasks-show-more"
                                                onSelect={() =>
                                                    setTasksShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="tasks-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredTasks.length - tasksShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}
                            </>
                        ) : menuPath.length === 0 ? (
                            <CommandGroup>
                                <CommandItem onSelect={() => setMenuPath(['workflows'])} value="root-workflows">
                                    <WorkflowIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Workflows</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                <CommandItem onSelect={() => setMenuPath(['files'])} value="root-files">
                                    <FileTextIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Files</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                <CommandItem onSelect={() => setMenuPath(['dataTables'])} value="root-data-tables">
                                    <DatabaseIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Data Tables</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                <CommandItem
                                    onSelect={() => setMenuPath(['knowledgeBases'])}
                                    value="root-knowledge-bases"
                                >
                                    <VectorSquareIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Knowledge Bases</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                {toolsBranch?.renderRootItem(() => setMenuPath(['tools']))}

                                <CommandItem
                                    onSelect={() => setMenuPath(['workflowExecutions'])}
                                    value="root-workflow-executions"
                                >
                                    <HistoryIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Workflow Executions</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                <CommandItem onSelect={() => setMenuPath(['mcpServers'])} value="root-mcp-servers">
                                    <ServerIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">MCP Servers</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                <CommandItem
                                    onSelect={() => setMenuPath(['apiCollections'])}
                                    value="root-api-collections"
                                >
                                    <LinkIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">API Collections</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                <CommandItem onSelect={() => setMenuPath(['tasks'])} value="root-tasks">
                                    <ClockIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Previous Tasks</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>
                            </CommandGroup>
                        ) : menuPath[0] === 'tools' ? (
                            toolsBranch?.renderBranch(
                                () => setMenuPath([]),
                                () => handleOpenChange(false)
                            )
                        ) : (
                            <>
                                <CommandGroup>
                                    <CommandItem
                                        // Two-step back inside the Workflows branch: from a level-2 child list
                                        // (['workflows', X]) the back button should pop to the parent list, not bounce
                                        // all the way to root. Single-level branches (files/dataTables/knowledgeBases)
                                        // always pop to root.
                                        onSelect={() =>
                                            setMenuPath(
                                                menuPath.length === 2 && menuPath[0] === 'workflows'
                                                    ? ['workflows']
                                                    : []
                                            )
                                        }
                                        value="back-to-root"
                                    >
                                        <ChevronLeftIcon className="mr-2 size-3.5" />

                                        <span className="flex-1 text-muted-foreground">Back</span>
                                    </CommandItem>
                                </CommandGroup>

                                {menuPath[0] === 'workflows' && menuPath.length === 1 && (
                                    <CommandGroup heading="Workflows — pick a project">
                                        {filteredWorkflowProjects.length === 0 && (
                                            <CommandEmpty>No projects with workflows.</CommandEmpty>
                                        )}

                                        {filteredWorkflowProjects.map((projectGroup) => (
                                            <CommandItem
                                                key={`workflow-project-${projectGroup.projectId}`}
                                                // Drilldown into the project's workflow list. The first level shows
                                                // projects, the second the workflows inside the chosen project.
                                                onSelect={() => setMenuPath(['workflows', projectGroup.projectId])}
                                                value={`workflow-project-${projectGroup.projectId}-${projectGroup.projectName}`}
                                            >
                                                <WorkflowIcon className="mr-2 size-3.5" />

                                                <span className="flex-1">{projectGroup.projectName}</span>

                                                <span className="mr-2 text-xs text-muted-foreground">
                                                    {projectGroup.workflows.length}
                                                </span>

                                                <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                            </CommandItem>
                                        ))}
                                    </CommandGroup>
                                )}

                                {menuPath[0] === 'workflows' && menuPath.length === 2 && (
                                    <CommandGroup
                                        heading={`Workflows — ${selectedWorkflowProject?.projectName ?? menuPath[1]}`}
                                    >
                                        {!selectedWorkflowProject || selectedWorkflowProject.workflows.length === 0 ? (
                                            <CommandEmpty>No workflows.</CommandEmpty>
                                        ) : (
                                            <>
                                                {selectedWorkflowProject.workflows
                                                    .slice(0, workflowsShowCount)
                                                    .map((workflow) => (
                                                        <CommandItem
                                                            key={`workflow-${workflow.id}`}
                                                            onSelect={() => handleSelectWorkflow(workflow)}
                                                            value={`workflow-${workflow.id}-${workflow.name}`}
                                                        >
                                                            <WorkflowIcon className="mr-2 size-3.5" />

                                                            {workflow.name}
                                                        </CommandItem>
                                                    ))}

                                                {selectedWorkflowProject.workflows.length > workflowsShowCount && (
                                                    <CommandItem
                                                        key="workflows-show-more"
                                                        onSelect={() =>
                                                            setWorkflowsShowCount(
                                                                (count) => count + SECTION_EXPAND_INCREMENT
                                                            )
                                                        }
                                                        value="workflows-show-more"
                                                    >
                                                        <span className="text-xs text-muted-foreground">
                                                            {`Show ${Math.min(SECTION_EXPAND_INCREMENT, selectedWorkflowProject.workflows.length - workflowsShowCount)} more…`}
                                                        </span>
                                                    </CommandItem>
                                                )}
                                            </>
                                        )}
                                    </CommandGroup>
                                )}

                                {menuPath[0] === 'files' && (
                                    <CommandGroup heading="Files">
                                        {visibleFiles.length === 0 && <CommandEmpty>No files.</CommandEmpty>}

                                        {visibleFiles.map((file) => (
                                            <CommandItem
                                                key={`file-${file.id}`}
                                                onSelect={() => handleSelect(String(file.id), 'file', file.name)}
                                                value={`file-${file.id}-${file.name}`}
                                            >
                                                <FileTextIcon className="mr-2 size-3.5" />

                                                {file.name}
                                            </CommandItem>
                                        ))}

                                        {filteredFiles.length > filesShowCount && (
                                            <CommandItem
                                                key="files-show-more"
                                                onSelect={() =>
                                                    setFilesShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="files-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredFiles.length - filesShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {menuPath[0] === 'dataTables' && (
                                    <CommandGroup heading="Data Tables">
                                        {visibleDataTables.length === 0 && <CommandEmpty>No data tables.</CommandEmpty>}

                                        {visibleDataTables.map((table) => (
                                            <CommandItem
                                                key={`dataTable-${table.id}`}
                                                onSelect={() =>
                                                    handleSelect(String(table.id), 'dataTable', table.baseName)
                                                }
                                                value={`dataTable-${table.id}-${table.baseName}`}
                                            >
                                                <DatabaseIcon className="mr-2 size-3.5" />

                                                {table.baseName}
                                            </CommandItem>
                                        ))}

                                        {filteredDataTables.length > dataTablesShowCount && (
                                            <CommandItem
                                                key="data-tables-show-more"
                                                onSelect={() =>
                                                    setDataTablesShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="data-tables-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredDataTables.length - dataTablesShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {menuPath[0] === 'knowledgeBases' && (
                                    <CommandGroup heading="Knowledge Bases">
                                        {visibleKnowledgeBases.length === 0 && (
                                            <CommandEmpty>No knowledge bases.</CommandEmpty>
                                        )}

                                        {visibleKnowledgeBases.map((knowledgeBase) => {
                                            if (knowledgeBase == null) {
                                                return null;
                                            }

                                            return (
                                                <CommandItem
                                                    key={`knowledgeBase-${knowledgeBase.id}`}
                                                    onSelect={() =>
                                                        handleSelect(
                                                            String(knowledgeBase.id),
                                                            'knowledgeBase',
                                                            knowledgeBase.name
                                                        )
                                                    }
                                                    value={`knowledgeBase-${knowledgeBase.id}-${knowledgeBase.name}`}
                                                >
                                                    <VectorSquareIcon className="mr-2 size-3.5" />

                                                    {knowledgeBase.name}
                                                </CommandItem>
                                            );
                                        })}

                                        {filteredKnowledgeBases.length > knowledgeBasesShowCount && (
                                            <CommandItem
                                                key="knowledge-bases-show-more"
                                                onSelect={() =>
                                                    setKnowledgeBasesShowCount(
                                                        (count) => count + SECTION_EXPAND_INCREMENT
                                                    )
                                                }
                                                value="knowledge-bases-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredKnowledgeBases.length - knowledgeBasesShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {menuPath[0] === 'workflowExecutions' && (
                                    <CommandGroup heading="Workflow Executions">
                                        {filteredWorkflowExecutions.length === 0 && (
                                            <CommandEmpty>No workflow executions.</CommandEmpty>
                                        )}

                                        {filteredWorkflowExecutions.map((execution) => {
                                            const label = execution.workflow?.label ?? `Execution ${execution.id}`;
                                            const projectName = execution.project?.name;
                                            const status = execution.job?.status;

                                            return (
                                                <CommandItem
                                                    key={`workflowExecution-${execution.id}`}
                                                    onSelect={() =>
                                                        handleSelect(String(execution.id), 'workflowExecution', label)
                                                    }
                                                    value={`workflowExecution-${execution.id}-${label}`}
                                                >
                                                    <HistoryIcon className="mr-2 size-3.5" />

                                                    <div className="flex min-w-0 flex-1 flex-col">
                                                        <span className="truncate">{label}</span>

                                                        <span className="truncate text-xs text-muted-foreground">
                                                            {[projectName, status].filter(Boolean).join(' · ')}
                                                        </span>
                                                    </div>
                                                </CommandItem>
                                            );
                                        })}

                                        {hasMoreWorkflowExecutions && (
                                            <CommandItem
                                                key="workflow-executions-show-more"
                                                onSelect={() => {
                                                    void fetchNextWorkflowExecutionsPage();
                                                }}
                                                value="workflow-executions-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {isFetchingMoreWorkflowExecutions ? 'Loading…' : 'Show more…'}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {menuPath[0] === 'mcpServers' && (
                                    <CommandGroup heading="MCP Servers">
                                        {visibleMcpServers.length === 0 && <CommandEmpty>No MCP servers.</CommandEmpty>}

                                        {visibleMcpServers.map((server) => {
                                            if (server == null) {
                                                return null;
                                            }

                                            return (
                                                <CommandItem
                                                    key={`mcpServer-${server.id}`}
                                                    onSelect={() =>
                                                        handleSelect(String(server.id), 'mcpServer', server.name)
                                                    }
                                                    value={`mcpServer-${server.id}-${server.name}`}
                                                >
                                                    <ServerIcon className="mr-2 size-3.5" />

                                                    <span className="flex-1 truncate">{server.name}</span>

                                                    {!server.enabled && (
                                                        <span className="ml-2 text-xs text-muted-foreground">
                                                            disabled
                                                        </span>
                                                    )}
                                                </CommandItem>
                                            );
                                        })}

                                        {filteredMcpServers.length > mcpServersShowCount && (
                                            <CommandItem
                                                key="mcp-servers-show-more"
                                                onSelect={() =>
                                                    setMcpServersShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="mcp-servers-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredMcpServers.length - mcpServersShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {menuPath[0] === 'apiCollections' && (
                                    <CommandGroup heading="API Collections">
                                        {visibleApiCollections.length === 0 && (
                                            <CommandEmpty>No API collections.</CommandEmpty>
                                        )}

                                        {visibleApiCollections.map((collection) => {
                                            const collectionId = collection.id;

                                            if (collectionId == null) {
                                                return null;
                                            }

                                            return (
                                                <CommandItem
                                                    key={`apiCollection-${collectionId}`}
                                                    onSelect={() =>
                                                        handleSelect(
                                                            String(collectionId),
                                                            'apiCollection',
                                                            collection.name
                                                        )
                                                    }
                                                    value={`apiCollection-${collectionId}-${collection.name}`}
                                                >
                                                    <LinkIcon className="mr-2 size-3.5" />

                                                    <span className="flex-1 truncate">{collection.name}</span>

                                                    {!collection.enabled && (
                                                        <span className="ml-2 text-xs text-muted-foreground">
                                                            disabled
                                                        </span>
                                                    )}
                                                </CommandItem>
                                            );
                                        })}

                                        {filteredApiCollections.length > apiCollectionsShowCount && (
                                            <CommandItem
                                                key="api-collections-show-more"
                                                onSelect={() =>
                                                    setApiCollectionsShowCount(
                                                        (count) => count + SECTION_EXPAND_INCREMENT
                                                    )
                                                }
                                                value="api-collections-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredApiCollections.length - apiCollectionsShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {menuPath[0] === 'tasks' && (
                                    <CommandGroup heading="Previous Tasks">
                                        {visibleTasks.length === 0 && <CommandEmpty>No previous tasks.</CommandEmpty>}

                                        {visibleTasks.map((task) => {
                                            // Tasks may have an empty title before the LLM-driven title generator runs.
                                            // Falling back to the lastPreview keeps the row meaningful in that window.
                                            const label = task.title || task.lastPreview || `Task ${task.id}`;

                                            return (
                                                <CommandItem
                                                    key={`task-${task.id}`}
                                                    onSelect={() => handleSelect(String(task.id), 'task', label)}
                                                    value={`task-${task.id}-${label}`}
                                                >
                                                    <ClockIcon className="mr-2 size-3.5" />

                                                    <span className="flex-1 truncate">{label}</span>
                                                </CommandItem>
                                            );
                                        })}

                                        {filteredTasks.length > tasksShowCount && (
                                            <CommandItem
                                                key="tasks-show-more"
                                                onSelect={() =>
                                                    setTasksShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="tasks-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredTasks.length - tasksShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}
                            </>
                        )}
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
};

export default ResourcePickerMenu;
