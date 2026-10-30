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
