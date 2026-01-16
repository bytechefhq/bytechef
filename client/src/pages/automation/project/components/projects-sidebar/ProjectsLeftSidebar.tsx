import Button from '@/components/Button/Button';
import {ButtonGroup} from '@/components/ui/button-group';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/hooks/use-toast';
import ProjectSelect from '@/pages/automation/project/components/projects-sidebar/components/ProjectSelect';
import ProjectWorkflowsList from '@/pages/automation/project/components/projects-sidebar/components/ProjectWorkflowsList';
import WorkflowsListFilter from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListFilter';
import WorkflowsListItem from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItem';
import WorkflowsListSkeleton from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListSkeleton';
import {useProjectsLeftSidebar} from '@/pages/automation/project/components/projects-sidebar/hooks/useProjectsLeftSidebar';
import handleImportProject from '@/pages/automation/project/utils/handleImportProject';
import handleImportWorkflow from '@/pages/automation/project/utils/handleImportWorkflow';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {useGetProjectWorkflowsQuery, useGetWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, LayoutTemplateIcon, PlusIcon, UploadIcon} from 'lucide-react';
import {RefObject, useEffect, useMemo, useRef, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';

interface ProjectsLeftSidebarProps {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    projectId: number;
    currentWorkflowId: string;
}

const ProjectsLeftSidebar = ({
    bottomResizablePanelRef,
    currentWorkflowId,
    onProjectClick,
    projectId,
}: ProjectsLeftSidebarProps) => {
    const [selectedProjectId, setSelectedProjectId] = useState(!isNaN(projectId) ? projectId : 0);
    const [sortBy, setSortBy] = useState('last-edited');
    const [searchValue, setSearchValue] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [showProjectDialog, setShowProjectDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const projectHiddenFileInputRef = useRef<HTMLInputElement>(null);
    const searchInputRef = useRef<HTMLInputElement>(null);
    const workflowHiddenFileInputRef = useRef<HTMLInputElement>(null);

    const {toast} = useToast();
    const navigate = useNavigate();

    const {captureProjectWorkflowImported} = useAnalytics();

    const ff_1041 = useFeatureFlagsStore()('ff-1041');
    const ff_2482 = useFeatureFlagsStore()('ff-2482');

    const {data: eachProjectWorkflows, isLoading: projectWorkflowsLoading} = useGetProjectWorkflowsQuery(
        selectedProjectId,
        selectedProjectId !== 0
    );
    const {data: allProjectsWorkflows, isLoading: allProjectsWorkflowsLoading} = useGetWorkflowsQuery(
        selectedProjectId === 0
    );
    const workflows = eachProjectWorkflows || allProjectsWorkflows;

    const {calculateTimeDifference, createProjectWorkflowMutation, getFilteredWorkflows, getWorkflowsProjectId} =
        useProjectsLeftSidebar({
            bottomResizablePanelRef,
            projectId: selectedProjectId === 0 ? projectId : selectedProjectId,
        });

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        data: projects,
        isLoading: projectsLoading,
        refetch: refetchProjects,
    } = useGetWorkspaceProjectsQuery({
        id: currentWorkspaceId!,
    });

    const findProjectIdByWorkflow = getWorkflowsProjectId(projects || []);

    const filteredWorkflowsList = useMemo(
        () => getFilteredWorkflows(workflows, sortBy, searchValue),
        [workflows, sortBy, searchValue, getFilteredWorkflows]
    );

    const queryClient = useQueryClient();

    const importProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: () => {
            captureProjectWorkflowImported();

            queryClient.invalidateQueries({queryKey: ProjectKeys.project(selectedProjectId)});
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            if (workflowHiddenFileInputRef.current) {
                workflowHiddenFileInputRef.current.value = '';
            }

            toast({
                description: 'Workflow is imported.',
            });
        },
    });

    useEffect(() => {
        setIsLoading(projectWorkflowsLoading || allProjectsWorkflowsLoading || projectsLoading);
    }, [projectWorkflowsLoading, allProjectsWorkflowsLoading, projectsLoading]);

    useEffect(() => {
        if (selectedProjectId === 0) {
            refetchProjects();
        }
    }, [selectedProjectId, refetchProjects]);

    useEffect(() => {
        setSelectedProjectId(!isNaN(projectId) ? projectId : 0);
    }, [projectId]);

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            if (searchInputRef.current) {
                searchInputRef.current.focus();
            }
        }, 50);

        return () => clearTimeout(timeoutId);
    }, [selectedProjectId]);

    return (
        <aside className="flex h-full flex-col items-center gap-2 bg-surface-main pt-3">
            <div className="mx-4 flex w-80 flex-col gap-2">
                {projects && (
                    <div className="flex items-center gap-2">
                        <ProjectSelect
                            projectId={projectId}
                            projects={projects}
                            selectedProjectId={selectedProjectId}
                            setSelectedProjectId={setSelectedProjectId}
                        />

                        {ff_2482 ? (
                            <DropdownMenu>
                                <Tooltip>
                                    <DropdownMenuTrigger asChild>
                                        <TooltipTrigger asChild>
                                            <Button
                                                aria-label="New project"
                                                className="data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                                                icon={<PlusIcon />}
                                                size="icon"
                                                variant="outline"
                                            />
                                        </TooltipTrigger>
                                    </DropdownMenuTrigger>

                                    <TooltipContent>New project</TooltipContent>
                                </Tooltip>

                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        onClick={() => setShowProjectDialog(true)}
                                    >
                                        <PlusIcon className="mr-2 size-4" />
                                        From Scratch
                                    </DropdownMenuItem>

                                    {ff_1041 && (
                                        <DropdownMenuItem
                                            className="cursor-pointer"
                                            onClick={() => navigate(`templates`)}
                                        >
                                            <LayoutTemplateIcon className="mr-2 size-4" />
                                            From Template
                                        </DropdownMenuItem>
                                    )}

                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        onClick={() => projectHiddenFileInputRef.current?.click()}
                                    >
                                        <UploadIcon className="mr-2 size-4" />
                                        Import Project
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        ) : (
                            <ProjectDialog
                                project={undefined}
                                triggerNode={
                                    <Button
                                        aria-label="New project"
                                        className="data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                                        icon={<PlusIcon />}
                                        size="icon"
                                        variant="outline"
                                    />
                                }
                            />
                        )}
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
                        className="w-full [&_svg]:size-5"
                        icon={<PlusIcon />}
                        label="Workflow"
                        onClick={() => setShowWorkflowDialog(true)}
                        variant="secondary"
                    />

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                className="data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary [&_svg]:size-5"
                                icon={<ChevronDownIcon />}
                                size="icon"
                                variant="secondary"
                            />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            {ff_1041 && (
                                <DropdownMenuItem
                                    className="cursor-pointer"
                                    onClick={() => navigate(`./../../../${selectedProjectId}/templates`)}
                                >
                                    <LayoutTemplateIcon /> From Template
                                </DropdownMenuItem>
                            )}

                            <DropdownMenuItem
                                className="cursor-pointer"
                                onClick={() => {
                                    if (workflowHiddenFileInputRef.current) {
                                        workflowHiddenFileInputRef.current.click();
                                    }
                                }}
                            >
                                <UploadIcon /> Import Workflow
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </ButtonGroup>
            </div>

            <ScrollArea className="mb-3 h-screen w-full overflow-y-auto px-4">
                {isLoading && <WorkflowsListSkeleton />}

                {!isLoading && (
                    <ul className="flex flex-col items-center gap-4">
                        {selectedProjectId === 0 &&
                            (projects ? (
                                projects.map((project) => (
                                    <ProjectWorkflowsList
                                        calculateTimeDifference={calculateTimeDifference}
                                        currentWorkflowId={currentWorkflowId}
                                        filteredWorkflowsList={filteredWorkflowsList}
                                        findProjectIdByWorkflow={findProjectIdByWorkflow}
                                        key={project.id}
                                        onProjectClick={onProjectClick}
                                        project={project}
                                        setSelectedProjectId={setSelectedProjectId}
                                    />
                                ))
                            ) : (
                                <span className="text-sm text-muted-foreground">No workflows found</span>
                            ))}

                        {selectedProjectId !== 0 && filteredWorkflowsList.length > 0 ? (
                            filteredWorkflowsList.map((workflow) => (
                                <WorkflowsListItem
                                    calculateTimeDifference={calculateTimeDifference}
                                    currentWorkflowId={currentWorkflowId}
                                    findProjectIdByWorkflow={findProjectIdByWorkflow}
                                    key={workflow.id}
                                    onProjectClick={onProjectClick}
                                    setSelectedProjectId={setSelectedProjectId}
                                    workflow={workflow}
                                />
                            ))
                        ) : (
                            <span className="text-sm text-muted-foreground">No workflows found</span>
                        )}
                    </ul>
                )}
            </ScrollArea>

            {showProjectDialog && <ProjectDialog onClose={() => setShowProjectDialog(false)} project={undefined} />}

            {showWorkflowDialog && (
                <WorkflowDialog
                    createWorkflowMutation={createProjectWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    projectId={selectedProjectId}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input
                accept=".json,.yaml,.yml"
                alt="file"
                className="hidden"
                onChange={(event) => handleImportWorkflow(event, selectedProjectId, importProjectWorkflowMutation)}
                ref={workflowHiddenFileInputRef}
                type="file"
            />

            <input
                accept=".zip"
                className="hidden"
                onChange={(event) => handleImportProject(event, currentWorkspaceId, queryClient)}
                ref={projectHiddenFileInputRef}
                type="file"
            />
        </aside>
    );
};

export default ProjectsLeftSidebar;
