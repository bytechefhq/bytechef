import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import {useCallback} from 'react';

export default function useOpenDataPillPanel(): () => void {
    const setDataPillPanelOpen = useDataPillPanelStore((state) => state.setDataPillPanelOpen);

    return useCallback(() => {
        if (!useDataPillPanelStore.getState().dataPillPanelHasContent) {
            return;
        }

        setDataPillPanelOpen(true);
    }, [setDataPillPanelOpen]);
}
