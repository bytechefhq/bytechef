import {Workflow} from '@/shared/middleware/automation/configuration';
import {
    ClickedDefinitionType,
    NodeDataType,
    PropertyAllType,
    TaskDispatcherContextType,
    UpdateWorkflowMutationType,
} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {Edge} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {WorkflowDataType} from '../stores/useWorkflowDataStore';
import calculateNodeInsertIndex from './calculateNodeInsertIndex';
import getFormattedName from './getFormattedName';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

const fallbackIcon = <ComponentIcon className="size-9 text-gray-700" />;

interface HandleTaskDispatcherClickProps {
    edge?: Edge;
    invalidateWorkflowQueries: () => void;
    queryClient: QueryClient;
    sourceNodeId?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    taskDispatcherDefinition: ClickedDefinitionType;
    taskDispatcherName: keyof typeof TASK_DISPATCHER_CONFIG;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow & WorkflowDataType;
}

export default async function handleTaskDispatcherClick({
    edge,
    invalidateWorkflowQueries,
    queryClient,
    sourceNodeId,
    taskDispatcherContext,
    taskDispatcherDefinition,
    taskDispatcherName,
    updateWorkflowMutation,
    workflow,
}: HandleTaskDispatcherClickProps) {
    const config = TASK_DISPATCHER_CONFIG[taskDispatcherName];

    if (!config) {
        console.error(`Unknown task dispatcher type: ${taskDispatcherName}`);

        return;
    }

    const {icon, name, title, version} = taskDispatcherDefinition;

    const workflowNodeName = getFormattedName(taskDispatcherDefinition.name!);

    const newNodeData: NodeDataType = {
        ...taskDispatcherDefinition,
        clusterElements: undefined,
        componentName: name,
        description: undefined,
        icon: icon ? <InlineSVG className="size-9 text-gray-700" src={icon} /> : fallbackIcon,
        label: title,
        name: workflowNodeName,
        taskDispatcher: true,
        type: `${name}/v${version}`,
        workflowNodeName,
    };

    if (taskDispatcherContext) {
        if (taskDispatcherContext.conditionId) {
            newNodeData.conditionData = {
                conditionCase: taskDispatcherContext.conditionCase as 'caseTrue' | 'caseFalse',
                conditionId: taskDispatcherContext.conditionId as string,
                index: (taskDispatcherContext.index ?? 0) as number,
            };
        } else if (taskDispatcherContext.loopId) {
            newNodeData.loopData = {
                index: (taskDispatcherContext.index ?? 0) as number,
                loopId: taskDispatcherContext.loopId as string,
            };
        } else if (taskDispatcherContext.branchId) {
            newNodeData.branchData = {
                branchId: taskDispatcherContext.branchId as string,
                caseKey: taskDispatcherContext.caseKey as string,
                index: (taskDispatcherContext.index ?? 0) as number,
            };
        } else if (taskDispatcherContext.parallelId) {
            newNodeData.parallelData = {
                index: (taskDispatcherContext.index ?? 0) as number,
                parallelId: taskDispatcherContext.parallelId as string,
            };
        } else if (taskDispatcherContext.eachId) {
            newNodeData.eachData = {
                eachId: taskDispatcherContext.eachId as string,
                index: (taskDispatcherContext.index ?? 0) as number,
            };
        }
    }

    const hasTaskDispatcherId = Object.entries(taskDispatcherContext ?? {}).some(
        ([key, value]) => key.endsWith('Id') && !!value
    );

    let nodeIndex = workflow.tasks?.length;

    if (hasTaskDispatcherId) {
        nodeIndex = taskDispatcherContext?.index;
    }

    if (edge) {
        nodeIndex = calculateNodeInsertIndex(edge.target);
    }

    saveWorkflowDefinition({
        invalidateWorkflowQueries,
        nodeData: {
            ...newNodeData,
            parameters: config.getInitialParameters(taskDispatcherDefinition?.properties as Array<PropertyAllType>),
            workflowNodeName,
        },
        nodeIndex,
        onSuccess: () =>
            handleComponentAddedSuccess({
                nodeData: newNodeData,
                queryClient,
                workflow,
            }),
        placeholderId: hasTaskDispatcherId ? undefined : sourceNodeId,
        taskDispatcherContext,
        updateWorkflowMutation,
    });
}
