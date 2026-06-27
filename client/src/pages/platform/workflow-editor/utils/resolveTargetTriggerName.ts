import {NodeDataType} from '@/shared/types';

/**
 * Resolves the trigger name to upsert when a trigger component is dropped onto an
 * existing trigger node.
 *
 * The synthetic "Manual" placeholder (rendered when a workflow has no real trigger)
 * carries `componentName`/`operationName` of `'manual'` and `data.name === 'manual'`,
 * but the first real trigger must be named `trigger_1`. Dropping onto it therefore
 * resolves to `trigger_1` (an append that creates the first trigger), NOT a trigger
 * literally named `manual`. Dropping onto a real trigger reuses its own name so the
 * upsert replaces it in place.
 */
export default function resolveTargetTriggerName(targetNodeData: NodeDataType): string | undefined {
    const isManualPlaceholder = targetNodeData.componentName === 'manual' && targetNodeData.operationName === 'manual';

    return isManualPlaceholder ? 'trigger_1' : targetNodeData.name;
}
