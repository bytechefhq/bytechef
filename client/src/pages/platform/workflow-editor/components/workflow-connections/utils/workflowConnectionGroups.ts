import {
    ComponentConnection,
    Workflow,
    WorkflowTask,
    WorkflowTestConfigurationConnection,
    WorkflowTrigger,
} from '@/shared/middleware/platform/configuration';

export interface WorkflowConnectionEntryI {
    connectionId?: number;
    description: string;
    id: string;
    name: string;
    required: boolean;
    workflowConnectionKey: string;
    workflowNodeName: string;
}

export interface WorkflowConnectionGroupI {
    componentName: string;
    componentVersion: number;
    connectedCount: number;
    connectionId?: number;
    distinctConnectionCount: number;
    entries: WorkflowConnectionEntryI[];
    id: string;
    missingCount: number;
    overrideCount: number;
}

export interface WorkflowConnectionChangeI {
    connectionId?: number;
    previousConnectionId?: number;
    workflowConnectionKey: string;
    workflowNodeName: string;
}

export interface WorkflowConnectionsSummaryI {
    connectedCount: number;
    missingCount: number;
    totalCount: number;
}

type WorkflowNodeType = Pick<WorkflowTask, 'clusterRoot' | 'connections' | 'label' | 'type'> | WorkflowTrigger;

function getOperationName(type: string): string {
    return type.split('/')[2] ?? '';
}

function getEntryLabel(
    workflowNode: WorkflowNodeType,
    componentConnection: ComponentConnection
): Pick<WorkflowConnectionEntryI, 'description' | 'name'> {
    const {componentName, key, workflowNodeName} = componentConnection;

    const clusterRoot = 'clusterRoot' in workflowNode && !!workflowNode.clusterRoot;

    if (clusterRoot && key !== componentName) {
        return {description: `in ${workflowNodeName}`, name: key};
    }

    return {
        description: workflowNode.label || getOperationName(workflowNode.type),
        name: key === componentName ? workflowNodeName : `${workflowNodeName} (${key})`,
    };
}

function buildEntry(
    workflowNode: WorkflowNodeType,
    componentConnection: ComponentConnection,
    connectionId: number | undefined
): WorkflowConnectionEntryI {
    const {key, required, workflowNodeName} = componentConnection;

    return {
        connectionId,
        ...getEntryLabel(workflowNode, componentConnection),
        id: `${workflowNodeName}:${key}`,
        required,
        workflowConnectionKey: key,
        workflowNodeName,
    };
}

function getGroupConnectionId(entries: WorkflowConnectionEntryI[]): number | undefined {
    const usageCounts = new Map<number, number>();

    for (const entry of entries) {
        if (entry.connectionId !== undefined) {
            usageCounts.set(entry.connectionId, (usageCounts.get(entry.connectionId) ?? 0) + 1);
        }
    }

    let groupConnectionId: number | undefined;
    let highestUsageCount = 0;
    let tied = false;

    for (const [connectionId, usageCount] of usageCounts) {
        if (usageCount > highestUsageCount) {
            groupConnectionId = connectionId;
            highestUsageCount = usageCount;
            tied = false;
        } else if (usageCount === highestUsageCount) {
            tied = true;
        }
    }

    return tied ? undefined : groupConnectionId;
}

function createGroup(
    componentName: string,
    componentVersion: number,
    entries: WorkflowConnectionEntryI[]
): WorkflowConnectionGroupI {
    const connectionId = getGroupConnectionId(entries);

    const connectedEntries = entries.filter((entry) => entry.connectionId !== undefined);

    return {
        componentName,
        componentVersion,
        connectedCount: connectedEntries.length,
        connectionId,
        distinctConnectionCount: new Set(connectedEntries.map((entry) => entry.connectionId)).size,
        entries,
        id: `${componentName}:${componentVersion}`,
        missingCount: entries.filter((entry) => entry.required && entry.connectionId === undefined).length,
        overrideCount:
            connectionId === undefined
                ? 0
                : connectedEntries.filter((entry) => entry.connectionId !== connectionId).length,
    };
}

export function buildWorkflowConnectionGroups(
    workflow: Workflow,
    testConfigurationConnections: WorkflowTestConfigurationConnection[] = []
): WorkflowConnectionGroupI[] {
    const groupedEntries = new Map<
        string,
        {componentName: string; componentVersion: number; entries: WorkflowConnectionEntryI[]}
    >();

    const workflowNodes: WorkflowNodeType[] = [...(workflow.triggers ?? []), ...(workflow.tasks ?? [])];

    for (const workflowNode of workflowNodes) {
        for (const componentConnection of workflowNode.connections ?? []) {
            const {componentName, componentVersion, key, workflowNodeName} = componentConnection;

            const groupId = `${componentName}:${componentVersion}`;

            let groupEntries = groupedEntries.get(groupId);

            if (!groupEntries) {
                groupEntries = {componentName, componentVersion, entries: []};

                groupedEntries.set(groupId, groupEntries);
            }

            const testConfigurationConnection = testConfigurationConnections.find(
                (connection) =>
                    connection.workflowNodeName === workflowNodeName && connection.workflowConnectionKey === key
            );

            groupEntries.entries.push(
                buildEntry(workflowNode, componentConnection, testConfigurationConnection?.connectionId)
            );
        }
    }

    return [...groupedEntries.values()].map(({componentName, componentVersion, entries}) =>
        createGroup(componentName, componentVersion, entries)
    );
}

export function isConnectionOverride(group: WorkflowConnectionGroupI, entry: WorkflowConnectionEntryI): boolean {
    return (
        group.connectionId !== undefined &&
        entry.connectionId !== undefined &&
        entry.connectionId !== group.connectionId
    );
}

function toChange(entry: WorkflowConnectionEntryI, connectionId: number | undefined): WorkflowConnectionChangeI {
    return {
        connectionId,
        previousConnectionId: entry.connectionId,
        workflowConnectionKey: entry.workflowConnectionKey,
        workflowNodeName: entry.workflowNodeName,
    };
}

export function getGroupConnectionChanges(
    group: WorkflowConnectionGroupI,
    connectionId: number
): WorkflowConnectionChangeI[] {
    return group.entries
        .filter((entry) => !isConnectionOverride(group, entry))
        .filter((entry) => entry.connectionId !== connectionId)
        .map((entry) => toChange(entry, connectionId));
}

export function getUnassignedConnectionChanges(group: WorkflowConnectionGroupI): WorkflowConnectionChangeI[] {
    if (group.connectionId === undefined) {
        return [];
    }

    return group.entries
        .filter((entry) => entry.connectionId === undefined)
        .map((entry) => toChange(entry, group.connectionId));
}

export function getWorkflowConnectionsSummary(groups: WorkflowConnectionGroupI[]): WorkflowConnectionsSummaryI {
    return groups.reduce<WorkflowConnectionsSummaryI>(
        (summary, group) => ({
            connectedCount: summary.connectedCount + group.connectedCount,
            missingCount: summary.missingCount + group.missingCount,
            totalCount: summary.totalCount + group.entries.length,
        }),
        {connectedCount: 0, missingCount: 0, totalCount: 0}
    );
}

export function filterWorkflowConnectionGroups(
    groups: WorkflowConnectionGroupI[],
    query: string,
    getGroupTitle: (group: WorkflowConnectionGroupI) => string
): WorkflowConnectionGroupI[] {
    const normalizedQuery = query.trim().toLowerCase();

    if (!normalizedQuery) {
        return groups;
    }

    const matches = (text: string) => text.toLowerCase().includes(normalizedQuery);

    return groups.filter(
        (group) =>
            matches(getGroupTitle(group)) ||
            group.entries.some((entry) => matches(entry.name) || matches(entry.description))
    );
}
