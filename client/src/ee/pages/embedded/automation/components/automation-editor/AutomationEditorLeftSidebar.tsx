import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AutomationEditorProjectSelect, {
    ALL_PROJECTS_VALUE,
} from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorProjectSelect';
import AutomationEditorWorkflowsListItem from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorWorkflowsListItem';
import {useAutomationEditorSidebarStore} from '@/ee/pages/embedded/automation/components/automation-editor/stores/useAutomationEditorSidebarStore';
import {useCreateAutomationProjectWorkflow} from '@/ee/pages/embedded/automation/hooks/useCreateAutomationProjectWorkflow';
import AutomationDialog, {
    AutomationFormValuesI,
} from '@/ee/pages/embedded/automations/components/automation-dialog/AutomationDialog';
import AutomationProjectDialog, {
    AutomationProjectFormValuesI,
} from '@/ee/pages/embedded/automations/components/automation-project-dialog/AutomationProjectDialog';
import WorkflowsListSkeleton from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListSkeleton';
import WorkflowsListFilter from '@/shared/components/workflow/WorkflowsListFilter';
import {
    AutomationProjectsQuery,
    useAutomationProjectCategoriesQuery,
    useAutomationProjectTagsQuery,
    useAutomationProjectsQuery,
    useCreateAutomationProjectMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, PlusIcon, UploadIcon} from 'lucide-react';
import {RefObject, useEffect, useMemo, useRef, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type AutomationProjectWorkflowTemplateType = AutomationProjectType['workflowTemplates'][number];

interface ProjectWorkflowsGroupI {
    project: AutomationProjectType;
    workflows: AutomationProjectWorkflowTemplateType[];
}

const getWorkflowLabel = (workflow: AutomationProjectWorkflowTemplateType) => workflow.label ?? workflow.workflowUuid;

const filterAndSortWorkflows = (
    workflows: AutomationProjectWorkflowTemplateType[],
    searchValue: string,
    sortBy: string
) => {
    const filteredWorkflows = workflows.filter((workflow) =>
        getWorkflowLabel(workflow).toLowerCase().includes(searchValue.toLowerCase())
    );

    if (sortBy === 'last-edited') {
        return filteredWorkflows.sort((firstWorkflow, secondWorkflow) =>
            (secondWorkflow.lastModifiedDate ?? '').localeCompare(firstWorkflow.lastModifiedDate ?? '')
        );
    }

    if (sortBy === 'reverse-alphabetical') {
        return filteredWorkflows.sort((firstWorkflow, secondWorkflow) =>
            getWorkflowLabel(secondWorkflow).localeCompare(getWorkflowLabel(firstWorkflow))
        );
    }

    return filteredWorkflows.sort((firstWorkflow, secondWorkflow) =>
        getWorkflowLabel(firstWorkflow).localeCompare(getWorkflowLabel(secondWorkflow))
    );
};

const ProjectStatusBadge = ({project}: {project: AutomationProjectType}) => {
    const projectPublished = project.lastPublishedVersion === project.version;

    return projectPublished ? (
        <Badge className="flex shrink-0 space-x-1 border-transparent" styleType="success-outline" weight="semibold">
            <span>V{project.version}</span>

            <span>PUBLISHED</span>
        </Badge>
    ) : (
        <Badge
            className="flex shrink-0 space-x-1 bg-surface-neutral-secondary text-content-neutral-primary"
            styleType="secondary-filled"
            weight="semibold"
        >
            <span>V{project.version}</span>

            <span>DRAFT</span>
        </Badge>
    );
};

interface AutomationEditorLeftSidebarProps {
    bottomResizablePanelRef?: RefObject<PanelImperativeHandle | null>;
    currentWorkflowId: string;
}

const AutomationEditorLeftSidebar = ({
    bottomResizablePanelRef,
    currentWorkflowId,
}: AutomationEditorLeftSidebarProps) => {
    const [searchValue, setSearchValue] = useState('');
    const [selectedProjectId, setSelectedProjectId] = useState<string>('');
    const [showProjectDialog, setShowProjectDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);
    const [sortBy, setSortBy] = useState('last-edited');

    const searchInputRef = useRef<HTMLInputElement>(null);

    const leftSidebarOpen = useAutomationEditorSidebarStore((state) => state.leftSidebarOpen);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {data: projectsData, isLoading: projectsIsLoading} = useAutomationProjectsQuery();
    const {data: categoriesData} = useAutomationProjectCategoriesQuery();
    const {data: tagsData} = useAutomationProjectTagsQuery();

    const createProjectMutation = useCreateAutomationProjectMutation();

    const projects = useMemo(() => projectsData?.automationProjects ?? [], [projectsData]);

    const currentProject = projects.find((automationProject) =>
        automationProject.workflowTemplates.some(
            (projectWorkflow) => projectWorkflow.workflowUuid === currentWorkflowId
        )
    );

    const currentProjectId = currentProject?.id;

    const allProjectsSelected = selectedProjectId === ALL_PROJECTS_VALUE;

    const targetProjectId = allProjectsSelected || !selectedProjectId ? currentProjectId : selectedProjectId;

    const {createWorkflow, handleWorkflowFileChange, workflowFileInputRef} = useCreateAutomationProjectWorkflow({
        bottomResizablePanelRef,
        projectId: targetProjectId,
    });

    const projectWorkflowsGroups = useMemo<ProjectWorkflowsGroupI[]>(() => {
        const visibleProjects = allProjectsSelected
            ? projects
            : projects.filter((automationProject) => automationProject.id === selectedProjectId);

        return visibleProjects
            .map((automationProject) => ({
                project: automationProject,
                workflows: filterAndSortWorkflows(automationProject.workflowTemplates, searchValue, sortBy),
            }))
            .filter((projectWorkflowsGroup) => projectWorkflowsGroup.workflows.length > 0);
    }, [allProjectsSelected, projects, searchValue, selectedProjectId, sortBy]);

    const handleCreateProjectSubmit = (values: AutomationProjectFormValuesI) => {
        createProjectMutation.mutate(
            {
                category: values.category || undefined,
                description: values.description || undefined,
                name: values.name,
                permissionExpression: values.permissionExpression,
                tags: values.tags,
            },
            {
                onSuccess: (data) => {
                    void queryClient.invalidateQueries({queryKey: ['automationProjectCategories']});
                    void queryClient.invalidateQueries({queryKey: ['automationProjectTags']});
                    void queryClient.invalidateQueries({queryKey: ['automationProjects']});

                    setSelectedProjectId(data.createAutomationProject);
                    setShowProjectDialog(false);
                },
            }
        );
    };

    const handleCreateWorkflowSubmit = (values: AutomationFormValuesI) => {
        createWorkflow(values);

        setShowWorkflowDialog(false);
    };

    const handleWorkflowClick = (workflowUuid: string) => {
        if (workflowUuid !== currentWorkflowId) {
            void navigate(`/embedded/automations/${workflowUuid}/editor`);
        }
    };

    const renderWorkflowsListItems = ({project, workflows}: ProjectWorkflowsGroupI) =>
        workflows.map((workflow) => (
            <AutomationEditorWorkflowsListItem
                currentWorkflowId={currentWorkflowId}
                key={workflow.workflowUuid}
                onWorkflowClick={handleWorkflowClick}
                project={project}
                workflow={workflow}
            />
        ));

    useEffect(() => {
        if (!currentProjectId) {
            return;
        }

        setSelectedProjectId(currentProjectId);
    }, [currentProjectId]);

    useEffect(() => {
        if (!leftSidebarOpen || projectsIsLoading) {
            return;
        }

        const timeoutId = setTimeout(() => {
            searchInputRef.current?.focus();
        }, 50);

        return () => clearTimeout(timeoutId);
    }, [leftSidebarOpen, projectsIsLoading, selectedProjectId]);

    return (
        <aside className="flex h-full min-w-[355px] flex-col items-center gap-2 bg-surface-main px-4 pt-2.5">
            <div className="flex w-full flex-col gap-4">
                {projectsIsLoading ? (
                    <div className="flex items-center gap-2">
                        <Skeleton className="h-9 flex-1 rounded-md" />

                        <Skeleton className="size-9 rounded-md" />
                    </div>
                ) : (
                    <div className="flex items-center gap-2">
                        <AutomationEditorProjectSelect
                            projectId={currentProject?.id ?? ''}
                            projects={projects}
                            selectedProjectId={selectedProjectId}
                            setSelectedProjectId={setSelectedProjectId}
                        />

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="New Project"
                                    icon={<PlusIcon />}
                                    onClick={() => setShowProjectDialog(true)}
                                    size="icon"
                                    variant="outline"
                                />
                            </TooltipTrigger>

                            <TooltipContent side="left">New Project</TooltipContent>
                        </Tooltip>
                    </div>
                )}

                <WorkflowsListFilter
                    ref={searchInputRef}
                    searchValue={searchValue}
                    setSearchValue={setSearchValue}
                    setSortBy={setSortBy}
                    sortBy={sortBy}
                />

                <ButtonGroup className="w-full">
                    <Button
                        className="flex-1 [&_svg]:size-5"
                        disabled={!targetProjectId}
                        icon={<PlusIcon />}
                        label="New Workflow"
                        onClick={() => setShowWorkflowDialog(true)}
                        variant="secondary"
                    />

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                aria-label="More workflow options"
                                className="data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary [&_svg]:size-5"
                                disabled={!targetProjectId}
                                icon={<ChevronDownIcon />}
                                size="icon"
                                variant="secondary"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem
                                className="cursor-pointer"
                                onClick={() => workflowFileInputRef.current?.click()}
                            >
                                <UploadIcon /> Import Workflow
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </ButtonGroup>
            </div>

            <ScrollArea className="mb-3 min-h-0 w-full flex-1 [&_[data-radix-scroll-area-viewport]>div]:block!">
                {projectsIsLoading && <WorkflowsListSkeleton />}

                {!projectsIsLoading && projectWorkflowsGroups.length > 0 && (
                    <ul className="flex flex-col gap-4">
                        {allProjectsSelected
                            ? projectWorkflowsGroups.map((projectWorkflowsGroup) => (
                                  <li
                                      className="max-w-full border-b border-stroke-neutral-secondary pb-4 last:border-b-0 last:pb-0"
                                      key={projectWorkflowsGroup.project.id}
                                  >
                                      <div className="flex w-full items-center justify-between gap-2">
                                          <Tooltip>
                                              <TooltipTrigger asChild>
                                                  <h2 className="min-w-0 truncate rounded-md px-1 py-2 text-lg font-medium">
                                                      {projectWorkflowsGroup.project.name}
                                                  </h2>
                                              </TooltipTrigger>

                                              {projectWorkflowsGroup.project.name.length > 25 && (
                                                  <TooltipContent className="max-w-80">
                                                      {projectWorkflowsGroup.project.name}
                                                  </TooltipContent>
                                              )}
                                          </Tooltip>

                                          <ProjectStatusBadge project={projectWorkflowsGroup.project} />
                                      </div>

                                      <ul className="flex flex-col gap-2">
                                          {renderWorkflowsListItems(projectWorkflowsGroup)}
                                      </ul>
                                  </li>
                              ))
                            : renderWorkflowsListItems(projectWorkflowsGroups[0])}
                    </ul>
                )}

                {!projectsIsLoading && projectWorkflowsGroups.length === 0 && (
                    <span className="block w-full py-2 text-sm text-muted-foreground">No workflows found</span>
                )}
            </ScrollArea>

            {showProjectDialog && (
                <AutomationProjectDialog
                    categories={categoriesData?.automationProjectCategories}
                    onClose={() => setShowProjectDialog(false)}
                    onSubmit={handleCreateProjectSubmit}
                    tags={tagsData?.automationProjectTags}
                />
            )}

            {showWorkflowDialog && (
                <AutomationDialog onClose={() => setShowWorkflowDialog(false)} onSubmit={handleCreateWorkflowSubmit} />
            )}

            <input
                accept=".json,.yaml,.yml"
                aria-label="Import workflow file"
                className="hidden"
                onChange={handleWorkflowFileChange}
                ref={workflowFileInputRef}
                type="file"
            />
        </aside>
    );
};

export default AutomationEditorLeftSidebar;
