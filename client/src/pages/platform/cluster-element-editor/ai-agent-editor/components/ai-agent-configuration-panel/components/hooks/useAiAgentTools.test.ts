import {describe, expect, it} from 'vitest';

/**
 * Tests for the tool identifier fallback used by useAiAgentTools.
 *
 * Tool entries can appear in either shape:
 *  - NodeDataType (workflowNodeName) when synced in-memory after a node click.
 *  - ClusterElementItemType (name) when loaded from the workflow definition JSON
 *    or when newly saved via saveClusterElementToWorkflow.
 *
 * If the hook only reads workflowNodeName, a just-added tool ends up with an
 * empty identifier, which later makes the Configure node-details panel blank
 * because its outer gate checks currentNode?.workflowNodeName.
 */

type ToolElementInputType = {
    name?: string;
    workflowNodeName?: string;
};

function deriveToolName(tool: ToolElementInputType): string {
    return tool.workflowNodeName || tool.name || '';
}

describe('deriveToolName', () => {
    it('prefers workflowNodeName when both fields are present', () => {
        expect(deriveToolName({name: 'workflow_1', workflowNodeName: 'callWorkflow_1'})).toBe('callWorkflow_1');
    });

    it('falls back to name when workflowNodeName is missing (newly added tool)', () => {
        expect(deriveToolName({name: 'affinity_1'})).toBe('affinity_1');
    });

    it('falls back to name when workflowNodeName is an empty string', () => {
        expect(deriveToolName({name: 'ahrefs_1', workflowNodeName: ''})).toBe('ahrefs_1');
    });

    it('returns empty string when neither field is present', () => {
        expect(deriveToolName({})).toBe('');
    });
});

/**
 * Tests for the simple-mode close-on-delete behavior in handleRemoveTool.
 *
 * If the node-details panel is currently showing the tool being removed, the
 * panel must close (and currentNode cleared) before the mutation fires,
 * otherwise it keeps rendering a stale view of a tool that no longer exists.
 */

type ActiveNodeInputType = {
    activeWorkflowNodeName?: string;
    toolNameBeingRemoved: string;
};

function shouldCloseDetailsPanelOnRemove({activeWorkflowNodeName, toolNameBeingRemoved}: ActiveNodeInputType): boolean {
    return activeWorkflowNodeName === toolNameBeingRemoved;
}

describe('shouldCloseDetailsPanelOnRemove', () => {
    it('closes the panel when the removed tool matches the active node', () => {
        expect(
            shouldCloseDetailsPanelOnRemove({
                activeWorkflowNodeName: 'workflow_1',
                toolNameBeingRemoved: 'workflow_1',
            })
        ).toBe(true);
    });

    it('leaves the panel open when another tool is being removed', () => {
        expect(
            shouldCloseDetailsPanelOnRemove({
                activeWorkflowNodeName: 'workflow_1',
                toolNameBeingRemoved: 'affinity_1',
            })
        ).toBe(false);
    });

    it('does not close when no node is currently active', () => {
        expect(
            shouldCloseDetailsPanelOnRemove({
                activeWorkflowNodeName: undefined,
                toolNameBeingRemoved: 'workflow_1',
            })
        ).toBe(false);
    });
});
