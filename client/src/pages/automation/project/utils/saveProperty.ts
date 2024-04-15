import {WorkflowModel} from '@/middleware/automation/configuration';
import {
    UpdateWorkflowNodeParameter200ResponseModel,
    UpdateWorkflowNodeParameterRequest,
} from '@/middleware/platform/configuration';
import {ComponentType} from '@/types/types';
import {UseMutationResult} from '@tanstack/react-query';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';

export default function saveProperty(
    name: string,
    path: string,
    currentComponentData: ComponentType,
    otherComponentData: Array<ComponentType>,
    setComponentData: (componentData: Array<ComponentType>) => void,
    updateWorkflowNodeParameterMutation: UseMutationResult<
        UpdateWorkflowNodeParameter200ResponseModel,
        Error,
        UpdateWorkflowNodeParameterRequest,
        unknown
    >,
    workflow: WorkflowModel & WorkflowTaskDataType,
    /* eslint-disable @typescript-eslint/no-explicit-any */
    value?: any,
    arrayIndex?: number
) {
    // TODO fix in Property.tsx path value, it should be without 'parameters.' prefix and should not contain '${arrayName}_${arrayIndex}' as suffix if array is updated

    path = path.replace('parameters.', '').replace('parameters', '');

    if (arrayIndex !== undefined && path.endsWith('_' + arrayIndex)) {
        path = path.substring(0, path.lastIndexOf('.'));
    }

    const {workflowNodeName} = currentComponentData;

    updateWorkflowNodeParameterMutation.mutate(
        {
            id: workflow.id!,
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
                const parameters = response.parameters;

                setComponentData([
                    ...otherComponentData,
                    {
                        ...currentComponentData,
                        parameters,
                    },
                ]);
            },
        }
    );
}
