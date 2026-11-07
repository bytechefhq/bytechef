import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';

import {getTask} from './getTask';

interface GetClusterRootTaskProps {
    tasks?: Array<WorkflowTask>;
    triggers?: Array<WorkflowTrigger>;
    workflowNodeName: string;
}

export function getClusterRootTask({
    tasks,
    triggers,
    workflowNodeName,
}: GetClusterRootTaskProps): WorkflowTask | undefined {
    const task = getTask({tasks: tasks ?? [], workflowNodeName});

    if (task) {
        return task;
    }

    return (triggers ?? []).find((trigger) => trigger?.name === workflowNodeName) as WorkflowTask | undefined;
}
