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

// ProseMirror/TipTap rely on DOM geometry APIs that jsdom does not fully implement.
// Provide minimal, test-only polyfills to avoid runtime errors like
// "TypeError: target.getClientRects is not a function" when computing selection coords.
// These values are neutral (zero rects) and sufficient for tests that don't assert layout.
// See: https://github.com/jsdom/jsdom/issues/3002
// and ProseMirror view's coordsAtPos/scrollToSelection internals.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const createZeroRect = (): any => ({
    x: 0,
    y: 0,
    width: 0,
    height: 0,
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    toJSON: () => ({}),
});

// Element geometry polyfills
if (!Element.prototype.getBoundingClientRect) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    Element.prototype.getBoundingClientRect = () => createZeroRect() as any;
}

if (!Element.prototype.getClientRects) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    Element.prototype.getClientRects = () => [createZeroRect()] as any;
}

// Range geometry polyfills
// jsdom provides Range, but some environments may miss geometry methods
// or return unsupported types for libraries expecting DOMRect/DOMRectList.
// Guard and polyfill as needed.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const RangeCtor: any = (globalThis as any).Range;

if (RangeCtor && !RangeCtor.prototype.getBoundingClientRect) {
    RangeCtor.prototype.getBoundingClientRect = () => createZeroRect();
}

if (RangeCtor && !RangeCtor.prototype.getClientRects) {
    RangeCtor.prototype.getClientRects = () => [createZeroRect()];
}

// document.elementFromPoint / elementsFromPoint polyfills for jsdom
// ProseMirror's posAtCoords relies on these APIs. jsdom may not provide them
// or they may be undefined on the specific root Document instance used by the editor.
// We provide neutral fallbacks that always return a valid Element.
// See: https://github.com/ProseMirror/prosemirror-view/blob/master/src/index.ts
// and https://github.com/jsdom/jsdom/issues/3002
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const ensurePointAPIs = (target: any) => {
    if (!target) {
        return;
    }

    if (!target.elementFromPoint) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        target.elementFromPoint = (_x: number, _y: number) => {
            const el = (document.querySelector('.ProseMirror') ||
                document.querySelector('[contenteditable="true"]')) as Element | null;
            return el ?? document.body;
        };
    }

    if (!target.elementsFromPoint) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        target.elementsFromPoint = (x: number, y: number) => {
            const el = (target.elementFromPoint as (x: number, y: number) => Element | null)(x, y);
            return el ? [el] : [];
        };
    }
};

// Apply to the global document instance
// eslint-disable-next-line @typescript-eslint/no-explicit-any
ensurePointAPIs(document as any);

// Apply to Document.prototype so calls bound to different documents also work
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const DocProto: any = (globalThis as any).Document?.prototype;

ensurePointAPIs(DocProto);

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
