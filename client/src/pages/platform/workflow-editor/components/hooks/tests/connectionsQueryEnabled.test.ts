import {describe, expect, it} from 'vitest';

/**
 * Specification test for the `enabled` condition of the workflow test
 * configuration connections query in useWorkflowNodeDetailsPanel.ts (line 182).
 *
 * NOTE: This tests a replicated condition, not the production code directly,
 * because the condition is inline in a React hook and cannot be imported.
 * If the condition in useWorkflowNodeDetailsPanel.ts changes, this test
 * must be updated to match.
 */

interface ConnectionsQueryEnabledParamsI {
    connectionRequired?: boolean;
    currentNode?: object;
    workflowId?: string;
}

function isConnectionsQueryEnabled({
    connectionRequired,
    currentNode,
    workflowId,
}: ConnectionsQueryEnabledParamsI): boolean {
    return !!workflowId && !!currentNode && !!connectionRequired;
}

describe('Connections Query Enabled', () => {
    const baseParams: ConnectionsQueryEnabledParamsI = {
        connectionRequired: true,
        currentNode: {componentName: 'slack', name: 'slack_1'},
        workflowId: 'workflow-123',
    };

    it('should be enabled when component requires connection', () => {
        expect(isConnectionsQueryEnabled(baseParams)).toBe(true);
    });

    it('should be disabled when component does not require connection', () => {
        expect(isConnectionsQueryEnabled({...baseParams, connectionRequired: false})).toBe(false);
    });

    it('should be disabled when component definition has not loaded yet', () => {
        expect(isConnectionsQueryEnabled({...baseParams, connectionRequired: undefined})).toBe(false);
    });

    it('should be disabled when currentNode is undefined', () => {
        expect(isConnectionsQueryEnabled({...baseParams, currentNode: undefined})).toBe(false);
    });

    it('should be disabled when workflowId is undefined', () => {
        expect(isConnectionsQueryEnabled({...baseParams, workflowId: undefined})).toBe(false);
    });

    it('should be disabled for nodes like var that do not require connections', () => {
        expect(
            isConnectionsQueryEnabled({
                connectionRequired: false,
                currentNode: {componentName: 'var', name: 'var_1'},
                workflowId: 'workflow-123',
            })
        ).toBe(false);
    });

    it('should be disabled for task dispatchers that do not require connections', () => {
        expect(
            isConnectionsQueryEnabled({
                connectionRequired: undefined,
                currentNode: {componentName: 'map', name: 'map_1', taskDispatcher: true},
                workflowId: 'workflow-123',
            })
        ).toBe(false);
    });
});
