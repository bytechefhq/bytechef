import {
    DeleteClusterElementParameter200Response,
    UpdateClusterElementParameterOperationRequest,
    UpdateWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {decodePath} from './encodingUtils';
import {enqueueWorkflowMutation} from './workflowMutationQueue';

interface SavePropertyProps {
    fromAi?: boolean;
    includeInMetadata?: boolean;
    path: string;
    successCallback?: () => void;
    type: string;
    updateClusterElementParameterMutation?: UseMutationResult<
        DeleteClusterElementParameter200Response & {workflowNodeName?: string},
        Error,
        UpdateClusterElementParameterOperationRequest,
        unknown
    >;
    updateWorkflowNodeParameterMutation: UseMutationResult<
        DeleteClusterElementParameter200Response & {workflowNodeName?: string},
        Error,
        UpdateWorkflowNodeParameterOperationRequest,
        unknown
    >;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    value?: any;
    workflowId: string;
}

export default function saveProperty({
    fromAi = false,
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

    function handleSuccess(response: DeleteClusterElementParameter200Response & {workflowNodeName?: string}) {
        if (successCallback) {
            successCallback();
        }

        if (currentComponent) {
            useWorkflowNodeDetailsPanelStore.getState().setCurrentComponent({
                ...currentComponent,
                displayConditions: response.displayConditions,
                metadata: response.metadata,
                parameters: response.parameters,
            });
        }

        if (currentNode) {
            useWorkflowNodeDetailsPanelStore.getState().setCurrentNode({
                ...currentNode,
                displayConditions: response.displayConditions,
                metadata: response.metadata,
                parameters: response.parameters,
            });
        }
    }

    if (currentNode && currentNode.clusterElementType) {
        const clusterElementType = currentNode.clusterElementType;
        const clusterElementWorkflowNodeName = currentNode.workflowNodeName;

        enqueueWorkflowMutation(() =>
            updateClusterElementParameterMutation!.mutateAsync(
                {
                    clusterElementType,
                    clusterElementWorkflowNodeName,
                    environmentId: environmentStore.getState().currentEnvironmentId,
                    id: workflowId,
                    updateClusterElementParameterRequest: {
                        fromAiInMetadata: fromAi,
                        includeInMetadata,
                        path: decodedPath,
                        type,
                        value,
                    },
                    workflowNodeName: rootClusterElementNodeData?.workflowNodeName ?? '',
                },
                {
                    onSuccess: (response) => handleSuccess(response),
                }
            )
        );

        return;
    }

    enqueueWorkflowMutation(() =>
        updateWorkflowNodeParameterMutation.mutateAsync(
            {
                environmentId: environmentStore.getState().currentEnvironmentId,
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
                onSuccess: (response) => handleSuccess(response),
            }
        )
    );
}
