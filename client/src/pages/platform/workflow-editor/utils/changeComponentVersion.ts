import {TASK_DISPATCHER_DATA_KEY_MAP} from '@/shared/constants';
import {
    ActionDefinition,
    ClusterElementDefinition,
    ComponentDefinition,
    ComponentDefinitionApi,
    TriggerDefinition,
} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {NodeDataType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getParametersForVersionChange from './getParametersForVersionChange';
import invalidateOperationQueries from './invalidateOperationQueries';
import resolveOperationNameForVersion from './resolveOperationNameForVersion';
import saveClusterElementFieldChange from './saveClusterElementFieldChange';
import saveTaskDispatcherSubtaskFieldChange from './saveTaskDispatcherSubtaskFieldChange';
import saveWorkflowDefinition from './saveWorkflowDefinition';

type OperationDefinitionType = ActionDefinition | ClusterElementDefinition | TriggerDefinition;

interface DeleteWorkflowNodeTestOutputMutationI {
    mutateAsync: (variables: {environmentId: number; id: string; workflowNodeName: string}) => Promise<unknown>;
}

interface ChangeComponentVersionProps {
    currentComponentDefinition: ComponentDefinition;
    currentNodeIndex?: number;
    currentOperationName: string;
    deleteWorkflowNodeTestOutputMutation: DeleteWorkflowNodeTestOutputMutationI;
    environmentId: number;
    fetchActionDefinition: (operationName: string, componentVersion: number) => Promise<OperationDefinitionType>;
    fetchClusterElementDefinition: (
        operationName: string,
        componentVersion: number
    ) => Promise<OperationDefinitionType>;
    fetchTriggerDefinition: (operationName: string, componentVersion: number) => Promise<OperationDefinitionType>;
    newComponentVersion: number;
    queryClient: QueryClient;
    setCurrentOperationName: (operationName: string) => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowId: string;
}

/**
 * Moves the node open in the details panel to another version of its component.
 *
 * The operation is kept when the target version still declares it, otherwise the first operation of the same kind
 * is used, and the parameters are carried over for the properties that version still declares.
 */
export default async function changeComponentVersion({
    currentComponentDefinition,
    currentNodeIndex,
    currentOperationName,
    deleteWorkflowNodeTestOutputMutation,
    environmentId,
    fetchActionDefinition,
    fetchClusterElementDefinition,
    fetchTriggerDefinition,
    newComponentVersion,
    queryClient,
    setCurrentOperationName,
    updateWorkflowMutation,
    workflowId,
}: ChangeComponentVersionProps): Promise<void> {
    const {currentNode, setCurrentNode, setOperationChangeInProgress} = useWorkflowNodeDetailsPanelStore.getState();

    if (!currentNode || !Number.isInteger(newComponentVersion)) {
        return;
    }

    if (currentComponentDefinition.version === newComponentVersion) {
        return;
    }

    setOperationChangeInProgress(true);

    try {
        const componentDefinitionRequest = {
            componentName: currentComponentDefinition.name,
            componentVersion: newComponentVersion,
        };

        const newComponentDefinition = await queryClient.fetchQuery({
            queryFn: () => new ComponentDefinitionApi().getComponentDefinition(componentDefinitionRequest),
            queryKey: ComponentDefinitionKeys.componentDefinition(componentDefinitionRequest),
            staleTime: DEFINITION_STALE_TIME,
        });

        const newOperationName = resolveOperationNameForVersion({
            clusterElementType: currentNode.clusterElementType,
            componentDefinition: newComponentDefinition,
            currentOperationName,
            trigger: currentNode.trigger,
        });

        if (!newOperationName) {
            console.error(
                `${currentComponentDefinition.name} v${newComponentVersion} has no operation the node could use`
            );

            setOperationChangeInProgress(false);

            return;
        }

        const isClusterElement = !!currentNode.clusterElementType;

        let newOperationDefinition: OperationDefinitionType;

        if (currentNode.trigger && !isClusterElement) {
            newOperationDefinition = await fetchTriggerDefinition(newOperationName, newComponentVersion);
        } else if (isClusterElement) {
            newOperationDefinition = await fetchClusterElementDefinition(newOperationName, newComponentVersion);
        } else {
            newOperationDefinition = await fetchActionDefinition(newOperationName, newComponentVersion);
        }

        if (!newOperationDefinition) {
            console.error(`newOperationDefinition not found for: ${newOperationName}`);

            setOperationChangeInProgress(false);

            return;
        }

        const newOperationProperties = (newOperationDefinition.properties ?? []) as Array<PropertyAllType>;

        const newParameters = getParametersForVersionChange({
            currentParameters: currentNode.parameters,
            properties: newOperationProperties,
        });

        setCurrentOperationName(newOperationName);

        await deleteWorkflowNodeTestOutputMutation.mutateAsync({
            environmentId,
            id: workflowId,
            workflowNodeName: currentNode.name,
        });

        invalidateOperationQueries(queryClient);

        const isTaskDispatcherSubtask = Object.values(TASK_DISPATCHER_DATA_KEY_MAP).some(
            (dataKey) => dataKey !== undefined && currentNode[dataKey as keyof NodeDataType] !== undefined
        );

        if (isTaskDispatcherSubtask) {
            saveTaskDispatcherSubtaskFieldChange({
                currentComponentDefinition: newComponentDefinition,
                currentNodeIndex: currentNodeIndex ?? 0,
                currentOperationProperties: newOperationProperties,
                fieldUpdate: {
                    field: 'operation',
                    value: newOperationName,
                },
                parameters: newParameters,
                updateWorkflowMutation,
            });

            return;
        }

        if (isClusterElement) {
            saveClusterElementFieldChange({
                currentComponentDefinition: newComponentDefinition,
                currentOperationProperties: newOperationProperties,
                fieldUpdate: {
                    field: 'operation',
                    value: newOperationName,
                },
                parameters: newParameters,
                updateWorkflowMutation,
            });

            return;
        }

        const {componentName, description, label, workflowNodeName} = currentNode;

        const newNodeType = `${componentName}/v${newComponentVersion}/${newOperationName}`;

        const newNodeMetadata = {
            ui: {
                nodePosition: currentNode.metadata?.ui?.nodePosition,
            },
        };

        await saveWorkflowDefinition({
            nodeData: {
                componentName,
                description,
                label,
                metadata: newNodeMetadata,
                name: workflowNodeName,
                operationName: newOperationName,
                parameters: newParameters,
                trigger: currentNode.trigger,
                type: newNodeType,
                version: newComponentVersion,
                workflowNodeName,
            },
            onSuccess: () => {
                setCurrentNode({
                    ...currentNode,
                    componentName,
                    displayConditions: {},
                    metadata: newNodeMetadata,
                    name: workflowNodeName,
                    operationName: newOperationName,
                    parameters: newParameters,
                    type: newNodeType,
                    version: newComponentVersion,
                    workflowNodeName,
                });

                setOperationChangeInProgress(false);
            },
            updateWorkflowMutation,
        });
    } catch (error) {
        console.error(`Failed to change ${currentComponentDefinition.name} to v${newComponentVersion}`, error);

        setOperationChangeInProgress(false);
    }
}
