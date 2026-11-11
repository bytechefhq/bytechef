import {describe, expect, it} from 'vitest';

import resolveAutomationIndexRedirect from './resolveAutomationIndexRedirect';

describe('resolveAutomationIndexRedirect', () => {
    it('lands on getting started until setup is done', () => {
        expect(resolveAutomationIndexRedirect({canEdit: true, completed: false})).toBe('getting-started');
    });

    it('lands on projects once setup is done', () => {
        expect(resolveAutomationIndexRedirect({canEdit: true, completed: true})).toBe('projects');
    });

    it('lands on projects for a viewer', () => {
        expect(resolveAutomationIndexRedirect({canEdit: false, completed: false})).toBe('projects');
    });

    it('lands on projects when completion is unknown', () => {
        expect(resolveAutomationIndexRedirect({canEdit: true, completed: undefined})).toBe('projects');
    });
});
