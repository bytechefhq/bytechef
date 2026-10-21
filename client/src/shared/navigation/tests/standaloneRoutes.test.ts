import {isStandaloneRoute} from '@/shared/navigation/standaloneRoutes';
import {describe, expect, it} from 'vitest';

describe('isStandaloneRoute', () => {
    it('matches the approval tasks page and its nested paths', () => {
        expect(isStandaloneRoute('/automation/approval-tasks')).toBe(true);
        expect(isStandaloneRoute('/automation/approval-tasks/42')).toBe(true);
    });

    it('matches the App Portal, which opens without the builder navigation', () => {
        expect(isStandaloneRoute('/automation/portal')).toBe(true);
    });

    it('does not match other pages or a shared prefix', () => {
        expect(isStandaloneRoute('/automation/projects')).toBe(false);
        expect(isStandaloneRoute('/automation/approval-tasks-archive')).toBe(false);
    });
});
