import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

import {getNestedClusterElements, isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

export type ClusterRootOutcomeType = 'BLOCKED' | 'WITHHELD';

/**
 * The guardrail outcome a cluster root card announces: blocked when any element, at any depth, was blocked, otherwise
 * withheld when any was withheld. Failed and redacted elements never count, the agent itself carries those.
 */
export default function getClusterRootOutcome(
    clusterElements: ClusterElementsType | undefined,
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>
): ClusterRootOutcomeType | undefined {
    let isWithheld = false;
    let isBlocked = false;

    const visit = (currentClusterElements: ClusterElementsType | undefined) => {
        if (!currentClusterElements) {
            return;
        }

        const visitElement = (element: ClusterElementItemType) => {
            const elementName = (element as {workflowNodeName?: string}).workflowNodeName ?? element.name;
            const status = workflowTestNodeStates[elementName]?.status;

            if (status === 'BLOCKED') {
                isBlocked = true;
            } else if (status === 'WITHHELD') {
                isWithheld = true;
            }

            visit(getNestedClusterElements(element));
        };

        Object.values(currentClusterElements).forEach((element) => {
            if (Array.isArray(element)) {
                element.forEach(visitElement);
            } else if (isPlainObject(element)) {
                visitElement(element as ClusterElementItemType);
            }
        });
    };

    visit(clusterElements);

    if (isBlocked) {
        return 'BLOCKED';
    }

    return isWithheld ? 'WITHHELD' : undefined;
}
