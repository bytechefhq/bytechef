import {Workflow} from '@/shared/middleware/platform/configuration';

const getWorkflowComponentNames = (workflow: Workflow): string[] => {
    const componentNames = new Set<string>([
        ...(workflow.workflowTaskComponentNames ?? []),
        ...(workflow.workflowTriggerComponentNames ?? []),
    ]);

    return Array.from(componentNames).sort();
};

export default getWorkflowComponentNames;
