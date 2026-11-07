import {TASK_DISPATCHER_NAMES} from '@/shared/constants';

import {getGraphFrameId} from './graph/graphFrameGeometry';

export function toWorkflowNodeNamePrefix(componentName: string): string {
    return componentName.replace(/-+([a-zA-Z0-9])/g, (_match, character: string) => character.toUpperCase());
}

const COMPONENT_NAME_BY_NODE_NAME_PREFIX: Record<string, string> = Object.fromEntries(
    TASK_DISPATCHER_NAMES.map((taskDispatcherName) => [
        toWorkflowNodeNamePrefix(taskDispatcherName),
        taskDispatcherName,
    ])
);

export function getWorkflowNodeComponentName(workflowNodeName: string): string {
    const nodeNamePrefix = workflowNodeName.split('_')[0];

    return COMPONENT_NAME_BY_NODE_NAME_PREFIX[nodeNamePrefix] || nodeNamePrefix;
}

export function getNestedBottomGhostId(taskNodeId: string): string {
    const componentName = getWorkflowNodeComponentName(taskNodeId);

    if (componentName === 'graph') {
        return getGraphFrameId(taskNodeId);
    }

    return `${taskNodeId}-${toWorkflowNodeNamePrefix(componentName)}-bottom-ghost`;
}
