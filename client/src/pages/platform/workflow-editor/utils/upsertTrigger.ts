import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function upsertTrigger(triggers: WorkflowTrigger[], newTrigger: WorkflowTrigger): WorkflowTrigger[] {
    const existingIndex = triggers.findIndex((trigger) => trigger.name === newTrigger.name);

    if (existingIndex === -1) {
        return [...triggers, newTrigger];
    }

    const existingTrigger = triggers[existingIndex];

    const keptClusterElements = existingTrigger.type === newTrigger.type ? existingTrigger.clusterElements : undefined;

    const mergedTrigger: WorkflowTrigger = {
        ...newTrigger,
        clusterElements: newTrigger.clusterElements ?? keptClusterElements,
        metadata: newTrigger.metadata ?? existingTrigger.metadata,
    };

    return [...triggers.slice(0, existingIndex), mergedTrigger, ...triggers.slice(existingIndex + 1)];
}
