import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';

interface GetBoxModeClusterRootIdsProps {
    clusterElementsViewMode: string;
    collapsedClusterRootIds?: Record<string, boolean>;
    tasks?: Array<WorkflowTask>;
    triggers?: Array<WorkflowTrigger>;
}

export default function getBoxModeClusterRootIds({
    clusterElementsViewMode,
    collapsedClusterRootIds,
    tasks,
    triggers,
}: GetBoxModeClusterRootIdsProps): Array<string> {
    if (clusterElementsViewMode !== 'box') {
        return [];
    }

    return [...(triggers ?? []), ...(tasks ?? [])]
        .filter((workflowNode) => workflowNode.clusterRoot && !collapsedClusterRootIds?.[workflowNode.name])
        .map((workflowNode) => workflowNode.name);
}
