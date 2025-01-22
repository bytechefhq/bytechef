import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

interface HandleComponentAddedSuccessProps {
    currentNode?: NodeDataType;
    nodeData: NodeDataType;
    queryClient: QueryClient;
    workflow: Workflow;
}

export default function handleComponentAddedSuccess({
    currentNode,
    nodeData,
    queryClient,
    workflow,
}: HandleComponentAddedSuccessProps) {
    queryClient.invalidateQueries({
        queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
            id: workflow.id!,
            lastWorkflowNodeName: currentNode?.name,
        }),
    });

    if (currentNode?.trigger && nodeData.trigger) {
        useWorkflowNodeDetailsPanelStore.getState().setCurrentNode({...currentNode, ...nodeData});
    }

    if (!currentNode?.trigger && !useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen) {
        useWorkflowNodeDetailsPanelStore.getState().setCurrentNode(nodeData);
    }

    if (!useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen) {
        useWorkflowNodeDetailsPanelStore.getState().setCurrentComponent({
            ...nodeData,
            workflowNodeName: nodeData.workflowNodeName ?? 'trigger_1',
        });

        useWorkflowNodeDetailsPanelStore.getState().setWorkflowNodeDetailsPanelOpen(true);
    }
}
