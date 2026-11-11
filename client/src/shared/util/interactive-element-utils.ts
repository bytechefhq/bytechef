const INTERACTIVE_SELECTORS = [
    '[data-interactive]',
    '[role="menuitem"]',
    '.dropdown-menu-item',
    '[data-radix-dropdown-menu-trigger]',
    '[data-radix-collapsible-trigger]',
    'a',
    'button',
    'input',
    'svg',
].join(', ');

const isInteractiveElementClick = (target: EventTarget | null): boolean =>
    target instanceof HTMLElement && target.closest(INTERACTIVE_SELECTORS) != null;

export default isInteractiveElementClick;
