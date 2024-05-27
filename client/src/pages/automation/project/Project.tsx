import PageLoader from '@/components/PageLoader';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/ProjectHeader';
import ProjectVersionHistorySheet from '@/pages/automation/project/components/ProjectVersionHistorySheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    ConnectionReactQueryProvider,
    RequestI,
} from '@/pages/platform/connection/providers/connectionReactQueryProvider';
import WorkflowCodeEditorSheet from '@/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/components/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import WorkflowInputsSheet from '@/pages/platform/workflow-editor/components/WorkflowInputsSheet';
import WorkflowNodesSidebar from '@/pages/platform/workflow-editor/components/WorkflowNodesSidebar';
import WorkflowOutputsSheet from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheet';
import {WorkflowMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowNodeParameterMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {RightSidebar} from '@/shared/layout/RightSidebar';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {
    useDeleteWorkflowNodeParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/automation/workflowNodeParameters.mutations';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {ProjectWorkflowKeys, useGetProjectWorkflowQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useGetWorkflowTestConfigurationQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CableIcon, Code2Icon, HistoryIcon, PuzzleIcon, SlidersIcon} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useParams} from 'react-router-dom';

const Project = () => {
    const [showProjectVersionHistorySheet, setShowProjectVersionHistorySheet] = useState(false);
    const [showWorkflowInputsSheet, setShowWorkflowInputsSheet] = useState(false);
    const [showWorkflowOutputsSheet, setShowWorkflowOutputsSheet] = useState(false);

    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore();
    const {
        setShowBottomPanelOpen,
        setShowEditWorkflowDialog,
        setShowWorkflowCodeEditorSheet,
        showWorkflowCodeEditorSheet,
    } = useWorkflowEditorStore();
    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore();
    const {setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {currentWorkspaceId} = useWorkspaceStore();
    const {setComponentDefinitions, setTaskDispatcherDefinitions, setWorkflow, workflow} = useWorkflowDataStore();

    const {projectId, projectWorkflowId} = useParams();

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

    const {data: currentWorkflow} = useGetProjectWorkflowQuery(+projectId!, +projectWorkflowId!);

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
        [...(workflow?.triggers ?? []), ...(workflow?.tasks ?? [])].flatMap((operation) =>
            operation.connections ? operation.connections : []
        ).length === 0;

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
                queryKey: ProjectWorkflowKeys.projectWorkflow(parseInt(projectId!), parseInt(projectWorkflowId!)),
            });

            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflows(parseInt(projectId!)),
            });

            setShowEditWorkflowDialog(false);
        },
        useUpdateWorkflowMutation: useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(parseInt(projectId!), parseInt(projectWorkflowId!)),
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
    }, [projectWorkflowId]);

    useEffect(() => {
        if (currentWorkflow) {
            setWorkflow({...currentWorkflow, componentNames, nodeNames});
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentWorkflow]);

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
                        <ProjectHeader
                            bottomResizablePanelRef={bottomResizablePanelRef}
                            projectId={parseInt(projectId)}
                            projectWorkflowId={parseInt(projectWorkflowId!)}
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
                                        useGetConnectionsQuery: (request: RequestI, enabled?: boolean) => {
                                            return useGetWorkspaceConnectionsQuery(
                                                {
                                                    id: currentWorkspaceId!,
                                                    ...request,
                                                },
                                                enabled
                                            );
                                        },
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
                                useGetConnectionsQuery: (request: RequestI, enabled?: boolean) => {
                                    return useGetWorkspaceConnectionsQuery(
                                        {
                                            id: currentWorkspaceId!,
                                            ...request,
                                        },
                                        enabled
                                    );
                                },
                            }}
                        >
                            <WorkflowMutationProvider
                                value={{
                                    updateWorkflowMutation,
                                }}
                            >
                                <WorkflowCodeEditorSheet
                                    onClose={() => setShowWorkflowCodeEditorSheet(false)}
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

export default Project;
