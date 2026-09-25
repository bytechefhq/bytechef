import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import {WorkflowIssueI} from '../stores/useWorkflowIssuesStore';
import {forEachNestedTaskGroup} from './taskTraversalUtils';

function collectClusterElementRootNames(
    clusterElements: unknown,
    clusterRootName: string,
    clusterElementRootNames: Map<string, string>
): void {
    if (!clusterElements || typeof clusterElements !== 'object') {
        return;
    }

    for (const clusterElementValue of Object.values(clusterElements)) {
        const clusterElementItems = Array.isArray(clusterElementValue) ? clusterElementValue : [clusterElementValue];

        for (const clusterElementItem of clusterElementItems) {
            if (!clusterElementItem || typeof clusterElementItem !== 'object') {
                continue;
            }

            for (const clusterElementName of [clusterElementItem.name, clusterElementItem.workflowNodeName]) {
                if (typeof clusterElementName === 'string') {
                    clusterElementRootNames.set(clusterElementName, clusterRootName);
                }
            }

            collectClusterElementRootNames(
                clusterElementItem.clusterElements,
                clusterRootName,
                clusterElementRootNames
            );
        }
    }
}

/**
 * Maps every cluster element name in the workflow to the name of the task that is its main cluster root.
 */
export function getClusterElementRootNames(tasks: Array<WorkflowTask> = []): Map<string, string> {
    const clusterElementRootNames = new Map<string, string>();

    const collectFromTasks = (currentTasks: Array<WorkflowTask>) => {
        for (const currentTask of currentTasks) {
            collectClusterElementRootNames(currentTask.clusterElements, currentTask.name, clusterElementRootNames);

            if (currentTask.parameters) {
                forEachNestedTaskGroup(currentTask.parameters as Record<string, unknown>, collectFromTasks);
            }
        }
    };

    collectFromTasks(tasks);

    return clusterElementRootNames;
}

/**
 * The validator reports cluster element issues under the cluster root's name, with a property path that starts with
 * the element names leading to the offending element (e.g. `openAi_1.model` or `rag_1.vectorStore_1.index`). The
 * deepest cluster element name at the start of the path owns the issue; otherwise the reported node owns it.
 */
export default function getWorkflowIssueOwnerName(
    issue: Pick<WorkflowIssueI, 'nodeName' | 'propertyPath'>,
    clusterElementRootNames: ReadonlyMap<string, string>
): string {
    let ownerName = issue.nodeName;

    if (!issue.propertyPath) {
        return ownerName;
    }

    for (const pathSegment of issue.propertyPath.split('.')) {
        if (!clusterElementRootNames.has(pathSegment)) {
            break;
        }

        ownerName = pathSegment;
    }

    return ownerName;
}

export function getWorkflowIssueOwnerPropertyPath(
    issue: Pick<WorkflowIssueI, 'propertyPath'>,
    clusterElementRootNames: ReadonlyMap<string, string>
): string | undefined {
    if (!issue.propertyPath) {
        return undefined;
    }

    const pathSegments = issue.propertyPath.split('.');

    let ownerSegmentCount = 0;

    while (ownerSegmentCount < pathSegments.length && clusterElementRootNames.has(pathSegments[ownerSegmentCount])) {
        ownerSegmentCount++;
    }

    return pathSegments.slice(ownerSegmentCount).join('.') || undefined;
}
