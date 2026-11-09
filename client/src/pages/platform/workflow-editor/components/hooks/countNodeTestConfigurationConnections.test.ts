import {describe, expect, it} from 'vitest';

import countNodeTestConfigurationConnections from './countNodeTestConfigurationConnections';

const agentTestConfigurationConnections = [
    {connectionId: 1081, workflowConnectionKey: 'anthropic_1', workflowNodeName: 'aiAgent_1'},
];

describe('countNodeTestConfigurationConnections', () => {
    it('counts nothing before the connections have loaded', () => {
        expect(
            countNodeTestConfigurationConnections({
                workflowNodeName: 'slack_1',
                workflowTestConfigurationConnections: undefined,
            })
        ).toBe(0);
    });

    it('counts every connection of a task node', () => {
        expect(
            countNodeTestConfigurationConnections({
                workflowNodeName: 'slack_1',
                workflowTestConfigurationConnections: [
                    {connectionId: 1084, workflowConnectionKey: 'slack', workflowNodeName: 'slack_1'},
                ],
            })
        ).toBe(1);
    });

    it('ignores the connections of sibling cluster elements under the same root', () => {
        expect(
            countNodeTestConfigurationConnections({
                clusterElementType: 'tools',
                workflowNodeName: 'daytona_1',
                workflowTestConfigurationConnections: agentTestConfigurationConnections,
            })
        ).toBe(0);
    });

    it('counts the connection set for the cluster element itself', () => {
        expect(
            countNodeTestConfigurationConnections({
                clusterElementType: 'tools',
                workflowNodeName: 'daytona_1',
                workflowTestConfigurationConnections: [
                    ...agentTestConfigurationConnections,
                    {connectionId: 1089, workflowConnectionKey: 'daytona_1', workflowNodeName: 'aiAgent_1'},
                ],
            })
        ).toBe(1);
    });
});
