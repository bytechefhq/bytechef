import {
    DeleteWorkflowNodeParameter200ResponseModelI,
    DeleteWorkflowNodeParameterRequestI,
} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';

export default function deleteProperty(
    workflowId: string,
    path: string,
    currentComponent: ComponentType,
    setCurrentComponent: (currentComponent: ComponentType) => void,
    deleteWorkflowNodeParameterMutation: UseMutationResult<
        DeleteWorkflowNodeParameter200ResponseModelI,
        Error,
        DeleteWorkflowNodeParameterRequestI,
        unknown
    >,
    name?: string,
    arrayIndex?: number
) {
    // TODO fix in Property.tsx path value, it should be without 'parameters.' prefix and should not contain '${arrayName}_${arrayIndex}' as suffix if array is updated

    path = path.replace('parameters.', '').replace('parameters', '');

    if (arrayIndex !== undefined && path.endsWith(arrayIndex.toString())) {
        path = path.substring(0, path.lastIndexOf('.'));
    }

    const {workflowNodeName} = currentComponent;

    deleteWorkflowNodeParameterMutation.mutate(
        {
            deleteWorkflowNodeParameterRequestModel: {
                arrayIndex,
                name: name === arrayIndex?.toString() ? undefined : name,
                path,
                workflowNodeName,
            },
            id: workflowId,
        },
        {
            onSuccess: (response) =>
                setCurrentComponent({
                    ...currentComponent,
                    parameters: response.parameters,
                }),
        }
    );
}

import {ComponentType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';
