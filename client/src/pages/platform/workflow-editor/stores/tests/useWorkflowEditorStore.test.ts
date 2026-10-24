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

describe('useWorkflowEditorStore - live cluster element call records', () => {
    beforeEach(() => {
        useWorkflowEditorStore.setState({workflowTestClusterElementCallRecords: {}});
    });

    it('clears live call records with the node states', () => {
        useWorkflowEditorStore
            .getState()
            .upsertWorkflowTestClusterElementCallRecord('10', {id: 'a', status: 'STARTED'});

        expect(useWorkflowEditorStore.getState().workflowTestClusterElementCallRecords['10']).toHaveLength(1);

        useWorkflowEditorStore.getState().resetWorkflowTestNodeStates('workflow-1');

        expect(useWorkflowEditorStore.getState().workflowTestClusterElementCallRecords).toEqual({});
    });

    it('updates a record in place and keeps fields the update leaves out', () => {
        const {upsertWorkflowTestClusterElementCallRecord} = useWorkflowEditorStore.getState();

        upsertWorkflowTestClusterElementCallRecord('10', {id: 'a', input: 'in', status: 'STARTED'});
        upsertWorkflowTestClusterElementCallRecord('10', {id: 'b', status: 'STARTED'});
        upsertWorkflowTestClusterElementCallRecord('10', {id: 'a', output: 'out', status: 'COMPLETED'});

        expect(useWorkflowEditorStore.getState().workflowTestClusterElementCallRecords['10']).toEqual([
            {id: 'a', input: 'in', output: 'out', status: 'COMPLETED'},
            {id: 'b', status: 'STARTED'},
        ]);
    });
});
