import PageLoader from '@/components/PageLoader';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import ProjectsSidebar from '@/pages/automation/project/components/projects-sidebar/ProjectsSidebar';
import ProjectsSidebarHeader from '@/pages/automation/project/components/projects-sidebar/ProjectsSidebarHeader';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
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
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {RightSidebar} from '@/shared/layout/RightSidebar';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {
    useDeleteWorkflowNodeParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
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
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {CableIcon, Code2Icon, PuzzleIcon, SlidersIcon, SparklesIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useParams} from 'react-router-dom';

const Project = () => {
    const [showWorkflowInputsSheet, setShowWorkflowInputsSheet] = useState(false);
    const [showWorkflowOutputsSheet, setShowWorkflowOutputsSheet] = useState(false);

    const {copilotPanelOpen, setContext, setCopilotPanelOpen} = useCopilotStore();
    const {leftSidebarOpen} = useProjectsLeftSidebarStore();
    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore();
    const {
        setShowBottomPanelOpen,
        setShowEditWorkflowDialog,
        setShowWorkflowCodeEditorSheet,
        showWorkflowCodeEditorSheet,
        workflowIsRunning,
        workflowTestExecution,
    } = useWorkflowEditorStore();
    const {setWorkflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {setWorkflowTestChatPanelOpen} = useWorkflowTestChatStore();
    const {currentWorkspaceId} = useWorkspaceStore();
    const {setComponentDefinitions, setTaskDispatcherDefinitions, setWorkflow, workflow} = useWorkflowDataStore();

    const {projectId, projectWorkflowId} = useParams();

    const bottomResizablePanelRef = useRef<ImperativePanelHandle>(null);

    const ff_1570 = useFeatureFlagsStore()('ff-1570');
    const ff_1840 = useFeatureFlagsStore()('ff-1840');

    const queryClient = useQueryClient();

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

    const {data: currentWorkflow} = useGetProjectWorkflowQuery(
        parseInt(projectId!),
        parseInt(projectWorkflowId!),
        !!projectId && !!projectWorkflowId
    );

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflowTestConfiguration} = useGetWorkflowTestConfigurationQuery({workflowId: workflow?.id!});

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
                queryKey: ProjectWorkflowKeys.projectWorkflows(parseInt(projectId!)),
            });

            setShowEditWorkflowDialog(false);
        },
        useUpdateWorkflowMutation: useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(parseInt(projectId!), parseInt(projectWorkflowId!)),
            });
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

    const rightSidebarNavigation = useMemo(
        () =>
            [
                {
                    icon: PuzzleIcon,
                    name: 'Components & Flow Controls',
                    onClick: () => {
                        setWorkflowNodeDetailsPanelOpen(false);
                        setWorkflowTestChatPanelOpen(false);
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
                {
                    icon: SparklesIcon,
                    name: 'Copilot',
                    onClick: () => {
                        if (copilotPanelOpen) {
                            setContext({parameters: {}, source: Source.WORKFLOW_EDITOR, workflowId: workflow.id!});
                        } else {
                            setContext(undefined);
                        }

                        setCopilotPanelOpen(!copilotPanelOpen);
                    },
                },
            ].filter((item) => {
                if (item.name === 'Copilot') {
                    return ff_1570;
                }

                if (item.name === 'Workflow Outputs') {
                    return ff_1840;
                }

                return true;
            }),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [copilotPanelOpen, ff_1840, rightSidebarOpen]
    );

    const workflowTestConfigurationInputs = useMemo(
        () => workflowTestConfiguration?.inputs ?? {},
        [workflowTestConfiguration]
    );

    const workflowTestConfigurationConnections = useMemo(
        () =>
            (workflowTestConfiguration?.connections ?? []).reduce(
                (map: {[key: string]: number}, workflowTestConfigurationConnection) => {
                    const {connectionId, workflowConnectionKey, workflowNodeName} = workflowTestConfigurationConnection;

                    map[`${workflowNodeName}_${workflowConnectionKey}`] = connectionId;

                    return map;
                },
                {}
            ),
        [workflowTestConfiguration]
    );

    const runDisabled = useMemo(() => {
        const requiredInputsMissing = (workflow?.inputs ?? []).some(
            (input) => input.required && !workflowTestConfigurationInputs[input.name]
        );

        const noTasks = (workflow?.tasks ?? []).length === 0;

        const requiredConnectionsMissing = (workflow?.tasks ?? [])
            .flatMap((task) => task.connections ?? [])
            .some(
                (workflowConnection) =>
                    workflowConnection.required &&
                    !workflowTestConfigurationConnections[
                        `${workflowConnection.workflowNodeName}_${workflowConnection.key}`
                    ]
            );

        return requiredInputsMissing || noTasks || requiredConnectionsMissing;
    }, [workflow, workflowTestConfigurationInputs, workflowTestConfigurationConnections]);

    const testConfigurationDisabled = useMemo(() => {
        const noInputs = (workflow?.inputs ?? []).length === 0;

        const noConnections =
            [...(workflow?.triggers ?? []), ...(workflow?.tasks ?? [])].flatMap(
                (operation) => operation.connections ?? []
            ).length === 0;

        return noInputs && noConnections;
    }, [workflow]);

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
        setWorkflowTestChatPanelOpen(false);

        useWorkflowNodeDetailsPanelStore.getState().reset();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectWorkflowId]);

    useEffect(() => {
        if (currentWorkflow) {
            setWorkflow({...currentWorkflow});
        }

        // Reset state when component unmounts
        return () => {
            setWorkflow({});
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentWorkflow]);

    return (
        <>
            <LayoutContainer
                className="bg-muted/50"
                leftSidebarBody={<ProjectsSidebar projectId={+projectId!} />}
                leftSidebarClass="bg-background"
                leftSidebarHeader={<Header right={<ProjectsSidebarHeader />} title="Projects" />}
                leftSidebarOpen={leftSidebarOpen}
                leftSidebarWidth="96"
                topHeader={
                    projectId && (
                        <ProjectHeader
                            bottomResizablePanelRef={bottomResizablePanelRef}
                            chatTrigger={
                                workflow.triggers &&
                                workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1
                            }
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
                                            updateWorkflowMutation: updateWorkflowEditorMutation,
                                        }}
                                    >
                                        <WorkflowNodeParameterMutationProvider
                                            value={{
                                                deleteWorkflowNodeParameterMutation,
                                                updateWorkflowNodeParameterMutation,
                                            }}
                                        >
                                            <div className="flex size-full">
                                                <WorkflowEditorLayout
                                                    componentDefinitions={componentDefinitions}
                                                    leftSidebarOpen={leftSidebarOpen}
                                                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                                                />

                                                {rightSidebarOpen &&
                                                    componentDefinitions &&
                                                    taskDispatcherDefinitions && (
                                                        <aside className="my-4 flex w-96">
                                                            <div className="flex-1">
                                                                <WorkflowNodesSidebar
                                                                    data={{
                                                                        componentDefinitions,
                                                                        taskDispatcherDefinitions,
                                                                    }}
                                                                />
                                                            </div>
                                                        </aside>
                                                    )}

                                                <aside>
                                                    <RightSidebar
                                                        className="mx-3 mt-4 rounded-lg border"
                                                        navigation={rightSidebarNavigation}
                                                    />
                                                </aside>
                                            </div>
                                        </WorkflowNodeParameterMutationProvider>
                                    </WorkflowMutationProvider>
                                </ConnectionReactQueryProvider>
                            </ResizablePanel>

                            <ResizableHandle className="bg-muted" />

                            <ResizablePanel className="bg-background" defaultSize={0} ref={bottomResizablePanelRef}>
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
