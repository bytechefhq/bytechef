import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useOnEnvironmentChange} from '../useOnEnvironmentChange';

const INITIAL_ENVIRONMENT_ID = 1;

describe('useOnEnvironmentChange', () => {
    beforeEach(() => {
        environmentStore.setState({currentEnvironmentId: INITIAL_ENVIRONMENT_ID, environments: []});
    });

    afterEach(() => {
        environmentStore.setState({currentEnvironmentId: INITIAL_ENVIRONMENT_ID, environments: []});
    });

    it('does not invoke callback on initial mount', () => {
        const callback = vi.fn();

        renderHook(() => useOnEnvironmentChange(callback));

        expect(callback).not.toHaveBeenCalled();
    });

    it('invokes callback when currentEnvironmentId changes', () => {
        const callback = vi.fn();

        renderHook(() => useOnEnvironmentChange(callback));

        act(() => {
            environmentStore.setState({currentEnvironmentId: 2});
        });

        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('does not invoke callback when environment is set to the same id', () => {
        const callback = vi.fn();

        renderHook(() => useOnEnvironmentChange(callback));

        act(() => {
            environmentStore.setState({currentEnvironmentId: INITIAL_ENVIRONMENT_ID});
        });

        expect(callback).not.toHaveBeenCalled();
    });

    it('invokes callback once per distinct environment change', () => {
        const callback = vi.fn();

        renderHook(() => useOnEnvironmentChange(callback));

        act(() => {
            environmentStore.setState({currentEnvironmentId: 2});
        });
        act(() => {
            environmentStore.setState({currentEnvironmentId: 3});
        });

        expect(callback).toHaveBeenCalledTimes(2);
    });

    it('uses the latest callback when the consumer re-renders with a new one', () => {
        const initialCallback = vi.fn();
        const laterCallback = vi.fn();

        const {rerender} = renderHook(({callback}) => useOnEnvironmentChange(callback), {
            initialProps: {callback: initialCallback},
        });

        rerender({callback: laterCallback});

        act(() => {
            environmentStore.setState({currentEnvironmentId: 2});
        });

        expect(initialCallback).not.toHaveBeenCalled();
        expect(laterCallback).toHaveBeenCalledTimes(1);
    });
});
