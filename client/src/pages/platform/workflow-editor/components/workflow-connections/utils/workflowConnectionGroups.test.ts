import {
    ComponentConnection,
    Workflow,
    WorkflowTestConfigurationConnection,
} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {
    buildWorkflowConnectionGroups,
    filterWorkflowConnectionGroups,
    getGroupConnectionChanges,
    getUnassignedConnectionChanges,
    getWorkflowConnectionsSummary,
    isConnectionOverride,
} from './workflowConnectionGroups';

const GOOGLE_SHEETS_TASK_NAMES = ['googleSheets_1', 'googleSheets_2', 'googleSheets_3', 'googleSheets_4'];

function createConnection(
    componentName: string,
    workflowNodeName: string,
    overrides: Partial<ComponentConnection> = {}
): ComponentConnection {
    return {componentName, componentVersion: 1, key: componentName, required: true, workflowNodeName, ...overrides};
}

function createNode(
    name: string,
    type: string,
    connections: ComponentConnection[],
    overrides: Record<string, unknown> = {}
) {
    return {connections, name, type, ...overrides};
}

function createWorkflow({tasks = [], triggers = []}: {tasks?: object[]; triggers?: object[]}): Workflow {
    return {tasks, triggers} as unknown as Workflow;
}

function createGoogleSheetsWorkflow(taskCount: number): Workflow {
    return createWorkflow({
        tasks: GOOGLE_SHEETS_TASK_NAMES.slice(0, taskCount).map((taskName) =>
            createNode(taskName, 'googleSheets/v1/insertRow', [createConnection('googleSheets', taskName)])
        ),
    });
}

function assignConnection(
    workflowNodeName: string,
    connectionId: number,
    workflowConnectionKey = 'googleSheets'
): WorkflowTestConfigurationConnection {
    return {connectionId, workflowConnectionKey, workflowNodeName};
}

describe('buildWorkflowConnectionGroups', () => {
    it('groups connections by component name and version in first-appearance order', () => {
        const workflow = createWorkflow({
            tasks: [
                createNode('slack_1', 'slack/v1/sendMessage', [createConnection('slack', 'slack_1')]),
                createNode('googleSheets_1', 'googleSheets/v1/insertRow', [
                    createConnection('googleSheets', 'googleSheets_1'),
                ]),
                createNode('googleSheets_2', 'googleSheets/v2/insertRow', [
                    createConnection('googleSheets', 'googleSheets_2', {componentVersion: 2}),
                ]),
            ],
            triggers: [
                createNode('googleSheets_trigger_1', 'googleSheets/v1/newRow', [
                    createConnection('googleSheets', 'googleSheets_trigger_1'),
                ]),
            ],
        });

        const groups = buildWorkflowConnectionGroups(workflow);

        expect(groups.map((group) => group.id)).toEqual(['googleSheets:1', 'slack:1', 'googleSheets:2']);
        expect(groups[0].entries.map((entry) => entry.id)).toEqual([
            'googleSheets_trigger_1:googleSheets',
            'googleSheets_1:googleSheets',
        ]);
        expect(groups[2]).toMatchObject({componentName: 'googleSheets', componentVersion: 2});
        expect(groups[2].entries.map((entry) => entry.id)).toEqual(['googleSheets_2:googleSheets']);
    });

    it('takes the most used connection as the group connection and counts overrides', () => {
        const [group] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(4), [
            assignConnection('googleSheets_1', 7),
            assignConnection('googleSheets_2', 7),
            assignConnection('googleSheets_3', 7),
            assignConnection('googleSheets_4', 9),
        ]);

        expect(group).toMatchObject({
            connectedCount: 4,
            connectionId: 7,
            distinctConnectionCount: 2,
            missingCount: 0,
            overrideCount: 1,
        });
        expect(group.entries.map((entry) => entry.connectionId)).toEqual([7, 7, 7, 9]);
        expect(group.entries.map((entry) => isConnectionOverride(group, entry))).toEqual([false, false, false, true]);
    });

    it('has no group connection on a tie', () => {
        const [group] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(2), [
            assignConnection('googleSheets_1', 7),
            assignConnection('googleSheets_2', 9),
        ]);

        expect(group.connectionId).toBeUndefined();
        expect(group.overrideCount).toBe(0);
        expect(group.distinctConnectionCount).toBe(2);
        expect(group.entries.map((entry) => isConnectionOverride(group, entry))).toEqual([false, false]);
    });

    it('has no group connection when no task has a connection', () => {
        const [group] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(2));

        expect(group).toMatchObject({connectedCount: 0, distinctConnectionCount: 0, missingCount: 2, overrideCount: 0});
        expect(group.connectionId).toBeUndefined();
    });

    it('counts only required unassigned entries as missing', () => {
        const workflow = createWorkflow({
            tasks: [
                createNode('slack_1', 'slack/v1/sendMessage', [createConnection('slack', 'slack_1')]),
                createNode('slack_2', 'slack/v1/sendMessage', [
                    createConnection('slack', 'slack_2', {required: false}),
                ]),
            ],
        });

        const [group] = buildWorkflowConnectionGroups(workflow);

        expect(group.missingCount).toBe(1);
        expect(group.connectedCount).toBe(0);
        expect(group.entries.map((entry) => entry.required)).toEqual([true, false]);
    });

    it('ignores test configuration connections of other nodes and keys', () => {
        const [group] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(1), [
            assignConnection('googleSheets_2', 7),
            assignConnection('googleSheets_1', 9, 'backup'),
        ]);

        expect(group.entries[0].connectionId).toBeUndefined();
    });

    it('labels cluster element entries with the element name and its root', () => {
        const workflow = createWorkflow({
            tasks: [
                createNode(
                    'aiAgent_1',
                    'aiAgent/v1/chat',
                    [
                        createConnection('aiAgent', 'aiAgent_1'),
                        createConnection('openAi', 'aiAgent_1', {key: 'openAi_1'}),
                    ],
                    {clusterRoot: true, label: 'Support Agent'}
                ),
            ],
        });

        const groups = buildWorkflowConnectionGroups(workflow, [assignConnection('aiAgent_1', 3, 'openAi_1')]);

        expect(groups.map((group) => group.id)).toEqual(['aiAgent:1', 'openAi:1']);
        expect(groups[0].entries[0]).toMatchObject({description: 'Support Agent', name: 'aiAgent_1'});
        expect(groups[1].entries).toEqual([
            {
                connectionId: 3,
                description: 'in aiAgent_1',
                id: 'aiAgent_1:openAi_1',
                name: 'openAi_1',
                required: true,
                workflowConnectionKey: 'openAi_1',
                workflowNodeName: 'aiAgent_1',
            },
        ]);
    });

    it('labels task entries with the node name and label', () => {
        const workflow = createWorkflow({
            tasks: [
                createNode(
                    'googleSheets_1',
                    'googleSheets/v1/insertRow',
                    [
                        createConnection('googleSheets', 'googleSheets_1'),
                        createConnection('googleSheets', 'googleSheets_1', {key: 'backup'}),
                    ],
                    {label: 'Insert Row'}
                ),
            ],
        });

        const [group] = buildWorkflowConnectionGroups(workflow);

        expect(group.entries.map((entry) => [entry.name, entry.description])).toEqual([
            ['googleSheets_1', 'Insert Row'],
            ['googleSheets_1 (backup)', 'Insert Row'],
        ]);
        expect(group.entries.map((entry) => entry.workflowConnectionKey)).toEqual(['googleSheets', 'backup']);
    });

    it('describes a task without a label by its operation', () => {
        const workflow = createWorkflow({
            tasks: [createNode('slack_1', 'slack/v1/sendMessage', [createConnection('slack', 'slack_1')])],
        });

        const [group] = buildWorkflowConnectionGroups(workflow);

        expect(group.entries[0].description).toBe('sendMessage');
    });
});

describe('getGroupConnectionChanges', () => {
    it('keeps overrides when the group has a connection', () => {
        const [group] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(4), [
            assignConnection('googleSheets_1', 7),
            assignConnection('googleSheets_2', 7),
            assignConnection('googleSheets_4', 9),
        ]);

        expect(getGroupConnectionChanges(group, 12)).toEqual([
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_1',
            },
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
            {
                connectionId: 12,
                previousConnectionId: undefined,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_3',
            },
        ]);
    });

    it('changes every entry when the group has no connection', () => {
        const [group] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(2), [
            assignConnection('googleSheets_1', 7),
            assignConnection('googleSheets_2', 9),
        ]);

        expect(getGroupConnectionChanges(group, 7)).toEqual([
            {
                connectionId: 7,
                previousConnectionId: 9,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
        ]);
        expect(getGroupConnectionChanges(group, 12)).toEqual([
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_1',
            },
            {
                connectionId: 12,
                previousConnectionId: 9,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
        ]);
    });

    it('returns nothing when every target already uses the connection', () => {
        const [group] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(3), [
            assignConnection('googleSheets_1', 7),
            assignConnection('googleSheets_2', 7),
            assignConnection('googleSheets_3', 9),
        ]);

        expect(getGroupConnectionChanges(group, 7)).toEqual([]);
    });
});

describe('getUnassignedConnectionChanges', () => {
    it('fills only empty entries with the group connection', () => {
        const [group] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(3), [
            assignConnection('googleSheets_1', 7),
        ]);

        expect(getUnassignedConnectionChanges(group)).toEqual([
            {
                connectionId: 7,
                previousConnectionId: undefined,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
            {
                connectionId: 7,
                previousConnectionId: undefined,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_3',
            },
        ]);

        const [groupWithoutConnection] = buildWorkflowConnectionGroups(createGoogleSheetsWorkflow(3), [
            assignConnection('googleSheets_1', 7),
            assignConnection('googleSheets_2', 9),
        ]);

        expect(getUnassignedConnectionChanges(groupWithoutConnection)).toEqual([]);
    });
});

describe('getWorkflowConnectionsSummary', () => {
    it('summarises connected, missing and total counts across groups', () => {
        const workflow = createWorkflow({
            tasks: [
                createNode('googleSheets_1', 'googleSheets/v1/insertRow', [
                    createConnection('googleSheets', 'googleSheets_1'),
                ]),
                createNode('googleSheets_2', 'googleSheets/v1/insertRow', [
                    createConnection('googleSheets', 'googleSheets_2'),
                ]),
                createNode('googleSheets_3', 'googleSheets/v1/insertRow', [
                    createConnection('googleSheets', 'googleSheets_3'),
                ]),
                createNode('slack_1', 'slack/v1/sendMessage', [createConnection('slack', 'slack_1')]),
                createNode('slack_2', 'slack/v1/sendMessage', [
                    createConnection('slack', 'slack_2', {required: false}),
                ]),
            ],
        });

        const groups = buildWorkflowConnectionGroups(workflow, [
            assignConnection('googleSheets_1', 7),
            assignConnection('googleSheets_2', 7),
        ]);

        expect(getWorkflowConnectionsSummary(groups)).toEqual({connectedCount: 2, missingCount: 2, totalCount: 5});
        expect(getWorkflowConnectionsSummary([])).toEqual({connectedCount: 0, missingCount: 0, totalCount: 0});
    });
});

describe('filterWorkflowConnectionGroups', () => {
    const titles: Record<string, string> = {googleSheets: 'Google Sheets', slack: 'Slack'};

    const getGroupTitle = (group: {componentName: string}) => titles[group.componentName];

    const workflow = createWorkflow({
        tasks: [
            createNode(
                'googleSheets_1',
                'googleSheets/v1/insertRow',
                [createConnection('googleSheets', 'googleSheets_1')],
                {label: 'Insert Row'}
            ),
            createNode(
                'googleSheets_2',
                'googleSheets/v1/findRow',
                [createConnection('googleSheets', 'googleSheets_2')],
                {label: 'Find Row'}
            ),
            createNode('slack_1', 'slack/v1/sendMessage', [createConnection('slack', 'slack_1')], {
                label: 'Send Message',
            }),
        ],
    });

    it('filters by group title and by entry name or description', () => {
        const groups = buildWorkflowConnectionGroups(workflow, [assignConnection('googleSheets_1', 7)]);

        const byTitle = filterWorkflowConnectionGroups(groups, 'google', getGroupTitle);

        expect(byTitle.map((group) => group.id)).toEqual(['googleSheets:1']);
        expect(byTitle[0].entries.map((entry) => entry.id)).toEqual([
            'googleSheets_1:googleSheets',
            'googleSheets_2:googleSheets',
        ]);

        const byDescription = filterWorkflowConnectionGroups(groups, 'FIND', getGroupTitle);

        expect(byDescription.map((group) => group.id)).toEqual(['googleSheets:1']);

        const byName = filterWorkflowConnectionGroups(groups, ' slack_1 ', getGroupTitle);

        expect(byName.map((group) => group.id)).toEqual(['slack:1']);

        expect(filterWorkflowConnectionGroups(groups, 'zzz', getGroupTitle)).toEqual([]);
        expect(filterWorkflowConnectionGroups(groups, '', getGroupTitle)).toBe(groups);
        expect(filterWorkflowConnectionGroups(groups, '   ', getGroupTitle)).toBe(groups);
    });

    it('returns the whole, identical group when one of its task descriptions matches', () => {
        const groups = buildWorkflowConnectionGroups(workflow, [assignConnection('googleSheets_1', 7)]);

        const [matchedGroup] = filterWorkflowConnectionGroups(groups, 'find row', getGroupTitle);

        expect(matchedGroup).toBe(groups[0]);
        expect(matchedGroup).toMatchObject({connectedCount: 1, connectionId: 7, missingCount: 1});
    });

    it('returns the full group with all its entries when a task name matches', () => {
        const groups = buildWorkflowConnectionGroups(workflow, [assignConnection('googleSheets_1', 7)]);

        const matchedGroups = filterWorkflowConnectionGroups(groups, 'googleSheets_2', getGroupTitle);

        expect(matchedGroups).toEqual([groups[0]]);
        expect(matchedGroups[0].entries.map((entry) => entry.id)).toEqual([
            'googleSheets_1:googleSheets',
            'googleSheets_2:googleSheets',
        ]);
    });
});
