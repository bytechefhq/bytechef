import {CLUSTER_ELEMENT_TYPE_TOOLS} from '@/shared/constants';

interface ResolveShowOutputTabProps {
    clusterElementType?: string;
    clusterRootWorkflowNodeName?: string;
    operationDefinition?: {outputDefined?: boolean};
    taskDispatcher?: boolean;
}

export default function resolveShowOutputTab({
    clusterElementType,
    clusterRootWorkflowNodeName,
    operationDefinition,
    taskDispatcher,
}: ResolveShowOutputTabProps): boolean {
    if (clusterElementType && clusterElementType !== CLUSTER_ELEMENT_TYPE_TOOLS) {
        return false;
    }

    if (clusterElementType === CLUSTER_ELEMENT_TYPE_TOOLS && !clusterRootWorkflowNodeName) {
        return false;
    }

    if (taskDispatcher && operationDefinition) {
        return !!operationDefinition.outputDefined;
    }

    return true;
}
