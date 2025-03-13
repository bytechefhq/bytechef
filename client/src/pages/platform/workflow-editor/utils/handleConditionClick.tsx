import {TaskDispatcherDefinitionApi, Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
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
    edgeId?: string;
    projectId: number;
    queryClient: QueryClient;
    sourceNodeId?: string;
    taskDispatcherContext?: Record<string, unknown>;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default async function handleConditionClick({
    clickedItem,
    edgeId,
    projectId,
    queryClient,
    sourceNodeId,
    taskDispatcherContext,
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

    if (!conditionDefinition || !workflow.definition) {
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

    const tasks: Array<WorkflowTask> = JSON.parse(workflow.definition).tasks;

    let taskIndex = tasks?.length;

    const isSourceConditionPlaceholderNode =
        sourceNodeId?.includes('condition') && sourceNodeId?.includes('placeholder');

    let conditionId: string | undefined;

    const hasTaskDispatcherContextValues =
        taskDispatcherContext && Object.values(taskDispatcherContext).filter(Boolean).length;

    if (hasTaskDispatcherContextValues && !sourceNodeId?.includes('condition-bottom-ghost')) {
        conditionId = taskDispatcherContext.conditionId as string;

        if (!conditionId || !tasks) {
            return;
        }

        taskIndex = tasks.findIndex((task) => task.name === conditionId) + 1;
    } else if (sourceNodeId && isSourceConditionPlaceholderNode) {
        if (edgeId && tasks) {
            taskIndex = tasks.findIndex((task) => task.name === sourceNodeId) + 1;
        }

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
    } else if (sourceNodeId && !isSourceConditionPlaceholderNode) {
        if (edgeId && tasks) {
            const targetTaskId = edgeId.split('=>')[1];

            taskIndex = tasks.findIndex((task) => task.name === targetTaskId);
        }
    }

    // Handle adding condition inside another condition
    if (taskDispatcherContext && taskDispatcherContext.conditionId) {
        conditionId = taskDispatcherContext.conditionId as string;

        newConditionNodeData.conditionData = {
            conditionCase: taskDispatcherContext.conditionCase as 'caseTrue' | 'caseFalse',
            conditionId: taskDispatcherContext.conditionId as string,
            index: (taskDispatcherContext.index ?? 0) as number,
        };
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
        placeholderId: taskDispatcherContext ? undefined : sourceNodeId,
        projectId,
        queryClient,
        taskDispatcherContext,
        updateWorkflowMutation,
    });
}
