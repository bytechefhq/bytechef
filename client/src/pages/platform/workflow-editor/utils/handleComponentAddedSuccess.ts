import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {invalidatePreviousWorkflowNodeOutputsForWorkflow} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

interface HandleComponentAddedSuccessProps {
    nodeData: NodeDataType;
    queryClient: QueryClient;
    workflow: Workflow;
}

/**
 * Opens the node details panel immediately for a newly added node.
 * Called right after the optimistic update so the user sees the panel
 * without waiting for the server response.
 */
export function openNodeDetailsPanelForNewNode(nodeData: NodeDataType): void {
    if (nodeData.clusterElements) {
        return;
    }

    const {
        currentComponent,
        currentNode,
        setActiveTab,
        setCurrentComponent,
        setCurrentNode,
        setPendingSaveNodeName,
        setWorkflowNodeDetailsPanelOpen,
        workflowNodeDetailsPanelOpen,
    } = useWorkflowNodeDetailsPanelStore.getState();

    if (nodeData.name) {
        setPendingSaveNodeName(nodeData.name);
    }

    if (workflowNodeDetailsPanelOpen) {
        if (currentNode?.trigger && nodeData.trigger) {
            setCurrentNode({...currentNode, ...nodeData});
            setCurrentComponent({...currentComponent, ...nodeData});
        }
    } else {
        setActiveTab('description');
        setCurrentNode({...nodeData, description: ''});
        setCurrentComponent({...nodeData, description: ''});
        setWorkflowNodeDetailsPanelOpen(true);
    }
}

export default function handleComponentAddedSuccess({
    nodeData,
    queryClient,
    workflow,
}: HandleComponentAddedSuccessProps) {
    invalidatePreviousWorkflowNodeOutputsForWorkflow(queryClient, workflow.id!);

    const {pendingSaveNodeName, setPendingSaveNodeName} = useWorkflowNodeDetailsPanelStore.getState();

    if (nodeData.name && pendingSaveNodeName === nodeData.name) {
        setPendingSaveNodeName(undefined);
    }
}
