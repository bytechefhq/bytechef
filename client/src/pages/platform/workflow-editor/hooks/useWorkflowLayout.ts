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
import {useParams} from 'react-router-dom';
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
        rootClusterElementNodeData,
        setShowWorkflowCodeEditorSheet,
        setShowWorkflowInputsSheet,
        setShowWorkflowOutputsSheet,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
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

    const {codeWorkflow, codeWorkflowLanguage, useGetComponentDefinitionsQuery} = useWorkflowEditor();
    const {integrationId, projectId} = useParams();

    const componentDefinitionsQueryParameters: object = {
        actionDefinitions: true,
        clusterElementDefinitions: true,
        include: includeComponents,
        triggerDefinitions: true,
    };

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

    // `workflowNodeName` as well as `name`: every node in the store is addressed by the former, and
    // the graph container the transition popover points this slot at carries no `name` of its own —
    // gating on `name` alone disabled the query there, which emptied the data pill panel completely.
    const currentNodeName = currentNode?.workflowNodeName ?? currentNode?.name;

    // The graph transition editor counts as a consumer even though neither panel is open. It lives on
    // the canvas and deliberately does not slide the Data Pill Panel over the graph it is pinned to —
    // but its condition field resolves data pills like any other expression field, and gating the
    // fetch on a visible panel left it offering "No data pills found" with nothing wrong upstream.
    const graphTransitionEditorOpen = !!currentNode?.dataPillAnchorNodeName;

    const shouldFetchPreviousWorkflowNodeOutputs =
        (dataPillPanelOpen || workflowNodeDetailsPanelOpen || graphTransitionEditorOpen) &&
        !!workflowNodes?.length &&
        !!currentNode &&
        !!currentNodeName;

    const {data: workflowNodeOutputs, isPending: isWorkflowNodeOutputsPending} = useGetPreviousWorkflowNodeOutputsQuery(
        {
            environmentId: currentEnvironmentId,
            id: workflow.id!,
            lastWorkflowNodeName: currentNode?.clusterElementType
                ? rootClusterElementNodeData?.workflowNodeName
                : (currentNode?.dataPillAnchorNodeName ?? currentNodeName),
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

        const isCodeWorkflow = codeWorkflow === true && !!codeWorkflowLanguage;

        const source = integrationId
            ? isCodeWorkflow
                ? Source.CODE_WORKFLOW_EMBEDDED
                : Source.WORKFLOW_EDITOR_EMBEDDED
            : isCodeWorkflow
              ? Source.CODE_WORKFLOW
              : Source.WORKFLOW_EDITOR;

        setContext({
            ...currentContext,
            mode: MODE.ASK,
            parameters: {
                ...(integrationId ? {integrationId} : {}),
                ...(isCodeWorkflow ? {language: codeWorkflowLanguage} : {}),
                ...(isCodeWorkflow && !integrationId && projectId ? {projectId} : {}),
            },
            source,
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
