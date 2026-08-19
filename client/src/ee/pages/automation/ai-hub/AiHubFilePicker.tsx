import Button from '@/components/Button/Button';
import {Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {getArtifactIcon} from '@/ee/pages/automation/ai-hub/artifacts/artifactIcons';
import {handleArtifactQuickOpen, isArtifactClickable} from '@/ee/pages/automation/ai-hub/artifacts/artifactOpen';
import {AiHubChatArtifactI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useAiHubChatArtifactsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {groupWorkflowsByProject} from '@/ee/pages/automation/ai-hub/resource-picker/groupWorkflowsByProject';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {
    useDataTablesQuery,
    useGetAssetFilesQuery,
    useKnowledgeBasesQuery,
    useWorkspaceProjectWorkflowsQuery,
} from '@/shared/middleware/graphql';
import {useInfiniteWorkspaceProjectWorkflowExecutionsQuery} from '@/shared/queries/automation/workflowExecutions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    ChevronLeftIcon,
    ChevronRightIcon,
    DatabaseIcon,
    FileTextIcon,
    HistoryIcon,
    PackageIcon,
    PlusIcon,
    VectorSquareIcon,
    WorkflowIcon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useDebouncedCallback} from 'use-debounce';

interface WorkflowItemI {
    id: string;
    name: string;
    projectId: string;
    projectName: string;
    projectWorkflowId: number;
}

const SECTION_INITIAL_CAP = 20;
const SECTION_EXPAND_INCREMENT = 50;

/**
 * Returns workflows enriched with their parent projectId and projectWorkflowId — both are required by
 * `openWorkflowTab` and aren't available on the bare workflow list. The composer doesn't need this metadata
 * (its references are scoped only to the workflow id) so it stays in this picker rather than the composer.
 *
 * <p>Same single-round-trip query {@code ResourcePickerMenu.useAllWorkspaceWorkflows} uses, gated on the picker being
 * open. It replaced a per-project request fan-out that saturated the browser's connection pool on large
 * workspaces.</p>
 */
function useAllWorkspaceWorkflows(workspaceId: number | undefined, enabled: boolean): WorkflowItemI[] {
    const {data} = useWorkspaceProjectWorkflowsQuery(
        {workspaceId: String(workspaceId ?? '')},
        {enabled: enabled && workspaceId != null}
    );

    const workspaceProjectWorkflows = data?.workspaceProjectWorkflows;

    return useMemo(() => {
        if (!workspaceProjectWorkflows) {
            return [];
        }

        return workspaceProjectWorkflows.map((workspaceProjectWorkflow) => ({
            id: workspaceProjectWorkflow.workflowId,
            name: workspaceProjectWorkflow.workflowLabel,
            projectId: workspaceProjectWorkflow.projectId,
            projectName: workspaceProjectWorkflow.projectName,
            // `workflowId` is the workflow's UUID string; the numeric join-entity id the workflow editor needs is
            // `projectWorkflowId`. Deriving it from Number(workflowId) yields NaN for real UUIDs, which renders as
            // "Workflow reference unavailable." in the viewer.
            projectWorkflowId: Number(workspaceProjectWorkflow.projectWorkflowId),
        }));
    }, [workspaceProjectWorkflows]);
}

type MenuPathType =
    [] | ['workflows'] | ['files'] | ['dataTables'] | ['knowledgeBases'] | ['workflowExecutions'] | ['artifacts'];

const AiHubFilePicker = () => {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [menuPath, setMenuPath] = useState<MenuPathType>([]);
    const [filesShowCount, setFilesShowCount] = useState(SECTION_INITIAL_CAP);
    const [workflowsShowCount, setWorkflowsShowCount] = useState(SECTION_INITIAL_CAP);
    const [dataTablesShowCount, setDataTablesShowCount] = useState(SECTION_INITIAL_CAP);
    const [knowledgeBasesShowCount, setKnowledgeBasesShowCount] = useState(SECTION_INITIAL_CAP);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const environmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);

    const openFileTab = useAiHubTabsStore((state) => state.openFileTab);
    const openWorkflowTab = useAiHubTabsStore((state) => state.openWorkflowTab);
    const openDataTableTab = useAiHubTabsStore((state) => state.openDataTableTab);
    const openKnowledgeBaseTab = useAiHubTabsStore((state) => state.openKnowledgeBaseTab);
    const openWorkflowExecutionTab = useAiHubTabsStore((state) => state.openWorkflowExecutionTab);

    const {data: filesData} = useGetAssetFilesQuery(
        {
            mimeTypePrefix: null,
            workspaceId: String(currentWorkspaceId),
        },
        {enabled: Boolean(currentWorkspaceId)}
    );

    const {data: dataTablesData} = useDataTablesQuery({
        environmentId: String(environmentId ?? DEVELOPMENT_ENVIRONMENT),
        workspaceId: String(currentWorkspaceId),
    });

    const {data: knowledgeBasesData} = useKnowledgeBasesQuery({
        environmentId: String(environmentId ?? DEVELOPMENT_ENVIRONMENT),
        workspaceId: String(currentWorkspaceId),
    });

    const {
        data: workflowExecutionsData,
        fetchNextPage: fetchNextWorkflowExecutionsPage,
        hasNextPage: hasMoreWorkflowExecutions,
        isFetchingNextPage: isFetchingMoreWorkflowExecutions,
    } = useInfiniteWorkspaceProjectWorkflowExecutionsQuery({
        environmentId: environmentId ?? DEVELOPMENT_ENVIRONMENT,
        id: currentWorkspaceId ?? 0,
    });

    // Warm cache hit — the Artifacts card issues this same query under the same key. Non-clickable
    // artifacts (a WORKFLOW_CREATED with no projectId in metadata, say) have nowhere to route to, so they
    // are filtered out rather than rendered as dead menu entries.
    const {data: chatArtifacts} = useAiHubChatArtifactsQuery(
        currentChatId,
        currentWorkspaceId ?? 0,
        Boolean(currentWorkspaceId)
    );

    const workflowItems = useAllWorkspaceWorkflows(currentWorkspaceId, open);

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

    const workflowProjectGroups = useMemo(
        () => groupWorkflowsByProject(filteredWorkflows.slice(0, workflowsShowCount)),
        [filteredWorkflows, workflowsShowCount]
    );

    const filteredWorkflowExecutions = useMemo(() => {
        // The workflow-executions REST endpoint types Page.content as Array<object>; narrow to the known
        // runtime shape so the renderer can read id / workflow.label / project.name / job.status.
        const executions = (workflowExecutionsData?.pages.flatMap((page) => page.content) ?? []) as Array<{
            id: number;
            job?: {status?: string};
            project?: {name?: string};
            workflow?: {label?: string};
        }>;

        return executions.filter((execution) => {
            const label = (execution.workflow?.label ?? '').toLowerCase();
            const projectName = (execution.project?.name ?? '').toLowerCase();

            return label.includes(lowerSearch) || projectName.includes(lowerSearch);
        });
    }, [workflowExecutionsData, lowerSearch]);

    const clickableArtifacts = useMemo(() => (chatArtifacts ?? []).filter(isArtifactClickable), [chatArtifacts]);

    const hasResults =
        filteredFiles.length > 0 ||
        filteredDataTables.length > 0 ||
        filteredKnowledgeBases.length > 0 ||
        filteredWorkflows.length > 0 ||
        filteredWorkflowExecutions.length > 0;

    const visibleFiles = filteredFiles.slice(0, filesShowCount);
    const visibleDataTables = filteredDataTables.slice(0, dataTablesShowCount);
    const visibleKnowledgeBases = filteredKnowledgeBases.slice(0, knowledgeBasesShowCount);

    const closeAndReset = () => {
        setOpen(false);
        setMenuPath([]);
        setSearch('');
        setDebouncedSearch('');
        setFilesShowCount(SECTION_INITIAL_CAP);
        setWorkflowsShowCount(SECTION_INITIAL_CAP);
        setDataTablesShowCount(SECTION_INITIAL_CAP);
        setKnowledgeBasesShowCount(SECTION_INITIAL_CAP);
    };

    const handleOpenChange = (nextOpen: boolean) => {
        setOpen(nextOpen);

        if (!nextOpen) {
            closeAndReset();
        }
    };

    const handleSelectFile = (id: string, name: string) => {
        openFileTab(id, name);
        closeAndReset();
    };

    const handleSelectWorkflow = (workflow: WorkflowItemI) => {
        openWorkflowTab(workflow.id, workflow.projectId, workflow.projectWorkflowId, workflow.name);
        closeAndReset();
    };

    const handleSelectDataTable = (id: string, name: string) => {
        openDataTableTab(id, name);
        closeAndReset();
    };

    const handleSelectKnowledgeBase = (id: string, name: string) => {
        openKnowledgeBaseTab(id, name);
        closeAndReset();
    };

    const handleSelectWorkflowExecution = (id: number, name: string) => {
        openWorkflowExecutionTab(id, name);
        closeAndReset();
    };

    const handleSelectArtifact = (artifact: AiHubChatArtifactI) => {
        void handleArtifactQuickOpen(artifact);
        closeAndReset();
    };

    return (
        <Popover onOpenChange={handleOpenChange} open={open}>
            <PopoverTrigger asChild>
                <Button aria-label="Add resource" icon={<PlusIcon />} size="icon" variant="ghost" />
            </PopoverTrigger>

            <PopoverContent align="end" className="w-72 p-0">
                <Command shouldFilter={false}>
                    {/*
                     * className override on the CommandInput's underlying input element neutralises the
                     * global `* { @apply border-border }` rule in styles/components.css that would otherwise
                     * paint a visible 1px theme-colored border around the input — blue when focused, black
                     * when not. The CommandInput primitive itself stays untouched (used by other surfaces
                     * with different design constraints); this override is scoped to this picker only.
                     */}

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
                                                onSelect={() => handleSelectFile(String(file.id), file.name)}
                                                value={`file-${file.id}-${file.name}`}
                                            >
                                                <FileTextIcon className="mr-2 size-3.5" />

                                                {file.name}
                                            </CommandItem>
                                        ))}
                                    </CommandGroup>
                                )}

                                {filteredWorkflows.length > 0 && (
                                    <>
                                        {workflowProjectGroups.map((group) => (
                                            <CommandGroup
                                                heading={group.projectName}
                                                key={`workflow-project-${group.projectId}`}
                                            >
                                                {group.workflows.map((workflow) => (
                                                    <CommandItem
                                                        key={`workflow-${workflow.id}`}
                                                        onSelect={() => handleSelectWorkflow(workflow)}
                                                        value={`workflow-${workflow.id}-${workflow.name}`}
                                                    >
                                                        <WorkflowIcon className="mr-2 size-3.5" />

                                                        {workflow.name}
                                                    </CommandItem>
                                                ))}
                                            </CommandGroup>
                                        ))}

                                        {filteredWorkflows.length > workflowsShowCount && (
                                            <CommandGroup>
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
                                                        {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredWorkflows.length - workflowsShowCount)} more…`}
                                                    </span>
                                                </CommandItem>
                                            </CommandGroup>
                                        )}
                                    </>
                                )}

                                {filteredDataTables.length > 0 && (
                                    <CommandGroup heading="Data Tables">
                                        {visibleDataTables.map((table) => (
                                            <CommandItem
                                                key={`dataTable-${table.id}`}
                                                onSelect={() => handleSelectDataTable(String(table.id), table.baseName)}
                                                value={`dataTable-${table.id}-${table.baseName}`}
                                            >
                                                <DatabaseIcon className="mr-2 size-3.5" />

                                                {table.baseName}
                                            </CommandItem>
                                        ))}
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
                                                        handleSelectKnowledgeBase(
                                                            String(knowledgeBase.id),
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
                                                    onSelect={() => handleSelectWorkflowExecution(execution.id, label)}
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

                                <CommandItem
                                    onSelect={() => setMenuPath(['workflowExecutions'])}
                                    value="root-workflow-executions"
                                >
                                    <HistoryIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Workflow Executions</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                <CommandItem onSelect={() => setMenuPath(['artifacts'])} value="root-artifacts">
                                    <PackageIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Artifacts</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>
                            </CommandGroup>
                        ) : (
                            <>
                                <CommandGroup>
                                    <CommandItem onSelect={() => setMenuPath([])} value="back-to-root">
                                        <ChevronLeftIcon className="mr-2 size-3.5" />

                                        <span className="flex-1 text-muted-foreground">Back</span>
                                    </CommandItem>
                                </CommandGroup>

                                {menuPath[0] === 'workflows' && (
                                    <>
                                        {workflowProjectGroups.length === 0 && (
                                            <CommandEmpty>No workflows.</CommandEmpty>
                                        )}

                                        {workflowProjectGroups.map((group) => (
                                            <CommandGroup
                                                heading={group.projectName}
                                                key={`workflow-project-${group.projectId}`}
                                            >
                                                {group.workflows.map((workflow) => (
                                                    <CommandItem
                                                        key={`workflow-${workflow.id}`}
                                                        onSelect={() => handleSelectWorkflow(workflow)}
                                                        value={`workflow-${workflow.id}-${workflow.name}`}
                                                    >
                                                        <WorkflowIcon className="mr-2 size-3.5" />

                                                        {workflow.name}
                                                    </CommandItem>
                                                ))}
                                            </CommandGroup>
                                        ))}

                                        {filteredWorkflows.length > workflowsShowCount && (
                                            <CommandGroup>
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
                                                        {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredWorkflows.length - workflowsShowCount)} more…`}
                                                    </span>
                                                </CommandItem>
                                            </CommandGroup>
                                        )}
                                    </>
                                )}

                                {menuPath[0] === 'files' && (
                                    <CommandGroup heading="Files">
                                        {visibleFiles.length === 0 && <CommandEmpty>No files.</CommandEmpty>}

                                        {visibleFiles.map((file) => (
                                            <CommandItem
                                                key={`file-${file.id}`}
                                                onSelect={() => handleSelectFile(String(file.id), file.name)}
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
                                                onSelect={() => handleSelectDataTable(String(table.id), table.baseName)}
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
                                                        handleSelectKnowledgeBase(
                                                            String(knowledgeBase.id),
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
                                                    onSelect={() => handleSelectWorkflowExecution(execution.id, label)}
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

                                {menuPath[0] === 'artifacts' && (
                                    <CommandGroup heading="Artifacts">
                                        {/*
                                         * cmdk's <CommandEmpty> only renders when the WHOLE Command has zero
                                         * registered CommandItems, not when this group is empty — the sibling
                                         * "Back" item (always mounted in every branch view) keeps that count
                                         * above zero, so <CommandEmpty> would silently render nothing here. A
                                         * plain element styled like CommandEmpty (`py-6 text-center text-sm`)
                                         * sidesteps that library limitation.
                                         */}

                                        {clickableArtifacts.length === 0 && (
                                            <div className="py-6 text-center text-sm text-muted-foreground">
                                                No artifacts yet.
                                            </div>
                                        )}

                                        {clickableArtifacts.map((artifact) => (
                                            <CommandItem
                                                key={`artifact-${artifact.id}`}
                                                onSelect={() => handleSelectArtifact(artifact)}
                                                value={`artifact-${artifact.id}-${artifact.artifactName}`}
                                            >
                                                {getArtifactIcon(artifact.kind)}

                                                <span className="ml-2 truncate">{artifact.artifactName}</span>
                                            </CommandItem>
                                        ))}
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

export default AiHubFilePicker;
