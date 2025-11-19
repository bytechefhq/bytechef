import {ComponentDefinition, Workflow} from '@/shared/middleware/platform/configuration';
import {ClusterElementsType, NodeDataType, PropertyAllType} from '@/shared/types';
import {UseMutationResult} from '@tanstack/react-query';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import {updateClusterRootElementField, updateNestedClusterElementField} from './clusterElementsFieldChangeUtils';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import {getTaskDispatcherTask} from './taskDispatcherConfig';

type FieldUpdateType = {
    field: 'operation' | 'label' | 'description';
    value: string;
};

interface SaveClusterElementFieldChangeProps {
    currentComponentDefinition: ComponentDefinition;
    currentOperationProperties?: Array<PropertyAllType>;
    fieldUpdate: FieldUpdateType;
    invalidateWorkflowQueries: () => void;
    updateWorkflowMutation: UseMutationResult<void, Error, {id: string; workflow: Workflow}, unknown>;
}

export default function saveClusterElementFieldChange({
    currentComponentDefinition,
    currentOperationProperties,
    fieldUpdate,
    invalidateWorkflowQueries,
    updateWorkflowMutation,
}: SaveClusterElementFieldChangeProps): void {
    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} =
        useWorkflowNodeDetailsPanelStore.getState();
    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore.getState();
    const {workflow} = useWorkflowDataStore.getState();

    if (!currentNode || !workflow.definition) {
        return;
    }

    const {componentName, name, workflowNodeName} = currentNode;

    if (!rootClusterElementNodeData?.workflowNodeName || !rootClusterElementNodeData?.componentName) {
        console.error('Root cluster element node data is missing required properties');

        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const mainClusterRootTask = getTaskDispatcherTask({
        taskDispatcherId: rootClusterElementNodeData.workflowNodeName,
        tasks: workflowDefinitionTasks,
    });

    if (!mainClusterRootTask) {
        return;
    }

    let updatedMainRootData: NodeDataType;
    let updatedClusterElements: ClusterElementsType;

    if (
        currentNode.clusterRoot &&
        currentNode.workflowNodeName === rootClusterElementNodeData.workflowNodeName &&
        !currentNode.isNestedClusterRoot
    ) {
        updatedMainRootData = updateClusterRootElementField({
            currentComponentDefinition,
            currentOperationProperties,
            fieldUpdate,
            mainRootElement: {
                ...mainClusterRootTask,
                componentName: rootClusterElementNodeData.componentName,
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            },
        });
    } else if (
        currentNode.clusterElementType &&
        currentNode.workflowNodeName !== rootClusterElementNodeData.workflowNodeName
    ) {
        const clusterElements = mainClusterRootTask.clusterElements;

        if (!clusterElements || Object.keys(clusterElements).length === 0) {
            return;
        }

        updatedClusterElements = updateNestedClusterElementField({
            clusterElements,
            currentComponentDefinition,
            currentOperationProperties,
            elementName: workflowNodeName,
            fieldUpdate,
        });

        updatedMainRootData = {
            ...mainClusterRootTask,
            clusterElements: updatedClusterElements,
            componentName: rootClusterElementNodeData.componentName,
            workflowNodeName: rootClusterElementNodeData.workflowNodeName,
        };
    } else {
        console.error('Unknown cluster element type or root element mismatch');

        return;
    }

    saveWorkflowDefinition({
        decorative: true,
        invalidateWorkflowQueries,
        nodeData: updatedMainRootData,
        onSuccess: () => {
            let commonUpdates: NodeDataType = {
                componentName,
                name,
                workflowNodeName,
            };

            if (fieldUpdate.field === 'operation') {
                commonUpdates = {
                    ...commonUpdates,
                    clusterElementName: fieldUpdate.value,
                    metadata: {
                        ui: {
                            nodePosition: currentNode.metadata?.ui?.nodePosition
                                ? currentNode.metadata?.ui?.nodePosition
                                : undefined,
                        },
                    },
                    operationName: fieldUpdate.value,
                    parameters: getParametersWithDefaultValues({
                        properties: currentOperationProperties as Array<PropertyAllType>,
                    }),
                    type: `${currentComponentDefinition.name}/v${currentComponentDefinition.version}/${fieldUpdate.value}`,
                };
            } else {
                commonUpdates[fieldUpdate.field] = fieldUpdate.value;
            }

            setCurrentNode({
                ...currentNode,
                ...commonUpdates,
            });

            setCurrentComponent({
                ...currentComponent,
                ...commonUpdates,
            });

            if (rootClusterElementNodeData) {
                if (currentNode.clusterRoot && !currentNode.isNestedClusterRoot) {
                    setRootClusterElementNodeData({
                        ...rootClusterElementNodeData,
                        ...commonUpdates,
                    });
                } else {
                    setRootClusterElementNodeData({
                        ...rootClusterElementNodeData,
                        clusterElements: updatedClusterElements,
                    });
                }
            }
        },
        updateWorkflowMutation,
    });
}
