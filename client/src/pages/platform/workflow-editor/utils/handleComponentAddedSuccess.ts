import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {ComponentType, NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

interface HandleComponentAddedSuccessProps {
    nodeData: Omit<NodeDataType, 'componentName' | 'workflowNodeName'> & {
        componentName?: string;
        workflowNodeName?: string;
    };
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
            setCurrentNode({...currentNode, ...nodeData} as NodeDataType);
            setCurrentComponent({...currentComponent, ...nodeData} as ComponentType);
        }
    } else if (!nodeData.clusterElements) {
        setCurrentNode({...nodeData, description: ''} as NodeDataType);
        setCurrentComponent({...nodeData, description: ''} as ComponentType);
        setWorkflowNodeDetailsPanelOpen(true);
    }
}
