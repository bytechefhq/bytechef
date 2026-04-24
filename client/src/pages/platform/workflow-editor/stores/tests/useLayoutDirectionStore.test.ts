import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useLayoutDirectionStore from '../useLayoutDirectionStore';

describe('useLayoutDirectionStore', () => {
    beforeEach(() => {
        // Reset to a clean state by clearing the Zustand store entirely
        useLayoutDirectionStore.setState({
            currentWorkflowUuid: '',
            directionsByWorkflowUuid: {},
            layoutDirection: 'TB',
        });
    });

    it('should initialize with default TB layout direction', () => {
        const {result} = renderHook(() => useLayoutDirectionStore());

        expect(result.current.layoutDirection).toBe('TB');
    });

    it('should set layout direction for the current workflow', () => {
        const {result} = renderHook(() => useLayoutDirectionStore());

        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-1');
        });

        act(() => {
            result.current.setLayoutDirection('LR');
        });

        expect(result.current.layoutDirection).toBe('LR');
        expect(result.current.directionsByWorkflowUuid['workflow-1']).toBe('LR');
    });

    it('should restore saved direction when switching to a workflow', () => {
        const {result} = renderHook(() => useLayoutDirectionStore());

        // Set workflow-1 to LR
        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-1');
        });

        act(() => {
            result.current.setLayoutDirection('LR');
        });

        // Switch to workflow-2 (should default to TB)
        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-2');
        });

        expect(result.current.layoutDirection).toBe('TB');

        // Switch back to workflow-1 (should restore LR)
        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-1');
        });

        expect(result.current.layoutDirection).toBe('LR');
    });

    it('should default to TB for unknown workflow UUIDs', () => {
        const {result} = renderHook(() => useLayoutDirectionStore());

        act(() => {
            result.current.setCurrentWorkflowUuid('unknown-workflow');
        });

        expect(result.current.layoutDirection).toBe('TB');
    });

    it('should not update state when setting the same workflow UUID', () => {
        const {result} = renderHook(() => useLayoutDirectionStore());

        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-1');
        });

        act(() => {
            result.current.setLayoutDirection('LR');
        });

        // Setting the same UUID should be a no-op
        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-1');
        });

        expect(result.current.layoutDirection).toBe('LR');
    });

    it('should store different directions for different workflows', () => {
        const {result} = renderHook(() => useLayoutDirectionStore());

        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-1');
        });

        act(() => {
            result.current.setLayoutDirection('LR');
        });

        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-2');
        });

        act(() => {
            result.current.setLayoutDirection('TB');
        });

        expect(result.current.directionsByWorkflowUuid['workflow-1']).toBe('LR');
        expect(result.current.directionsByWorkflowUuid['workflow-2']).toBe('TB');
    });

    it('should toggle layout direction', () => {
        const {result} = renderHook(() => useLayoutDirectionStore());

        act(() => {
            result.current.setCurrentWorkflowUuid('workflow-1');
        });

        expect(result.current.layoutDirection).toBe('TB');

        act(() => {
            result.current.setLayoutDirection('LR');
        });

        expect(result.current.layoutDirection).toBe('LR');

        act(() => {
            result.current.setLayoutDirection('TB');
        });

        expect(result.current.layoutDirection).toBe('TB');
    });
});
