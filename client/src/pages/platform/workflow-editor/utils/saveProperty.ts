import {convertNameToSnakeCase} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {
    DeleteClusterElementParameter200Response,
    UpdateClusterElementParameterOperationRequest,
    UpdateWorkflowNodeParameterOperationRequest,
} from '@/shared/middleware/platform/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
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

    function handleSuccess(
        response: DeleteClusterElementParameter200Response & {workflowNodeName?: string},
        updatedWorkflowNodeName: string
    ) {
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

        if (response.parameters && updatedWorkflowNodeName) {
            useWorkflowDataStore
                .getState()
                .updateWorkflowNodeParameters(updatedWorkflowNodeName, response.parameters, response.version);
        }
    }

    if (currentNode && currentNode.clusterElementType) {
        if (!updateClusterElementParameterMutation) {
            return;
        }

        const clusterElementType = convertNameToSnakeCase(currentNode.clusterElementType as string);
        const clusterElementWorkflowNodeName = currentNode.workflowNodeName;

        enqueueWorkflowMutation(() =>
            updateClusterElementParameterMutation.mutateAsync(
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
                    onError: (error) => {
                        console.error('Failed to save cluster element parameter:', error);
                    },
                    onSuccess: (response) => handleSuccess(response, clusterElementWorkflowNodeName),
                }
            )
        );

        return;
    }

    const nodeWorkflowNodeName = rootClusterElementNodeData?.workflowNodeName || currentNode?.workflowNodeName || '';

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
                workflowNodeName: nodeWorkflowNodeName,
            },
            {
                onError: (error) => {
                    console.error('Failed to save workflow node parameter:', error);
                },
                onSuccess: (response) => handleSuccess(response, nodeWorkflowNodeName),
            }
        )
    );
}
