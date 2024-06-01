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
