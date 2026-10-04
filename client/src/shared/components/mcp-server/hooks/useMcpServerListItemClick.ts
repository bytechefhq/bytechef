import {MouseEvent, useCallback, useRef} from 'react';

import isCollapsibleRowClick, {ROW_INTERACTIVE_SELECTORS} from '../utils/isCollapsibleRowClick';

const SERVER_ROW_INTERACTIVE_SELECTORS = [...ROW_INTERACTIVE_SELECTORS, 'svg'];

const useMcpServerListItemClick = () => {
    const toolsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    const handleMcpServerListItemClick = useCallback((event: MouseEvent) => {
        if (!isCollapsibleRowClick(event, SERVER_ROW_INTERACTIVE_SELECTORS)) {
            return;
        }

        if (toolsCollapsibleTriggerRef.current?.contains(event.target as HTMLElement)) {
            return;
        }

        toolsCollapsibleTriggerRef.current?.click();
    }, []);

    return {handleMcpServerListItemClick, toolsCollapsibleTriggerRef};
};

export default useMcpServerListItemClick;
