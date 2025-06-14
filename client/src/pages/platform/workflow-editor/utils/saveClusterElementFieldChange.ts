import {ComponentDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {QueryClient, UseMutationResult} from '@tanstack/react-query';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import saveWorkflowDefinition from './saveWorkflowDefinition';

type FieldUpdateType = {
    field: 'operation' | 'label' | 'description';
    value: string;
};

interface SaveClusterElementFieldChangeProps {
    currentComponentDefinition: ComponentDefinition;
    currentClusterElementName: string;
    currentOperationProperties?: Array<PropertyAllType>;
    fieldUpdate: FieldUpdateType;
    invalidateWorkflowQueries: () => void;
    queryClient: QueryClient;
    updateWorkflowMutation: UseMutationResult<void, Error, {id: string; workflow: Workflow}, unknown>;
}

export default function saveClusterElementFieldChange({
    currentClusterElementName,
    currentComponentDefinition,
    currentOperationProperties,
    fieldUpdate,
    invalidateWorkflowQueries,
    updateWorkflowMutation,
}: SaveClusterElementFieldChangeProps): void {
    const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();
    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore.getState();
    const {workflow} = useWorkflowDataStore.getState();

    if (!currentNode || !currentNode?.clusterElementType || !workflow.definition) {
        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const currentParentTask = workflowDefinitionTasks?.find(
        (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
    );
    const existingClusterElements = currentParentTask?.clusterElements;

    const clusterElementType = currentNode.clusterElementType;

    const objectKey = clusterElementType;

    const clusterElements = {...existingClusterElements};

    if (Array.isArray(clusterElements[objectKey])) {
        clusterElements[objectKey] = clusterElements[objectKey].map((element) => {
            if (element.name === currentClusterElementName) {
                switch (fieldUpdate.field) {
                    case 'operation':
                        return {
                            label: element.label,
                            name: element.name,
                            operationName: fieldUpdate.value,
                            parameters: getParametersWithDefaultValues({
                                properties: currentOperationProperties as Array<PropertyAllType>,
                            }),
                            type: `${currentComponentDefinition.name}/v${currentComponentDefinition.version}/${fieldUpdate.value}`,
                        };

                    case 'label':
                        return {...element, label: fieldUpdate.value};

                    case 'description':
                        return {...element, description: fieldUpdate.value};

                    default:
                        return element;
                }
            }
            return element;
        });
    } else if (clusterElements[objectKey]) {
        if (clusterElements[objectKey].name === currentClusterElementName) {
            switch (fieldUpdate.field) {
                case 'operation': {
                    clusterElements[objectKey] = {
                        label: clusterElements[objectKey].label,
                        name: clusterElements[objectKey].name,
                        operationName: fieldUpdate.value,
                        parameters: getParametersWithDefaultValues({
                            properties: currentOperationProperties as Array<PropertyAllType>,
                        }),
                        type: `${currentComponentDefinition.name}/v${currentComponentDefinition.version}/${fieldUpdate.value}`,
                    };

                    break;
                }

                case 'label':
                    clusterElements[objectKey] = {
                        ...clusterElements[objectKey],
                        label: fieldUpdate.value,
                    };

                    break;

                case 'description':
                    clusterElements[objectKey] = {
                        ...clusterElements[objectKey],
                        description: fieldUpdate.value,
                    };

                    break;
            }
        }
    }

    const updatedNodeData = {
        ...currentParentTask,
        clusterElements,
    };

    if (rootClusterElementNodeData) {
        setRootClusterElementNodeData({
            ...rootClusterElementNodeData,
            clusterElements,
        });
    }

    saveWorkflowDefinition({
        invalidateWorkflowQueries,
        nodeData: updatedNodeData,
        onSuccess: () => {
            if (fieldUpdate.field === 'operation') {
                setCurrentNode({
                    ...currentNode,
                    operationName: fieldUpdate.value,
                    parameters: getParametersWithDefaultValues({
                        properties: currentOperationProperties as Array<PropertyAllType>,
                    }),
                    type: `${currentComponentDefinition.name}/v${currentComponentDefinition.version}/${fieldUpdate.value}`,
                });
            } else {
                setCurrentNode({
                    ...currentNode,
                    [fieldUpdate.field]: fieldUpdate.value,
                });
            }
        },
        updateWorkflowMutation,
    });
}
