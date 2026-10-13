import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {useEffect} from 'react';

const useCopilotCompactHeader = () => {
    const setCompactHeader = useCopilotPanelStore((state) => state.setCompactHeader);

    useEffect(() => {
        setCompactHeader(true);

        return () => setCompactHeader(false);
    }, [setCompactHeader]);
};

export default useCopilotCompactHeader;
