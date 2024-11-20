import {PATH_DIGIT_PREFIX, PATH_SPACE_REPLACEMENT} from '@/shared/constants';
import {
    UpdateWorkflowNodeParameter200Response,
    UpdateWorkflowNodeParameterOperationRequest,
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
        UpdateWorkflowNodeParameter200Response,
        Error,
        UpdateWorkflowNodeParameterOperationRequest,
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

    if (path.includes(PATH_SPACE_REPLACEMENT)) {
        path = path.replace(new RegExp(PATH_SPACE_REPLACEMENT, 'g'), ' ');
    }

    if (path.includes(PATH_DIGIT_PREFIX)) {
        path = path.replace(new RegExp(PATH_DIGIT_PREFIX, 'g'), '');
    }

    updateWorkflowNodeParameterMutation.mutate(
        {
            id: workflowId,
            updateWorkflowNodeParameterRequest: {
                includeInMetadata,
                path,
                type,
                value,
                workflowNodeName,
            },
        },
        {
            onSuccess: (response) => {
                if (successCallback) {
                    successCallback();
                }

                setCurrentComponent({
                    ...currentComponent,
                    displayConditions: response.displayConditions,
                    metadata: response.metadata,
                    parameters: response.parameters,
                });
            },
        }
    );
}
