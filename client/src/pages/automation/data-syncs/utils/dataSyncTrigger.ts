import {describeCadence, fromCadenceParameters} from '@/pages/automation/agents/utils/agentScheduleCron';
import {DataSyncTriggerType} from '@/shared/middleware/graphql';

export function describeTrigger(
    triggerType: DataSyncTriggerType,
    triggerParameters: Record<string, unknown> | null | undefined
): string {
    if (triggerType === DataSyncTriggerType.Manual) {
        return 'Manual';
    }

    const parameters = triggerParameters ?? {};

    return describeCadence(fromCadenceParameters(parameters)) || String(parameters.expression ?? 'Scheduled');
}
