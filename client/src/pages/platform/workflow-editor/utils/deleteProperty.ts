import {
    DeleteClusterElementParameterRequest,
    DeleteWorkflowNodeParameter200Response,
    DeleteWorkflowNodeParameterRequest,
} from '@/shared/middleware/platform/configuration';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function deleteProperty(
    workflowId: string,
    path: string,
    deleteWorkflowNodeParameterMutation: UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteWorkflowNodeParameterOperationRequest,
        unknown
    >,
    deleteClusterElementParameterMutation?: UseMutationResult<
        DeleteWorkflowNodeParameter200Response,
        Error,
        DeleteClusterElementParameterRequest,
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
                deleteWorkflowNodeParameterRequest: {
                    path,
                },
                id: workflowId,
                workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
            },
            {
                onSuccess: (response) => {
                    const {setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

                    setCurrentComponent({
                        ...currentComponent,
                        parameters: response.parameters,
                    });

                    if (currentNode) {
                        setCurrentNode({
                            ...currentNode,
                            parameters: response.parameters,
                        });
                    }
                },
            }
        );

        return;
    }

    deleteWorkflowNodeParameterMutation.mutate(
        {
            deleteClusterElementParameterRequest: {
                path,
            },
            id: workflowId,
            workflowNodeName: currentComponent?.workflowNodeName,
        },
        {
            onSuccess: (response) => {
                const {setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

                setCurrentComponent({
                    ...currentComponent,
                    parameters: response.parameters,
                });

                if (currentNode) {
                    setCurrentNode({
                        ...currentNode,
                        parameters: response.parameters,
                    });
                }
            },
        }
    );
}
