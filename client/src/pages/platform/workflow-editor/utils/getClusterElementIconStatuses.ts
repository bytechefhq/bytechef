import {ClusterElementItemType, ClusterElementsType} from '@/shared/types';

import {
    getNestedClusterElements,
    getTypeSegments,
    isPlainObject,
} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

type NodeStatusType = WorkflowTestNodeStateI['status'];

const STATUS_PRIORITY: Record<NodeStatusType, number> = {
    COMPLETED: 0,
    FAILED: 2,
    RUNNING: 1,
};

/**
 * The status each cluster element icon of a compact cluster root card shows, keyed by icon URL. The card draws one icon
 * per component, so an icon takes the worst status of the elements behind it: failed, then running, then completed.
 */
export default function getClusterElementIconStatuses(
    clusterElements: ClusterElementsType | undefined,
    workflowTestNodeStates: Record<string, WorkflowTestNodeStateI>
): Record<string, NodeStatusType> {
    const iconStatuses: Record<string, NodeStatusType> = {};

    const visit = (currentClusterElements: ClusterElementsType | undefined) => {
        if (!currentClusterElements) {
            return;
        }

        const visitElement = (element: ClusterElementItemType) => {
            const elementName = (element as {workflowNodeName?: string}).workflowNodeName ?? element.name;
            const status = workflowTestNodeStates[elementName]?.status;

            if (status) {
                const icon = `/icons/${getTypeSegments(element.type).componentName}.svg`;
                const currentStatus = iconStatuses[icon];

                if (!currentStatus || STATUS_PRIORITY[status] > STATUS_PRIORITY[currentStatus]) {
                    iconStatuses[icon] = status;
                }
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

    return iconStatuses;
}
