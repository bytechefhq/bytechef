import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useLayoutEngineStore from '../useLayoutEngineStore';

describe('useLayoutEngineStore', () => {
    beforeEach(() => {
        useLayoutEngineStore.setState({layoutEngine: 'dagre'});
    });

    it('should initialize with the dagre engine', () => {
        const {result} = renderHook(() => useLayoutEngineStore());

        expect(result.current.layoutEngine).toBe('dagre');
    });

    it('should switch to the elk engine', () => {
        const {result} = renderHook(() => useLayoutEngineStore());

        act(() => {
            result.current.setLayoutEngine('elk');
        });

        expect(result.current.layoutEngine).toBe('elk');
    });

    it('should switch back to the dagre engine', () => {
        const {result} = renderHook(() => useLayoutEngineStore());

        act(() => {
            result.current.setLayoutEngine('elk');
        });

        act(() => {
            result.current.setLayoutEngine('dagre');
        });

        expect(result.current.layoutEngine).toBe('dagre');
    });
});
