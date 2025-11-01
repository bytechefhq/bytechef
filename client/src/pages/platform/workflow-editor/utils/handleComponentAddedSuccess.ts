import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
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
    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode, setWorkflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore.getState();

    if (nodeData.clusterElements) {
        return;
    }

    queryClient.invalidateQueries({
        queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
            environmentId: environmentStore.getState().currentEnvironmentId,
            id: workflow.id!,
            lastWorkflowNodeName: currentNode?.name,
        }),
    });

    if (useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen) {
        if (currentNode?.trigger && nodeData.trigger) {
            setCurrentNode({...currentNode, ...nodeData});
            setCurrentComponent({...currentComponent, ...nodeData});
        }
    } else if (!nodeData.clusterElements) {
        setCurrentNode({...nodeData, description: ''});
        setCurrentComponent({...nodeData, description: ''});
        setWorkflowNodeDetailsPanelOpen(true);
    }
}
