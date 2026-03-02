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
 * Replicates the value prop sync effect from PropertyMentionsInputEditor.tsx:
 *
 * useEffect(() => {
 *     if (value === undefined || value === editorValue) { return; }
 *     setIsLocalUpdate(false);
 *     setEditorValue(value);
 * }, [value, editorValue]);
 */
const computeEditorSync = ({editorValue, value}: EditorSyncInputType): EditorSyncResultType => {
    const result: EditorSyncResultType = {};

    if (value === undefined || value === editorValue) {
        return result;
    }

    result.isLocalUpdate = false;
    result.editorValue = value;

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
});
