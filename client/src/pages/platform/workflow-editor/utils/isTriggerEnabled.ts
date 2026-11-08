import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function isTriggerEnabled(trigger: Pick<WorkflowTrigger, 'disabled' | 'type'>): boolean {
    return !trigger.disabled || trigger.type?.split('/')[0] === 'manual';
}
