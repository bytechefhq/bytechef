import {TaskDispatcherDefinitionApi, Workflow} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherKeys} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {ClickedDefinitionType, NodeDataType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {Component1Icon} from '@radix-ui/react-icons';
import {QueryClient} from '@tanstack/react-query';
import InlineSVG from 'react-inlinesvg';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleConditionClickProps {
    clickedItem: ClickedDefinitionType;
    edge?: boolean;
    projectId: number;
    queryClient: QueryClient;
    sourceNodeId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default async function handleConditionClick({
    clickedItem,
    edge,
    projectId,
    queryClient,
    sourceNodeId,
    updateWorkflowMutation,
    workflow,
}: HandleConditionClickProps) {
    const conditionDefinition = await queryClient.fetchQuery({
        queryFn: () =>
            new TaskDispatcherDefinitionApi().getTaskDispatcherDefinition({
                taskDispatcherName: clickedItem.name,
                taskDispatcherVersion: clickedItem.version,
            }),
        queryKey: TaskDispatcherKeys.taskDispatcherDefinition({
            taskDispatcherName: clickedItem.name,
            taskDispatcherVersion: clickedItem.version,
        }),
    });

    if (!conditionDefinition) {
        return;
    }

    const workflowNodeName = getFormattedName(clickedItem.name!);

    const newConditionNodeData: NodeDataType = {
        ...conditionDefinition,
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
        type: `${conditionDefinition.name}/v${conditionDefinition.version}`,
        workflowNodeName,
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
                    properties: conditionDefinition?.properties as Array<PropertyAllType>,
                }),
                caseFalse: [],
                caseTrue: [],
            },
            workflowNodeName,
        },
        nodeIndex: taskIndex,
        onSuccess: () =>
            handleComponentAddedSuccess({
                nodeData: newConditionNodeData,
                queryClient,
                workflow,
            }),
        placeholderId: sourceNodeId,
        projectId,
        queryClient,
        updateWorkflowMutation,
    });
}
