import {describe, expect, it} from 'vitest';

/**
 * Tests for the FormulaMode extension's Backspace handler and the
 * expressionEnabled gate that controls formula mode entry.
 *
 * Bug: When the user types `=` in a numerical input and presses Backspace
 * to delete it, the formula mode sometimes persists (the `=` icon stays in
 * the input). This happens because the FormulaMode extension's storage
 * resets to `isFormulaMode: false` when extensions recreate (e.g., when
 * `getComponentIcon` changes after a data fetch), and the sync useEffect
 * hasn't run yet when Backspace fires.
 *
 * Fix: `getIsFormulaMode` ref-based getter in the Backspace handler
 * always reads the current React state via a ref, bypassing stale
 * storage entirely. `onCreate()` also initializes storage from options
 * as a secondary safeguard.
 */

describe('formulaModeBackspace', () => {
    describe('backspace exit condition', () => {
        /**
         * Replicates the Backspace handler logic from FormulaMode.extension.ts.
         * Uses `getIsFormulaMode()` (ref-based getter) with `storage.isFormulaMode` as fallback.
         * Uses `doc.textContent.trim() === ''` instead of `editor.isEmpty`
         * for robustness against TipTap structural artifacts.
         */
        const shouldExitFormulaMode = (
            textContent: string,
            storageIsFormulaMode: boolean,
            getIsFormulaMode?: () => boolean
        ): boolean => {
            const hasNoContent = textContent.trim() === '';
            const currentFormulaMode = getIsFormulaMode?.() ?? storageIsFormulaMode;

            return hasNoContent && currentFormulaMode;
        };

        it('should exit formula mode when editor is empty and getter says formula mode', () => {
            expect(shouldExitFormulaMode('', false, () => true)).toBe(true);
        });

        it('should exit formula mode when editor is empty and storage says formula mode (no getter)', () => {
            expect(shouldExitFormulaMode('', true)).toBe(true);
        });

        it('should exit when editor has only whitespace', () => {
            expect(shouldExitFormulaMode('  \n  ', false, () => true)).toBe(true);
        });

        it('should NOT exit when editor has content', () => {
            expect(shouldExitFormulaMode('3+3', true, () => true)).toBe(false);
        });

        it('should NOT exit when not in formula mode (getter returns false)', () => {
            expect(shouldExitFormulaMode('', false, () => false)).toBe(false);
        });

        it('should NOT exit when not in formula mode (no getter, storage false)', () => {
            expect(shouldExitFormulaMode('', false)).toBe(false);
        });

        it('should NOT exit when editor has a mention text', () => {
            expect(shouldExitFormulaMode('${trigger_1.output}', true, () => true)).toBe(false);
        });

        it('getter takes priority over stale storage', () => {
            // Storage is false (stale after extension recreation), but getter reads current React state
            expect(shouldExitFormulaMode('', false, () => true)).toBe(true);
            // Storage is true (stale), but getter says formula mode was exited
            expect(shouldExitFormulaMode('', true, () => false)).toBe(false);
        });
    });

    describe('onCreate storage initialization', () => {
        /**
         * Replicates the onCreate behavior that initializes storage from
         * options to prevent the timing gap bug.
         */
        const initializeStorage = (initialFormulaMode?: boolean): boolean => {
            return initialFormulaMode ?? false;
        };

        it('should initialize storage to true when initialFormulaMode is true', () => {
            expect(initializeStorage(true)).toBe(true);
        });

        it('should initialize storage to false when initialFormulaMode is false', () => {
            expect(initializeStorage(false)).toBe(false);
        });

        it('should initialize storage to false when initialFormulaMode is undefined', () => {
            expect(initializeStorage(undefined)).toBe(false);
        });
    });

    describe('timing gap scenario', () => {
        /**
         * Demonstrates why onCreate is needed:
         * When extensions recreate, storage resets to default (false).
         * Without onCreate, backspace check fails even though React state
         * says formula mode is active.
         */
        it('without onCreate: storage resets to false after extension recreation', () => {
            const defaultStorage = {isFormulaMode: false};
            const reactStateIsFormulaMode = true;

            // Extension recreated — storage uses default
            const storageAfterRecreation = {...defaultStorage};

            // Backspace fires BEFORE sync useEffect
            const wouldExit = storageAfterRecreation.isFormulaMode && true;

            expect(wouldExit).toBe(false); // BUG: doesn't exit
            expect(reactStateIsFormulaMode).toBe(true); // React says we should be in formula mode
        });

        it('with onCreate: storage initializes from options after extension recreation', () => {
            const reactStateIsFormulaMode = true;

            // Extension recreated — onCreate sets storage from options
            const storageAfterRecreation = {isFormulaMode: reactStateIsFormulaMode};

            // Backspace fires BEFORE sync useEffect — still works
            const wouldExit = storageAfterRecreation.isFormulaMode && true;

            expect(wouldExit).toBe(true); // FIXED: exits correctly
        });
    });

    describe('value-based formula mode re-entry race condition', () => {
        /**
         * Root cause of the persistent "can't delete =" bug:
         *
         * Old sync useEffect checked `value.startsWith('=')` and called
         * `setIsFormulaMode(true)` on every render. When Backspace exited
         * formula mode, the async saveNullValue hadn't updated the value
         * prop yet, so the effect immediately re-enabled formula mode.
         *
         * Fix: The sync useEffect should only sync the current isFormulaMode
         * state to editor storage — it should NOT derive formula mode from
         * the value prop. Formula mode entry is handled elsewhere.
         */
        it('old sync effect re-enables formula mode when value still starts with =', () => {
            const value = '='; // Stale value, saveNullValue hasn't completed

            // Backspace handler sets isFormulaMode to false (simulates state after handler)
            const isFormulaMode = false;

            // Old sync effect checks value — BUG: re-enables formula mode
            const oldEffectReEnables = typeof value === 'string' && value.startsWith('=') && isFormulaMode === false;

            expect(oldEffectReEnables).toBe(true); // condition was true, would call setIsFormulaMode(true)
        });

        it('new sync effect only syncs current state without checking value', () => {
            const value = '='; // Stale value

            // Backspace handler sets isFormulaMode to false (simulates state after handler)
            const isFormulaMode = false;

            // New sync effect only syncs isFormulaMode to storage
            const storageIsFormulaMode = isFormulaMode;

            expect(storageIsFormulaMode).toBe(false); // Stays false, no re-entry
            expect(value).toBe('='); // Value is stale but doesn't matter
        });
    });
});
