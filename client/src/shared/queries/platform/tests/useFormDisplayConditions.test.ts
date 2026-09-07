import {ComponentOperationType} from '@/shared/middleware/graphql';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useFormDisplayConditions from '../useFormDisplayConditions';

const hoisted = vi.hoisted(() => ({
    lastCall: undefined as undefined | {options: {enabled: boolean}; variables: Record<string, unknown>},
    result: undefined as undefined | {componentPropertyDisplayConditions: Record<string, boolean>},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    ComponentOperationType: {Action: 'ACTION', ClusterElement: 'CLUSTER_ELEMENT', Trigger: 'TRIGGER'},
    useComponentPropertyDisplayConditionsQuery: (variables: Record<string, unknown>, options: {enabled: boolean}) => {
        hoisted.lastCall = {options, variables};

        return {data: options.enabled ? hoisted.result : undefined};
    },
}));

const properties = {
    componentName: 'httpClient',
    componentVersion: 1,
    operationName: 'post',
    operationType: ComponentOperationType.ClusterElement,
};

describe('useFormDisplayConditions', () => {
    beforeEach(() => {
        vi.useFakeTimers();

        hoisted.lastCall = undefined;
        hoisted.result = {componentPropertyDisplayConditions: {"bodyContentType == 'JSON'": true}};
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('returns the evaluated conditions once they arrive', () => {
        const {result} = renderHook(() =>
            useFormDisplayConditions({...properties, parameters: {bodyContentType: 'JSON'}})
        );

        expect(hoisted.lastCall?.options.enabled).toBe(true);
        expect(result.current.displayConditions).toEqual({"bodyContentType == 'JSON'": true});
    });

    // An empty map is a form whose defaults have not been applied yet, not a verdict that nothing holds. Querying
    // on it would hide every conditional property until the debounce elapsed.
    it('stays unevaluated while the form holds no parameters', () => {
        const {result} = renderHook(() => useFormDisplayConditions({...properties, parameters: {}}));

        expect(hoisted.lastCall?.options.enabled).toBe(false);
        expect(result.current.displayConditions).toBeUndefined();
    });

    it('stays unevaluated without a component version', () => {
        const {result} = renderHook(() =>
            useFormDisplayConditions({
                ...properties,
                componentVersion: undefined,
                parameters: {bodyContentType: 'JSON'},
            })
        );

        expect(hoisted.lastCall?.options.enabled).toBe(false);
        expect(result.current.displayConditions).toBeUndefined();
    });

    it('stays unevaluated when the caller disables it', () => {
        renderHook(() =>
            useFormDisplayConditions({...properties, enabled: false, parameters: {bodyContentType: 'JSON'}})
        );

        expect(hoisted.lastCall?.options.enabled).toBe(false);
    });

    // Typing must not fire a request per keystroke, but the conditions still have to catch up on their own.
    it('debounces the parameters it evaluates against', () => {
        const {rerender} = renderHook(
            (parameters: Record<string, unknown>) => useFormDisplayConditions({...properties, parameters}),
            {initialProps: {bodyContentType: 'JSON'} as Record<string, unknown>}
        );

        rerender({bodyContentType: 'XML'});

        expect(hoisted.lastCall?.variables.parameters).toEqual({bodyContentType: 'JSON'});

        act(() => {
            vi.advanceTimersByTime(300);
        });

        expect(hoisted.lastCall?.variables.parameters).toEqual({bodyContentType: 'XML'});
    });
    // The query key carries the parameters, so every edit empties `data` until the next response lands. An
    // undefined verdict means "unevaluated", which shows every conditional property, so dropping it mid-edit
    // flashed the whole set of mutually exclusive properties on each keystroke.
    it('keeps the last evaluated conditions while the next evaluation is in flight', () => {
        const {rerender, result} = renderHook(
            (parameters: Record<string, unknown>) => useFormDisplayConditions({...properties, parameters}),
            {initialProps: {bodyContentType: 'JSON'} as Record<string, unknown>}
        );

        expect(result.current.displayConditions).toEqual({"bodyContentType == 'JSON'": true});

        hoisted.result = undefined;

        rerender({bodyContentType: 'XML'});

        expect(result.current.displayConditions).toEqual({"bodyContentType == 'JSON'": true});
        expect(result.current.isEvaluating).toBe(false);
    });

    it('reports it is evaluating until the first verdict arrives', () => {
        hoisted.result = undefined;

        const {result} = renderHook(() =>
            useFormDisplayConditions({...properties, parameters: {bodyContentType: 'JSON'}})
        );

        expect(result.current.isEvaluating).toBe(true);
    });

    it('does not report it is evaluating when the query never runs', () => {
        const {result} = renderHook(() => useFormDisplayConditions({...properties, parameters: {}}));

        expect(result.current.isEvaluating).toBe(false);
    });
});
