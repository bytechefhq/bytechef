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
import {
    WorkflowExecutionModel,
    WorkflowTestApi,
} from '@/middleware/helios/execution';
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
import WorkflowCodeEditorSheet from '@/pages/automation/project/components/WorkflowCodeEditorSheet';
import WorkflowDialog from '@/pages/automation/project/components/WorkflowDialog';
import WorkflowInputsSheet from '@/pages/automation/project/components/WorkflowInputsSheet';
import WorkflowTestConfigurationDialog from '@/pages/automation/project/components/WorkflowTestConfigurationDialog';
import useRightSidebarStore from '@/pages/automation/project/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import ProjectDialog from '@/pages/automation/projects/ProjectDialog';
import WorkflowExecutionDetailsAccordion from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsAccordion';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import {ProjectCategoryKeys} from '@/queries/projectCategories.queries';
import {ProjectTagKeys} from '@/queries/projectTags.quries';
import {
    ProjectKeys,
    useGetProjectQuery,
    useGetProjectWorkflowsQuery,
} from '@/queries/projects.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {
    ChevronDownIcon,
    DotsVerticalIcon,
    PlusIcon,
} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {
    CircleDotDashedIcon,
    Code2Icon,
    PlayIcon,
    PuzzleIcon,
    RefreshCwIcon,
    RefreshCwOffIcon,
    SlidersIcon,
    SquareIcon,
} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useLoaderData, useNavigate, useParams} from 'react-router-dom';

import PageLoader from '../../../components/PageLoader/PageLoader';
import LayoutContainer from '../../../layouts/LayoutContainer';
import ProjectWorkflow from './ProjectWorkflow';
import WorkflowNodesSidebar from './components/WorkflowNodesSidebar';
import useLeftSidebarStore from './stores/useLeftSidebarStore';

const workflowTestApi = new WorkflowTestApi();

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

const Project = () => {
    const [currentWorkflow, setCurrentWorkflow] = useState<
        WorkflowModel | undefined
    >();
    const [filter, setFilter] = useState('');
    const [showDeleteProjectAlertDialog, setShowDeleteProjectAlertDialog] =
        useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] =
        useState(false);
    const [showEditProjectDialog, setShowEditProjectDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const [
        showWorkflowTestConfigurationDialog,
        setShowWorkflowTestConfigurationDialog,
    ] = useState(false);
    const [showWorkflowCodeEditorSheet, setShowWorkflowCodeEditorSheet] =
        useState(false);
    const [showWorkflowInputsSheet, setShowWorkflowInputsSheet] =
        useState(false);
    const [workflowExecution, setWorkflowExecution] =
        useState<WorkflowExecutionModel>();
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);

    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore();
    const {leftSidebarOpen, setLeftSidebarOpen} = useLeftSidebarStore();
    const {setNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();

    const {toast} = useToast();

    const {projectId, workflowId} = useParams();
    const navigate = useNavigate();

    const rightSidebarNavigation: {
        name: string;
        icon: React.ForwardRefExoticComponent<
            Omit<React.SVGProps<SVGSVGElement>, 'ref'>
        >;
        onClick?: () => void;
    }[] = [
        {
            icon: PuzzleIcon,
            name: 'Components & Control Flows',
            onClick: () => {
                setNodeDetailsPanelOpen(false);
                setRightSidebarOpen(!rightSidebarOpen);
            },
        },
        {
            icon: SlidersIcon,
            name: 'Workflow Inputs',
            onClick: () => {
                setShowWorkflowInputsSheet(true);
            },
        },
        {
            icon: Code2Icon,
            name: 'Workflow Code Editor',
            onClick: () => {
                setShowWorkflowCodeEditorSheet(true);
            },
        },
    ];

    const {data: project} = useGetProjectQuery(
        parseInt(projectId!),
        useLoaderData() as ProjectModel
    );

    const {
        data: componentDefinitions,
        error: componentsError,
        isLoading: componentsIsLoading,
    } = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {
        data: taskDispatcherDefinitions,
        error: taskDispatcherDefinitionsError,
        isLoading: taskDispatcherDefinitionsLoading,
    } = useGetTaskDispatcherDefinitionsQuery();

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

    const {
        data: projectWorkflows,
        error: projectWorkflowsError,
        isLoading: projectWorkflowsLoading,
    } = useGetProjectWorkflowsQuery(project?.id as number);

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (workflow) => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projectWorkflows(parseInt(projectId!)),
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
            setShowDeleteWorkflowAlertDialog(false);

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

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projectWorkflows(+projectId!),
            });

            setShowEditWorkflowDialog(false);
        },
    });

    const queryClient = useQueryClient();

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
    }, [projectWorkflows, workflowId]);

    useEffect(() => {
        if (currentWorkflow?.id) {
            navigate(
                `/automation/projects/${projectId}/workflows/${currentWorkflow.id}`
            );
        }
    }, [currentWorkflow, navigate, projectId, setNodeDetailsPanelOpen]);

    useEffect(() => {
        setWorkflowIsRunning(false);
    }, [setWorkflowIsRunning, workflowExecution, setWorkflowExecution]);

    const handleDeleteProjectAlertDialogClick = () => {
        if (project?.id) {
            deleteProjectMutation.mutate(project.id);
        }
    };

    const handleDeleteWorkflowAlertDialogClick = () => {
        if (project?.id && currentWorkflow?.id) {
            deleteWorkflowMutationMutation.mutate({
                id: project?.id,
                workflowId: currentWorkflow?.id,
            });
        }
    };

    const handleProjectDialogCloseClick = () => {
        setShowEditProjectDialog(false);

        if (project) {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(project.id!),
            });
        }
    };

    const handleRunWorkflowClick = () => {
        if (currentWorkflow?.inputs && currentWorkflow?.inputs?.length > 0) {
            setShowWorkflowTestConfigurationDialog(true);
        } else {
            setWorkflowIsRunning(true);
            setLeftSidebarOpen(true);

            workflowTestApi
                .testWorkflow({
                    testParametersModel: {
                        workflowId: currentWorkflow?.id,
                    },
                })
                .then(setWorkflowExecution)
                .catch(() => setWorkflowIsRunning(false));
        }
    };

    const handleWorkflowTestConfigurationDialogRunWorkflowClick = (inputs?: {
        [key: string]: object;
    }) => {
        setWorkflowIsRunning(true);
        setLeftSidebarOpen(true);
        setShowWorkflowTestConfigurationDialog(false);

        workflowTestApi
            .testWorkflow({
                testParametersModel: {
                    inputs,
                    workflowId: currentWorkflow?.id,
                },
            })
            .then(setWorkflowExecution)
            .catch(() => setWorkflowIsRunning(false));
    };

    const handleProjectWorkflowValueChange = (id: string) => {
        setCurrentWorkflow(
            projectWorkflows!.find(
                (workflow: WorkflowModel) => workflow.id === id
            )!
        );

        navigate(`/automation/projects/${projectId}/workflows/${id}`);
    };

    const handleWorkflowCodeEditorSheetSave = (definition: string) => {
        setShowWorkflowCodeEditorSheet(false);

        if (currentWorkflow && currentWorkflow.id) {
            updateWorkflowMutation.mutate({
                id: currentWorkflow.id,
                workflowRequestModel: {
                    definition,
                },
            });
        }
    };

    return (
        <PageLoader
            errors={[
                componentsError,
                taskDispatcherDefinitionsError,
                projectWorkflowsError,
            ]}
            loading={
                componentsIsLoading ||
                taskDispatcherDefinitionsLoading ||
                projectWorkflowsLoading
            }
        >
            <LayoutContainer
                className="bg-muted dark:bg-background"
                header={
                    <header className="my-4 ml-4 flex items-center">
                        <div className="flex">
                            <div className="mr-2 flex items-center">
                                <h1>{project?.name}</h1>

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button size="icon" variant="ghost">
                                            <ChevronDownIcon />
                                        </Button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent>
                                        <DropdownMenuItem
                                            onClick={() =>
                                                setShowEditProjectDialog(true)
                                            }
                                        >
                                            Edit
                                        </DropdownMenuItem>

                                        {project && (
                                            <DropdownMenuItem
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
                                            className="text-red-600"
                                            onClick={() =>
                                                setShowDeleteProjectAlertDialog(
                                                    true
                                                )
                                            }
                                        >
                                            Delete
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </div>

                            <div className="mr-2 flex rounded-md border border-input bg-white shadow-sm">
                                {currentWorkflow && !!projectWorkflows && (
                                    <Select
                                        defaultValue={workflowId}
                                        name="projectWorkflowSelect"
                                        onValueChange={
                                            handleProjectWorkflowValueChange
                                        }
                                        value={currentWorkflow.id || workflowId}
                                    >
                                        <SelectTrigger className="mr-0.5 border-0 bg-white shadow-none">
                                            <SelectValue placeholder="Select a workflow" />
                                        </SelectTrigger>

                                        <SelectContent>
                                            <SelectGroup>
                                                <SelectLabel>
                                                    Workflows
                                                </SelectLabel>

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

                                    <DropdownMenuContent>
                                        <DropdownMenuItem
                                            onClick={() => {
                                                setShowEditWorkflowDialog(true);
                                            }}
                                        >
                                            Edit
                                        </DropdownMenuItem>

                                        {project && currentWorkflow && (
                                            <DropdownMenuItem
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
                                            className="text-red-600"
                                            onClick={() => {
                                                setShowDeleteWorkflowAlertDialog(
                                                    true
                                                );
                                            }}
                                        >
                                            Delete
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>

                                {!!projectId && (
                                    <WorkflowDialog
                                        createWorkflowRequestMutation={
                                            createProjectWorkflowMutation
                                        }
                                        parentId={+projectId}
                                        triggerNode={
                                            <Button
                                                className="border-0 bg-white shadow-none"
                                                size="icon"
                                                variant="outline"
                                            >
                                                <PlusIcon className="mx-2 h-5 w-5" />
                                            </Button>
                                        }
                                    />
                                )}
                            </div>
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

                        <div>
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <>
                                        {!workflowIsRunning && (
                                            <Button
                                                className="mr-1 bg-success text-success-foreground hover:bg-success/80"
                                                onClick={handleRunWorkflowClick}
                                                size="sm"
                                                variant="secondary"
                                            >
                                                <PlayIcon className="mr-0.5 h-5" />{' '}
                                                Run
                                            </Button>
                                        )}

                                        {workflowIsRunning && (
                                            <Button
                                                className="mr-1"
                                                onClick={() => {
                                                    // TODO
                                                }}
                                                size="sm"
                                                variant="destructive"
                                            >
                                                <SquareIcon className="mr-0.5 h-5" />{' '}
                                                Running
                                            </Button>
                                        )}
                                    </>
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
                    </header>
                }
                leftSidebarBody={
                    <div className="py-1.5">
                        {!workflowIsRunning ? (
                            workflowExecution ? (
                                <WorkflowExecutionDetailsAccordion
                                    workflowExecution={workflowExecution}
                                />
                            ) : (
                                <div className="absolute inset-x-0 bottom-0 top-2/4">
                                    <div className="flex w-full flex-col items-center text-gray-500">
                                        <div className="text-gray-400">
                                            <RefreshCwOffIcon className="h-16 w-16" />
                                        </div>

                                        <div>
                                            Workflow has not yet been executed.
                                        </div>
                                    </div>
                                </div>
                            )
                        ) : (
                            <div className="absolute inset-x-0 bottom-0 top-2/4">
                                <div className="flex w-full flex-col items-center">
                                    <div className="flex animate-spin space-x-2 text-gray-400">
                                        <RefreshCwIcon className="h-16 w-16" />
                                    </div>

                                    <div className="text-gray-500">
                                        Workflow is running...
                                    </div>
                                </div>
                            </div>
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
                                        componentDefinitions:
                                            componentDefinitions,
                                        taskDispatcherDefinitions:
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

            <AlertDialog open={showDeleteProjectAlertDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>
                            Are you absolutely sure?
                        </AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently
                            delete the project and workflows it contains.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel
                            onClick={() =>
                                setShowDeleteProjectAlertDialog(false)
                            }
                        >
                            Cancel
                        </AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-red-600"
                            onClick={handleDeleteProjectAlertDialogClick}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            <AlertDialog open={showDeleteWorkflowAlertDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>
                            Are you absolutely sure?
                        </AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently
                            delete the workflow.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel
                            onClick={() =>
                                setShowDeleteWorkflowAlertDialog(false)
                            }
                        >
                            Cancel
                        </AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-red-600"
                            onClick={handleDeleteWorkflowAlertDialogClick}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditProjectDialog && project && (
                <ProjectDialog
                    onClose={handleProjectDialogCloseClick}
                    project={project}
                />
            )}

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    updateWorkflowMutationMutation={updateWorkflowMutation}
                    workflow={currentWorkflow}
                />
            )}

            {currentWorkflow && (
                <>
                    {showWorkflowTestConfigurationDialog && (
                        <WorkflowTestConfigurationDialog
                            onClose={() =>
                                setShowWorkflowTestConfigurationDialog(false)
                            }
                            onWorkflowRun={
                                handleWorkflowTestConfigurationDialogRunWorkflowClick
                            }
                            workflow={currentWorkflow}
                        />
                    )}

                    {showWorkflowCodeEditorSheet && (
                        <WorkflowCodeEditorSheet
                            onClose={() => {
                                setShowWorkflowCodeEditorSheet(false);
                            }}
                            onSave={handleWorkflowCodeEditorSheetSave}
                            onWorkflowRunClick={handleRunWorkflowClick}
                            workflow={currentWorkflow}
                            workflowIsRunning={workflowIsRunning}
                        />
                    )}

                    {projectId && showWorkflowInputsSheet && (
                        <WorkflowInputsSheet
                            onClose={() => setShowWorkflowInputsSheet(false)}
                            projectId={+projectId}
                            workflow={currentWorkflow}
                        />
                    )}
                </>
            )}
        </PageLoader>
    );
};

export default Project;
