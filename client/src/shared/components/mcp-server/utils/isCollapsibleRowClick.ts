import type {MouseEvent} from 'react';

const ROW_INTERACTIVE_SELECTORS = [
    '[data-interactive]',
    '.dropdown-menu-item',
    '[data-radix-dropdown-menu-item]',
    '[data-radix-dropdown-menu-trigger]',
    '[data-radix-collapsible-trigger]',
    '[role="menuitem"]',
    'a',
    'button',
    'input',
];

/**
 * Tells whether a click on a collapsible row should toggle it: the click must land on the row itself, not on one of
 * its interactive elements, and not on portaled content (dropdown menu items, dialogs) whose clicks bubble through
 * the React tree without being DOM descendants of the row.
 */
const isCollapsibleRowClick = (event: MouseEvent, interactiveSelectors: string[] = ROW_INTERACTIVE_SELECTORS) => {
    const target = event.target as HTMLElement;

    if (!event.currentTarget.contains(target)) {
        return false;
    }

    return !target.closest(interactiveSelectors.join(', '));
};

export {ROW_INTERACTIVE_SELECTORS};

export default isCollapsibleRowClick;
