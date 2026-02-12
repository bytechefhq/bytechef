import {
    DeleteClusterElementParameter200Response,
    DeleteClusterElementParameterOperationRequest,
    DeleteWorkflowNodeParameterRequest,
} from '@/shared/middleware/platform/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {decodePath} from './encodingUtils';
import {enqueueWorkflowMutation} from './workflowMutationQueue';

export default function deleteProperty(
    workflowId: string,
    path: string,
    deleteWorkflowNodeParameterMutation: UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteWorkflowNodeParameterRequest,
        unknown
    >,
    deleteClusterElementParameterMutation?: UseMutationResult<
        DeleteClusterElementParameter200Response,
        Error,
        DeleteClusterElementParameterOperationRequest,
        unknown
    >
) {
    const currentComponent = useWorkflowNodeDetailsPanelStore.getState().currentComponent;
    const currentNode = useWorkflowNodeDetailsPanelStore.getState().currentNode;
    const rootClusterElementNodeData = useWorkflowEditorStore.getState().rootClusterElementNodeData;

    const decodedPath = decodePath(path);

    if (!currentComponent) {
        console.error('No current component found in the store');

        return;
    }

    if (currentNode?.clusterElementType) {
        if (!deleteClusterElementParameterMutation) {
            return;
        }

        const clusterElementType = currentNode.clusterElementType;
        const clusterElementWorkflowNodeName = currentNode.workflowNodeName;

        enqueueWorkflowMutation(() =>
            deleteClusterElementParameterMutation.mutateAsync(
                {
                    clusterElementType,
                    clusterElementWorkflowNodeName,
                    deleteClusterElementParameterRequest: {
                        path: decodedPath,
                    },
                    environmentId: environmentStore.getState().currentEnvironmentId,
                    id: workflowId,
                    workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
                },
                {
                    onSuccess: (response) => {
                        const {setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

                        setCurrentComponent({
                            ...currentComponent,
                            displayConditions: response.displayConditions,
                            metadata: response.metadata,
                            parameters: response.parameters,
                        });

                        setCurrentNode({
                            ...currentNode,
                            displayConditions: response.displayConditions,
                            metadata: response.metadata,
                            parameters: response.parameters,
                        });
                    },
                }
            )
        );

        return;
    }

    enqueueWorkflowMutation(() =>
        deleteWorkflowNodeParameterMutation.mutateAsync(
            {
                deleteClusterElementParameterRequest: {
                    path: decodedPath,
                },
                environmentId: environmentStore.getState().currentEnvironmentId,
                id: workflowId,
                workflowNodeName: rootClusterElementNodeData?.workflowNodeName || currentNode?.workflowNodeName || '',
            },
            {
                onSuccess: (response) => {
                    const {setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

                    setCurrentComponent({
                        ...currentComponent,
                        displayConditions: response.displayConditions,
                        metadata: response.metadata,
                        parameters: response.parameters,
                    });

                    if (currentNode) {
                        setCurrentNode({
                            ...currentNode,
                            displayConditions: response.displayConditions,
                            metadata: response.metadata,
                            parameters: response.parameters,
                        });
                    }
                },
            }
        )
    );
}
