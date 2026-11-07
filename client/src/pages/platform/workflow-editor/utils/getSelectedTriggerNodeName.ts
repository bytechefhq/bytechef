import {NodeDataType} from '@/shared/types';

export default function getSelectedTriggerNodeName({
    currentNode,
    workflowNodeDetailsPanelOpen,
}: {
    currentNode?: NodeDataType;
    workflowNodeDetailsPanelOpen: boolean;
}): string | undefined {
    return workflowNodeDetailsPanelOpen && currentNode?.trigger ? currentNode.workflowNodeName : undefined;
}
