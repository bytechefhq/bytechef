import {
    UpdateWorkflowNodeParameter200ResponseModelI,
    UpdateWorkflowNodeParameterRequestI,
} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import {ComponentType} from '@/shared/types';
import {UseMutationResult} from '@tanstack/react-query';

interface SavePropertyProps {
    arrayIndex?: number;
    currentComponent: ComponentType;
    name: string;
    path: string;
    setCurrentComponent: (currentComponent: ComponentType | undefined) => void;
    successCallback?: () => void;
    updateWorkflowNodeParameterMutation: UseMutationResult<
        UpdateWorkflowNodeParameter200ResponseModelI,
        Error,
        UpdateWorkflowNodeParameterRequestI,
        unknown
    >;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    value?: any;
    workflowId: string;
}

export default function saveProperty({
    arrayIndex,
    currentComponent,
    name,
    path,
    setCurrentComponent,
    successCallback,
    updateWorkflowNodeParameterMutation,
    value,
    workflowId,
}: SavePropertyProps) {
    // TODO fix in Property.tsx path value, it should be without 'parameters.' prefix and should not contain '${arrayName}_${arrayIndex}' as suffix if array is updated

    path = path.replace('parameters.', '').replace('parameters', '');

    if (arrayIndex !== undefined && path.endsWith(arrayIndex.toString())) {
        path = path.substring(0, path.lastIndexOf('.'));
    }

    const {workflowNodeName} = currentComponent;

    updateWorkflowNodeParameterMutation.mutate(
        {
            id: workflowId,
            updateWorkflowNodeParameterRequestModel: {
                arrayIndex,
                name: name === arrayIndex?.toString() ? undefined : name,
                path,
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
