import {beforeEach, describe, expect, it} from 'vitest';

import useDataPillPanelStore from '../useDataPillPanelStore';

describe('useDataPillPanelStore', () => {
    beforeEach(() => {
        useDataPillPanelStore.setState({dataPillPanelHasContent: false, dataPillPanelOpen: false});
    });

    it('starts out with nothing to show', () => {
        expect(useDataPillPanelStore.getState().dataPillPanelHasContent).toBe(false);
    });

    it('records whether the panel would render anything', () => {
        useDataPillPanelStore.getState().setDataPillPanelHasContent(true);

        expect(useDataPillPanelStore.getState().dataPillPanelHasContent).toBe(true);

        useDataPillPanelStore.getState().setDataPillPanelHasContent(false);

        expect(useDataPillPanelStore.getState().dataPillPanelHasContent).toBe(false);
    });

    it('keeps the open state independent of the content flag', () => {
        useDataPillPanelStore.getState().setDataPillPanelOpen(true);
        useDataPillPanelStore.getState().setDataPillPanelHasContent(true);

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });
});
