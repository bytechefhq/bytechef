import {Command, CommandGroup, CommandInput, CommandItem, CommandList} from '@/components/ui/command';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {ReferencedResourceKindType} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {useAiAgentsQuery} from '@/shared/middleware/graphql';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {BotIcon, ChevronLeftIcon, ChevronRightIcon, WorkflowIcon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';
import {useDebouncedCallback} from 'use-debounce';

export interface ResourcePickerSelectionI {
    id: string;
    kind: ReferencedResourceKindType;
    name: string;
    projectId?: string;
    projectWorkflowId?: number;
}

export interface ResourcePickerMenuPropsI {
    workspaceId: number;
    open?: boolean;
    trigger: ReactNode;
    onOpenChange?: (open: boolean) => void;
    onSelect: (selection: ResourcePickerSelectionI) => void;
}

interface WorkflowItemI {
    id: string;
    name: string;
    projectId: string;
    projectName: string;
    projectWorkflowId: number;
}

const SECTION_INITIAL_CAP = 20;
const SECTION_EXPAND_INCREMENT = 50;

type MenuPathType = [] | ['aiAgents'] | ['workflows'] | ['workflows', string];

const EmptyMessage = ({children}: {children: ReactNode}) => (
    <div className="py-6 text-center text-sm text-muted-foreground">{children}</div>
);

const ResourcePickerMenu = ({
    onOpenChange,
    onSelect,
    open: controlledOpen,
    trigger,
    workspaceId,
}: ResourcePickerMenuPropsI) => {
    const [uncontrolledOpen, setUncontrolledOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [menuPath, setMenuPath] = useState<MenuPathType>([]);
    const [aiAgentsShowCount, setAiAgentsShowCount] = useState(SECTION_INITIAL_CAP);
    const [workflowsShowCount, setWorkflowsShowCount] = useState(SECTION_INITIAL_CAP);

    const open = controlledOpen ?? uncontrolledOpen;
    const selectedProjectId = menuPath.length === 2 ? menuPath[1] : undefined;

    const {data: projects, isLoading: projectsLoading} = useGetWorkspaceProjectsQuery(
        {id: workspaceId},
        open && isWorkspaceIdKnown(workspaceId)
    );

    const {data: aiAgentsData, isLoading: aiAgentsLoading} = useAiAgentsQuery(
        {workspaceId: String(workspaceId)},
        {enabled: open && isWorkspaceIdKnown(workspaceId)}
    );

    const {data: projectWorkflows, isLoading: projectWorkflowsLoading} = useGetProjectWorkflowsQuery(
        Number(selectedProjectId ?? 0),
        open && selectedProjectId != null
    );

    const handleSearchChange = useDebouncedCallback((value: string) => {
        setDebouncedSearch(value);
    }, 300);

    const lowerSearch = debouncedSearch.toLowerCase();

    const filteredWorkflowProjects = useMemo(
        () =>
            (projects ?? []).filter(
                (project) =>
                    project.id != null &&
                    (project.projectWorkflowIds?.length ?? 0) > 0 &&
                    project.name.toLowerCase().includes(lowerSearch)
            ),
        [projects, lowerSearch]
    );

    const filteredAiAgents = useMemo(
        () =>
            (aiAgentsData?.aiAgents ?? []).filter(
                (aiAgent) => aiAgent != null && aiAgent.title.toLowerCase().includes(lowerSearch)
            ),
        [aiAgentsData, lowerSearch]
    );

    const selectedWorkflowProject = useMemo(() => {
        if (selectedProjectId == null) {
            return null;
        }

        return projects?.find((project) => String(project.id) === selectedProjectId) ?? null;
    }, [projects, selectedProjectId]);

    const filteredWorkflows = useMemo<WorkflowItemI[]>(() => {
        if (selectedProjectId == null || !projectWorkflows) {
            return [];
        }

        return projectWorkflows
            .filter((workflow) => workflow.id != null && (workflow.label ?? '').toLowerCase().includes(lowerSearch))
            .map((workflow) => ({
                id: workflow.id!,
                name: workflow.label || workflow.id!,
                projectId: selectedProjectId,
                projectName: selectedWorkflowProject?.name ?? '',
                projectWorkflowId: workflow.projectWorkflowId ?? 0,
            }));
    }, [lowerSearch, projectWorkflows, selectedProjectId, selectedWorkflowProject]);

    const showsWorkflowProjects =
        (menuPath.length === 1 && menuPath[0] === 'workflows') || (menuPath.length === 0 && debouncedSearch !== '');

    const showsAiAgents = menuPath[0] === 'aiAgents' || (menuPath.length === 0 && debouncedSearch !== '');

    const isRootMenu = menuPath.length === 0 && debouncedSearch === '';

    const resetSearch = () => {
        setSearch('');
        setDebouncedSearch('');
        setAiAgentsShowCount(SECTION_INITIAL_CAP);
        setWorkflowsShowCount(SECTION_INITIAL_CAP);
    };

    const navigateTo = (path: MenuPathType) => {
        setMenuPath(path);

        resetSearch();
    };

    const handleOpenChange = (nextOpen: boolean) => {
        setUncontrolledOpen(nextOpen);

        if (!nextOpen) {
            setMenuPath([]);

            resetSearch();
        }

        onOpenChange?.(nextOpen);
    };

    const handleSelectAiAgent = (aiAgentId: string, title: string) => {
        onSelect({id: aiAgentId, kind: 'aiAgent', name: title});

        handleOpenChange(false);
    };

    const handleSelectWorkflow = (workflow: WorkflowItemI) => {
        onSelect({
            id: workflow.id,
            kind: 'workflow',
            name: workflow.name,
            projectId: workflow.projectId,
            projectWorkflowId: workflow.projectWorkflowId,
        });

        handleOpenChange(false);
    };

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
                        {isRootMenu ? (
                            <CommandGroup>
                                <CommandItem onSelect={() => navigateTo(['workflows'])} value="root-workflows">
                                    <WorkflowIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">Workflows</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>

                                <CommandItem onSelect={() => navigateTo(['aiAgents'])} value="root-ai-agents">
                                    <BotIcon className="mr-2 size-3.5" />

                                    <span className="flex-1">AI Agents</span>

                                    <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                </CommandItem>
                            </CommandGroup>
                        ) : (
                            <>
                                {menuPath.length > 0 && (
                                    <CommandGroup>
                                        <CommandItem
                                            onSelect={() => navigateTo(menuPath.length === 2 ? ['workflows'] : [])}
                                            value="back-to-root"
                                        >
                                            <ChevronLeftIcon className="mr-2 size-3.5" />

                                            <span className="flex-1 text-muted-foreground">Back</span>
                                        </CommandItem>
                                    </CommandGroup>
                                )}

                                {showsAiAgents && (
                                    <CommandGroup heading="AI Agents">
                                        {filteredAiAgents.length === 0 && (
                                            <EmptyMessage>
                                                {aiAgentsLoading ? 'Loading…' : 'No AI Agents.'}
                                            </EmptyMessage>
                                        )}

                                        {filteredAiAgents.slice(0, aiAgentsShowCount).map((aiAgent) => (
                                            <CommandItem
                                                key={`ai-agent-${aiAgent.id}`}
                                                onSelect={() => handleSelectAiAgent(aiAgent.id, aiAgent.title)}
                                                value={`ai-agent-${aiAgent.id}-${aiAgent.title}`}
                                            >
                                                <BotIcon className="mr-2 size-3.5" />

                                                {aiAgent.title}
                                            </CommandItem>
                                        ))}

                                        {filteredAiAgents.length > aiAgentsShowCount && (
                                            <CommandItem
                                                key="ai-agents-show-more"
                                                onSelect={() =>
                                                    setAiAgentsShowCount((count) => count + SECTION_EXPAND_INCREMENT)
                                                }
                                                value="ai-agents-show-more"
                                            >
                                                <span className="text-xs text-muted-foreground">
                                                    {`Show ${Math.min(SECTION_EXPAND_INCREMENT, filteredAiAgents.length - aiAgentsShowCount)} more…`}
                                                </span>
                                            </CommandItem>
                                        )}
                                    </CommandGroup>
                                )}

                                {showsWorkflowProjects && (
                                    <CommandGroup heading="Workflows (pick a project)">
                                        {filteredWorkflowProjects.length === 0 && (
                                            <EmptyMessage>
                                                {projectsLoading ? 'Loading…' : 'No projects with workflows.'}
                                            </EmptyMessage>
                                        )}

                                        {filteredWorkflowProjects.map((project) => (
                                            <CommandItem
                                                key={`workflow-project-${project.id}`}
                                                onSelect={() => navigateTo(['workflows', String(project.id)])}
                                                value={`workflow-project-${project.id}-${project.name}`}
                                            >
                                                <WorkflowIcon className="mr-2 size-3.5" />

                                                <span className="flex-1">{project.name}</span>

                                                <span className="mr-2 text-xs text-muted-foreground">
                                                    {project.projectWorkflowIds?.length ?? 0}
                                                </span>

                                                <ChevronRightIcon className="size-3.5 text-muted-foreground" />
                                            </CommandItem>
                                        ))}
                                    </CommandGroup>
                                )}

                                {selectedProjectId != null && (
                                    <CommandGroup
                                        heading={`Workflows in ${selectedWorkflowProject?.name ?? selectedProjectId}`}
                                    >
                                        {filteredWorkflows.length === 0 ? (
                                            <EmptyMessage>
                                                {projectWorkflowsLoading ? 'Loading…' : 'No workflows.'}
                                            </EmptyMessage>
                                        ) : (
                                            <>
                                                {filteredWorkflows.slice(0, workflowsShowCount).map((workflow) => (
                                                    <CommandItem
                                                        key={`workflow-${workflow.id}`}
                                                        onSelect={() => handleSelectWorkflow(workflow)}
                                                        value={`workflow-${workflow.id}-${workflow.name}`}
                                                    >
                                                        <WorkflowIcon className="mr-2 size-3.5" />

                                                        {workflow.name}
                                                    </CommandItem>
                                                ))}

                                                {filteredWorkflows.length > workflowsShowCount && (
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
                                                )}
                                            </>
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
