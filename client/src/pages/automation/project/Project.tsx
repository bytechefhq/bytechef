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
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Label} from '@/components/ui/label';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectLabel,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import {RightSidebar} from '@/layouts/RightSidebar';
import {ProjectModel, ProjectStatusModel} from '@/middleware/automation/configuration';
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
import ProjectVersionHistorySheet from '@/pages/automation/project/components/ProjectVersionHistorySheet';
import WorkflowCodeEditorSheet from '@/pages/automation/project/components/WorkflowCodeEditorSheet';
import WorkflowExecutionsTestOutput from '@/pages/automation/project/components/WorkflowExecutionsTestOutput';
import WorkflowInputsSheet from '@/pages/automation/project/components/WorkflowInputsSheet';
import useRightSidebarStore from '@/pages/automation/project/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/automation/project/stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/automation/project/stores/useWorkflowNodeDetailsPanelStore';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
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
import {DotsVerticalIcon, PlusIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {
    CircleDotIcon,
    Code2Icon,
    HistoryIcon,
    PlayIcon,
    PuzzleIcon,
    SettingsIcon,
    SlidersIcon,
    SquareChevronRightIcon,
    SquareIcon,
} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useParams} from 'react-router-dom';

import PageLoader from '../../../components/PageLoader';
import LayoutContainer from '../../../layouts/LayoutContainer';
import ProjectWorkflowEditor from './components/ProjectWorkflowEditor';
import WorkflowNodesSidebar from './components/WorkflowNodesSidebar';

const workflowTestApi = new WorkflowTestApi();

const PublishPopover = ({project}: {project: ProjectModel}) => {
    const [open, setOpen] = useState(false);
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            toast({
                description: 'The project is published.',
            });

            setOpen(false);
        },
    });

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                <Button className="hover:bg-gray-200" disabled={!!project?.publishedDate} size="icon" variant="ghost">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <CircleDotIcon className="h-5" />
                        </TooltipTrigger>

                        <TooltipContent>
                            {project?.publishedDate ? `The project is published` : `Publish the project`}
                        </TooltipContent>
                    </Tooltip>
                </Button>
            </PopoverTrigger>

            <PopoverContent align="end" className="flex h-full w-96 flex-col justify-between space-y-4">
                <h3 className="font-semibold">Publish Project</h3>

                <div className="flex-1">
                    <Label>Description</Label>

                    <Textarea className="h-28" onChange={(event) => setDescription(event.target.value)}></Textarea>
                </div>

                <div className="flex justify-end">
                    <Button
                        disabled={!!project?.publishedDate}
                        onClick={() =>
                            publishProjectMutation.mutate({
                                id: project.id!,
                                publishProjectRequestModel: {
                                    description,
                                },
                            })
                        }
                        size="sm"
                    >
                        Publish
                    </Button>
                </div>
            </PopoverContent>
        </Popover>
    );
};

const Project = () => {
    const [showBottomPanelOpen, setShowBottomPanelOpen] = useState(false);
    const [showDeleteProjectAlertDialog, setShowDeleteProjectAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditProjectDialog, setShowEditProjectDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);
    const [showWorkflowCodeEditorSheet, setShowWorkflowCodeEditorSheet] = useState(false);
    const [showWorkflowInputsSheet, setShowWorkflowInputsSheet] = useState(false);
    const [workflowTestExecution, setWorkflowTestExecution] = useState<WorkflowTestExecutionModel>();
    const [workflowIsRunning, setWorkflowIsRunning] = useState(false);

    const bottomResizablePanelRef = useRef<ImperativePanelHandle>(null);

    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore();

    const {setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setComponentDefinitions, setProjectId, setTaskDispatcherDefinitions, setWorkflow, workflow} =
        useWorkflowDataStore();

    const {componentNames, nodeNames} = workflow;

    const {projectId, workflowId} = useParams();
    const navigate = useNavigate();

    const rightSidebarNavigation: {
        name: string;
        icon: React.ForwardRefExoticComponent<Omit<React.SVGProps<SVGSVGElement>, 'ref'>>;
        onClick?: () => void;
    }[] = [
        {
            icon: HistoryIcon,
            name: 'Project Version History',
            onClick: () => {
                setShowProjectVersionHistorySheet(true);
            },
        },
        {
            icon: PuzzleIcon,
            name: 'Components & Flow Controls',
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

    const {data: project} = useGetProjectQuery(
        parseInt(projectId!),
        useLoaderData() as ProjectModel,
        !showDeleteProjectAlertDialog
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

    const {
        data: projectWorkflows,
        error: projectWorkflowsError,
        isLoading: projectWorkflowsLoading,
    } = useGetProjectWorkflowsQuery(project?.id as number, !showDeleteProjectAlertDialog);

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

            setShowBottomPanelOpen(false);
            setWorkflow({...workflow, componentNames, nodeNames});

            if (bottomResizablePanelRef.current) {
                bottomResizablePanelRef.current.resize(0);
            }

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

    const updateWorkflowMutation = useUpdateWorkflowMutation({
        onSuccess: (workflow) => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.project(+projectId!)});

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
        setShowBottomPanelOpen(true);
        setWorkflowTestExecution(undefined);
        setWorkflowIsRunning(true);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(35);
        }

        if (workflow?.id) {
            workflowTestApi
                .testWorkflow({
                    id: workflow?.id,
                })
                .then((workflowTestExecution) => {
                    setWorkflowTestExecution(workflowTestExecution);
                    setWorkflowIsRunning(false);

                    if (bottomResizablePanelRef.current && bottomResizablePanelRef.current.getSize() === 0) {
                        bottomResizablePanelRef.current.resize(35);
                    }
                })
                .catch(() => {
                    setWorkflowIsRunning(false);
                    setWorkflowTestExecution(undefined);

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
        setShowBottomPanelOpen(false);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(0);
        }

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
                leftSidebarOpen={false}
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
                    <header className="flex items-center border-b py-2 pl-3 pr-2.5">
                        <div className="flex flex-1 items-center space-x-2">
                            <h1>{project?.name}</h1>

                            {project && (
                                <Badge
                                    className="flex space-x-1"
                                    variant={project.status === ProjectStatusModel.Published ? 'success' : 'secondary'}
                                >
                                    <span>V{project.projectVersion}</span>

                                    <span>
                                        {project.status === ProjectStatusModel.Published ? `Published` : 'Draft'}
                                    </span>
                                </Badge>
                            )}
                        </div>

                        <div className="flex items-center space-x-6">
                            <div className="flex space-x-1">
                                {workflow && !!projectWorkflows && (
                                    <Select
                                        defaultValue={workflowId}
                                        name="projectWorkflowSelect"
                                        onValueChange={handleProjectWorkflowValueChange}
                                        value={workflowId}
                                    >
                                        <SelectTrigger className="mr-0.5 w-60 border-0 shadow-none hover:bg-gray-200">
                                            <SelectValue className="font-semibold" placeholder="Select a workflow" />
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
                                        <div>
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <Button className="hover:bg-gray-200" size="icon" variant="ghost">
                                                        <DotsVerticalIcon />
                                                    </Button>
                                                </TooltipTrigger>

                                                <TooltipContent>Workflow Settings</TooltipContent>
                                            </Tooltip>
                                        </div>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
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
                                            <Button className="hover:bg-gray-200" size="icon" variant="ghost">
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

                                {!workflowIsRunning && (
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <Button
                                                className="hover:bg-gray-200"
                                                disabled={runDisabled}
                                                onClick={handleRunClick}
                                                size="icon"
                                                variant="ghost"
                                            >
                                                <PlayIcon className="h-5 text-success" />
                                            </Button>
                                        </TooltipTrigger>

                                        <TooltipContent>
                                            {runDisabled
                                                ? `The workflow cannot be executed. Please set all required workflow input parameters, connections and component properties.`
                                                : `Run the current workflow`}
                                        </TooltipContent>
                                    </Tooltip>
                                )}

                                {workflowIsRunning && (
                                    <Button
                                        className="hover:bg-gray-200"
                                        onClick={() => {
                                            // TODO
                                        }}
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <SquareIcon className="h-5 text-destructive" />
                                    </Button>
                                )}

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            className="hover:bg-gray-200"
                                            onClick={() => {
                                                setShowBottomPanelOpen(!showBottomPanelOpen);

                                                if (bottomResizablePanelRef.current) {
                                                    bottomResizablePanelRef.current.resize(
                                                        !showBottomPanelOpen ? 35 : 0
                                                    );
                                                }
                                            }}
                                            size="icon"
                                            variant="ghost"
                                        >
                                            <SquareChevronRightIcon className="h-5" />
                                        </Button>
                                    </TooltipTrigger>

                                    <TooltipContent>Show the current workflow test execution output</TooltipContent>
                                </Tooltip>
                            </div>

                            <div className="flex space-x-1">
                                {project && <PublishPopover project={project} />}

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <div>
                                            <Tooltip>
                                                <TooltipTrigger asChild>
                                                    <Button className="hover:bg-gray-200" size="icon" variant="ghost">
                                                        <SettingsIcon className="h-5" />
                                                    </Button>
                                                </TooltipTrigger>

                                                <TooltipContent>Project Settings</TooltipContent>
                                            </Tooltip>
                                        </div>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
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
                        </div>
                    </header>
                }
            >
                <PageLoader
                    errors={[componentsError, taskDispatcherDefinitionsError, projectWorkflowsError]}
                    loading={componentsIsLoading || taskDispatcherDefinitionsLoading || projectWorkflowsLoading}
                >
                    {componentDefinitions && !!taskDispatcherDefinitions && workflow?.id && (
                        <ResizablePanelGroup className="flex-1" direction="vertical">
                            <ResizablePanel className="relative" defaultSize={65}>
                                <ProjectWorkflowEditor
                                    componentDefinitions={componentDefinitions}
                                    projectId={+projectId!}
                                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                                    updateWorkflowMutation={updateWorkflowMutation}
                                />
                            </ResizablePanel>

                            <ResizableHandle />

                            <ResizablePanel className="bg-white" defaultSize={0} ref={bottomResizablePanelRef}>
                                <WorkflowExecutionsTestOutput
                                    onCloseClick={() => {
                                        setShowBottomPanelOpen(false);

                                        if (bottomResizablePanelRef.current) {
                                            bottomResizablePanelRef.current.resize(0);
                                        }
                                    }}
                                    workflowIsRunning={workflowIsRunning}
                                    workflowTestExecution={workflowTestExecution}
                                />
                            </ResizablePanel>
                        </ResizablePanelGroup>
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
                    {showProjectVersionHistorySheet && (
                        <ProjectVersionHistorySheet
                            onClose={() => {
                                setShowProjectVersionHistorySheet(false);
                            }}
                            projectId={+projectId!}
                        />
                    )}

                    {showWorkflowCodeEditorSheet && (
                        <WorkflowCodeEditorSheet
                            onClose={() => {
                                setShowWorkflowCodeEditorSheet(false);
                            }}
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
