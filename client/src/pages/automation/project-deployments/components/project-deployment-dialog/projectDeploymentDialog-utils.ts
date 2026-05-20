import {ComponentConnection, Workflow} from '@/shared/middleware/automation/configuration';

const getWorkflowComponentConnections = (workflow: Workflow, workflows?: Workflow[]): ComponentConnection[] => {
    const taskConnections = workflow.tasks?.flatMap((task) => task.connections ?? []) ?? [];
    const triggerConnections = workflow.triggers?.flatMap((trigger) => trigger.connections ?? []) ?? [];

    const subflows = workflow.tasks?.filter((task) => task.type.startsWith('subflow') && task.parameters?.workflowUuid);

    const subflowConnectionsByWorkflowUuid = new Map<string, ComponentConnection[]>();

    if (subflows?.length && workflows?.length) {
        subflows.forEach((subflow) => {
            const subflowWorkflowUuid = subflow.parameters?.workflowUuid;

            if (!subflowWorkflowUuid) {
                return;
            }

            const subflowWorkflow = workflows.find((workflow) => workflow.workflowUuid === subflowWorkflowUuid);

            if (!subflowWorkflow) {
                return;
            }

            const subflowTaskConnections = subflowWorkflow.tasks?.flatMap((task) => task.connections ?? []) ?? [];
            const subflowTriggerConnections =
                subflowWorkflow.triggers?.flatMap((trigger) => trigger.connections ?? []) ?? [];

            subflowConnectionsByWorkflowUuid.set(subflowWorkflowUuid, [
                ...subflowTaskConnections,
                ...subflowTriggerConnections,
            ]);
        });
    }

    const subflowConnections: ComponentConnection[] = [];

    subflowConnectionsByWorkflowUuid.forEach((connections, workflowUuid) => {
        connections.forEach((connection) => {
            subflowConnections.push({
                ...connection,
                subflowWorkflowUuid: workflowUuid,
            });
        });
    });

    return [...taskConnections, ...triggerConnections, ...subflowConnections];
};

export default getWorkflowComponentConnections;
