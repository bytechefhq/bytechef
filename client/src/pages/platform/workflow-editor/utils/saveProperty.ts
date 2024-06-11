import {
    UpdateWorkflowNodeParameter200ResponseModel,
    UpdateWorkflowNodeParameterRequest,
} from '@/shared/middleware/platform/configuration';
import {ComponentType} from '@/shared/types';
import {UseMutationResult} from '@tanstack/react-query';

interface SavePropertyProps {
    currentComponent: ComponentType;
    includeInMetadata?: boolean;
    path: string;
    setCurrentComponent: (currentComponent: ComponentType | undefined) => void;
    successCallback?: () => void;
    type: string;
    updateWorkflowNodeParameterMutation: UseMutationResult<
        UpdateWorkflowNodeParameter200ResponseModel,
        Error,
        UpdateWorkflowNodeParameterRequest,
        unknown
    >;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    value?: any;
    workflowId: string;
}

export default function saveProperty({
    currentComponent,
    includeInMetadata = false,
    path,
    setCurrentComponent,
    successCallback,
    type,
    updateWorkflowNodeParameterMutation,
    value,
    workflowId,
}: SavePropertyProps) {
    const {workflowNodeName} = currentComponent;

    updateWorkflowNodeParameterMutation.mutate(
        {
            id: workflowId,
            updateWorkflowNodeParameterRequestModel: {
                includeInMetadata,
                path,
                type,
                value,
                workflowNodeName,
            },
        },
        {
            onSuccess: (response) => {
                successCallback && successCallback();

                setCurrentComponent({
                    ...currentComponent,
                    displayConditions: response.displayConditions,
                    parameters: response.parameters,
                });
            },
        }
    );
}
