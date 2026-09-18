import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function upsertTrigger(triggers: WorkflowTrigger[], newTrigger: WorkflowTrigger): WorkflowTrigger[] {
    const existingIndex = triggers.findIndex((trigger) => trigger.name === newTrigger.name);

    if (existingIndex === -1) {
        return [...triggers, newTrigger];
    }

    const mergedTrigger: WorkflowTrigger = {
        ...newTrigger,
        metadata: newTrigger.metadata ?? triggers[existingIndex].metadata,
    };

    return [...triggers.slice(0, existingIndex), mergedTrigger, ...triggers.slice(existingIndex + 1)];
}
