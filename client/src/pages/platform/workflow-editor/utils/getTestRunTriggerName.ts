import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function getTestRunTriggerName(workflow: {triggers?: Array<Pick<WorkflowTrigger, 'name'>>}) {
    return workflow.triggers?.[0]?.name || 'trigger_1';
}
