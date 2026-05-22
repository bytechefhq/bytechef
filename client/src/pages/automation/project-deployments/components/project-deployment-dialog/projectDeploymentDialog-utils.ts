import {ComponentConnection, Workflow, WorkflowInput} from '@/shared/middleware/automation/configuration';

// Walks a subflow workflow and all of its nested subflows, tagging every collected connection
// with the chain of subflow workflow uuids leading to it (root subflow -> leaf subflow). The
// path lets the UI rebuild the nested grouping tree. The path itself acts as the cycle guard:
// a subflow is skipped only if it already appears in its own ancestor chain, so the same
// workflow may still appear in separate sibling branches.
const collectSubflowConnections = (
    subflowWorkflow: Workflow,
    workflows: Workflow[],
    parentUuidPath: string[]
): ComponentConnection[] => {
    const taskConnections = subflowWorkflow.tasks?.flatMap((task) => task.connections ?? []) ?? [];
    const triggerConnections = subflowWorkflow.triggers?.flatMap((trigger) => trigger.connections ?? []) ?? [];

    const ownConnections: ComponentConnection[] = [...taskConnections, ...triggerConnections].map((connection) => ({
        ...connection,
        subflowWorkflowUuid: parentUuidPath[parentUuidPath.length - 1],
        subflowWorkflowUuidPath: parentUuidPath,
    }));

    const nestedSubflows = subflowWorkflow.tasks?.filter(
        (task) => task.type.startsWith('subflow') && task.parameters?.workflowUuid
    );

    const nestedConnections: ComponentConnection[] = [];

    if (nestedSubflows?.length) {
        for (const nestedSubflow of nestedSubflows) {
            const nestedWorkflowUuid = nestedSubflow.parameters?.workflowUuid;

            if (!nestedWorkflowUuid || parentUuidPath.includes(nestedWorkflowUuid)) {
                continue;
            }

            const nestedWorkflow = workflows.find((workflow) => workflow.workflowUuid === nestedWorkflowUuid);

            if (!nestedWorkflow) {
                continue;
            }

            nestedConnections.push(
                ...collectSubflowConnections(nestedWorkflow, workflows, [...parentUuidPath, nestedWorkflowUuid])
            );
        }
    }

    return [...ownConnections, ...nestedConnections];
};

const getWorkflowComponentConnections = (workflow: Workflow, workflows?: Workflow[]): ComponentConnection[] => {
    const taskConnections = workflow.tasks?.flatMap((task) => task.connections ?? []) ?? [];
    const triggerConnections = workflow.triggers?.flatMap((trigger) => trigger.connections ?? []) ?? [];

    const subflows = workflow.tasks?.filter((task) => task.type.startsWith('subflow') && task.parameters?.workflowUuid);

    const subflowConnections: ComponentConnection[] = [];

    if (subflows?.length && workflows?.length) {
        for (const subflow of subflows) {
            const subflowWorkflowUuid = subflow.parameters?.workflowUuid;

            if (!subflowWorkflowUuid) {
                continue;
            }

            const subflowWorkflow = workflows.find((workflow) => workflow.workflowUuid === subflowWorkflowUuid);

            if (!subflowWorkflow) {
                continue;
            }

            subflowConnections.push(...collectSubflowConnections(subflowWorkflow, workflows, [subflowWorkflowUuid]));
        }
    }

    return [...taskConnections, ...triggerConnections, ...subflowConnections];
};

// Walks a subflow workflow and all of its nested subflows, tagging every collected input with the
// chain of subflow workflow uuids leading to it (root subflow -> leaf subflow). Each subflow's own
// inputs come from its `inputs` property. Mirrors collectSubflowConnections: the path acts as the
// cycle guard, so the same workflow may still appear in separate sibling branches.
const collectSubflowInputs = (
    subflowWorkflow: Workflow,
    workflows: Workflow[],
    parentUuidPath: string[]
): WorkflowInput[] => {
    const ownInputs: WorkflowInput[] = (subflowWorkflow.inputs ?? []).map((input) => ({
        ...input,
        subflowWorkflowUuid: parentUuidPath[parentUuidPath.length - 1],
        subflowWorkflowUuidPath: parentUuidPath,
    }));

    const nestedSubflows = subflowWorkflow.tasks?.filter(
        (task) => task.type.startsWith('subflow') && task.parameters?.workflowUuid
    );

    const nestedInputs: WorkflowInput[] = [];

    if (nestedSubflows?.length) {
        for (const nestedSubflow of nestedSubflows) {
            const nestedWorkflowUuid = nestedSubflow.parameters?.workflowUuid;

            if (!nestedWorkflowUuid || parentUuidPath.includes(nestedWorkflowUuid)) {
                continue;
            }

            const nestedWorkflow = workflows.find((workflow) => workflow.workflowUuid === nestedWorkflowUuid);

            if (!nestedWorkflow) {
                continue;
            }

            nestedInputs.push(
                ...collectSubflowInputs(nestedWorkflow, workflows, [...parentUuidPath, nestedWorkflowUuid])
            );
        }
    }

    return [...ownInputs, ...nestedInputs];
};

export const getWorkflowInputs = (workflow: Workflow, workflows?: Workflow[]): WorkflowInput[] => {
    const ownInputs = workflow.inputs ?? [];

    const subflows = workflow.tasks?.filter((task) => task.type.startsWith('subflow') && task.parameters?.workflowUuid);

    const subflowInputs: WorkflowInput[] = [];

    if (subflows?.length && workflows?.length) {
        for (const subflow of subflows) {
            const subflowWorkflowUuid = subflow.parameters?.workflowUuid;

            if (!subflowWorkflowUuid) {
                continue;
            }

            const subflowWorkflow = workflows.find((workflow) => workflow.workflowUuid === subflowWorkflowUuid);

            if (!subflowWorkflow) {
                continue;
            }

            subflowInputs.push(...collectSubflowInputs(subflowWorkflow, workflows, [subflowWorkflowUuid]));
        }
    }

    return [...ownInputs, ...subflowInputs];
};

export default getWorkflowComponentConnections;
