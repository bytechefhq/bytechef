import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

interface HandleComponentAddedSuccessProps {
    nodeData: NodeDataType;
    queryClient: QueryClient;
    workflow: Workflow;
}

export default function handleComponentAddedSuccess({
    nodeData,
    queryClient,
    workflow,
}: HandleComponentAddedSuccessProps) {
    const {currentComponent, currentNode} = useWorkflowNodeDetailsPanelStore.getState();

    queryClient.invalidateQueries({
        queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
            id: workflow.id!,
            lastWorkflowNodeName: currentNode?.name,
        }),
    });

    if (currentNode?.trigger && nodeData.trigger) {
        useWorkflowNodeDetailsPanelStore.getState().setCurrentNode({...currentNode, ...nodeData});
        useWorkflowNodeDetailsPanelStore.getState().setCurrentComponent({...currentComponent, ...nodeData});
    } else if (!currentNode?.trigger) {
        if (!useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen) {
            useWorkflowNodeDetailsPanelStore.getState().setCurrentNode({
                ...nodeData,
                workflowNodeName: nodeData.workflowNodeName ?? 'trigger_1',
            });

            useWorkflowNodeDetailsPanelStore.getState().setCurrentComponent({
                ...nodeData,
                workflowNodeName: nodeData.workflowNodeName ?? 'trigger_1',
            });

            useWorkflowNodeDetailsPanelStore.getState().setWorkflowNodeDetailsPanelOpen(true);
        } else {
            useWorkflowNodeDetailsPanelStore.getState().setCurrentNode(nodeData);
            useWorkflowNodeDetailsPanelStore.getState().setCurrentComponent(nodeData);
        }
    }
}
