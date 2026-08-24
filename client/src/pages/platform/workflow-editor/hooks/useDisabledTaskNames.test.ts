import {SPACE} from '@/shared/constants';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import * as getEffectivelyDisabledTaskNamesModule from '../utils/getEffectivelyDisabledTaskNames';
import useDisabledTaskNames from './useDisabledTaskNames';

function setDefinition(definition: string | undefined) {
    useWorkflowDataStore.setState((state) => ({
        workflow: {...state.workflow, definition},
    }));
}

function makeDefinition(disabledName?: string) {
    return JSON.stringify(
        {
            tasks: [
                {name: 'action_1', parameters: {}, type: 'test/v1/action'},
                {
                    disabled: disabledName === 'action_2' || undefined,
                    name: 'action_2',
                    parameters: {},
                    type: 'test/v1/action',
                },
            ],
        },
        null,
        SPACE
    );
}

describe('useDisabledTaskNames', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('returns the effectively disabled task names for the current definition', () => {
        setDefinition(makeDefinition('action_2'));

        const {result} = renderHook(() => useDisabledTaskNames());

        expect(result.current).toEqual(new Set(['action_2']));
    });

    it('returns an empty set when the definition is missing', () => {
        setDefinition(undefined);

        const {result} = renderHook(() => useDisabledTaskNames());

        expect(result.current).toEqual(new Set());
    });

    it('returns an empty set when the definition cannot be parsed', () => {
        setDefinition('{not json');

        const {result} = renderHook(() => useDisabledTaskNames());

        expect(result.current).toEqual(new Set());
    });

    it('walks the definition once and hands every caller the same set instance', () => {
        setDefinition(makeDefinition('action_2'));

        // Prime the cache before spying so the assertion counts only post-spy walks
        renderHook(() => useDisabledTaskNames());

        const walkSpy = vi.spyOn(getEffectivelyDisabledTaskNamesModule, 'getEffectivelyDisabledTaskNames');

        const first = renderHook(() => useDisabledTaskNames());
        const second = renderHook(() => useDisabledTaskNames());
        const third = renderHook(() => useDisabledTaskNames());

        expect(walkSpy).not.toHaveBeenCalled();
        expect(first.result.current).toBe(second.result.current);
        expect(second.result.current).toBe(third.result.current);
    });

    it('recomputes when the definition changes', () => {
        setDefinition(makeDefinition('action_2'));

        const {rerender, result} = renderHook(() => useDisabledTaskNames());

        expect(result.current).toEqual(new Set(['action_2']));

        setDefinition(makeDefinition());

        rerender();

        expect(result.current).toEqual(new Set());
    });
});
