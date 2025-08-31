import {useEnvironmentStore} from '@/pages/automation/stores/useEnvironmentStore';
import {
    UpdateClusterElementParameter200Response,
    UpdateWorkflowNodeParameterRequest,
} from '@/shared/middleware/platform/configuration';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {decodePath} from './encodingUtils';

interface SavePropertyProps {
    includeInMetadata?: boolean;
    path: string;
    successCallback?: () => void;
    type: string;
    updateWorkflowNodeParameterMutation: UseMutationResult<
        UpdateClusterElementParameter200Response & {workflowNodeName?: string},
        Error,
        UpdateWorkflowNodeParameterRequest,
        unknown
    >;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    value?: any;
    workflowId: string;
}

export default function saveProperty({
    includeInMetadata = false,
    path,
    successCallback,
    type,
    updateWorkflowNodeParameterMutation,
    value,
    workflowId,
}: SavePropertyProps) {
    const currentComponent = useWorkflowNodeDetailsPanelStore.getState().currentComponent;

    if (!currentComponent) {
        console.error('No current component found in the store');

        return;
    }

    const decodedPath = decodePath(path);

    updateWorkflowNodeParameterMutation.mutate(
        {
            environmentId: useEnvironmentStore.getState().currentEnvironmentId,
            id: workflowId,
            updateClusterElementParameterRequest: {
                includeInMetadata,
                path: decodedPath,
                type,
                value,
            },
            workflowNodeName: currentComponent.workflowNodeName,
        },
        {
            onSuccess: (response) => {
                if (successCallback) {
                    successCallback();
                }

                useWorkflowNodeDetailsPanelStore.getState().setCurrentComponent({
                    ...currentComponent,
                    displayConditions: response.displayConditions,
                    metadata: response.metadata,
                    parameters: response.parameters,
                });
            },
        }
    );
}
