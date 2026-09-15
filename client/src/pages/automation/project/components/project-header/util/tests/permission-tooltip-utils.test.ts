import {getDisabledControlTooltip} from '@/pages/automation/project/components/project-header/util/permission-tooltip-utils';
import {describe, expect, it} from 'vitest';

const MESSAGES = {
    deniedMessage: 'You do not have permission to publish this project',
    unmetPreconditionMessage: 'No changes to publish',
};

describe('getDisabledControlTooltip', () => {
    it('reports the denial only once the scopes have loaded without the required one', () => {
        expect(
            getDisabledControlTooltip({
                ...MESSAGES,
                granted: false,
                permissionsError: false,
                permissionsLoading: false,
                permissionsUnknown: false,
            })
        ).toBe('You do not have permission to publish this project');
    });

    it('reports loading rather than a denial while the scopes are in flight', () => {
        // The bug this guards: useHasWorkspaceScope collapses 'loading' into false, so an EDITOR who genuinely holds
        // the scope was told "You do not have permission" on every project open until the fetch landed.
        expect(
            getDisabledControlTooltip({
                ...MESSAGES,
                granted: false,
                permissionsError: false,
                permissionsLoading: true,
                permissionsUnknown: false,
            })
        ).toBe('Checking your permissions');
    });

    it('reports a failed check rather than a denial when the scope fetch errored', () => {
        expect(
            getDisabledControlTooltip({
                ...MESSAGES,
                granted: false,
                permissionsError: true,
                permissionsLoading: false,
                permissionsUnknown: false,
            })
        ).toBe('Could not verify your permissions');
    });

    it('reports an undetermined check rather than a denial when the edition never resolved', () => {
        // The hole the loading/error split left open. /actuator/info never retries after a non-200, so the edition can
        // stay unresolved for a whole session: the EE-only scope query is never sent, nothing loads, nothing errors,
        // and the store holds no entry — which arrives here as granted/loading/error all false and used to fall
        // through to deniedMessage. No server refused anything, so the tooltip must not say one did.
        expect(
            getDisabledControlTooltip({
                ...MESSAGES,
                granted: false,
                permissionsError: false,
                permissionsLoading: false,
                permissionsUnknown: true,
            })
        ).toBe('Still determining your permissions');
    });

    it('falls back to the business reason when the user is entitled', () => {
        expect(
            getDisabledControlTooltip({
                ...MESSAGES,
                granted: true,
                permissionsError: false,
                permissionsLoading: false,
                permissionsUnknown: false,
            })
        ).toBe('No changes to publish');
    });

    it('prefers loading over error when both are set', () => {
        // Both flags are ORed across two scope lookups, so a re-fetch after a failure can briefly report both. The
        // fresher state wins, and neither is a denial.
        expect(
            getDisabledControlTooltip({
                ...MESSAGES,
                granted: false,
                permissionsError: true,
                permissionsLoading: true,
                permissionsUnknown: false,
            })
        ).toBe('Checking your permissions');
    });

    it('prefers loading over an unresolved edition when both are set', () => {
        // The callers OR an unresolved workspace id into permissionsLoading, so both can be true at once on first
        // paint. Either way the answer is "not yet", never a denial.
        expect(
            getDisabledControlTooltip({
                ...MESSAGES,
                granted: false,
                permissionsError: false,
                permissionsLoading: true,
                permissionsUnknown: true,
            })
        ).toBe('Checking your permissions');
    });
});
