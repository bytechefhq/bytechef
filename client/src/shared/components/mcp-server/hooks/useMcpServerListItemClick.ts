import {MouseEvent, useCallback, useRef} from 'react';

const INTERACTIVE_SELECTORS = [
    '[data-interactive]',
    '.dropdown-menu-item',
    '[data-radix-dropdown-menu-item]',
    '[data-radix-dropdown-menu-trigger]',
    '[data-radix-collapsible-trigger]',
    'button',
    'input',
    'svg',
].join(', ');

const useMcpServerListItemClick = () => {
    const toolsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    const handleMcpServerListItemClick = useCallback((event: MouseEvent) => {
        const target = event.target as HTMLElement;

        // Clicks from portaled content (dropdown menu items, dialogs) bubble through the React tree but are not
        // DOM descendants of the list item, so they must not toggle the tools collapsible.
        if (!event.currentTarget.contains(target)) {
            return;
        }

        if (target.closest(INTERACTIVE_SELECTORS)) {
            return;
        }

        if (toolsCollapsibleTriggerRef.current?.contains(target)) {
            return;
        }

        toolsCollapsibleTriggerRef.current?.click();
    }, []);

    return {handleMcpServerListItemClick, toolsCollapsibleTriggerRef};
};

export default useMcpServerListItemClick;
