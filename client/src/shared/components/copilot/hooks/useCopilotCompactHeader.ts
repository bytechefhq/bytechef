import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {useEffect} from 'react';

/**
 * Lines the Copilot panel header up with a page header that is shorter than the default one, such as a workflow
 * editor toolbar, for as long as the calling header is mounted.
 */
const useCopilotCompactHeader = () => {
    const setCompactHeader = useCopilotPanelStore((state) => state.setCompactHeader);

    useEffect(() => {
        setCompactHeader(true);

        return () => setCompactHeader(false);
    }, [setCompactHeader]);
};

export default useCopilotCompactHeader;
