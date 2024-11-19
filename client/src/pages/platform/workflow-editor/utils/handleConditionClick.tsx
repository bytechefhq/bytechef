import {TaskDispatcherDefinitionApi, Workflow} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowNodeOutputKeys} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {ClickedDefinitionType, NodeType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {Component1Icon} from '@radix-ui/react-icons';
import {QueryClient} from '@tanstack/react-query';
import InlineSVG from 'react-inlinesvg';
import {Node} from 'reactflow';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleConditionClickProps {
    clickedItem: ClickedDefinitionType;
    currentNode?: NodeType;
    edge?: boolean;
    getNodes: () => Array<Node>;
    queryClient: QueryClient;
    sourceNodeId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default async function handleConditionClick({
    clickedItem,
    currentNode,
    edge,
    getNodes,
    queryClient,
    sourceNodeId,
    updateWorkflowMutation,
    workflow,
}: HandleConditionClickProps) {
    const clickedTaskDispatcherDefinition = await queryClient.fetchQuery({
        queryFn: () =>
            new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition({
                taskDispatcherName: clickedItem.name,
                taskDispatcherVersion: clickedItem.version,
            }),
        queryKey: ComponentDefinitionKeys.componentDefinition({
            componentName: clickedItem.name,
        }),
    });

    if (!clickedTaskDispatcherDefinition) {
        return;
    }

    const nodes = getNodes();

    const workflowNodeName = getFormattedName(clickedItem.name!, nodes);

    const newConditionNodeData = {
        ...clickedTaskDispatcherDefinition,
        componentName: clickedItem.name,
        icon: (
            <>
                {clickedItem.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={clickedItem.icon} />
                ) : (
                    <Component1Icon className="size-9 text-gray-700" />
                )}
            </>
        ),
        label: clickedItem?.title,
        name: workflowNodeName,
        taskDispatcher: true,
        type: `${clickedTaskDispatcherDefinition.name}/v${clickedTaskDispatcherDefinition.version}`,
    };

    const {tasks} = workflow;

    let taskIndex = tasks?.length;

    if (edge && tasks) {
        sourceNodeId = sourceNodeId.split('=>')[0];

        taskIndex = tasks.findIndex((task) => task.name === sourceNodeId) + 1;
    }

    let conditionId: string | undefined;

    if (sourceNodeId.includes('condition') && sourceNodeId.includes('placeholder')) {
        conditionId = sourceNodeId.split('-')[0];

        if (!conditionId || !tasks) {
            return;
        }

        taskIndex = tasks.findIndex((task) => task.name === conditionId) + 1;

        if (sourceNodeId.includes('left') || sourceNodeId.includes('right')) {
            taskIndex = parseInt(sourceNodeId.split('-')[3]);
        } else {
            conditionId = undefined;
        }
    }

    saveWorkflowDefinition({
        conditionId,
        nodeData: {
            ...newConditionNodeData,
            parameters: {
                ...getParametersWithDefaultValues({
                    properties: clickedTaskDispatcherDefinition?.properties as Array<PropertyAllType>,
                }),
                caseFalse: [],
                caseTrue: [],
            },
        },
        nodeIndex: taskIndex,
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: WorkflowNodeOutputKeys.filteredPreviousWorkflowNodeOutputs({
                    id: workflow.id!,
                    lastWorkflowNodeName: currentNode?.name,
                }),
            });
        },
        placeholderId: sourceNodeId,
        queryClient,
        updateWorkflowMutation,
        workflow,
    });
}
