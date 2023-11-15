import ToggleGroup, {IToggleItem} from '@/components/ToggleGroup/ToggleGroup';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import {RightSidebar} from '@/layouts/RightSidebar';
import {ProjectModel, WorkflowModel} from '@/middleware/helios/configuration';
import {useCreateProjectWorkflowMutation} from '@/mutations/projectWorkflows.mutations';
import {
    useDeleteProjectMutation,
    useDuplicateProjectMutation,
    usePublishProjectMutation,
} from '@/mutations/projects.mutations';
import {
    useDeleteWorkflowMutation,
    useDuplicateWorkflowMutation,
    useUpdateWorkflowMutation,
} from '@/mutations/workflows.mutations';
import WorkflowDialog from '@/pages/automation/project/components/WorkflowDialog';
import useRightSidebarStore from '@/pages/automation/project/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import ProjectDialog from '@/pages/automation/projects/ProjectDialog';
import WorkflowExecutionsDetailsAccordion from '@/pages/automation/workflow-executions/components/WorkflowExecutionsDetailsAccordion';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import {ProjectCategoryKeys} from '@/queries/projectCategories.queries';
import {ProjectTagKeys} from '@/queries/projectTags.quries';
import {ProjectKeys, useGetProjectQuery} from '@/queries/projects.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {
    WorkflowKeys,
    useGetProjectWorkflowsQuery,
} from '@/queries/workflows.queries';
import {ChevronDownIcon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {
    CircleDotDashedIcon,
    Code2Icon,
    PlayIcon,
    PuzzleIcon,
} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useLoaderData, useNavigate, useParams} from 'react-router-dom';

import PageLoader from '../../../components/PageLoader/PageLoader';
import LayoutContainer from '../../../layouts/LayoutContainer';
import ProjectWorkflow from './ProjectWorkflow';
import WorkflowNodesSidebar from './components/WorkflowNodesSidebar';
import useLeftSidebarStore from './stores/useLeftSidebarStore';
import useWorkflowDataStore from './stores/useWorkflowDataStore';

const Project = () => {
    const [currentWorkflow, setCurrentWorkflow] = useState<WorkflowModel>({});
    const [filter, setFilter] = useState('');
    const [showDeleteProjectDialog, setShowDeleteProjectDialog] =
        useState(false);
    const [showDeleteWorkflowDialog, setShowDeleteWorkflowDialog] =
        useState(false);
    const [showEditProjectDialog, setShowEditProjectDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore();
    const {leftSidebarOpen, setLeftSidebarOpen} = useLeftSidebarStore();
    const {setNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();

    const {toast} = useToast();

    const {projectId, workflowId} = useParams();
    const navigate = useNavigate();

    const headerToggleItems: IToggleItem[] = [
        {
            label: 'Build',
            value: 'build',
        },
        {
            label: 'Debug',
            value: 'debug',
        },
    ];

    const rightSidebarNavigation = [
        {
            icon: PuzzleIcon,
            name: 'Components & Control Flows',
            onClick: () => {
                setNodeDetailsPanelOpen(false);

                setRightSidebarOpen(!rightSidebarOpen);
            },
        },
        {
            icon: Code2Icon,
            name: 'Workflow Code Editor',
        },
    ];

    const queryClient = useQueryClient();

    const {data: project} = useGetProjectQuery(
        parseInt(projectId!),
        useLoaderData() as ProjectModel
    );

    const {
        data: componentDefinitions,
        error: componentsError,
        isLoading: componentsLoading,
    } = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {
        data: taskDispatcherDefinitions,
        error: taskDispatcherDefinitionsError,
        isLoading: taskDispatcherDefinitionsLoading,
    } = useGetTaskDispatcherDefinitionsQuery();

    const {
        data: projectWorkflows,
        error: projectWorkflowsError,
        isLoading: projectWorkflowsLoading,
    } = useGetProjectWorkflowsQuery(project?.id as number);

    const {setComponentDefinitions, setTaskDispatcherDefinitions} =
        useWorkflowDataStore();

    useEffect(() => {
        if (componentDefinitions) {
            setComponentDefinitions(componentDefinitions);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [componentDefinitions?.length]);

    useEffect(() => {
        if (taskDispatcherDefinitions) {
            setTaskDispatcherDefinitions(taskDispatcherDefinitions);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [taskDispatcherDefinitions?.length]);

    const createProjectWorkflowRequestMutation =
        useCreateProjectWorkflowMutation({
            onSuccess: (workflow) => {
                queryClient.invalidateQueries({
                    queryKey: WorkflowKeys.projectWorkflows(
                        parseInt(projectId!)
                    ),
                });

                setCurrentWorkflow(workflow);
            },
        });

    const deleteProjectMutation = useDeleteProjectMutation({
        onSuccess: () => {
            navigate('/automation/projects');

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
            queryClient.invalidateQueries({
                queryKey: ProjectCategoryKeys.projectCategories,
            });
            queryClient.invalidateQueries({
                queryKey: ProjectTagKeys.projectTags,
            });
        },
    });

    const deleteWorkflowMutationMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            setShowDeleteWorkflowDialog(false);

            navigate('/automation/projects');

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            navigate(
                `/automation/projects/${project?.id}/workflows/${project?.workflowIds![0]}`
            );
        },
    });

    const duplicateWorkflowMutationMutation = useDuplicateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: (project) => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast({
                description: `The project ${project.name} is published.`,
            });
        },
    });

    const updateWorkflowMutationMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            setShowEditWorkflowDialog(false);
        },
    });

    const testWorkflowMutation = useTestWorkflowMutation({
        onSuccess: (workflowExecution) => {
            setWorkflowExecution(workflowExecution);
        },
    });

    useEffect(() => {
        if (projectWorkflows) {
            setCurrentWorkflow(
                projectWorkflows.find(
                    (workflow: WorkflowModel) => workflow.id === workflowId
                )!
            );
        }

        setLeftSidebarOpen(false);

        setNodeDetailsPanelOpen(false);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowId]);

    useEffect(() => {
        if (currentWorkflow?.id) {
            navigate(
                `/automation/projects/${projectId}/workflows/${currentWorkflow.id}`
            );
        }
    }, [currentWorkflow, navigate, projectId, setNodeDetailsPanelOpen]);

    return (
        <PageLoader
            errors={[
                componentsError,
                taskDispatcherDefinitionsError,
                projectWorkflowsError,
            ]}
            loading={
                componentsLoading ||
                taskDispatcherDefinitionsLoading ||
                projectWorkflowsLoading
            }
        >
            <LayoutContainer
                className="bg-muted dark:bg-background"
                header={
                    <header className="ml-4 flex items-center">
                        <div className="mr-4 flex items-center">
                            <h1>{project?.name}</h1>

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button size="icon" variant="ghost">
                                        <ChevronDownIcon />
                                    </Button>
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem
                                        className="text-xs"
                                        onClick={() =>
                                            setShowEditProjectDialog(true)
                                        }
                                    >
                                        Edit
                                    </DropdownMenuItem>

                                    {project && (
                                        <DropdownMenuItem
                                            className="text-xs"
                                            onClick={() =>
                                                duplicateProjectMutation.mutate(
                                                    project.id!
                                                )
                                            }
                                        >
                                            Duplicate
                                        </DropdownMenuItem>
                                    )}

                                    <DropdownMenuSeparator />

                                    <DropdownMenuItem
                                        className="text-xs text-red-600"
                                        onClick={() =>
                                            setShowDeleteProjectDialog(true)
                                        }
                                    >
                                        Delete
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        </div>

                        <div className="mx-2 my-4 flex rounded-md border border-input bg-white shadow-sm">
                            {currentWorkflow && !!projectWorkflows && (
                                <Select
                                    defaultValue={workflowId}
                                    name="projectWorkflowSelect"
                                    onValueChange={(value) => {
                                        setCurrentWorkflow(
                                            projectWorkflows.find(
                                                (workflow: WorkflowModel) =>
                                                    workflow.id === value
                                            )!
                                        );

                                        navigate(
                                            `/automation/projects/${projectId}/workflows/${value}`
                                        );
                                    }}
                                    value={currentWorkflow.id || workflowId}
                                >
                                    <SelectTrigger className="mr-0.5 border-0 bg-white shadow-none">
                                        <SelectValue placeholder="Select a workflow" />
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectGroup>
                                            <SelectLabel>Workflows</SelectLabel>

                                            {projectWorkflows.map(
                                                (workflow) => (
                                                    <SelectItem
                                                        key={workflow.id!}
                                                        value={workflow.id!}
                                                    >
                                                        {workflow.label!}
                                                    </SelectItem>
                                                )
                                            )}
                                        </SelectGroup>
                                    </SelectContent>
                                </Select>
                            )}

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        className="border-0 bg-white shadow-none"
                                        size="icon"
                                        variant="outline"
                                    >
                                        <DotsVerticalIcon />
                                    </Button>
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem
                                        className="text-xs"
                                        onClick={() => {
                                            setShowEditWorkflowDialog(true);
                                        }}
                                    >
                                        Edit
                                    </DropdownMenuItem>

                                    {project && currentWorkflow && (
                                        <DropdownMenuItem
                                            className="text-xs"
                                            onClick={() =>
                                                duplicateWorkflowMutationMutation.mutate(
                                                    {
                                                        id: project.id!,
                                                        workflowId:
                                                            currentWorkflow.id!,
                                                    }
                                                )
                                            }
                                        >
                                            Duplicate
                                        </DropdownMenuItem>
                                    )}

                                    <DropdownMenuSeparator />

                                    <DropdownMenuItem
                                        className="text-xs text-red-600"
                                        onClick={() => {
                                            setShowDeleteWorkflowDialog(true);
                                        }}
                                    >
                                        Delete
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>

                            {!!projectId && (
                                <WorkflowDialog
                                    createWorkflowRequestMutation={
                                        createProjectWorkflowRequestMutation
                                    }
                                    parentId={+projectId}
                                    triggerClassName="border-0 shadow-none"
                                />
                            )}

                            {showEditWorkflowDialog && (
                                <WorkflowDialog
                                    onClose={() =>
                                        setShowEditWorkflowDialog(false)
                                    }
                                    showTrigger={false}
                                    updateWorkflowMutationMutation={
                                        updateWorkflowMutationMutation
                                    }
                                    visible
                                    workflow={currentWorkflow}
                                />
                            )}
                        </div>

                        <div className="flex flex-1 justify-center">
                            <ToggleGroup
                                defaultValue="build"
                                onValueChange={() =>
                                    setLeftSidebarOpen(!leftSidebarOpen)
                                }
                                toggleItems={headerToggleItems}
                                value={leftSidebarOpen ? 'debug' : 'build'}
                            />
                        </div>

                        <div className="flex justify-end">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        className="mr-1 bg-success text-success-foreground hover:bg-success/80"
                                        onClick={() => {
                                            setLeftSidebarOpen(true);
                                            setWorkflowExecution(undefined);

                                            testWorkflowMutation.mutate({
                                                workflowId:
                                                    currentWorkflow.id ||
                                                    workflowId,
                                            });
                                        }}
                                        size="sm"
                                        variant="secondary"
                                    >
                                        <PlayIcon className="mr-0.5 h-5" /> Run
                                    </Button>
                                </TooltipTrigger>

                                <TooltipContent>
                                    Debug current workflow
                                </TooltipContent>
                            </Tooltip>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        className="mr-4"
                                        disabled={!!project?.publishedDate}
                                        onClick={() =>
                                            publishProjectMutation.mutate({
                                                id: +projectId!,
                                            })
                                        }
                                        size="sm"
                                    >
                                        <CircleDotDashedIcon className="mr-0.5 h-5" />{' '}
                                        Publish
                                    </Button>
                                </TooltipTrigger>

                                <TooltipContent>
                                    {`${
                                        !project?.publishedDate
                                            ? 'Project is not published'
                                            : 'Project is published'
                                    }`}
                                </TooltipContent>
                            </Tooltip>
                        </div>

                        <AlertDialog open={showDeleteProjectDialog}>
                            <AlertDialogContent>
                                <AlertDialogHeader>
                                    <AlertDialogTitle>
                                        Are you absolutely sure?
                                    </AlertDialogTitle>

                                    <AlertDialogDescription>
                                        This action cannot be undone. This will
                                        permanently delete the project and
                                        workflows it contains.
                                    </AlertDialogDescription>
                                </AlertDialogHeader>

                                <AlertDialogFooter>
                                    <AlertDialogCancel
                                        onClick={() =>
                                            setShowDeleteProjectDialog(false)
                                        }
                                    >
                                        Cancel
                                    </AlertDialogCancel>

                                    <AlertDialogAction
                                        className="bg-red-600"
                                        onClick={() => {
                                            if (project?.id) {
                                                deleteProjectMutation.mutate(
                                                    project.id
                                                );
                                            }
                                        }}
                                    >
                                        Delete
                                    </AlertDialogAction>
                                </AlertDialogFooter>
                            </AlertDialogContent>
                        </AlertDialog>

                        <AlertDialog open={showDeleteWorkflowDialog}>
                            <AlertDialogContent>
                                <AlertDialogHeader>
                                    <AlertDialogTitle>
                                        Are you absolutely sure?
                                    </AlertDialogTitle>

                                    <AlertDialogDescription>
                                        This action cannot be undone. This will
                                        permanently delete the workflow.
                                    </AlertDialogDescription>
                                </AlertDialogHeader>

                                <AlertDialogFooter>
                                    <AlertDialogCancel
                                        onClick={() =>
                                            setShowDeleteWorkflowDialog(false)
                                        }
                                    >
                                        Cancel
                                    </AlertDialogCancel>

                                    <AlertDialogAction
                                        className="bg-red-600"
                                        onClick={() => {
                                            if (
                                                project?.id &&
                                                currentWorkflow?.id
                                            ) {
                                                deleteWorkflowMutationMutation.mutate(
                                                    {
                                                        id: project?.id,
                                                        workflowId:
                                                            currentWorkflow?.id,
                                                    }
                                                );
                                            }
                                        }}
                                    >
                                        Delete
                                    </AlertDialogAction>
                                </AlertDialogFooter>
                            </AlertDialogContent>
                        </AlertDialog>

                        {showEditProjectDialog && project && (
                            <ProjectDialog
                                onClose={() => {
                                    setShowEditProjectDialog(false);

                                    queryClient.invalidateQueries({
                                        queryKey: ProjectKeys.project(
                                            project.id!
                                        ),
                                    });
                                }}
                                project={project}
                                showTrigger={false}
                                visible
                            />
                        )}
                    </header>
                }
                leftSidebarBody={
                    <div className="py-1.5">
                        {workflowExecution && (
                            <WorkflowExecutionsDetailsAccordion
                                workflowExecution={workflowExecution}
                            />
                        )}
                    </div>
                }
                leftSidebarOpen={leftSidebarOpen}
                leftSidebarWidth="96"
                rightSidebarBody={
                    <>
                        {componentDefinitions &&
                            !!taskDispatcherDefinitions && (
                                <WorkflowNodesSidebar
                                    data={{
                                        componentDefinitions,
                                        taskDispatcherDefinitions,
                                    }}
                                    filter={filter}
                                />
                            )}
                    </>
                }
                rightSidebarHeader={
                    <div className="px-3 py-4">
                        <Input
                            className="mb-0 px-3 py-4"
                            name="workflowElementsFilter"
                            onChange={(event) => setFilter(event.target.value)}
                            placeholder="Filter workflow nodes"
                            value={filter}
                        />
                    </div>
                }
                rightSidebarOpen={rightSidebarOpen}
                rightSidebarWidth="96"
                rightToolbarBody={
                    <RightSidebar navigation={rightSidebarNavigation} />
                }
                rightToolbarOpen={true}
            >
                {componentDefinitions && !!taskDispatcherDefinitions && (
                    <ProjectWorkflow
                        componentDefinitions={componentDefinitions}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default Project;
