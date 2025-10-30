import {cleanup, render} from '@testing-library/react';
import {ReactElement} from 'react';
import {afterEach, vi} from 'vitest';

afterEach(() => {
    cleanup();
});

const customRender = (ui: ReactElement, options = {}) =>
    render(ui, {
        // wrap provider(s) here if needed
        wrapper: ({children}) => <>{children}</>,
        ...options,
    });

export const windowResizeObserver = () => {
    class MockResizeObserver {
        disconnect() {}
        /* eslint-disable @typescript-eslint/no-unused-vars */
        observe(_target?: Element, _options?: ResizeObserverOptions) {}
        /* eslint-disable @typescript-eslint/no-unused-vars */
        unobserve(_target?: Element) {}
    }

    // Assign as constructor-compatible classes
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (window as any).ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;
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
