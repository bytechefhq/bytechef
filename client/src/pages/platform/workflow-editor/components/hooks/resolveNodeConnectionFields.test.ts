import {ComponentConnection, WorkflowTestConfigurationConnection} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import resolveNodeConnectionFields from './resolveNodeConnectionFields';

/**
 * Tests for resolveNodeConnectionFields, extracted from the connection-sync effect in
 * useWorkflowNodeDetailsPanel.
 *
 * Regression for the "Response to Workflow Call" Output Schema bug: a freshly added connectionless
 * node never gets a `connections` field (it is `undefined`), whereas a server-loaded node has
 * `connections: []`. Normalizing the former to `[]` keeps the node shape consistent before and after
 * a reload, so consumers that distinguish the two (e.g. the DYNAMIC_PROPERTIES query gate) behave the
 * same on first add as after a refresh.
 */

const connection = (key: string): ComponentConnection => ({
    componentName: key,
    componentVersion: 1,
    key,
    required: true,
    workflowNodeName: 'node_1',
});

const testConfigConnection = (
    overrides: Partial<WorkflowTestConfigurationConnection>
): WorkflowTestConfigurationConnection => ({
    connectionId: 1,
    workflowConnectionKey: 'key',
    workflowNodeName: 'node_1',
    ...overrides,
});

describe('resolveNodeConnectionFields', () => {
    it('normalizes a freshly added connectionless node (connections undefined) to []', () => {
        expect(resolveNodeConnectionFields({workflowNodeName: 'workflow_1'}, [], {})).toEqual({connections: []});
    });

    it('returns null for a server-loaded connectionless node that already has connections: []', () => {
        expect(resolveNodeConnectionFields({connections: [], workflowNodeName: 'workflow_1'}, [], {})).toBeNull();
    });

    it('resolves the cluster element connectionId by workflowConnectionKey === node workflowNodeName', () => {
        const connections = [connection('slack')];

        expect(
            resolveNodeConnectionFields({clusterElementType: 'MODEL', workflowNodeName: 'slack_1'}, connections, {
                workflowTestConfigurationConnections: [
                    testConfigConnection({connectionId: 7, workflowConnectionKey: 'slack_1'}),
                ],
            })
        ).toEqual({connectionId: 7, connections});
    });

    it('resolves the main cluster root connectionId when the node is the root cluster element', () => {
        const connections = [connection('slack')];

        expect(
            resolveNodeConnectionFields({workflowNodeName: 'agent_1'}, connections, {
                rootClusterElementWorkflowNodeName: 'agent_1',
                workflowTestConfigurationConnections: [
                    testConfigConnection({
                        connectionId: 9,
                        workflowConnectionKey: 'slack',
                        workflowNodeName: 'agent_1',
                    }),
                ],
            })
        ).toEqual({connectionId: 9, connections});
    });

    it('resolves the first configured connectionId for a regular node with connections', () => {
        const connections = [connection('gmail')];

        expect(
            resolveNodeConnectionFields({workflowNodeName: 'gmail_1'}, connections, {
                workflowTestConfigurationConnections: [
                    testConfigConnection({
                        connectionId: 3,
                        workflowConnectionKey: 'gmail',
                        workflowNodeName: 'gmail_1',
                    }),
                ],
            })
        ).toEqual({connectionId: 3, connections});
    });

    it('leaves connectionId undefined for a regular node with connections but no test configuration', () => {
        const connections = [connection('gmail')];

        expect(resolveNodeConnectionFields({workflowNodeName: 'gmail_1'}, connections, {})).toEqual({
            connectionId: undefined,
            connections,
        });
    });
});
