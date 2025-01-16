import {
    UpdateWorkflowNodeParameter200Response,
    UpdateWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {ComponentType} from '@/shared/types';
import {UseMutationResult} from '@tanstack/react-query';

import {decodePath} from './encodingUtils';

interface SavePropertyProps {
    currentComponent: ComponentType;
    includeInMetadata?: boolean;
    path: string;
    setCurrentComponent: (currentComponent: ComponentType | undefined) => void;
    successCallback?: () => void;
    type: string;
    updateWorkflowNodeParameterMutation: UseMutationResult<
        UpdateWorkflowNodeParameter200Response & {workflowNodeName: string},
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

    const decodedPath = decodePath(path);

    updateWorkflowNodeParameterMutation.mutate(
        {
            id: workflowId,
            updateWorkflowNodeParameterRequest: {
                includeInMetadata,
                path: decodedPath,
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

                if (response.workflowNodeName === currentComponent.name) {
                    setCurrentComponent({
                        ...currentComponent,
                        displayConditions: response.displayConditions,
                        metadata: response.metadata,
                        parameters: response.parameters,
                    });
                }
            },
        }
    );
}
