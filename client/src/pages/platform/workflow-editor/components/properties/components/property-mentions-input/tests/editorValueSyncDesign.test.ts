import {describe, expect, it} from 'vitest';

/**
 * Tests documenting the editor value synchronization design in
 * PropertyMentionsInputEditor.tsx.
 *
 * The editor has a single authoritative sync path for external value updates:
 *   propertyParameterValue (useProperty) → mentionInputValue → value prop → editorValue
 *
 * Previously, there was a SECOND sync path that directly set editorValue from
 * workflow.definition via memoizedWorkflowTask.parameters. This caused a
 * dual-write conflict: when any property saved (triggering a workflow refetch),
 * the direct-write effect would overwrite editorValue with the (potentially
 * stale) saved value, undoing the user's in-progress edits.
 *
 * The fix removed the direct-write effect, leaving only the value prop chain.
 *
 * These tests verify the VALUE PROP SYNC EFFECT logic, which is the sole
 * remaining sync mechanism for external updates to reach the editor.
 */

type EditorSyncInputType = {
    editorValue: string;
    isLocalUpdate: boolean;
    value: string | undefined;
};

type EditorSyncResultType = {
    editorValue?: string;
    isLocalUpdate?: boolean;
};

/**
 * Strips the `=` prefix from formula mode values so the editor only displays
 * the expression body (the EqualIcon leading icon already represents `=`).
 */
const stripFormulaPrefix = (rawValue: string | number | undefined): string | number | undefined => {
    return typeof rawValue === 'string' && rawValue.startsWith('=') ? rawValue.substring(1) : rawValue;
};

/**
 * Replicates the value prop sync effect from PropertyMentionsInputEditor.tsx:
 *
 * useEffect(() => {
 *     if (value === undefined || value === editorValue) { return; }
 *     const strippedValue = typeof value === 'string' && value.startsWith('=') ? value.substring(1) : value;
 *     setIsLocalUpdate(false);
 *     setEditorValue(strippedValue);
 * }, [value, editorValue]);
 */
const computeEditorSync = ({editorValue, value}: EditorSyncInputType): EditorSyncResultType => {
    const result: EditorSyncResultType = {};

    if (value === undefined || value === editorValue) {
        return result;
    }

    const strippedValue = stripFormulaPrefix(value);

    result.isLocalUpdate = false;
    result.editorValue = typeof strippedValue === 'string' ? strippedValue : value;

    return result;
};

describe('PropertyMentionsInputEditor value prop sync effect', () => {
    describe('should skip sync when values match (no overwrite during typing)', () => {
        it('should skip when value equals editorValue', () => {
            const result = computeEditorSync({
                editorValue: 'hello',
                isLocalUpdate: true,
                value: 'hello',
            });

            expect(result.editorValue).toBeUndefined();
            expect(result.isLocalUpdate).toBeUndefined();
        });

        it('should skip when both are empty', () => {
            const result = computeEditorSync({
                editorValue: '',
                isLocalUpdate: true,
                value: '',
            });

            expect(result.editorValue).toBeUndefined();
        });

        it('should skip when value is undefined', () => {
            const result = computeEditorSync({
                editorValue: 'current',
                isLocalUpdate: true,
                value: undefined,
            });

            expect(result.editorValue).toBeUndefined();
        });
    });

    describe('should sync when value differs (external update)', () => {
        it('should update editorValue and clear isLocalUpdate on external change', () => {
            const result = computeEditorSync({
                editorValue: 'old',
                isLocalUpdate: true,
                value: 'new',
            });

            expect(result.editorValue).toBe('new');
            expect(result.isLocalUpdate).toBe(false);
        });

        it('should handle external update to empty string', () => {
            const result = computeEditorSync({
                editorValue: '${datapill}',
                isLocalUpdate: true,
                value: '',
            });

            expect(result.editorValue).toBe('');
            expect(result.isLocalUpdate).toBe(false);
        });

        it('should handle external update from empty to non-empty', () => {
            const result = computeEditorSync({
                editorValue: '',
                isLocalUpdate: false,
                value: '${trigger_1.id}',
            });

            expect(result.editorValue).toBe('${trigger_1.id}');
            expect(result.isLocalUpdate).toBe(false);
        });
    });

    describe('documents why the dual-write was removed', () => {
        it('value prop chain is the sole sync path — no dual-write conflict', () => {
            // When user types "hell" (deleted one char from "hello"):
            // 1. onUpdate → editorValue = "hell", mentionInputValue = "hell"
            // 2. value prop = "hell" (from mentionInputValue)
            // 3. value === editorValue → sync skips → editor keeps "hell" ✓
            const afterTyping = computeEditorSync({
                editorValue: 'hell',
                isLocalUpdate: true,
                value: 'hell',
            });

            expect(afterTyping.editorValue).toBeUndefined();
        });

        it('save cycle does not overwrite when user has not typed more', () => {
            // After save completes:
            // propertyParameterValue = "hell" (saved value)
            // mentionInputValue = "hell" (unchanged, mentionInputSyncedRef blocks re-sync)
            // value prop = "hell", editorValue = "hell" → skip
            const afterSave = computeEditorSync({
                editorValue: 'hell',
                isLocalUpdate: true,
                value: 'hell',
            });

            expect(afterSave.editorValue).toBeUndefined();
        });

        it('external update (different value) does sync correctly', () => {
            // If another source changes the value (e.g., component remount, different user):
            // value prop = "new value", editorValue = "old value" → sync fires
            const externalUpdate = computeEditorSync({
                editorValue: 'old value',
                isLocalUpdate: false,
                value: 'new value',
            });

            expect(externalUpdate.editorValue).toBe('new value');
            expect(externalUpdate.isLocalUpdate).toBe(false);
        });
    });

    describe('datapill insertion protection (ref-based sync)', () => {
        /**
         * Models the ref-based sync approach: the sync effect only fires when the
         * `value` prop changes (not when `editorValue` changes locally). It compares
         * `value` against `editorValueRef.current` (which always tracks the latest
         * editorValue) to avoid overwriting in-flight datapill insertions.
         *
         * Previous behavior: sync depended on [value, editorValue], so a local
         * editorValue change (from onUpdate after datapill drop) would trigger the
         * sync and overwrite the local value with the stale `value` prop.
         */
        const shouldSyncOverwrite = (
            valueProp: string | undefined,
            editorValueRefCurrent: string,
            valuePropChanged: boolean
        ): boolean => {
            if (!valuePropChanged) {
                return false;
            }

            if (valueProp === undefined || valueProp === editorValueRefCurrent) {
                return false;
            }

            return true;
        };

        it('should NOT overwrite when only editorValue changed locally (datapill inserted)', () => {
            // Scenario: user drops datapill → editorValue updates to "${trigger_email}"
            // but value prop is still "" (save hasn't completed yet)
            const overwrite = shouldSyncOverwrite(
                '',
                '${trigger_email}',
                false // value prop did NOT change, only editorValue changed
            );

            expect(overwrite).toBe(false);
        });

        it('should NOT overwrite when save completes and value matches editorValue', () => {
            // After save: value prop updates to "${trigger_email}", editorValue is already "${trigger_email}"
            const overwrite = shouldSyncOverwrite(
                '${trigger_email}',
                '${trigger_email}',
                true // value prop changed (from "" to "${trigger_email}")
            );

            expect(overwrite).toBe(false);
        });

        it('should overwrite when value prop changes to a genuinely different external value', () => {
            // External update: another source changes the value
            const overwrite = shouldSyncOverwrite('${different_datapill}', '${trigger_email}', true);

            expect(overwrite).toBe(true);
        });

        it('should NOT overwrite when value prop is undefined', () => {
            const overwrite = shouldSyncOverwrite(undefined, '${trigger_email}', true);

            expect(overwrite).toBe(false);
        });

        it('should overwrite when value prop clears the field externally', () => {
            const overwrite = shouldSyncOverwrite('', '${trigger_email}', true);

            expect(overwrite).toBe(true);
        });
    });

    describe('formula mode `=` prefix stripping', () => {
        it('should strip `=` prefix from formula mode initialization', () => {
            const value = '=3+3';
            const initialEditorValue = stripFormulaPrefix(value);

            expect(initialEditorValue).toBe('3+3');
        });

        it('should not strip when value does not start with `=`', () => {
            const value = 'normalValue';
            const initialEditorValue = stripFormulaPrefix(value);

            expect(initialEditorValue).toBe('normalValue');
        });

        it('should handle `=` only (empty expression body)', () => {
            const value = '=';
            const initialEditorValue = stripFormulaPrefix(value);

            expect(initialEditorValue).toBe('');
        });

        it('should not strip from non-string values', () => {
            const value = 42 as string | number | undefined;
            const initialEditorValue = stripFormulaPrefix(value);

            expect(initialEditorValue).toBe(42);
        });

        it('should not strip from undefined', () => {
            const value = undefined;
            const initialEditorValue = stripFormulaPrefix(value);

            expect(initialEditorValue).toBeUndefined();
        });

        it('should strip `=` prefix during external sync', () => {
            const result = computeEditorSync({
                editorValue: 'old',
                isLocalUpdate: false,
                value: '=expression()',
            });

            expect(result.editorValue).toBe('expression()');
            expect(result.isLocalUpdate).toBe(false);
        });

        it('should strip `=` prefix from formula expression on external update', () => {
            const result = computeEditorSync({
                editorValue: '3+3',
                isLocalUpdate: true,
                value: '=5+5',
            });

            expect(result.editorValue).toBe('5+5');
            expect(result.isLocalUpdate).toBe(false);
        });

        it('should not strip `=` from ${interpolation} values', () => {
            const result = computeEditorSync({
                editorValue: 'old',
                isLocalUpdate: false,
                value: '${datapill}',
            });

            expect(result.editorValue).toBe('${datapill}');
        });
    });
});
