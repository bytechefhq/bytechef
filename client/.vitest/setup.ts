import '@testing-library/jest-dom';
import {vi} from 'vitest';

// Mock window.location to prevent JSDOM navigation errors
Object.defineProperty(window, 'location', {
    value: {
        href: 'http://localhost:3000',
        assign: vi.fn(),
        replace: vi.fn(),
        reload: vi.fn(),
    },
    writable: true,
});

// Mock window.open to prevent navigation errors
Object.defineProperty(window, 'open', {
    value: vi.fn(),
    writable: true,
});

// Polyfill requestAnimationFrame/cancelAnimationFrame
if (!globalThis.requestAnimationFrame) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (globalThis as any).requestAnimationFrame = (cb: FrameRequestCallback) => setTimeout(() => cb(Date.now()), 16);
}
if (!globalThis.cancelAnimationFrame) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (globalThis as any).cancelAnimationFrame = (id: number) => clearTimeout(id);
}

// Polyfill matchMedia used by some components/libs
if (!window.matchMedia) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (window as any).matchMedia = (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(), // deprecated
        removeListener: vi.fn(), // deprecated
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    });
}

// Provide constructor-compatible ResizeObserver for libraries like floating-ui/cmdk
if (!('ResizeObserver' in window)) {
    class MockResizeObserver {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        observe(_target?: Element, _options?: ResizeObserverOptions) {}
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        unobserve(_target?: Element) {}
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        disconnect() {}
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (window as any).ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;
}

// Provide constructor-compatible IntersectionObserver if missing
if (!('IntersectionObserver' in window)) {
    class MockIntersectionObserver {
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        constructor(_cb: IntersectionObserverCallback, _options?: IntersectionObserverInit) {}
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        observe(_target: Element) {}
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        unobserve(_target: Element) {}
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        disconnect() {}
        takeRecords(): IntersectionObserverEntry[] {
            return [];
        }
        root: Element | Document | null = null;
        rootMargin = '';
        thresholds: number[] = [];
    }
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (window as any).IntersectionObserver = MockIntersectionObserver as unknown as typeof IntersectionObserver;
}

// Ensure scrollIntoView exists
if (!Element.prototype.scrollIntoView) {
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    Element.prototype.scrollIntoView = vi.fn();
}

// Mock analytics libraries to avoid loading real SDKs in tests
vi.mock('posthog-js', () => {
    const posthog = {
        init: vi.fn(),
        capture: vi.fn(),
        identify: vi.fn(),
        reset: vi.fn(),
        isFeatureEnabled: vi.fn().mockReturnValue(false),
        onFeatureFlags: vi.fn(),
    };
    return {default: posthog};
});

vi.mock('posthog-js/react', () => {
    return {
        PostHogProvider: ({children}: {children: React.ReactNode}) => children,
        usePostHog: () => ({
            capture: vi.fn(),
            identify: vi.fn(),
            reset: vi.fn(),
        }),
    };
});
