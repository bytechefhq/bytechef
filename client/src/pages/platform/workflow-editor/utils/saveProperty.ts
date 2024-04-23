import {UpdateWorkflowNodeParameterRequest} from '@/middleware/platform/configuration';
import {UpdateWorkflowNodeParameter200ResponseModel} from '@/middleware/platform/configuration/models/UpdateWorkflowNodeParameter200ResponseModel';
import {ComponentType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';

interface SavePropertyProps {
    arrayIndex?: number;
    currentComponentData: ComponentType;
    name: string;
    otherComponentData: Array<ComponentType>;
    path: string;
    setComponentData: (componentData: Array<ComponentType>) => void;
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
    arrayIndex,
    currentComponentData,
    name,
    otherComponentData,
    path,
    setComponentData,
    updateWorkflowNodeParameterMutation,
    value,
    workflowId,
}: SavePropertyProps) {
    // TODO fix in Property.tsx path value, it should be without 'parameters.' prefix and should not contain '${arrayName}_${arrayIndex}' as suffix if array is updated

    path = path.replace('parameters.', '').replace('parameters', '');

    if (arrayIndex !== undefined && path.endsWith('_' + arrayIndex)) {
        path = path.substring(0, path.lastIndexOf('.'));
    }

    const {workflowNodeName} = currentComponentData;

    updateWorkflowNodeParameterMutation.mutate(
        {
            id: workflowId,
            updateWorkflowNodeParameterRequestModel: {
                arrayIndex,
                name: name.endsWith('_' + arrayIndex) ? undefined : name,
                path,
                value,
                workflowNodeName,
            },
        },
        {
            onSuccess: (response) => {
                setComponentData([
                    ...otherComponentData,
                    {
                        ...currentComponentData,
                        displayConditions: response.displayConditions,
                        parameters: response.parameters,
                    },
                ]);
            },
        }
    );
}
