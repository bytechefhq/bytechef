import {
    UpdateWorkflowNodeParameter200ResponseModelI,
    UpdateWorkflowNodeParameterRequestI,
} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import {ComponentType} from '@/shared/types';
import {UseMutationResult} from '@tanstack/react-query';

interface SavePropertyProps {
    currentComponent: ComponentType;
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
    currentComponent,
    path,
    setCurrentComponent,
    successCallback,
    updateWorkflowNodeParameterMutation,
    value,
    workflowId,
}: SavePropertyProps) {
    // TODO fix in Property.tsx path value, it should be without 'parameters.' prefix and should not contain '${arrayName}_${arrayIndex}' as suffix if array is updated

    path = path.replace('parameters.', '').replace('parameters', '');

    const {workflowNodeName} = currentComponent;

    updateWorkflowNodeParameterMutation.mutate(
        {
            id: workflowId,
            updateWorkflowNodeParameterRequestModel: {
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
