import PageLoader from '@/components/PageLoader';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import IntegrationHeader from '@/pages/embedded/integration/components/integration-header/IntegrationHeader';
import IntegrationsSidebar from '@/pages/embedded/integration/components/integrations-sidebar/IntegrationsSidebar';
import IntegrationsSidebarHeader from '@/pages/embedded/integration/components/integrations-sidebar/IntegrationsSidebarHeader';
import useIntegrationsLeftSidebarStore from '@/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import {ConnectionReactQueryProvider} from '@/pages/platform/connection/providers/connectionReactQueryProvider';
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
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {RightSidebar} from '@/shared/layout/RightSidebar';
import {useCreateConnectionMutation} from '@/shared/mutations/embedded/connections.mutations';
import {useUpdateWorkflowMutation} from '@/shared/mutations/embedded/workflows.mutations';
import {
    useDeleteWorkflowNodeParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/shared/queries/embedded/connections.queries';
import {
    IntegrationWorkflowKeys,
    useGetIntegrationWorkflowQuery,
} from '@/shared/queries/embedded/integrationWorkflows.queries';
import {IntegrationKeys} from '@/shared/queries/embedded/integrations.queries';
import {WorkflowKeys} from '@/shared/queries/embedded/workflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useGetWorkflowTestConfigurationQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {CableIcon, Code2Icon, PuzzleIcon, SlidersIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';
import {useParams} from 'react-router-dom';

const Integration = () => {
    const [showWorkflowInputsSheet, setShowWorkflowInputsSheet] = useState(false);
    const [showWorkflowOutputsSheet, setShowWorkflowOutputsSheet] = useState(false);

    const {leftSidebarOpen} = useIntegrationsLeftSidebarStore();
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
    const {setComponentDefinitions, setTaskDispatcherDefinitions, setWorkflow, workflow} = useWorkflowDataStore();

    const {integrationId, integrationWorkflowId} = useParams();

    const bottomResizablePanelRef = useRef<ImperativePanelHandle>(null);

    const ff_1840 = useFeatureFlagsStore()('ff-1840');

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

    const {data: curWorkflow} = useGetIntegrationWorkflowQuery(+integrationId!, +integrationWorkflowId!);

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflowTestConfiguration} = useGetWorkflowTestConfigurationQuery({workflowId: workflow?.id!});

    const queryClient = useQueryClient();

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(parseInt(integrationId!)),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const updateWorkflowMutation = useUpdatePlatformWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationWorkflowKeys.integrationWorkflows(parseInt(integrationId!)),
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
                queryKey: IntegrationKeys.integration(parseInt(integrationId!)),
            });

            queryClient.invalidateQueries({
                queryKey: WorkflowKeys.workflow(workflow.id!),
            });
        },
    });

    const rightSidebarNavigation: {
        name?: string;
        icon?: React.ForwardRefExoticComponent<Omit<React.SVGProps<SVGSVGElement>, 'ref'>>;
        onClick?: () => void;
        separator?: boolean;
    }[] = useMemo(
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
            ].filter((item) => (item.name === 'Workflow Outputs' ? ff_1840 : true)),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [ff_1840, rightSidebarOpen]
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

        useWorkflowDataStore.getState().reset();
        useWorkflowNodeDetailsPanelStore.getState().reset();

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [integrationWorkflowId]);

    useEffect(() => {
        if (curWorkflow) {
            setWorkflow({...curWorkflow});
        }

        // Reset state when component unmounts
        return () => {
            setWorkflow({});
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [curWorkflow]);

    return (
        <>
            <LayoutContainer
                className="bg-muted/50"
                leftSidebarBody={<IntegrationsSidebar integrationId={+integrationId!} />}
                leftSidebarClass="bg-background"
                leftSidebarHeader={<Header right={<IntegrationsSidebarHeader />} title="Integrations" />}
                leftSidebarOpen={leftSidebarOpen}
                leftSidebarWidth="96"
                topHeader={
                    integrationId && (
                        <IntegrationHeader
                            bottomResizablePanelRef={bottomResizablePanelRef}
                            integrationId={parseInt(integrationId)}
                            integrationWorkflowId={parseInt(integrationWorkflowId!)}
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
                                            <div className="flex size-full">
                                                <WorkflowEditorLayout
                                                    componentDefinitions={componentDefinitions}
                                                    leftSidebarOpen={leftSidebarOpen}
                                                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                                                    updateWorkflowMutation={updateWorkflowMutation}
                                                />

                                                {rightSidebarOpen &&
                                                    componentDefinitions &&
                                                    taskDispatcherDefinitions && (
                                                        <aside className="mb-4 flex w-96">
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
                                                        className="mx-1.5 rounded-lg border"
                                                        navigation={rightSidebarNavigation}
                                                    />
                                                </aside>
                                            </div>
                                        </WorkflowNodeParameterMutationProvider>
                                    </WorkflowMutationProvider>
                                </ConnectionReactQueryProvider>
                            </ResizablePanel>

                            <ResizableHandle className="bg-muted" />

                            <ResizablePanel
                                className="border-r border-r-border/50 bg-background"
                                defaultSize={0}
                                ref={bottomResizablePanelRef}
                            >
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

                    {integrationId && showWorkflowInputsSheet && (
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

                    {integrationId && showWorkflowOutputsSheet && (
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

export default Integration;
