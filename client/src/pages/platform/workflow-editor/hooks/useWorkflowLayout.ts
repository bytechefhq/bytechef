import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import filterWorkflowNodeOutputs from '@/pages/platform/workflow-editor/utils/filterWorkflowNodeOutputs';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {useGetPreviousWorkflowNodeOutputsQuery} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useGetWorkflowTestConfigurationQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

export const useWorkflowLayout = (includeComponents?: string[]) => {
    const {copilotPanelOpen, setCopilotPanelOpen} = useCopilotPanelStore(
        useShallow((state) => ({
            copilotPanelOpen: state.copilotPanelOpen,
            setCopilotPanelOpen: state.setCopilotPanelOpen,
        }))
    );
    const setContext = useCopilotStore((state) => state.setContext);
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {rightSidebarOpen, setRightSidebarOpen} = useRightSidebarStore(
        useShallow((state) => ({
            rightSidebarOpen: state.rightSidebarOpen,
            setRightSidebarOpen: state.setRightSidebarOpen,
        }))
    );
    const {workflow, workflowNodes} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
            workflowNodes: state.workflowNodes,
        }))
    );
    const {
        clusterElementsCanvasOpen,
        rootClusterElementNodeData,
        setShowWorkflowCodeEditorSheet,
        setShowWorkflowInputsSheet,
        setShowWorkflowOutputsSheet,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            clusterElementsCanvasOpen: state.clusterElementsCanvasOpen,
            rootClusterElementNodeData: state.rootClusterElementNodeData,
            setShowWorkflowCodeEditorSheet: state.setShowWorkflowCodeEditorSheet,
            setShowWorkflowInputsSheet: state.setShowWorkflowInputsSheet,
            setShowWorkflowOutputsSheet: state.setShowWorkflowOutputsSheet,
        }))
    );
    const {currentNode, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                currentNode: state.currentNode,
                setWorkflowNodeDetailsPanelOpen: state.setWorkflowNodeDetailsPanelOpen,
                workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
            }))
        );
    const setWorkflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.setWorkflowTestChatPanelOpen);

    const {data: workflowTestConfiguration} = useGetWorkflowTestConfigurationQuery({
        environmentId: currentEnvironmentId,
        workflowId: workflow.id!,
    });

    const testConfigurationDisabled = useMemo(() => {
        const noInputs = (workflow?.inputs ?? []).length === 0;

        const noConnections =
            [...(workflow?.triggers ?? []), ...(workflow?.tasks ?? [])].flatMap(
                (operation) => operation.connections ?? []
            ).length === 0;

        return noInputs && noConnections;
    }, [workflow]);

    const {useGetComponentDefinitionsQuery} = useWorkflowEditor();

    let componentDefinitionsQueryParameters: object = {
        actionDefinitions: true,
        include: includeComponents,
        triggerDefinitions: true,
    };

    if (clusterElementsCanvasOpen) {
        componentDefinitionsQueryParameters = {};
    }

    const {
        data: componentDefinitions,
        error: componentsError,
        isLoading: componentsIsLoading,
    } = useGetComponentDefinitionsQuery!(componentDefinitionsQueryParameters);

    const {
        data: taskDispatcherDefinitions,
        error: taskDispatcherDefinitionsError,
        isLoading: taskDispatcherDefinitionsLoading,
    } = useGetTaskDispatcherDefinitionsQuery();

    const shouldFetchPreviousWorkflowNodeOutputs =
        (dataPillPanelOpen || workflowNodeDetailsPanelOpen) &&
        !!workflowNodes?.length &&
        !!currentNode &&
        !!currentNode?.name;

    const {data: workflowNodeOutputs, isPending: isWorkflowNodeOutputsPending} = useGetPreviousWorkflowNodeOutputsQuery(
        {
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            lastWorkflowNodeName: currentNode?.clusterElementType
                ? rootClusterElementNodeData?.workflowNodeName
                : currentNode?.name,
        },
        shouldFetchPreviousWorkflowNodeOutputs
    );

    const workflowNodeOutputsForFilter = shouldFetchPreviousWorkflowNodeOutputs ? workflowNodeOutputs : undefined;

    let filteredWorkflowNodeOutputs;
    let previousComponentDefinitions: ComponentDefinitionBasic[] = [];

    if (workflowNodeOutputsForFilter && componentDefinitions) {
        const filteredNodeOutputs = filterWorkflowNodeOutputs(
            workflowNodeOutputsForFilter,
            componentDefinitions,
            taskDispatcherDefinitions ?? []
        );

        filteredWorkflowNodeOutputs = filteredNodeOutputs.outputs;
        previousComponentDefinitions = filteredNodeOutputs.definitions;
    }

    const handleComponentsAndFlowControlsClick = () => {
        setWorkflowNodeDetailsPanelOpen(false);
        setWorkflowTestChatPanelOpen(false);
        setRightSidebarOpen(!rightSidebarOpen);
    };

    const handleCopilotClick = () => {
        const {context: currentContext} = useCopilotStore.getState();

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });

        if (!copilotPanelOpen) {
            setCopilotPanelOpen(!copilotPanelOpen);
        }
    };

    const handleWorkflowCodeEditorClick = () => setShowWorkflowCodeEditorSheet(true);

    const handleWorkflowInputsClick = () => setShowWorkflowInputsSheet(true);

    const handleWorkflowOutputsClick = () => setShowWorkflowOutputsSheet(true);

    useEffect(() => {
        if (componentDefinitions) {
            useWorkflowDataStore.getState().setComponentDefinitions(componentDefinitions);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [componentDefinitions?.length]);

    useEffect(() => {
        if (taskDispatcherDefinitions) {
            useWorkflowDataStore.getState().setTaskDispatcherDefinitions(taskDispatcherDefinitions);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [taskDispatcherDefinitions?.length]);

    return {
        componentDefinitions,
        componentsError,
        componentsIsLoading,
        filteredWorkflowNodeOutputs,
        handleComponentsAndFlowControlsClick,
        handleCopilotClick,
        handleWorkflowCodeEditorClick,
        handleWorkflowInputsClick,
        handleWorkflowOutputsClick,
        isWorkflowNodeOutputsPending,
        previousComponentDefinitions,
        taskDispatcherDefinitions,
        taskDispatcherDefinitionsError,
        taskDispatcherDefinitionsLoading,
        testConfigurationDisabled,
        workflowTestConfiguration,
    };
};
