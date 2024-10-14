import {Workflow} from '@/shared/middleware/automation/configuration';
import {ActionDefinition} from '@/shared/middleware/platform/configuration';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {ClickedOperationType, NodeType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {Instance} from 'reactflow';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleConditionChildOperationClickProps {
    componentNames: Array<string>;
    currentNode?: NodeType;
    operation: ClickedOperationType;
    operationDefinition: ActionDefinition;
    placeholderId: string;
    queryClient: QueryClient;
    setEdges: Instance.SetEdges<unknown>;
    setNodes: Instance.SetNodes<unknown>;
    setWorkflow: (workflowDefinition: Workflow & WorkflowTaskDataType) => void;
    sourceNodeId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function handleConditionChildOperationClick({
    componentNames,
    currentNode,
    operation,
    operationDefinition,
    queryClient,
    setNodes,
    setWorkflow,
    sourceNodeId,
    updateWorkflowMutation,
    workflow,
}: HandleConditionChildOperationClickProps) {
    const {componentLabel, componentName, icon, type} = operation;

    setNodes((nodes) => {
        const workflowNodeName = getFormattedName(componentName!, nodes);

        const newWorkflowNodeData = {
            componentName: componentName,
            icon: icon && <InlineSVG className="size-9" loader={<ComponentIcon className="size-9" />} src={icon} />,
            label: componentLabel,
            metadata: {ui: {condition: sourceNodeId}},
            name: workflowNodeName,
            type: type,
            workflowNodeName,
        };

        const newWorkflowNode = {
            data: newWorkflowNodeData,
            id: workflowNodeName,
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        setWorkflow({
            ...workflow,
            componentNames: [...componentNames, componentName],
            nodeNames: [...workflow.nodeNames, workflowNodeName],
        });

        let taskAfterCurrentIndex = workflow.tasks?.length;

        const duplicateSourceNode = workflow.tasks?.find((task) => task.metadata?.ui?.condition === sourceNodeId);

        if (duplicateSourceNode) {
            taskAfterCurrentIndex = workflow.tasks?.findIndex((task) => task.metadata?.ui?.condition === sourceNodeId);
        }

        saveWorkflowDefinition(
            {
                ...newWorkflowNodeData,
                parameters: getParametersWithDefaultValues({
                    properties: operationDefinition?.properties as Array<PropertyAllType>,
                }),
            },
            workflow!,
            updateWorkflowMutation,
            queryClient,
            taskAfterCurrentIndex,
            () => {
                queryClient.invalidateQueries({
                    queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                        id: workflow.id!,
                        lastWorkflowNodeName: currentNode?.name,
                    }),
                });
            }
        );

        if (taskAfterCurrentIndex !== undefined && taskAfterCurrentIndex !== -1) {
            return [
                ...nodes.slice(0, taskAfterCurrentIndex + 1),
                newWorkflowNode,
                ...nodes.slice(taskAfterCurrentIndex + 1),
            ];
        }

        return [...nodes, newWorkflowNode];
    });
}
