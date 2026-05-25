import {ComponentConnection, Workflow, WorkflowInput} from '@/shared/middleware/automation/configuration';

export interface SubflowDuplicateStubI {
    subflowWorkflowUuid: string;
    subflowWorkflowUuidPath: string[];
}

const collectSubflowConnections = (
    subflowWorkflow: Workflow,
    workflows: Workflow[],
    parentUuidPath: string[],
    seenUuids: Set<string>,
    stubs: SubflowDuplicateStubI[]
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

            if (seenUuids.has(nestedWorkflowUuid)) {
                stubs.push({
                    subflowWorkflowUuid: nestedWorkflowUuid,
                    subflowWorkflowUuidPath: [...parentUuidPath, nestedWorkflowUuid],
                });

                continue;
            }

            const nestedWorkflow = workflows.find((workflow) => workflow.workflowUuid === nestedWorkflowUuid);

            if (!nestedWorkflow) {
                continue;
            }

            seenUuids.add(nestedWorkflowUuid);

            nestedConnections.push(
                ...collectSubflowConnections(
                    nestedWorkflow,
                    workflows,
                    [...parentUuidPath, nestedWorkflowUuid],
                    seenUuids,
                    stubs
                )
            );
        }
    }

    return [...ownConnections, ...nestedConnections];
};

const walkWorkflowConnections = (
    workflow: Workflow,
    workflows?: Workflow[]
): {connections: ComponentConnection[]; stubs: SubflowDuplicateStubI[]} => {
    const taskConnections = workflow.tasks?.flatMap((task) => task.connections ?? []) ?? [];
    const triggerConnections = workflow.triggers?.flatMap((trigger) => trigger.connections ?? []) ?? [];

    const subflows = workflow.tasks?.filter((task) => task.type.startsWith('subflow') && task.parameters?.workflowUuid);

    const subflowConnections: ComponentConnection[] = [];
    const seenUuids = new Set<string>();
    const stubs: SubflowDuplicateStubI[] = [];

    if (subflows?.length && workflows?.length) {
        for (const subflow of subflows) {
            const subflowWorkflowUuid = subflow.parameters?.workflowUuid;

            if (!subflowWorkflowUuid) {
                continue;
            }

            if (seenUuids.has(subflowWorkflowUuid)) {
                stubs.push({
                    subflowWorkflowUuid,
                    subflowWorkflowUuidPath: [subflowWorkflowUuid],
                });

                continue;
            }

            const subflowWorkflow = workflows.find((workflow) => workflow.workflowUuid === subflowWorkflowUuid);

            if (!subflowWorkflow) {
                continue;
            }

            seenUuids.add(subflowWorkflowUuid);

            subflowConnections.push(
                ...collectSubflowConnections(subflowWorkflow, workflows, [subflowWorkflowUuid], seenUuids, stubs)
            );
        }
    }

    return {connections: [...taskConnections, ...triggerConnections, ...subflowConnections], stubs};
};

const getWorkflowComponentConnections = (workflow: Workflow, workflows?: Workflow[]): ComponentConnection[] =>
    walkWorkflowConnections(workflow, workflows).connections;

export const getSubflowConnectionStubs = (workflow: Workflow, workflows?: Workflow[]): SubflowDuplicateStubI[] =>
    walkWorkflowConnections(workflow, workflows).stubs;

const collectSubflowInputs = (
    subflowWorkflow: Workflow,
    workflows: Workflow[],
    parentUuidPath: string[],
    seenUuids: Set<string>,
    stubs: SubflowDuplicateStubI[]
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

            if (seenUuids.has(nestedWorkflowUuid)) {
                stubs.push({
                    subflowWorkflowUuid: nestedWorkflowUuid,
                    subflowWorkflowUuidPath: [...parentUuidPath, nestedWorkflowUuid],
                });

                continue;
            }

            const nestedWorkflow = workflows.find((workflow) => workflow.workflowUuid === nestedWorkflowUuid);

            if (!nestedWorkflow) {
                continue;
            }

            seenUuids.add(nestedWorkflowUuid);

            nestedInputs.push(
                ...collectSubflowInputs(
                    nestedWorkflow,
                    workflows,
                    [...parentUuidPath, nestedWorkflowUuid],
                    seenUuids,
                    stubs
                )
            );
        }
    }

    return [...ownInputs, ...nestedInputs];
};

const walkWorkflowInputs = (
    workflow: Workflow,
    workflows?: Workflow[]
): {inputs: WorkflowInput[]; stubs: SubflowDuplicateStubI[]} => {
    const ownInputs = workflow.inputs ?? [];

    const subflows = workflow.tasks?.filter((task) => task.type.startsWith('subflow') && task.parameters?.workflowUuid);

    const subflowInputs: WorkflowInput[] = [];
    const seenUuids = new Set<string>();
    const stubs: SubflowDuplicateStubI[] = [];

    if (subflows?.length && workflows?.length) {
        for (const subflow of subflows) {
            const subflowWorkflowUuid = subflow.parameters?.workflowUuid;

            if (!subflowWorkflowUuid) {
                continue;
            }

            if (seenUuids.has(subflowWorkflowUuid)) {
                stubs.push({
                    subflowWorkflowUuid,
                    subflowWorkflowUuidPath: [subflowWorkflowUuid],
                });

                continue;
            }

            const subflowWorkflow = workflows.find((workflow) => workflow.workflowUuid === subflowWorkflowUuid);

            if (!subflowWorkflow) {
                continue;
            }

            seenUuids.add(subflowWorkflowUuid);

            subflowInputs.push(
                ...collectSubflowInputs(subflowWorkflow, workflows, [subflowWorkflowUuid], seenUuids, stubs)
            );
        }
    }

    return {inputs: [...ownInputs, ...subflowInputs], stubs};
};

export const getWorkflowInputs = (workflow: Workflow, workflows?: Workflow[]): WorkflowInput[] =>
    walkWorkflowInputs(workflow, workflows).inputs;

export const getSubflowInputStubs = (workflow: Workflow, workflows?: Workflow[]): SubflowDuplicateStubI[] =>
    walkWorkflowInputs(workflow, workflows).stubs;

export default getWorkflowComponentConnections;
