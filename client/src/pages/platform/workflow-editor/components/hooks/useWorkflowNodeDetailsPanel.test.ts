import {describe, expect, it} from 'vitest';

/**
 * Tests for the workflowTestConfigurationConnections query enabled condition
 * in useWorkflowNodeDetailsPanel.
 *
 * The enabled condition determines whether saved test configuration connections
 * are fetched. If disabled, connection selection in the ConnectionTab will not
 * persist because the sync effect resets the selection when the query data is undefined.
 */

type EnabledConditionInputType = {
    componentDefinitionConnection: boolean;
    currentNode: {clusterRoot?: boolean; isNestedClusterRoot?: boolean} | null;
    workflowId: string | undefined;
};

/**
 * Mirrors the enabled condition from useWorkflowNodeDetailsPanel:
 *   !!workflow.id && !!currentNode &&
 *   (!!currentComponentDefinition?.connection || (!!currentNode.clusterRoot && !currentNode.isNestedClusterRoot))
 */
function isTestConfigConnectionsQueryEnabled({
    componentDefinitionConnection,
    currentNode,
    workflowId,
}: EnabledConditionInputType): boolean {
    return (
        !!workflowId &&
        !!currentNode &&
        (componentDefinitionConnection || (!!currentNode.clusterRoot && !currentNode.isNestedClusterRoot))
    );
}

describe('workflowTestConfigurationConnections query enabled condition', () => {
    it('should be enabled when component has connection defined and connectionRequired is true (e.g., Gmail)', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: true,
                currentNode: {},
                workflowId: 'wf-1',
            })
        ).toBe(true);
    });

    it('should be enabled when component has connection defined but connectionRequired is false (e.g., httpClient)', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: true,
                currentNode: {},
                workflowId: 'wf-1',
            })
        ).toBe(true);
    });

    it('should be enabled for cluster root nodes even without a connection', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: false,
                currentNode: {clusterRoot: true, isNestedClusterRoot: false},
                workflowId: 'wf-1',
            })
        ).toBe(true);
    });

    it('should be disabled for nested cluster root nodes without a connection', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: false,
                currentNode: {clusterRoot: true, isNestedClusterRoot: true},
                workflowId: 'wf-1',
            })
        ).toBe(false);
    });

    it('should be disabled when component has no connection and node is not a cluster root', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: false,
                currentNode: {},
                workflowId: 'wf-1',
            })
        ).toBe(false);
    });

    it('should be disabled when currentNode is null', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: true,
                currentNode: null,
                workflowId: 'wf-1',
            })
        ).toBe(false);
    });

    it('should be disabled when workflowId is undefined', () => {
        expect(
            isTestConfigConnectionsQueryEnabled({
                componentDefinitionConnection: true,
                currentNode: {},
                workflowId: undefined,
            })
        ).toBe(false);
    });
});
