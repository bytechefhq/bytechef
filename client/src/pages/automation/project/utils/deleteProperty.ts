import {
    DeleteWorkflowNodeParameter200ResponseModel,
    DeleteWorkflowNodeParameterRequest,
} from '@/middleware/platform/configuration';
import {ComponentType} from '@/types/types';

export default function deleteProperty(
    workflowId: string,
    path: string,
    name: string,
    currentComponentData: ComponentType,
    otherComponentData: Array<ComponentType>,
    setComponentData: (componentData: Array<ComponentType>) => void,
    setDirty: (dirty: boolean) => void,
    deleteWorkflowNodeParameterMutation: UseMutationResult<
        DeleteWorkflowNodeParameter200ResponseModel,
        Error,
        DeleteWorkflowNodeParameterRequest,
        unknown
    >,
    arrayIndex?: number
) {
    // TODO fix in Property.tsx path value, it should be without 'parameters.' prefix and should not contain '${arrayName}_${arrayIndex}' as suffix if array is updated

    path = path.replace('parameters.', '').replace('parameters', '');

    if (arrayIndex !== undefined && path.endsWith('_' + arrayIndex)) {
        path = path.substring(0, path.lastIndexOf('.'));
    }

    const {workflowNodeName} = currentComponentData;

    deleteWorkflowNodeParameterMutation.mutate(
        {
            deleteWorkflowNodeParameterRequestModel: {
                arrayIndex,
                name: name.endsWith('_' + arrayIndex) ? undefined : name,
                path,
                workflowNodeName,
            },
            id: workflowId,
        },
        {
            onSuccess: (response) => {
                const parameters = response.parameters;

                setComponentData([
                    ...otherComponentData,
                    {
                        ...currentComponentData,
                        parameters,
                    },
                ]);

                setDirty(true);
            },
        }
    );
}

import {UseMutationResult} from '@tanstack/react-query';
