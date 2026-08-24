import {TASK_DISPATCHER_DATA_KEY_MAP} from '@/shared/constants';
import {
    ActionDefinition,
    ClusterElementDefinition,
    ComponentDefinition,
    TriggerDefinition,
} from '@/shared/middleware/platform/configuration';
import {NodeDataType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import invalidateOperationQueries from './invalidateOperationQueries';
import saveClusterElementFieldChange from './saveClusterElementFieldChange';
import saveTaskDispatcherSubtaskFieldChange from './saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from './saveWorkflowDefinition';

type OperationDefinitionType = ActionDefinition | ClusterElementDefinition | TriggerDefinition;

interface DeleteWorkflowNodeTestOutputMutationI {
    mutateAsync: (variables: {environmentId: number; id: string; workflowNodeName: string}) => Promise<unknown>;
}

interface ChangeOperationProps {
    currentComponentDefinition?: ComponentDefinition;
    currentNodeIndex?: number;
    currentOperationName: string;
    deleteWorkflowNodeTestOutputMutation: DeleteWorkflowNodeTestOutputMutationI;
    environmentId: number;
    fetchActionDefinition: (operationName: string) => Promise<OperationDefinitionType | undefined>;
    fetchClusterElementDefinition: (operationName: string) => Promise<OperationDefinitionType | undefined>;
    fetchTriggerDefinition: (operationName: string) => Promise<OperationDefinitionType | undefined>;
    isClusterElement: boolean;
    newOperationName: string;
    queryClient: QueryClient;
    setCurrentOperationName: (operationName: string) => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowId: string;
}

/**
 * Switches the node open in the details panel to another operation of its component.
 *
 * The operation change stays in progress until the workflow is saved, and a switch picked while another one is
 * still saving is ignored.
 */
export default async function changeOperation({
    currentComponentDefinition,
    currentNodeIndex,
    currentOperationName,
    deleteWorkflowNodeTestOutputMutation,
    environmentId,
    fetchActionDefinition,
    fetchClusterElementDefinition,
    fetchTriggerDefinition,
    isClusterElement,
    newOperationName,
    queryClient,
    setCurrentOperationName,
    updateWorkflowMutation,
    workflowId,
}: ChangeOperationProps): Promise<void> {
    const {currentNode, operationChangeInProgress, setCurrentNode, setOperationChangeInProgress} =
        useWorkflowNodeDetailsPanelStore.getState();

    if (
        currentOperationName === newOperationName ||
        !currentComponentDefinition ||
        !currentNode ||
        operationChangeInProgress
    ) {
        return;
    }

    setCurrentOperationName(newOperationName);

    setOperationChangeInProgress(true);

    const handleSaveError = () => setOperationChangeInProgress(false);

    try {
        let newOperationDefinition: OperationDefinitionType | undefined;

        if (currentNode.trigger && !isClusterElement) {
            newOperationDefinition = await fetchTriggerDefinition(newOperationName);
        } else if (isClusterElement) {
            newOperationDefinition = await fetchClusterElementDefinition(newOperationName);
        } else {
            newOperationDefinition = await fetchActionDefinition(newOperationName);
        }

        if (!newOperationDefinition) {
            console.error(`newOperationDefinition not found for: ${newOperationName}`);

            setOperationChangeInProgress(false);

            return;
        }

        await deleteWorkflowNodeTestOutputMutation.mutateAsync({
            environmentId,
            id: workflowId,
            workflowNodeName: currentNode.name,
        });

        invalidateOperationQueries(queryClient);

        const newOperationProperties = newOperationDefinition.properties as Array<PropertyAllType>;

        const isTaskDispatcherSubtask = Object.values(TASK_DISPATCHER_DATA_KEY_MAP).some(
            (dataKey) => dataKey !== undefined && currentNode[dataKey as keyof NodeDataType] !== undefined
        );

        if (isTaskDispatcherSubtask) {
            saveTaskDispatcherSubtaskFieldChange({
                currentComponentDefinition,
                currentNodeIndex: currentNodeIndex ?? 0,
                currentOperationProperties: newOperationProperties,
                fieldUpdate: {
                    field: 'operation',
                    value: newOperationName,
                },
                onError: handleSaveError,
                updateWorkflowMutation,
            });

            return;
        }

        if (currentNode.clusterElementType) {
            saveClusterElementFieldChange({
                currentComponentDefinition,
                currentOperationProperties: newOperationProperties,
                fieldUpdate: {
                    field: 'operation',
                    value: newOperationName,
                },
                onError: handleSaveError,
                updateWorkflowMutation,
            });

            return;
        }

        const {componentName, description, disabled, label, workflowNodeName} = currentNode;

        const name = workflowNodeName || '';
        const parameters = getParametersWithDefaultValues({properties: newOperationProperties});
        const type = `${componentName}/v${currentComponentDefinition.version}/${newOperationName}`;

        await saveWorkflowDefinition({
            nodeData: {
                componentName,
                description,
                disabled,
                label,
                name,
                operationName: newOperationName,
                parameters,
                trigger: currentNode.trigger,
                type,
                workflowNodeName,
            },
            onError: handleSaveError,
            onSuccess: () => {
                setCurrentNode({
                    ...currentNode,
                    componentName,
                    displayConditions: {},
                    metadata: {},
                    name,
                    operationName: newOperationName,
                    parameters,
                    type,
                    workflowNodeName,
                });

                setOperationChangeInProgress(false);
            },
            updateWorkflowMutation,
        });
    } catch (error) {
        console.error(`Failed to change ${currentNode.name} to ${newOperationName}`, error);

        setOperationChangeInProgress(false);
    }
}
