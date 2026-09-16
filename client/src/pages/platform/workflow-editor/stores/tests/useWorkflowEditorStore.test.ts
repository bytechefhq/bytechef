import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowEditorStore from '../useWorkflowEditorStore';

describe('useWorkflowEditorStore - nodesLocked', () => {
    beforeEach(() => {
        useWorkflowEditorStore.setState({nodesLocked: true});
    });

    it('defaults nodesLocked to true', () => {
        expect(useWorkflowEditorStore.getInitialState().nodesLocked).toBe(true);
    });

    it('setNodesLocked updates the value', () => {
        useWorkflowEditorStore.getState().setNodesLocked(false);

        expect(useWorkflowEditorStore.getState().nodesLocked).toBe(false);

        useWorkflowEditorStore.getState().setNodesLocked(true);

        expect(useWorkflowEditorStore.getState().nodesLocked).toBe(true);
    });
});
