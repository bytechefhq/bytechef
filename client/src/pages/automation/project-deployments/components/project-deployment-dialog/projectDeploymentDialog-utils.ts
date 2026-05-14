import {ComponentConnection, Workflow} from '@/shared/middleware/automation/configuration';

const getWorkflowComponentConnections = (workflow: Workflow): ComponentConnection[] => [
    ...(workflow?.tasks ?? []).flatMap((task) => task.connections ?? []),
    ...(workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []),
];

export default getWorkflowComponentConnections;
