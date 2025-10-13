import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {ScrollArea} from '@/components/ui/scroll-area';
import {useToast} from '@/hooks/use-toast';
import ProjectSelect from '@/pages/automation/project/components/projects-sidebar/components/ProjectSelect';
import ProjectWorkflowsList from '@/pages/automation/project/components/projects-sidebar/components/ProjectWorkflowsList';
import WorkflowsListFilter from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListFilter';
import WorkflowsListItem from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItem';
import WorkflowsListSkeleton from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListSkeleton';
import {useProjectsLeftSidebar} from '@/pages/automation/project/components/projects-sidebar/hooks/useProjectsLeftSidebar';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useCreateProjectWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {useGetProjectWorkflowsQuery, useGetWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {LayoutTemplateIcon, PlusIcon, UploadIcon} from 'lucide-react';
import {ChangeEvent, RefObject, useEffect, useMemo, useRef, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useNavigate} from 'react-router-dom';

interface ProjectsLeftSidebarProps {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    projectId: number;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    currentWorkflowId: string;
}

const ProjectsLeftSidebar = ({
    bottomResizablePanelRef,
    currentWorkflowId,
    onProjectClick,
    projectId,
    updateWorkflowMutation,
}: ProjectsLeftSidebarProps) => {
    const [selectedProjectId, setSelectedProjectId] = useState(projectId || 0);
    const [sortBy, setSortBy] = useState('last-edited');
    const [searchValue, setSearchValue] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

    const hiddenFileInputRef = useRef<HTMLInputElement>(null);
    const searchInputRef = useRef<HTMLInputElement>(null);
    const {toast} = useToast();
    const navigate = useNavigate();

    const {captureProjectWorkflowImported} = useAnalytics();

    const ff_1041 = useFeatureFlagsStore()('ff-1041');

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
            projectId,
        });

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: projects, refetch: refetchProjects} = useGetWorkspaceProjectsQuery({
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

            if (hiddenFileInputRef.current) {
                hiddenFileInputRef.current.value = '';
            }

            toast({
                description: 'Workflow is imported.',
            });
        },
    });

    const handleImportProjectWorkflowClick = (definition: string) => {
        importProjectWorkflowMutation.mutate({
            id: selectedProjectId,
            workflow: {
                definition,
            },
        });
    };

    const handleFileChange = async (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            const file = e.target.files[0];

            /* eslint-disable @typescript-eslint/no-explicit-any */
            const definition = await (typeof (file as any).text === 'function'
                ? (file as Blob).text()
                : new Response(file).text());

            handleImportProjectWorkflowClick(definition);
        }
    };

    useEffect(() => {
        setIsLoading(projectWorkflowsLoading || allProjectsWorkflowsLoading);
    }, [projectWorkflowsLoading, allProjectsWorkflowsLoading]);

    useEffect(() => {
        if (selectedProjectId === 0) {
            refetchProjects();
        }
    }, [selectedProjectId, refetchProjects]);

    useEffect(() => {
        const timeoutId = setTimeout(() => {
            if (searchInputRef.current) {
                searchInputRef.current.focus();
            }
        }, 50);

        return () => clearTimeout(timeoutId);
    }, [selectedProjectId]);

    return (
        <aside className="flex h-full flex-col items-center gap-2 bg-surface-main pt-0.5">
            <div className="flex flex-col gap-2 px-4">
                {projects && (
                    <ProjectSelect
                        projectId={projectId}
                        projects={projects}
                        selectedProjectId={selectedProjectId}
                        setSelectedProjectId={setSelectedProjectId}
                    />
                )}

                <WorkflowsListFilter
                    ref={searchInputRef}
                    searchValue={searchValue}
                    setSearchValue={setSearchValue}
                    setSortBy={setSortBy}
                    sortBy={sortBy}
                />

                {selectedProjectId === projectId && (
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button
                                className="w-full bg-surface-neutral-secondary py-2 hover:bg-background [&_svg]:size-5"
                                size="icon"
                                variant="ghost"
                            >
                                <div className="flex items-center justify-center gap-2">
                                    <PlusIcon />

                                    <span>New workflow</span>
                                </div>
                            </Button>
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => setShowWorkflowDialog(true)}>
                                <PlusIcon /> From Scratch
                            </DropdownMenuItem>

                            {ff_1041 && (
                                <DropdownMenuItem onClick={() => navigate(`./../../templates`)}>
                                    <LayoutTemplateIcon /> From Template
                                </DropdownMenuItem>
                            )}

                            <DropdownMenuItem
                                onClick={() => {
                                    if (hiddenFileInputRef.current) {
                                        hiddenFileInputRef.current.click();
                                    }
                                }}
                            >
                                <UploadIcon /> Import Workflow
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                )}
            </div>

            <ScrollArea className="mb-3 h-screen w-full overflow-y-auto px-4 [scrollbar-color:theme(colors.stroke-neutral-secondary)_transparent] [scrollbar-gutter:stable] [scrollbar-width:thin]">
                {isLoading && <WorkflowsListSkeleton />}

                {!isLoading && (
                    <ul className="flex flex-col items-center gap-4">
                        {selectedProjectId === 0 &&
                            (projects || []).map((project) => (
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
                            ))}

                        {selectedProjectId !== 0 &&
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
                            ))}
                    </ul>
                )}
            </ScrollArea>

            {showWorkflowDialog && (
                <WorkflowDialog
                    createWorkflowMutation={createProjectWorkflowMutation}
                    onClose={() => setShowWorkflowDialog(false)}
                    projectId={projectId}
                    updateWorkflowMutation={updateWorkflowMutation}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}

            <input alt="file" className="hidden" onChange={handleFileChange} ref={hiddenFileInputRef} type="file" />
        </aside>
    );
};

export default ProjectsLeftSidebar;
