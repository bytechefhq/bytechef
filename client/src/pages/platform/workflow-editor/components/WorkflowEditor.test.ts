import {describe, expect, it} from 'vitest';

import {NON_PERSISTED_NODE_TYPES} from './WorkflowEditor';

describe('NON_PERSISTED_NODE_TYPES', () => {
    it('should contain all ghost and placeholder node types', () => {
        expect(NON_PERSISTED_NODE_TYPES).toContain('placeholder');
        expect(NON_PERSISTED_NODE_TYPES).toContain('taskDispatcherTopGhostNode');
        expect(NON_PERSISTED_NODE_TYPES).toContain('taskDispatcherBottomGhostNode');
        expect(NON_PERSISTED_NODE_TYPES).toContain('taskDispatcherLeftGhostNode');
    });

    it('should have exactly 4 non-persisted types', () => {
        expect(NON_PERSISTED_NODE_TYPES.size).toBe(4);
    });

    it('should not include persisted node types', () => {
        expect(NON_PERSISTED_NODE_TYPES.has('workflow')).toBe(false);
        expect(NON_PERSISTED_NODE_TYPES.has('aiAgentNode')).toBe(false);
    });
});
