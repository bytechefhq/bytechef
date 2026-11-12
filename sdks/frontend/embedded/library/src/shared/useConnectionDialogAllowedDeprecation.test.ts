import {renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import useConnectionDialogAllowedDeprecation from './useConnectionDialogAllowedDeprecation';

describe('useConnectionDialogAllowedDeprecation', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('does not warn when the prop is omitted', () => {
        const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        renderHook(() => useConnectionDialogAllowedDeprecation('OmittedComponent', undefined));

        expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('warns once per component when the prop is passed', () => {
        const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        const {rerender} = renderHook(
            ({connectionDialogAllowed}) =>
                useConnectionDialogAllowedDeprecation('PassedComponent', connectionDialogAllowed),
            {initialProps: {connectionDialogAllowed: true}}
        );

        rerender({connectionDialogAllowed: false});

        renderHook(() => useConnectionDialogAllowedDeprecation('PassedComponent', true));

        expect(consoleWarnSpy).toHaveBeenCalledTimes(1);
        expect(consoleWarnSpy).toHaveBeenCalledWith(expect.stringContaining('PassedComponent'));
    });
});
