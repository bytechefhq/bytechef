import {NodeDataType} from '@/shared/types';

export default function resolveTargetTriggerName(targetNodeData: NodeDataType): string | undefined {
    const isManualPlaceholder = targetNodeData.componentName === 'manual' && targetNodeData.operationName === 'manual';

    return isManualPlaceholder ? 'trigger_1' : targetNodeData.name;
}
