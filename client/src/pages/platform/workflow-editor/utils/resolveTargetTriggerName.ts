import {NodeDataType} from '@/shared/types';

export default function resolveTargetTriggerName(targetNodeData: NodeDataType): string | undefined {
    return targetNodeData.workflowNodeName ?? targetNodeData.name;
}
