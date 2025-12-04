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

    if (currentNode && currentNode.clusterElementType) {
        updateClusterElementParameterMutation?.mutate(
            {
                clusterElementType: currentNode.clusterElementType,
                clusterElementWorkflowNodeName: currentNode.workflowNodeName,
                environmentId: environmentStore.getState().currentEnvironmentId,
                id: workflowId,
                updateClusterElementParameterRequest: {
                    fromAiInMetadata: fromAi,
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

                    useWorkflowNodeDetailsPanelStore.getState().setCurrentNode({
                        ...currentNode,
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
            environmentId: environmentStore.getState().currentEnvironmentId,
            id: workflowId,
            updateWorkflowNodeParameterRequest: {
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

                if (currentNode) {
                    useWorkflowNodeDetailsPanelStore.getState().setCurrentNode({
                        ...currentNode,
                        displayConditions: response.displayConditions,
                        metadata: response.metadata,
                        parameters: response.parameters,
                    });
                }
            },
        }
    );
}
