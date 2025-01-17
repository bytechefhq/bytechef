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

    if (!currentComponent) {
        console.error('No current component found in the store');

        return;
    }

    deleteWorkflowNodeParameterMutation.mutate(
        {
            deleteWorkflowNodeParameterRequest: {
                path,
                workflowNodeName: currentComponent?.workflowNodeName,
            },
            id: workflowId,
        },
        {
            onSuccess: (response) =>
                useWorkflowNodeDetailsPanelStore.getState().setCurrentComponent({
                    ...currentComponent,
                    parameters: response.parameters,
                }),
        }
    );
}
