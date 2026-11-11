import {describe, expect, it} from 'vitest';

import shouldShowAutomationGettingStarted from './shouldShowAutomationGettingStarted';

describe('shouldShowAutomationGettingStarted', () => {
    it('shows the nav item until setup is done', () => {
        expect(shouldShowAutomationGettingStarted({canEdit: true, completed: false, development: true})).toBe(true);
    });

    it('hides the nav item once done', () => {
        expect(shouldShowAutomationGettingStarted({canEdit: true, completed: true, development: true})).toBe(false);
    });

    it('hides the nav item for a viewer', () => {
        expect(shouldShowAutomationGettingStarted({canEdit: false, completed: false, development: true})).toBe(false);
    });

    it('hides the nav item while completion is unknown', () => {
        expect(shouldShowAutomationGettingStarted({canEdit: true, completed: undefined, development: true})).toBe(
            false
        );
    });

    it('hides the nav item outside Development', () => {
        expect(shouldShowAutomationGettingStarted({canEdit: true, completed: false, development: false})).toBe(false);
    });
});
