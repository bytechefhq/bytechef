import {renderHook} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import {useInitialLoadComplete} from '../useInitialLoadComplete';

describe('useInitialLoadComplete', () => {
    it('is false while the first load is running', () => {
        const {result} = renderHook(() => useInitialLoadComplete(true));

        expect(result.current).toBe(false);
    });

    it('is true when the data is already loaded', () => {
        const {result} = renderHook(() => useInitialLoadComplete(false));

        expect(result.current).toBe(true);
    });

    it('stays true when a later reload starts', () => {
        const {rerender, result} = renderHook(({isLoading}) => useInitialLoadComplete(isLoading), {
            initialProps: {isLoading: true},
        });

        rerender({isLoading: false});

        expect(result.current).toBe(true);

        rerender({isLoading: true});

        expect(result.current).toBe(true);
    });
});
