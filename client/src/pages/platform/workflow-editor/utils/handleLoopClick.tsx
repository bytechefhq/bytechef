import {Workflow} from '@/shared/middleware/automation/configuration';
import {TaskDispatcherDefinitionApi} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherKeys} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {ClickedDefinitionType, NodeDataType, PropertyAllType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleLoopClickProps {
    clickedItem: ClickedDefinitionType;
    edge?: boolean;
    queryClient: QueryClient;
    sourceNodeId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowTaskDataType;
}

export default async function handleLoopClick({
    clickedItem,
    edge,
    queryClient,
    sourceNodeId,
    updateWorkflowMutation,
    workflow,
}: HandleLoopClickProps) {
    const loopDefinition = await queryClient.fetchQuery({
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

    if (!loopDefinition) {
        return;
    }

    const workflowNodeName = getFormattedName(clickedItem.name!);

    const newLoopNodeData: NodeDataType = {
        ...loopDefinition,
        componentName: clickedItem.name,
        icon: (
            <>
                {clickedItem.icon ? (
                    <InlineSVG className="size-9 text-gray-700" src={clickedItem.icon} />
                ) : (
                    <ComponentIcon className="size-9 text-gray-700" />
                )}
            </>
        ),
        label: clickedItem?.title,
        name: workflowNodeName,
        taskDispatcher: true,
        type: `${loopDefinition.name}/v${loopDefinition.version}`,
        workflowNodeName,
    };

    const {tasks} = workflow;

    let taskIndex = tasks?.length;

    if (edge && tasks) {
        sourceNodeId = sourceNodeId.split('=>')[0];

        taskIndex = tasks.findIndex((task) => task.name === sourceNodeId) + 1;
    }

    let loopId: string | undefined;

    if (sourceNodeId.includes('loop') && sourceNodeId.includes('placeholder')) {
        loopId = sourceNodeId.split('-')[0];

        if (!loopId || !tasks) {
            return;
        }

        taskIndex = tasks.findIndex((task) => task.name === loopId) + 1;
    }

    saveWorkflowDefinition({
        loopId,
        nodeData: {
            ...newLoopNodeData,
            parameters: {
                ...getParametersWithDefaultValues({
                    properties: loopDefinition?.properties as Array<PropertyAllType>,
                }),
            },
        },
        nodeIndex: taskIndex,
        onSuccess: () =>
            handleComponentAddedSuccess({
                nodeData: newLoopNodeData,
                queryClient,
                workflow,
            }),
        placeholderId: sourceNodeId,
        queryClient,
        updateWorkflowMutation,
    });
}
