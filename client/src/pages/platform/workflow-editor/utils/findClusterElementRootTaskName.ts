import {WorkflowTask} from '@/shared/middleware/platform/configuration';

interface ClusterElementLikeI {
    clusterElements?: unknown;
    name?: string;
    workflowNodeName?: string;
}

function containsClusterElement(clusterElements: unknown, elementName: string): boolean {
    if (!clusterElements || typeof clusterElements !== 'object') {
        return false;
    }

    return Object.values(clusterElements as Record<string, unknown>).some((value) => {
        const elements = Array.isArray(value) ? value : [value];

        return elements.some((element) => {
            if (!element || typeof element !== 'object') {
                return false;
            }

            const clusterElement = element as ClusterElementLikeI;

            return (
                clusterElement.workflowNodeName === elementName ||
                clusterElement.name === elementName ||
                containsClusterElement(clusterElement.clusterElements, elementName)
            );
        });
    });
}

export default function findClusterElementRootTaskName(
    tasks: Array<WorkflowTask> | undefined,
    elementName: string
): string | undefined {
    return tasks?.find((task) => containsClusterElement(task.clusterElements, elementName))?.name;
}
