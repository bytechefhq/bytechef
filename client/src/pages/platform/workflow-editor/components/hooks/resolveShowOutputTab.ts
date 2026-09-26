import {CLUSTER_ELEMENT_TYPE_TOOLS} from '@/shared/constants';

interface ResolveShowOutputTabProps {
    clusterElementType?: string;
    clusterRootWorkflowNodeName?: string;
    operationDefinition?: {outputDefined?: boolean};
    taskDispatcher?: boolean;
}

/**
 * Decides whether the node details panel shows the Output tab. The tab also carries the Test button,
 * so an action, trigger or tool that declares no output keeps it: running it is still how its side
 * effects are exercised from the editor. Only a task dispatcher without a declared output hides it,
 * because dispatchers are never tested on their own.
 */
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
