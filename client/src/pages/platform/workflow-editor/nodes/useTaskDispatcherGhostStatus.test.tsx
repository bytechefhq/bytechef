import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it} from 'vitest';

import {WorkflowExecutionOverlayContext} from '../providers/workflowExecutionOverlayContext';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useTaskDispatcherGhostStatus from './useTaskDispatcherGhostStatus';

describe('useTaskDispatcherGhostStatus', () => {
    beforeEach(() => {
        useWorkflowEditorStore.setState({
            workflowTestNodeStates: {loop_1: {status: 'FAILED'}},
            workflowTestNodeStatesWorkflowId: 'another-workflow',
        });
    });

    it('reads the dispatcher status of the execution shown on a read-only canvas', () => {
        const wrapper = ({children}: {children: ReactNode}) => (
            <WorkflowExecutionOverlayContext.Provider
                value={{isRunning: false, nodeStates: {loop_1: {status: 'COMPLETED'}}, taskExecutions: []}}
            >
                {children}
            </WorkflowExecutionOverlayContext.Provider>
        );

        const {result} = renderHook(() => useTaskDispatcherGhostStatus({taskDispatcherId: 'loop_1'}), {wrapper});

        expect(result.current).toBe('COMPLETED');
    });

    it('ignores an editor test run that belongs to another workflow', () => {
        const {result} = renderHook(() => useTaskDispatcherGhostStatus({taskDispatcherId: 'loop_1'}));

        expect(result.current).toBeUndefined();
    });
});
