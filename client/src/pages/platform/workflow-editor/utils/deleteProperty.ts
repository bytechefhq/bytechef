import {
    DeleteWorkflowNodeParameter200Response,
    DeleteWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';

export default function deleteProperty(
    workflowId: string,
    path: string,
    deleteWorkflowNodeParameterMutation: UseMutationResult<
        DeleteWorkflowNodeParameter200Response,
        Error,
        DeleteWorkflowNodeParameterOperationRequest,
        unknown
    >
) {
    const currentComponent = useWorkflowNodeDetailsPanelStore.getState().currentComponent;
    const currentNode = useWorkflowNodeDetailsPanelStore.getState().currentNode;

    if (!currentComponent) {
        console.error('No current component found in the store');

        return;
    }

    deleteWorkflowNodeParameterMutation.mutate(
        {
            deleteWorkflowNodeParameterRequest: {
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
