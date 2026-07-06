import {describe, expect, it} from 'vitest';

type ReconcileResultType = {action: 'adopt'; value: unknown} | {action: 'clear'} | {action: 'none'};

function reconcileEmptyPropertyParameterValue(
    propertyParameterValue: unknown,
    authoritativeParameterValue: unknown
): ReconcileResultType {
    const isEmpty = propertyParameterValue === '' || propertyParameterValue === undefined;

    if (!isEmpty) {
        return {action: 'none'};
    }

    const hasAuthoritativeValue =
        authoritativeParameterValue !== undefined &&
        authoritativeParameterValue !== null &&
        authoritativeParameterValue !== '';

    if (hasAuthoritativeValue) {
        return {action: 'adopt', value: authoritativeParameterValue};
    }

    return {action: 'clear'};
}

describe('selectValueReconciliation', () => {
    it('adopts the authoritative parameterValue when propertyParameterValue is stale-empty (skills reset)', () => {
        expect(reconcileEmptyPropertyParameterValue('', 1063)).toEqual({action: 'adopt', value: 1063});
    });

    it('adopts when propertyParameterValue is undefined but parameterValue is a valid id', () => {
        expect(reconcileEmptyPropertyParameterValue(undefined, 1057)).toEqual({action: 'adopt', value: 1057});
    });

    it('clears when both the local state and the authoritative value are empty', () => {
        expect(reconcileEmptyPropertyParameterValue('', '')).toEqual({action: 'clear'});
        expect(reconcileEmptyPropertyParameterValue('', undefined)).toEqual({action: 'clear'});
        expect(reconcileEmptyPropertyParameterValue(undefined, null)).toEqual({action: 'clear'});
    });

    it('adopts a boolean false authoritative value rather than clearing', () => {
        expect(reconcileEmptyPropertyParameterValue('', false)).toEqual({action: 'adopt', value: false});
    });

    it('does nothing when propertyParameterValue is already a non-empty value', () => {
        expect(reconcileEmptyPropertyParameterValue(1063, 1063)).toEqual({action: 'none'});
    });
});
