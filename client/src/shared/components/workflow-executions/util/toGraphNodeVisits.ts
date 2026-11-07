import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';

const GRAPH_TYPE_PREFIX = 'graph/';

export interface GraphNodeVisitI {
    nodeName: string;
    taskExecution: TaskExecution;
    visitNumber: number;
}

export function isGraphTaskExecution(taskExecution: TaskExecution): boolean {
    return !!taskExecution.type?.startsWith(GRAPH_TYPE_PREFIX);
}

export function toGraphNodeVisits(children: TaskExecution[]): GraphNodeVisitI[] {
    const visitCountByNodeName = new Map<string, number>();

    return children.map((child) => {
        const nodeName = child.workflowTask?.name ?? 'unknown';
        const visitNumber = (visitCountByNodeName.get(nodeName) ?? 0) + 1;

        visitCountByNodeName.set(nodeName, visitNumber);

        return {nodeName, taskExecution: child, visitNumber};
    });
}
