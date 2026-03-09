import {describe, expect, it} from 'vitest';

/**
 * Replicates the display condition loading state logic from useProperty.ts.
 * Tests that cached query data (isSuccess=true) correctly resolves loading,
 * preventing infinite skeleton when React Query serves from cache.
 */

interface DisplayConditionLoadingParamsI {
    currentComponentDisplayConditions?: Record<string, boolean>;
    displayCondition?: string;
    isPending: boolean;
    isSuccess: boolean;
    type?: string;
}

/**
 * Derives whether the display condition fetching flag should be active.
 * Mirrors the useEffect at useProperty.ts:1506-1514.
 */
const getIsFetchingCurrentDisplayCondition = ({
    currentComponentDisplayConditions,
    displayCondition,
    isSuccess,
}: Pick<
    DisplayConditionLoadingParamsI,
    'currentComponentDisplayConditions' | 'displayCondition' | 'isSuccess'
>): boolean => {
    // Initial state is true (useState(true) in useProperty.ts:219)
    let isFetchingCurrentDisplayCondition = true;

    if (displayCondition && currentComponentDisplayConditions?.[displayCondition] !== undefined) {
        isFetchingCurrentDisplayCondition = true;

        if (isSuccess) {
            isFetchingCurrentDisplayCondition = false;
        }
    }

    return isFetchingCurrentDisplayCondition;
};

/**
 * Mirrors the isLoadingDisplayCondition derivation at useProperty.ts:1523-1531.
 */
const getIsLoadingDisplayCondition = ({
    currentComponentDisplayConditions,
    displayCondition,
    isPending,
    isSuccess,
    type,
}: DisplayConditionLoadingParamsI): boolean => {
    const hasDisplayConditionsQuery = true;

    const isFetchingCurrentDisplayCondition = getIsFetchingCurrentDisplayCondition({
        currentComponentDisplayConditions,
        displayCondition,
        isSuccess,
    });

    return !!(
        displayCondition &&
        type !== 'ARRAY' &&
        type !== 'OBJECT' &&
        (isPending ||
            (hasDisplayConditionsQuery &&
                currentComponentDisplayConditions?.[displayCondition] !== undefined &&
                isFetchingCurrentDisplayCondition))
    );
};

describe('Display Condition Loading State', () => {
    const defaultParams: DisplayConditionLoadingParamsI = {
        currentComponentDisplayConditions: {'someCondition == true': true},
        displayCondition: 'someCondition == true',
        isPending: false,
        isSuccess: true,
        type: 'STRING',
    };

    describe('when query data comes from cache (isSuccess=true, isPending=false)', () => {
        it('should NOT show loading skeleton', () => {
            const result = getIsLoadingDisplayCondition({
                ...defaultParams,
                isPending: false,
                isSuccess: true,
            });

            expect(result).toBe(false);
        });
    });

    describe('when query is pending (first load, no cache)', () => {
        it('should show loading skeleton', () => {
            const result = getIsLoadingDisplayCondition({
                ...defaultParams,
                isPending: true,
                isSuccess: false,
            });

            expect(result).toBe(true);
        });
    });

    describe('when query has not succeeded yet but is not pending (enabled=false)', () => {
        it('should show loading skeleton when component has the display condition', () => {
            const result = getIsLoadingDisplayCondition({
                ...defaultParams,
                isPending: false,
                isSuccess: false,
            });

            expect(result).toBe(true);
        });
    });

    describe('when property has no display condition', () => {
        it('should NOT show loading skeleton regardless of query state', () => {
            const result = getIsLoadingDisplayCondition({
                ...defaultParams,
                displayCondition: undefined,
                isPending: true,
                isSuccess: false,
            });

            expect(result).toBe(false);
        });
    });

    describe('when property type is ARRAY or OBJECT', () => {
        it('should NOT show loading skeleton for ARRAY type', () => {
            const result = getIsLoadingDisplayCondition({
                ...defaultParams,
                isPending: true,
                isSuccess: false,
                type: 'ARRAY',
            });

            expect(result).toBe(false);
        });

        it('should NOT show loading skeleton for OBJECT type', () => {
            const result = getIsLoadingDisplayCondition({
                ...defaultParams,
                isPending: true,
                isSuccess: false,
                type: 'OBJECT',
            });

            expect(result).toBe(false);
        });
    });

    describe('when component does not define the display condition', () => {
        it('should NOT show loading skeleton even if query is pending', () => {
            const result = getIsLoadingDisplayCondition({
                ...defaultParams,
                currentComponentDisplayConditions: {},
                isPending: false,
                isSuccess: false,
            });

            expect(result).toBe(false);
        });
    });

    describe('regression: isFetchedAfterMount vs isSuccess', () => {
        it('should resolve loading when cached data is available (the bug scenario)', () => {
            // Scenario: user opens node, switches away, comes back within staleTime.
            // React Query serves cached data: isPending=false, isSuccess=true,
            // but isFetchedAfterMount would be false (no network request after mount).
            // With the fix (using isSuccess), loading should resolve immediately.
            const result = getIsLoadingDisplayCondition({
                currentComponentDisplayConditions: {'type == "FILE"': true},
                displayCondition: 'type == "FILE"',
                isPending: false,
                isSuccess: true,
                type: 'STRING',
            });

            expect(result).toBe(false);
        });

        it('should still show loading when no data exists at all', () => {
            // First visit: no cache, query is fetching
            const result = getIsLoadingDisplayCondition({
                currentComponentDisplayConditions: {'type == "FILE"': true},
                displayCondition: 'type == "FILE"',
                isPending: true,
                isSuccess: false,
                type: 'STRING',
            });

            expect(result).toBe(true);
        });
    });
});
