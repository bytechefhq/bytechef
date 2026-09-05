import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useDataPillPanelStore from '../../stores/useDataPillPanelStore';
import useOpenDataPillPanel from '../useOpenDataPillPanel';

describe('useOpenDataPillPanel', () => {
    beforeEach(() => {
        useDataPillPanelStore.setState({dataPillPanelHasContent: false, dataPillPanelOpen: false});
    });

    it('opens the panel when it has something to render', () => {
        useDataPillPanelStore.setState({dataPillPanelHasContent: true});

        const {result} = renderHook(() => useOpenDataPillPanel());

        act(() => result.current());

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });

    it('leaves the panel closed when it would render nothing', () => {
        const {result} = renderHook(() => useOpenDataPillPanel());

        act(() => result.current());

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });

    it('reads the content flag at call time rather than at render time', () => {
        const {result} = renderHook(() => useOpenDataPillPanel());

        useDataPillPanelStore.setState({dataPillPanelHasContent: true});

        act(() => result.current());

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });
});
