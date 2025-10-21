import {environmentStore} from '@/pages/automation/stores/useEnvironmentStore';
import {
    DeleteClusterElementParameter200Response,
    DeleteClusterElementParameterOperationRequest,
    DeleteWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function deleteProperty(
    includeInMetadata = false,
    workflowId: string,
    path: string,
    deleteWorkflowNodeParameterMutation: UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteWorkflowNodeParameterOperationRequest,
        unknown
    >,
    deleteClusterElementParameterMutation?: UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteClusterElementParameterOperationRequest,
        unknown
    >
) {
    const currentComponent = useWorkflowNodeDetailsPanelStore.getState().currentComponent;
    const currentNode = useWorkflowNodeDetailsPanelStore.getState().currentNode;
    const rootClusterElementNodeData = useWorkflowEditorStore.getState().rootClusterElementNodeData;

    if (!currentComponent) {
        console.error('No current component found in the store');

        return;
    }

    if (currentNode?.clusterElementType) {
        deleteClusterElementParameterMutation?.mutate(
            {
                clusterElementType: currentNode?.clusterElementType,
                clusterElementWorkflowNodeName: currentNode?.workflowNodeName,
                deleteClusterElementParameterRequest: {
                    includeInMetadata,
                    path,
                },
                environmentId: environmentStore.getState().currentEnvironmentId,
                id: workflowId,
                workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
            },
            {
                onSuccess: (response) => {
                    const {setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

                    setCurrentComponent({
                        ...currentComponent,
                        metadata: response.metadata,
                        parameters: response.parameters,
                    });

                    setCurrentNode({
                        ...currentNode,
                        metadata: response.metadata,
                        parameters: response.parameters,
                    });
                },
            }
        );

        return;
    }

    deleteWorkflowNodeParameterMutation.mutate(
        {
            deleteWorkflowNodeParameterRequest: {
                includeInMetadata,
                path,
            },
            environmentId: environmentStore.getState().currentEnvironmentId,
            id: workflowId,
            workflowNodeName: currentComponent?.workflowNodeName,
        },
        {
            onSuccess: (response) => {
                const {setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

                setCurrentComponent({
                    ...currentComponent,
                    metadata: response.metadata,
                    parameters: response.parameters,
                });

                if (currentNode) {
                    setCurrentNode({
                        ...currentNode,
                        metadata: response.metadata,
                        parameters: response.parameters,
                    });
                }
            },
        }
    );
}
