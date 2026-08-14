import {createContext, useContext} from 'react';

interface LeftSidebarToggleContextI {
    /** False on pages that render no left sidebar — the header must not offer to toggle one. */
    hasLeftSidebar: boolean;
    leftSidebarOpen: boolean;
    toggleLeftSidebar: () => void;
}

/**
 * Lets the shared `Header` offer a left-sidebar toggle without every page threading its own open state
 * through to it.
 *
 * `LayoutContainer` renders the header inside its own tree, so it can provide this — and it already owns the
 * sidebar. The alternative was a `useState` plus two props on each of the ~34 pages that have a sidebar, which
 * is the same behaviour written thirty-four times.
 *
 * The default has `hasLeftSidebar: false`, so a `Header` rendered outside a `LayoutContainer` (or on a page
 * with no sidebar) shows no toggle rather than one that does nothing.
 *
 * A page that passes `leftSidebarOpen` is treated as controlling its own sidebar and gets no toggle — so
 * passing a CONSTANT there silently opts the page out. Pass it only when the value actually varies (the AI
 * Hub rail); otherwise omit it.
 */
const LeftSidebarToggleContext = createContext<LeftSidebarToggleContextI>({
    hasLeftSidebar: false,
    leftSidebarOpen: true,
    toggleLeftSidebar: () => {},
});

export const useLeftSidebarToggle = (): LeftSidebarToggleContextI => useContext(LeftSidebarToggleContext);

export default LeftSidebarToggleContext;
