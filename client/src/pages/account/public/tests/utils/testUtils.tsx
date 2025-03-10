import {cleanup} from '@testing-library/react';
import {vi} from 'vitest';

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
