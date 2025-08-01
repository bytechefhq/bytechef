import {
    UpdateClusterElementParameterRequest,
    UpdateWorkflowNodeParameter200Response,
    UpdateWorkflowNodeParameterRequest,
} from '@/shared/middleware/platform/configuration';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {decodePath} from './encodingUtils';

interface SavePropertyProps {
    includeInMetadata?: boolean;
    path: string;
    successCallback?: () => void;
    type: string;
    updateClusterElementParameterMutation?: UseMutationResult<
        UpdateWorkflowNodeParameter200Response & {workflowNodeName?: string},
        Error,
        UpdateClusterElementParameterRequest,
        unknown
    >;
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
    updateClusterElementParameterMutation,
    updateWorkflowNodeParameterMutation,
    value,
    workflowId,
}: SavePropertyProps) {
    const {currentComponent, currentNode} = useWorkflowNodeDetailsPanelStore.getState();
    const {rootClusterElementNodeData} = useWorkflowEditorStore.getState();

    if (!currentComponent) {
        console.error('No current component found in the store');

        return;
    }

    const decodedPath = decodePath(path);

    if (currentNode && currentNode.clusterElementType) {
        updateClusterElementParameterMutation?.mutate(
            {
                clusterElementType: currentNode.clusterElementType,
                clusterElementWorkflowNodeName: currentNode.workflowNodeName,
                id: workflowId,
                updateWorkflowNodeParameterRequest: {
                    includeInMetadata,
                    path: decodedPath,
                    type,
                    value,
                },
                workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
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

        return;
    }

    updateWorkflowNodeParameterMutation.mutate(
        {
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
