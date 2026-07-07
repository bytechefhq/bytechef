import {
    ComponentConnection,
    ProjectDeploymentWorkflow,
    ProjectDeploymentWorkflowConnection,
    Workflow,
    WorkflowInput,
} from '@/shared/middleware/automation/configuration';

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

const prefixSubflowInputName = (name: string, subflowWorkflowUuid: string): string => `${subflowWorkflowUuid}::${name}`;

const collectSubflowInputs = (
    subflowWorkflow: Workflow,
    workflows: Workflow[],
    parentUuidPath: string[],
    seenUuids: Set<string>,
    stubs: SubflowDuplicateStubI[]
): WorkflowInput[] => {
    const subflowWorkflowUuid = parentUuidPath[parentUuidPath.length - 1];

    const ownInputs: WorkflowInput[] = (subflowWorkflow.inputs ?? []).map((input) => ({
        ...input,
        label: input.label || input.name,
        name: input.name ? prefixSubflowInputName(input.name, subflowWorkflowUuid) : input.name,
        subflowInputName: input.name,
        subflowWorkflowUuid,
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

const collectReachableSubflowUuids = (workflow: Workflow, workflows: Workflow[], reachable: Set<string>): void => {
    const subflows = workflow.tasks?.filter((task) => task.type.startsWith('subflow') && task.parameters?.workflowUuid);

    if (!subflows?.length) {
        return;
    }

    for (const subflow of subflows) {
        const subflowWorkflowUuid = subflow.parameters?.workflowUuid;

        if (!subflowWorkflowUuid || reachable.has(subflowWorkflowUuid)) {
            continue;
        }

        reachable.add(subflowWorkflowUuid);

        const subflowWorkflow = workflows.find((candidate) => candidate.workflowUuid === subflowWorkflowUuid);

        if (subflowWorkflow) {
            collectReachableSubflowUuids(subflowWorkflow, workflows, reachable);
        }
    }
};

/**
 * Returns the workflowUuid set of every subflow transitively referenced by the given parent workflows. Subflows are
 * visually nested under their parent in the deployment dialog and have no enable toggle of their own, so they are
 * implicitly enabled whenever a parent is enabled.
 */
export const getReachableSubflowUuids = (parentWorkflows: Workflow[], workflows: Workflow[]): Set<string> => {
    const reachable = new Set<string>();

    for (const parentWorkflow of parentWorkflows) {
        collectReachableSubflowUuids(parentWorkflow, workflows, reachable);
    }

    return reachable;
};

export interface SubflowDeploymentValuesI {
    connections: ProjectDeploymentWorkflowConnection[];
    inputs: Record<string, unknown>;
}

/**
 * The deployment dialog renders a parent workflow together with all of its (transitive) subflow connections and inputs
 * as one merged list, so the form stores every value under the parent's projectDeploymentWorkflow entry. At submit time
 * each value must instead be attributed to the workflow it actually belongs to. This splits the parent's merged form
 * values back out per owning workflowUuid by aligning them against the same merged connection/input lists the UI built.
 *
 * The returned map is keyed by owning subflow workflowUuid; the parent's own values are returned separately. Connection
 * values are aligned positionally to the merged connection list (grouping is render-only, storage stays positional), so
 * a grouped connection lands on whichever workflow/subflow physically owns that node.
 */
export const splitSubflowDeploymentValues = (
    parentWorkflow: Workflow,
    workflows: Workflow[],
    formConnections: ProjectDeploymentWorkflowConnection[] | undefined,
    formInputs: Record<string, unknown> | undefined
): {ownValues: SubflowDeploymentValuesI; subflowValuesByUuid: Map<string, SubflowDeploymentValuesI>} => {
    const ownValues: SubflowDeploymentValuesI = {connections: [], inputs: {}};
    const subflowValuesByUuid = new Map<string, SubflowDeploymentValuesI>();

    const getBucket = (subflowWorkflowUuid?: string): SubflowDeploymentValuesI => {
        if (!subflowWorkflowUuid) {
            return ownValues;
        }

        let bucket = subflowValuesByUuid.get(subflowWorkflowUuid);

        if (!bucket) {
            bucket = {connections: [], inputs: {}};

            subflowValuesByUuid.set(subflowWorkflowUuid, bucket);
        }

        return bucket;
    };

    const mergedConnections = getWorkflowComponentConnections(parentWorkflow, workflows);

    mergedConnections.forEach((mergedConnection, index) => {
        const connectionId = formConnections?.[index]?.connectionId;

        if (connectionId == null) {
            return;
        }

        getBucket(mergedConnection.subflowWorkflowUuid).connections.push({
            connectionId,
            workflowConnectionKey: mergedConnection.key,
            workflowNodeName: mergedConnection.workflowNodeName,
        });
    });

    const mergedInputs = getWorkflowInputs(parentWorkflow, workflows);

    for (const mergedInput of mergedInputs) {
        if (!mergedInput.name || !(mergedInput.name in (formInputs ?? {}))) {
            continue;
        }

        const originalInputName = mergedInput.subflowInputName ?? mergedInput.name;

        getBucket(mergedInput.subflowWorkflowUuid).inputs[originalInputName] = formInputs![mergedInput.name];
    }

    return {ownValues, subflowValuesByUuid};
};

/**
 * A workflow is only a "subflow" (nested under a parent, no toggle of its own) if some workflow in this same set
 * actually invokes it via a `subflow` task. A workflow merely having the `workflow` component's "New Workflow Call"
 * trigger is not sufficient — MCP tool workflows use that trigger too, but are top-level entries, not subflows.
 */
const isSubflowWorkflow = (workflow: Workflow | undefined, reachableSubflowUuids: Set<string>): boolean =>
    !!workflow?.workflowUuid && reachableSubflowUuids.has(workflow.workflowUuid);

/**
 * Maps the form's projectDeploymentWorkflow entries into the array sent to the backend. Subflows are nested under their
 * parent in the dialog, so each subflow of an enabled parent is implicitly enabled and the parent's merged
 * connection/input values are redistributed to the workflow that actually owns them (see splitSubflowDeploymentValues).
 * Disabled workflows are submitted with their connections/inputs cleared.
 */
export const buildDeploymentWorkflows = (
    formWorkflows: ProjectDeploymentWorkflow[] | undefined,
    workflows: Workflow[]
): ProjectDeploymentWorkflow[] | undefined => {
    if (!formWorkflows) {
        return formWorkflows;
    }

    const workflowsById = new Map(workflows.map((workflow) => [workflow.id, workflow]));

    const reachableSubflowUuids = getReachableSubflowUuids(workflows, workflows);

    const enabledParentWorkflows = formWorkflows
        .filter((formWorkflow) => formWorkflow.enabled)
        .map((formWorkflow) => workflowsById.get(formWorkflow.workflowId))
        .filter((workflow): workflow is Workflow => !!workflow && !isSubflowWorkflow(workflow, reachableSubflowUuids));

    const enabledSubflowUuids = getReachableSubflowUuids(enabledParentWorkflows, workflows);

    const ownValuesByWorkflowId = new Map<string, SubflowDeploymentValuesI>();
    const subflowValuesByUuid = new Map<string, SubflowDeploymentValuesI>();

    for (const parentWorkflow of enabledParentWorkflows) {
        const parentEntry = formWorkflows.find((formWorkflow) => formWorkflow.workflowId === parentWorkflow.id);

        const {ownValues, subflowValuesByUuid: parentSubflowValues} = splitSubflowDeploymentValues(
            parentWorkflow,
            workflows,
            parentEntry?.connections,
            parentEntry?.inputs as Record<string, unknown> | undefined
        );

        ownValuesByWorkflowId.set(parentWorkflow.id!, ownValues);

        for (const [subflowWorkflowUuid, values] of parentSubflowValues) {
            subflowValuesByUuid.set(subflowWorkflowUuid, values);
        }
    }

    return formWorkflows.map((formWorkflow) => {
        const workflow = workflowsById.get(formWorkflow.workflowId);

        const subflowEnabled = !!workflow?.workflowUuid && enabledSubflowUuids.has(workflow.workflowUuid);
        const enabled = formWorkflow.enabled || subflowEnabled;

        if (!enabled) {
            return {...formWorkflow, connections: [], enabled, inputs: {}};
        }

        const values = subflowEnabled
            ? subflowValuesByUuid.get(workflow!.workflowUuid!)
            : ownValuesByWorkflowId.get(formWorkflow.workflowId!);

        return {
            ...formWorkflow,
            connections: values?.connections ?? [],
            enabled,
            inputs: values?.inputs ?? {},
        };
    });
};

export default getWorkflowComponentConnections;
