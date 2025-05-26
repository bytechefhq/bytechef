import {cleanup, render} from '@testing-library/react';
import {afterEach, vi} from 'vitest';

afterEach(() => {
    cleanup();
});

const customRender = (ui: React.ReactElement, options = {}) =>
    render(ui, {
        // wrap provider(s) here if needed
        wrapper: ({children}) => <>{children}</>,
        ...options,
    });

export const windowResizeObserver = () => {
    window.ResizeObserver = vi.fn(() => ({
        disconnect: vi.fn(),
        observe: vi.fn(),
        unobserve: vi.fn(),
    }));
};

export const resetAll = () => {
    cleanup();

    vi.clearAllMocks();

    if (window.ResizeObserver) {
        delete (window as unknown as {ResizeObserver?: ResizeObserver}).ResizeObserver;
    }

    vi.resetModules();
};

export function mockScrollIntoView() {
    Element.prototype.scrollIntoView = vi.fn();
}

export * from '@testing-library/react';
export {default as userEvent} from '@testing-library/user-event';
// override render export
export {customRender as render};
