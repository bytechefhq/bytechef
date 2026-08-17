/**
 * Whether a click landed on something that handles the click itself, so a row-level handler must stay out of
 * its way.
 *
 * List rows make the whole row clickable — opening a detail page, expanding a collapsible — while carrying
 * switches, menus and tag editors that mean something different. The alternative, stopping propagation on the
 * containers that hold those controls, also swallows every click on the empty space around them, which is why
 * clicking beside a badge used to do nothing at all. This inverts it: the row acts unless the click reached an
 * actual control.
 *
 * `svg` is in the list because an icon inside a button is what the pointer usually hits, and `closest` walks up
 * from there; `[data-interactive]` is the opt-in for anything the selectors below cannot describe.
 */
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
