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
import {ProjectModel, ProjectStatusModel, UpdateWorkflowRequest} from '@/middleware/automation/configuration';
import {WorkflowModel} from '@/middleware/platform/configuration';
import {WorkflowTestApi} from '@/middleware/platform/workflow/test';
import {useCreateConnectionMutation} from '@/mutations/automation/connections.mutations';
import {
    useDeleteProjectMutation,
    useDuplicateProjectMutation,
    usePublishProjectMutation,
} from '@/mutations/automation/projects.mutations';
import {
    useCreateProjectWorkflowMutation,
    useDeleteWorkflowMutation,
    useDuplicateWorkflowMutation,
} from '@/mutations/automation/workflows.mutations';
import {
    useDeleteWorkflowNodeParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/mutations/platform/workflowNodeParameters.mutations';
import ProjectVersionHistorySheet from '@/pages/automation/project/components/ProjectVersionHistorySheet';
import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import {ConnectionReactQueryProvider} from '@/pages/platform/connection/providers/connectionReactQueryProvider';
import WorkflowCodeEditorSheet from '@/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/components/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import WorkflowInputsSheet from '@/pages/platform/workflow-editor/components/WorkflowInputsSheet';
import WorkflowNodesSidebar from '@/pages/platform/workflow-editor/components/WorkflowNodesSidebar';
import WorkflowOutputsSheet from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheet';
import useUpdatePlatformWorkflowMutation from '@/pages/platform/workflow-editor/mutations/workflows.mutations';
import {WorkflowMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowNodeParameterMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useWorkflowNodeDetailsPanelStore} from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/queries/automation/connections.queries';
import {ProjectCategoryKeys} from '@/queries/automation/projectCategories.queries';
import {ProjectTagKeys} from '@/queries/automation/projectTags.queries';
import {ProjectWorkflowKeys, useGetProjectWorkflowsQuery} from '@/queries/automation/projectWorkflows.queries';
import {ProjectKeys, useGetProjectQuery} from '@/queries/automation/projects.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/platform/taskDispatcherDefinitions.queries';
import {useGetWorkflowTestConfigurationQuery} from '@/queries/platform/workflowTestConfigurations.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/queries/platform/workflows.queries';
import {DotsVerticalIcon, PlusIcon} from '@radix-ui/react-icons';
import {UseMutationResult, useQueryClient} from '@tanstack/react-query';
import {
    CableIcon,
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
import {RefObject, useEffect, useRef, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useLoaderData, useNavigate, useParams} from 'react-router-dom';

import PageLoader from '../../../components/PageLoader';
import LayoutContainer from '../../../layouts/LayoutContainer';

const workflowTestApi = new WorkflowTestApi();

const DeleteProjectAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => {
    return (
        <AlertDialog open={true}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                    <AlertDialogDescription>
                        This action cannot be undone. This will permanently delete the project and workflows it
                        contains.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <AlertDialogFooter>
                    <AlertDialogCancel onClick={() => onClose()}>Cancel</AlertDialogCancel>

                    <AlertDialogAction className="bg-red-600" onClick={() => onDelete()}>
                        Delete
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};

const DeleteWorkflowAlertDialog = ({onClose, onDelete}: {onClose: () => void; onDelete: () => void}) => (
    <AlertDialog open={true}>
        <AlertDialogContent>
            <AlertDialogHeader>
                <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                <AlertDialogDescription>
                    This action cannot be undone. This will permanently delete the workflow.
                </AlertDialogDescription>
            </AlertDialogHeader>

            <AlertDialogFooter>
                <AlertDialogCancel onClick={() => onClose}>Cancel</AlertDialogCancel>

                <AlertDialogAction className="bg-red-600" onClick={() => onDelete()}>
                    Delete
                </AlertDialogAction>
            </AlertDialogFooter>
        </AlertDialogContent>
    </AlertDialog>
);

const Header = ({
    bottomResizablePanelRef,
    projectId,
    runDisabled,
    updateWorkflowMutation,
}: {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    projectId: number;
    runDisabled: boolean;
    updateWorkflowMutation: UseMutationResult<WorkflowModel, Error, UpdateWorkflowRequest, unknown>;
}) => {
    const [showDeleteProjectAlertDialog, setShowDeleteProjectAlertDialog] = useState(false);
    const [showDeleteWorkflowAlertDialog, setShowDeleteWorkflowAlertDialog] = useState(false);
    const [showEditProjectDialog, setShowEditProjectDialog] = useState(false);

    const {
        setShowBottomPanelOpen,
        setShowEditWorkflowDialog,
        setWorkflowIsRunning,
        setWorkflowTestExecution,
        showEditWorkflowDialog,
        workflowIsRunning,
    } = useWorkflowEditorStore();
    const {setWorkflow, workflow} = useWorkflowDataStore();

    const navigate = useNavigate();

    const {componentNames, nodeNames} = workflow;

    const {data: project} = useGetProjectQuery(
        projectId,
        useLoaderData() as ProjectModel,
        !showDeleteProjectAlertDialog
    );

    const queryClient = useQueryClient();

    const createProjectWorkflowMutation = useCreateProjectWorkflowMutation({
        onSuccess: (workflow) => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(projectId),
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

    const handleDeleteProjectAlertDialogClick = () => {
        if (projectId) {
            deleteProjectMutation.mutate(projectId);
        }
    };

    const handleDeleteWorkflowAlertDialogClick = () => {
        if (projectId && workflow.id) {
            deleteWorkflowMutation.mutate({
                id: projectId,
                workflowId: workflow.id,
            });
        }
    };

    const handleProjectWorkflowValueChange = (id: string) => {
        setWorkflowTestExecution(undefined);

        navigate(`/automation/projects/${projectId}/workflows/${id}`);
    };

    const handleRunClick = () => {
        setShowBottomPanelOpen(true);
        setWorkflowIsRunning(true);
        setWorkflowTestExecution(undefined);

        if (bottomResizablePanelRef.current) {
            bottomResizablePanelRef.current.resize(35);
        }

        if (workflow.id) {
            workflowTestApi
                .testWorkflow({
                    id: workflow.id,
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
                });
        }
    };

    return (
        <header className="flex items-center border-b py-2 pl-3 pr-2.5">
            <div className="flex flex-1 items-center space-x-2">
                <h1>{project?.name}</h1>

                {project && <ProjectVersionBadge project={project} />}
            </div>

            <div className="flex items-center space-x-6">
                <div className="flex space-x-1">
                    <WorkflowSelect
                        onValueChange={handleProjectWorkflowValueChange}
                        projectId={projectId}
                        workflowId={workflow.id!}
                    />

                    <WorkflowDropDownMenu
                        onShowDeleteWorkflowAlertDialog={() => setShowDeleteWorkflowAlertDialog(true)}
                        projectId={projectId}
                        workflowId={workflow.id!}
                    />

                    {!!projectId && (
                        <WorkflowDialog
                            createWorkflowMutation={createProjectWorkflowMutation}
                            parentId={projectId}
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

                    {workflowIsRunning ? (
                        <StopButton />
                    ) : (
                        <RunButton onRunClick={handleRunClick} runDisabled={runDisabled} />
                    )}

                    <OutputButton bottomResizablePanelRef={bottomResizablePanelRef} />
                </div>

                {project && (
                    <div className="flex space-x-1">
                        <PublishPopover project={project} />

                        <ProjectDropDownMenu
                            onDelete={() => setShowDeleteProjectAlertDialog(true)}
                            onEdit={() => setShowEditProjectDialog(true)}
                            project={project}
                        />
                    </div>
                )}
            </div>

            {showDeleteProjectAlertDialog && (
                <DeleteProjectAlertDialog
                    onClose={() => setShowDeleteProjectAlertDialog(false)}
                    onDelete={handleDeleteProjectAlertDialogClick}
                />
            )}

            {showDeleteWorkflowAlertDialog && (
                <DeleteWorkflowAlertDialog
                    onClose={() => setShowDeleteWorkflowAlertDialog(false)}
                    onDelete={handleDeleteWorkflowAlertDialogClick}
                />
            )}

            {showEditProjectDialog && project && (
                <ProjectDialog onClose={() => setShowEditProjectDialog(false)} project={project} />
            )}

            {showEditWorkflowDialog && (
                <WorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflowId={workflow.id}
                />
            )}
        </header>
    );
};

const OutputButton = ({bottomResizablePanelRef}: {bottomResizablePanelRef: RefObject<ImperativePanelHandle>}) => {
    const {setShowBottomPanelOpen, showBottomPanelOpen} = useWorkflowEditorStore();

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-gray-200"
                    onClick={() => {
                        setShowBottomPanelOpen(!showBottomPanelOpen);

                        if (bottomResizablePanelRef.current) {
                            bottomResizablePanelRef.current.resize(!showBottomPanelOpen ? 35 : 0);
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
    );
};

const Project = () => {
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);
    const [showWorkflowCodeEditorSheet, setShowWorkflowCodeEditorSheet] = useState(false);
    const [showWorkflowInputsSheet, setShowWorkflowInputsSheet] = useState(false);
    const [showWorkflowOutputsSheet, setShowWorkflowOutputsSheet] = useState(false);

    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore();
    const {setShowBottomPanelOpen, setShowEditWorkflowDialog} = useWorkflowEditorStore();
    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore();
    const {setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setComponentDefinitions, setTaskDispatcherDefinitions, setWorkflow, workflow} = useWorkflowDataStore();

    const {projectId, workflowId} = useParams();

    const bottomResizablePanelRef = useRef<ImperativePanelHandle>(null);

    const {componentNames, nodeNames} = workflow;

    const rightSidebarNavigation: {
        name?: string;
        icon?: React.ForwardRefExoticComponent<Omit<React.SVGProps<SVGSVGElement>, 'ref'>>;
        onClick?: () => void;
        separator?: boolean;
    }[] = [
        {
            icon: HistoryIcon,
            name: 'Project Version History',
            onClick: () => {
                setShowProjectVersionHistorySheet(true);
            },
        },
        {
            separator: true,
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
            icon: CableIcon,
            name: 'Workflow Outputs',
            onClick: () => setShowWorkflowOutputsSheet(true),
        },
        {
            icon: Code2Icon,
            name: 'Workflow Code Editor',
            onClick: () => setShowWorkflowCodeEditorSheet(true),
        },
    ];

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

    const {data: currentWorkflow} = useGetWorkflowQuery(workflowId!);

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

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(parseInt(projectId!)),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const updateWorkflowMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(parseInt(projectId!)),
            });

            setShowEditWorkflowDialog(false);
        },
        workflowId: workflow.id!,
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(parseInt(projectId!)),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

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
        setWorkflowNodeDetailsPanelOpen(false);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowId]);

    useEffect(() => {
        if (currentWorkflow) {
            setWorkflow({...currentWorkflow, componentNames, nodeNames});
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentWorkflow, workflowId]);

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
                    projectId && (
                        <Header
                            bottomResizablePanelRef={bottomResizablePanelRef}
                            projectId={parseInt(projectId)}
                            runDisabled={runDisabled}
                            updateWorkflowMutation={updateWorkflowMutation}
                        />
                    )
                }
            >
                <PageLoader
                    errors={[componentsError, taskDispatcherDefinitionsError]}
                    loading={componentsIsLoading || taskDispatcherDefinitionsLoading}
                >
                    {componentDefinitions && !!taskDispatcherDefinitions && workflow?.id && (
                        <ResizablePanelGroup className="flex-1" direction="vertical">
                            <ResizablePanel className="relative" defaultSize={65}>
                                <ConnectionReactQueryProvider
                                    value={{
                                        ConnectionKeys: ConnectionKeys,
                                        useCreateConnectionMutation: useCreateConnectionMutation,
                                        useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                        useGetConnectionsQuery: useGetConnectionsQuery,
                                    }}
                                >
                                    <WorkflowMutationProvider
                                        value={{
                                            updateWorkflowMutation,
                                        }}
                                    >
                                        <WorkflowNodeParameterMutationProvider
                                            value={{
                                                deleteWorkflowNodeParameterMutation,
                                                updateWorkflowNodeParameterMutation,
                                            }}
                                        >
                                            <WorkflowEditorLayout
                                                componentDefinitions={componentDefinitions}
                                                taskDispatcherDefinitions={taskDispatcherDefinitions}
                                                updateWorkflowMutation={updateWorkflowMutation}
                                            />
                                        </WorkflowNodeParameterMutationProvider>
                                    </WorkflowMutationProvider>
                                </ConnectionReactQueryProvider>
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
                        <ConnectionReactQueryProvider
                            value={{
                                ConnectionKeys: ConnectionKeys,
                                useCreateConnectionMutation: useCreateConnectionMutation,
                                useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                useGetConnectionsQuery: useGetConnectionsQuery,
                            }}
                        >
                            <WorkflowMutationProvider
                                value={{
                                    updateWorkflowMutation,
                                }}
                            >
                                <WorkflowCodeEditorSheet
                                    onClose={() => {
                                        setShowWorkflowCodeEditorSheet(false);
                                    }}
                                    runDisabled={runDisabled}
                                    testConfigurationDisabled={testConfigurationDisabled}
                                    workflow={workflow}
                                    workflowTestConfiguration={workflowTestConfiguration}
                                />
                            </WorkflowMutationProvider>
                        </ConnectionReactQueryProvider>
                    )}

                    {projectId && showWorkflowInputsSheet && (
                        <WorkflowMutationProvider
                            value={{
                                updateWorkflowMutation,
                            }}
                        >
                            <WorkflowInputsSheet
                                onClose={() => setShowWorkflowInputsSheet(false)}
                                workflow={workflow}
                                workflowTestConfiguration={workflowTestConfiguration}
                            />
                        </WorkflowMutationProvider>
                    )}

                    {projectId && showWorkflowOutputsSheet && (
                        <WorkflowMutationProvider
                            value={{
                                updateWorkflowMutation,
                            }}
                        >
                            <WorkflowOutputsSheet
                                onClose={() => setShowWorkflowOutputsSheet(false)}
                                workflow={workflow}
                            />
                        </WorkflowMutationProvider>
                    )}
                </>
            )}
        </>
    );
};

const ProjectDropDownMenu = ({
    onDelete,
    onEdit,
    project,
}: {
    onDelete: () => void;
    onEdit: () => void;
    project: ProjectModel;
}) => {
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const duplicateProjectMutation = useDuplicateProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});

            navigate(`/automation/projects/${project?.id}/workflows/${project?.workflowIds![0]}`);
        },
    });

    return (
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
                <DropdownMenuItem onClick={() => onEdit()}>Edit</DropdownMenuItem>

                {project && (
                    <DropdownMenuItem onClick={() => duplicateProjectMutation.mutate(project.id!)}>
                        Duplicate
                    </DropdownMenuItem>
                )}

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-red-600" onClick={() => onDelete()}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

const ProjectVersionBadge = ({project}: {project: ProjectModel}) => (
    <Badge
        className="flex space-x-1"
        variant={project.status === ProjectStatusModel.Published ? 'success' : 'secondary'}
    >
        <span>V{project.projectVersion}</span>

        <span>{project.status === ProjectStatusModel.Published ? `Published` : 'Draft'}</span>
    </Badge>
);

const PublishPopover = ({project}: {project: ProjectModel}) => {
    const [open, setOpen] = useState(false);
    const [description, setDescription] = useState<string | undefined>(undefined);

    const {toast} = useToast();

    const queryClient = useQueryClient();

    const publishProjectMutation = usePublishProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.project(project.id!),
            });

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

const RunButton = ({onRunClick, runDisabled}: {onRunClick: () => void; runDisabled: boolean}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-gray-200"
                    disabled={runDisabled}
                    onClick={() => onRunClick()}
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
    );
};

const StopButton = () => (
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
);

const WorkflowDropDownMenu = ({
    onShowDeleteWorkflowAlertDialog,
    projectId,
    workflowId,
}: {
    onShowDeleteWorkflowAlertDialog: () => void;
    projectId: number;
    workflowId: string;
}) => {
    const {setShowEditWorkflowDialog} = useWorkflowEditorStore();

    const queryClient = useQueryClient();

    const duplicateWorkflowMutation = useDuplicateWorkflowMutation({
        onError: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectKeys.projects});
        },
    });

    return (
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

                <DropdownMenuItem
                    onClick={() =>
                        duplicateWorkflowMutation.mutate({
                            id: projectId,
                            workflowId: workflowId,
                        })
                    }
                >
                    Duplicate
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="text-red-600" onClick={() => onShowDeleteWorkflowAlertDialog()}>
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

const WorkflowSelect = ({
    onValueChange,
    projectId,
    workflowId,
}: {
    onValueChange: (id: string) => void;
    projectId: number;
    workflowId: string;
}) => {
    const {data: projectWorkflows} = useGetProjectWorkflowsQuery(projectId);

    return (
        <Select defaultValue={workflowId} name="projectWorkflowSelect" onValueChange={onValueChange} value={workflowId}>
            <SelectTrigger className="mr-0.5 w-60 border-0 shadow-none hover:bg-gray-200">
                <SelectValue className="font-semibold" placeholder="Select a workflow" />
            </SelectTrigger>

            {projectWorkflows && (
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
            )}
        </Select>
    );
};

export default Project;
