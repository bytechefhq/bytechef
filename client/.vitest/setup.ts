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
