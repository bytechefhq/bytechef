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
import {ProjectModel} from '@/middleware/automation/configuration';
import {WorkflowTestApi, WorkflowTestExecutionModel} from '@/middleware/platform/workflow/test';
import {useCreateProjectWorkflowMutation} from '@/mutations/automation/projectWorkflows.mutations';
import {
    useDeleteProjectMutation,
    useDuplicateProjectMutation,
    usePublishProjectMutation,
} from '@/mutations/automation/projects.mutations';
import {
    useDeleteWorkflowMutation,
    useDuplicateWorkflowMutation,
    useUpdateWorkflowMutation,
} from '@/mutations/automation/workflows.mutations';
import WorkflowCodeEditorSheet from '@/pages/automation/project/components/WorkflowCodeEditorSheet';
import WorkflowInputsSheet from '@/pages/automation/project/components/WorkflowInputsSheet';
import useRightSidebarStore from '@/pages/automation/project/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import WorkflowExecutionDetailsAccordion from '@/pages/automation/workflow-executions/components/WorkflowExecutionDetailsAccordion';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {ProjectCategoryKeys} from '@/queries/automation/projectCategories.queries';
import {ProjectTagKeys} from '@/queries/automation/projectTags.queries';
import {ProjectKeys, useGetProjectQuery} from '@/queries/automation/projects.queries';
import {WorkflowKeys, useGetProjectWorkflowsQuery, useGetWorkflowQuery} from '@/queries/automation/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/platform/taskDispatcherDefinitions.queries';
import {
    WorkflowTestConfigurationKeys,
    useGetWorkflowTestConfigurationQuery,
} from '@/queries/platform/workflowTestConfigurations.queries';
import {ChevronDownIcon, DotsVerticalIcon, PlusIcon} from '@radix-ui/react-icons';
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

import PageLoader from '../../../components/PageLoader';
import LayoutContainer from '../../../layouts/LayoutContainer';
import ProjectWorkflow from './components/ProjectWorkflow';
import ToggleGroup, {ToggleItemType} from './components/ToggleGroup';
import WorkflowNodesSidebar from './components/WorkflowNodesSidebar';
import useLeftSidebarStore from './stores/useLeftSidebarStore';

const workflowTestApi = new WorkflowTestApi();

const headerToggleItems: Array<ToggleItemType> = [
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
    const [showDeleteProjectAlertDialog, setShowDeleteProjectAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditProjectDialog, setShowEditProjectDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const [showWorkflowCodeEditorSheet, setShowWorkflowCodeEditorSheet] = useState(false);
    const [showWorkflowInputsSheet, setShowWorkflowInputsSheet] = useState(false);
    const [workflowTestExecution, setWorkflowTestExecution] = useState<WorkflowTestExecutionModel>();
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);

    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore();
    const {leftSidebarOpen, setLeftSidebarOpen} = useLeftSidebarStore();
    const {setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setComponentDefinitions, setProjectId, setTaskDispatcherDefinitions, setWorkflow, workflow} =
        useWorkflowDataStore();

    const {componentNames, nodeNames} = workflow;

    const {toast} = useToast();

    const {projectId, workflowId} = useParams();
    const navigate = useNavigate();

    const rightSidebarNavigation: {
        name: string;
        icon: React.ForwardRefExoticComponent<Omit<React.SVGProps<SVGSVGElement>, 'ref'>>;
        onClick?: () => void;
    }[] = [
        {
            icon: PuzzleIcon,
            name: 'Components & Control Flows',
            onClick: () => {
                setWorkflowNodeDetailsPanelOpen(false);

                setRightSidebarOpen(!rightSidebarOpen);
            },
        },
        {
            icon: SlidersIcon,
            name: 'Workflow Inputs',
            onClick: () => setShowWorkflowInputsSheet(true),
        },
        {
            icon: Code2Icon,
            name: 'Workflow Code Editor',
            onClick: () => setShowWorkflowCodeEditorSheet(true),
        },
    ];

    const {data: project} = useGetProjectQuery(parseInt(projectId!), useLoaderData() as ProjectModel);

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

    const {
        data: projectWorkflows,
        error: projectWorkflowsError,
        isLoading: projectWorkflowsLoading,
    } = useGetProjectWorkflowsQuery(project?.id as number);

    const {data: projectWorkflow} = useGetWorkflowQuery(workflowId!);

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflowTestConfiguration} = useGetWorkflowTestConfigurationQuery({workflowId: workflow?.id!});

    const workflowTestConfigurationInputs =
        workflowTestConfiguration && workflowTestConfiguration.inputs ? workflowTestConfiguration.inputs : {};

    const workflowTestConfigurationConnections = (
        workflowTestConfiguration && workflowTestConfiguration.connections ? workflowTestConfiguration.connections : []
    ).reduce(function (map: {[key: string]: number}, workflowTestConfigurationConnection) {
        map[
            workflowTestConfigurationConnection.workflowNodeName +
                '_' +
                workflowTestConfigurationConnection.workflowConnectionKey
        ] = workflowTestConfigurationConnection.connectionId;

        return map;
    }, {});

    const runDisabled =
        (workflow?.inputs ?? []).filter((input) => input.required && !workflowTestConfigurationInputs[input.name])
            .length > 0 ||
        (workflow?.tasks ?? []).length === 0 ||
        (workflow?.tasks ?? [])
            .flatMap((task) => (task.connections ? task.connections : []))
            .filter(
                (workflowConnection) =>
                    workflowConnection.required &&
                    !workflowTestConfigurationConnections[
                        workflowConnection.workflowNodeName + '_' + workflowConnection.key
                    ]
            ).length > 0;

    const testConfigurationDisabled =
        (workflow?.inputs ?? []).length === 0 &&
        (workflow?.tasks ?? []).flatMap((task) => (task.connections ? task.connections : [])).length === 0;

    const queryClient = useQueryClient();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (workflow) => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.projectWorkflows(parseInt(projectId!)),
            });

            setWorkflow({...workflow, componentNames, nodeNames});

            navigate(`/automation/projects/${projectId}/workflows/${workflow.id}`);
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

    const deleteWorkflowMutation = useDeleteWorkflowMutation({
        onSuccess: () => {
            setShowDeleteWorkflowAlertDialog(false);

            navigate('/automation/projects');

            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            navigate(`/automation/projects/${project?.id}/workflows/${project?.workflowIds![0]}`);
        },
    });

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
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
        onSuccess: (workflow) => {
            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.projectWorkflows(+projectId!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id!),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });

            setShowEditWorkflowDialog(false);
        },
    });

    const handleDeleteProjectAlertDialogClick = () => {
        if (project?.id) {
            deleteProjectMutation.mutate(project.id);
        }
    };

    const handleDeleteWorkflowAlertDialogClick = () => {
        if (project?.id && workflow?.id) {
            deleteWorkflowMutation.mutate({
                id: project?.id,
                workflowId: workflow?.id,
            });
        }
    };

    const handleProjectDialogClose = () => {
        setShowEditProjectDialog(false);

        if (project) {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(project.id!),
            });
        }
    };

    const handleRunClick = () => {
        setLeftSidebarOpen(true);
        setWorkflowTestExecution(undefined);
        setWorkflowIsRunning(true);

        if (workflow?.id) {
            workflowTestApi
                .testWorkflow({
                    id: workflow?.id,
                })
                .then((workflowTestExecution) => {
                    setWorkflowTestExecution(workflowTestExecution);
                    setWorkflowIsRunning(false);
                })
                .catch(() => {
                    setWorkflowIsRunning(false);

                    queryClient.invalidateQueries({
                        queryKey: WorkflowKeys.projectWorkflows(parseInt(projectId!)),
                    });
                });
        }
    };

    const handleProjectWorkflowValueChange = (id: string) => {
        setWorkflowTestExecution(undefined);

        navigate(`/automation/projects/${projectId}/workflows/${id}`);
    };

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

    useEffect(() => {
        setLeftSidebarOpen(false);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (projectId) {
            setProjectId(+projectId);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectId]);

    useEffect(() => {
        setWorkflowNodeDetailsPanelOpen(false);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowId]);

    useEffect(() => {
        if (projectWorkflow) {
            setWorkflow({...projectWorkflow, componentNames, nodeNames});
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectWorkflow, workflowId]);

    return (
        <>
            <LayoutContainer
                className="bg-muted dark:bg-background"
                leftSidebarBody={
                    <>
                        {!workflowIsRunning ? (
                            workflowTestExecution?.job ? (
                                <WorkflowExecutionDetailsAccordion job={workflowTestExecution.job} />
                            ) : (
                                <div className="flex size-full items-center justify-center gap-x-1 p-3 text-muted-foreground">
                                    <RefreshCwOffIcon className="size-5" />

                                    <span>Workflow has not yet been executed.</span>
                                </div>
                            )
                        ) : (
                            <div className="flex size-full items-center justify-center gap-x-1 p-3">
                                <span className="flex animate-spin text-gray-400">
                                    <RefreshCwIcon className="size-5" />
                                </span>

                                <span className="text-muted-foreground">Workflow is running...</span>
                            </div>
                        )}
                    </>
                }
                leftSidebarOpen={leftSidebarOpen}
                leftSidebarWidth="112"
                rightSidebarBody={
                    componentDefinitions &&
                    taskDispatcherDefinitions && (
                        <WorkflowNodesSidebar
                            data={{
                                componentDefinitions,
                                taskDispatcherDefinitions,
                            }}
                        />
                    )
                }
                rightSidebarOpen={rightSidebarOpen}
                rightSidebarWidth="96"
                rightToolbarBody={<RightSidebar navigation={rightSidebarNavigation} />}
                rightToolbarOpen={true}
                topHeader={
                    <header className="flex items-center border-b px-3 py-2">
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
                                        <DropdownMenuItem onClick={() => setShowEditProjectDialog(true)}>
                                            Edit
                                        </DropdownMenuItem>

                                        {project && (
                                            <DropdownMenuItem
                                                onClick={() => duplicateProjectMutation.mutate(project.id!)}
                                            >
                                                Duplicate
                                            </DropdownMenuItem>
                                        )}

                                        <DropdownMenuSeparator />

                                        <DropdownMenuItem
                                            className="text-red-600"
                                            onClick={() => setShowDeleteProjectAlertDialog(true)}
                                        >
                                            Delete
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </div>

                            <div className="mr-2 flex rounded-md border border-input bg-white shadow-sm">
                                {workflow && !!projectWorkflows && (
                                    <Select
                                        defaultValue={workflowId}
                                        name="projectWorkflowSelect"
                                        onValueChange={handleProjectWorkflowValueChange}
                                        value={workflowId}
                                    >
                                        <SelectTrigger className="mr-0.5 border-0 bg-white shadow-none">
                                            <SelectValue placeholder="Select a workflow" />
                                        </SelectTrigger>

                                        <SelectContent>
                                            <SelectGroup>
                                                <SelectLabel>Workflows</SelectLabel>

                                                {projectWorkflows.map((workflow) => (
                                                    <SelectItem key={workflow.id!} value={workflow.id!}>
                                                        {workflow.label!}
                                                    </SelectItem>
                                                ))}
                                            </SelectGroup>
                                        </SelectContent>
                                    </Select>
                                )}

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button className="border-0 bg-white shadow-none" size="icon" variant="outline">
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

                                        {project && workflow && (
                                            <DropdownMenuItem
                                                onClick={() =>
                                                    duplicateWorkflowMutation.mutate({
                                                        id: project.id!,
                                                        workflowId: workflow.id!,
                                                    })
                                                }
                                            >
                                                Duplicate
                                            </DropdownMenuItem>
                                        )}

                                        <DropdownMenuSeparator />

                                        <DropdownMenuItem
                                            className="text-red-600"
                                            onClick={() => {
                                                setShowDeleteWorkflowAlertDialog(true);
                                            }}
                                        >
                                            Delete
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>

                                {!!projectId && (
                                    <WorkflowDialog
                                        createWorkflowMutation={createProjectWorkflowMutation}
                                        parentId={parseInt(projectId)}
                                        triggerNode={
                                            <Button
                                                className="border-0 bg-white shadow-none"
                                                size="icon"
                                                variant="outline"
                                            >
                                                <Tooltip>
                                                    <TooltipTrigger asChild>
                                                        <PlusIcon className="mx-2 size-5" />
                                                    </TooltipTrigger>

                                                    <TooltipContent>Create new workflow</TooltipContent>
                                                </Tooltip>
                                            </Button>
                                        }
                                    />
                                )}
                            </div>
                        </div>

                        <div className="flex flex-1 justify-center">
                            <ToggleGroup
                                defaultValue="build"
                                onValueChange={() => setLeftSidebarOpen(!leftSidebarOpen)}
                                toggleItems={headerToggleItems}
                                value={leftSidebarOpen ? 'debug' : 'build'}
                            />
                        </div>

                        <div className="flex items-center space-x-1">
                            {!workflowIsRunning && runDisabled && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span tabIndex={0}>
                                            <Button
                                                className="font-semibold"
                                                disabled={runDisabled}
                                                size="sm"
                                                variant="success"
                                            >
                                                <PlayIcon className="h-5" /> Run
                                            </Button>
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>
                                        The workflow cannot be executed. Please set all required workflow input
                                        parameters, connections and component properties.
                                    </TooltipContent>
                                </Tooltip>
                            )}

                            {!workflowIsRunning && !runDisabled && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span tabIndex={0}>
                                            <Button
                                                className="font-semibold"
                                                disabled={runDisabled}
                                                onClick={handleRunClick}
                                                size="sm"
                                                variant="success"
                                            >
                                                <PlayIcon className="h-5" /> Run
                                            </Button>
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>Run the current workflow</TooltipContent>
                                </Tooltip>
                            )}

                            {workflowIsRunning && (
                                <Button
                                    onClick={() => {
                                        // TODO
                                    }}
                                    size="sm"
                                    variant="destructive"
                                >
                                    <SquareIcon className="h-5" /> Running
                                </Button>
                            )}

                            {!project?.publishedDate && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            onClick={() =>
                                                publishProjectMutation.mutate({
                                                    id: +projectId!,
                                                })
                                            }
                                            size="sm"
                                        >
                                            <CircleDotDashedIcon className="h-5" /> Publish
                                        </Button>
                                    </TooltipTrigger>

                                    <TooltipContent>Publish the project.</TooltipContent>
                                </Tooltip>
                            )}

                            {project?.publishedDate && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span>
                                            <Button disabled={!!project?.publishedDate} size="sm">
                                                <CircleDotDashedIcon className="h-5" /> Publish
                                            </Button>
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>The project is published</TooltipContent>
                                </Tooltip>
                            )}
                        </div>
                    </header>
                }
            >
                <PageLoader
                    errors={[componentsError, taskDispatcherDefinitionsError, projectWorkflowsError]}
                    loading={componentsIsLoading || taskDispatcherDefinitionsLoading || projectWorkflowsLoading}
                >
                    {componentDefinitions && !!taskDispatcherDefinitions && workflow?.id && (
                        <ProjectWorkflow
                            componentDefinitions={componentDefinitions}
                            projectId={+projectId!}
                            taskDispatcherDefinitions={taskDispatcherDefinitions}
                            updateWorkflowMutation={updateWorkflowMutation}
                        />
                    )}
                </PageLoader>
            </LayoutContainer>

            <AlertDialog open={showDeleteProjectAlertDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the project and workflows it
                            contains.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteProjectAlertDialog(false)}>
                            Cancel
                        </AlertDialogCancel>

                        <AlertDialogAction className="bg-red-600" onClick={handleDeleteProjectAlertDialogClick}>
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            <AlertDialog open={showDeleteWorkflowAlertDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the workflow.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteWorkflowAlertDialog(false)}>
                            Cancel
                        </AlertDialogCancel>

                        <AlertDialogAction className="bg-red-600" onClick={handleDeleteWorkflowAlertDialogClick}>
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditProjectDialog && project && <ProjectDialog onClose={handleProjectDialogClose} project={project} />}

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflowId={workflow.id}
                />
            )}

            {workflow && (
                <>
                    {showWorkflowCodeEditorSheet && (
                        <WorkflowCodeEditorSheet
                            onClose={() => {
                                setShowWorkflowCodeEditorSheet(false);
                            }}
                            projectId={+projectId!}
                            runDisabled={runDisabled}
                            testConfigurationDisabled={testConfigurationDisabled}
                            workflow={workflow}
                            workflowTestConfiguration={workflowTestConfiguration}
                        />
                    )}

                    {projectId && showWorkflowInputsSheet && (
                        <WorkflowInputsSheet
                            onClose={() => setShowWorkflowInputsSheet(false)}
                            projectId={+projectId}
                            workflow={workflow}
                            workflowTestConfiguration={workflowTestConfiguration}
                        />
                    )}
                </>
            )}
        </>
    );
};

export default Project;
