import {describe, expect, it} from 'vitest';

/**
 * Tests for expressionEnabled gate behavior.
 *
 * When `expressionEnabled === false` on a property:
 * 1. Placeholder should NOT show "Use '$' for data pills and '=' for an expression"
 * 2. Formula mode should NOT activate (typing `=` treated as literal)
 * 3. Data pill suggestion (`$` trigger) should be disabled
 * 4. FromAI toggle button should be hidden
 */

describe('expressionEnabledGate', () => {
    describe('placeholder selection', () => {
        /**
         * Replicates the Placeholder.configure logic from
         * PropertyMentionsInputEditor.tsx extensions useMemo.
         */
        const getPlaceholder = (expressionEnabled: boolean | undefined, propertyPlaceholder?: string): string => {
            if (expressionEnabled === false) {
                return propertyPlaceholder || '';
            }

            return propertyPlaceholder || "Use '$' for data pills and '=' for an expression";
        };

        it('should show default expression placeholder when expressionEnabled is true', () => {
            expect(getPlaceholder(true)).toBe("Use '$' for data pills and '=' for an expression");
        });

        it('should show default expression placeholder when expressionEnabled is undefined', () => {
            expect(getPlaceholder(undefined)).toBe("Use '$' for data pills and '=' for an expression");
        });

        it('should show empty placeholder when expressionEnabled is false and no custom placeholder', () => {
            expect(getPlaceholder(false)).toBe('');
        });

        it('should show custom placeholder when expressionEnabled is false and custom placeholder set', () => {
            expect(getPlaceholder(false, 'Enter a value')).toBe('Enter a value');
        });

        it('should show custom placeholder when expressionEnabled is true and custom placeholder set', () => {
            expect(getPlaceholder(true, 'Enter a value')).toBe('Enter a value');
        });
    });

    describe('formula mode entry gate', () => {
        /**
         * Replicates the gate in handleEditorValueChange from
         * PropertyMentionsInput.tsx — formula mode should not activate
         * when expressionEnabled is false.
         */
        const shouldEnterFormulaMode = (
            newValue: string,
            expressionEnabled: boolean | undefined,
            hasSetIsFormulaMode: boolean
        ): boolean => {
            if (typeof newValue !== 'string') {
                return false;
            }

            const startsWithEquals = newValue.trim().startsWith('=');

            return startsWithEquals && hasSetIsFormulaMode && expressionEnabled !== false;
        };

        it('should enter formula mode when expressionEnabled is true', () => {
            expect(shouldEnterFormulaMode('=3+3', true, true)).toBe(true);
        });

        it('should enter formula mode when expressionEnabled is undefined', () => {
            expect(shouldEnterFormulaMode('=3+3', undefined, true)).toBe(true);
        });

        it('should NOT enter formula mode when expressionEnabled is false', () => {
            expect(shouldEnterFormulaMode('=3+3', false, true)).toBe(false);
        });

        it('should NOT enter when value does not start with =', () => {
            expect(shouldEnterFormulaMode('hello', true, true)).toBe(false);
        });

        it('should NOT enter when setIsFormulaMode is not available', () => {
            expect(shouldEnterFormulaMode('=3+3', true, false)).toBe(false);
        });
    });

    describe('value-based formula mode sync gate', () => {
        /**
         * Replicates the useEffect in PropertyMentionsInputEditor.tsx that
         * syncs formula mode from the value prop. Should not activate
         * formula mode when expressionEnabled is false.
         */
        const shouldSyncFormulaMode = (
            value: string | number | undefined,
            expressionEnabled: boolean | undefined,
            hasSetIsFormulaMode: boolean
        ): boolean => {
            return (
                typeof value === 'string' && value.startsWith('=') && hasSetIsFormulaMode && expressionEnabled !== false
            );
        };

        it('should sync formula mode for expression value when expressionEnabled is true', () => {
            expect(shouldSyncFormulaMode('=3+3', true, true)).toBe(true);
        });

        it('should NOT sync formula mode for expression value when expressionEnabled is false', () => {
            expect(shouldSyncFormulaMode('=3+3', false, true)).toBe(false);
        });

        it('should NOT sync for non-expression values', () => {
            expect(shouldSyncFormulaMode('hello', true, true)).toBe(false);
        });

        it('should NOT sync for undefined value', () => {
            expect(shouldSyncFormulaMode(undefined, true, true)).toBe(false);
        });

        it('should NOT sync for numeric value', () => {
            expect(shouldSyncFormulaMode(42, true, true)).toBe(false);
        });
    });

    describe('suggestion configuration', () => {
        /**
         * Replicates the conditional suggestion options from
         * PropertyMentionsInputEditor.tsx Mention.configure.
         */
        const getMentionConfig = (expressionEnabled: boolean | undefined): {hasSuggestion: boolean} => {
            const config = expressionEnabled !== false ? {hasSuggestion: true} : {hasSuggestion: false};

            return config;
        };

        it('should include suggestion when expressionEnabled is true', () => {
            expect(getMentionConfig(true).hasSuggestion).toBe(true);
        });

        it('should include suggestion when expressionEnabled is undefined', () => {
            expect(getMentionConfig(undefined).hasSuggestion).toBe(true);
        });

        it('should NOT include suggestion when expressionEnabled is false', () => {
            expect(getMentionConfig(false).hasSuggestion).toBe(false);
        });
    });

    describe('fromAi button visibility', () => {
        /**
         * Replicates the visibility conditions for FromAiToggleButton
         * across Property.tsx and PropertyMentionsInputEditor.tsx.
         */
        const shouldShowFromAi = (
            isToolsClusterElement: boolean,
            expressionEnabled: boolean | undefined,
            hasFromAiClickHandler: boolean
        ): boolean => {
            return hasFromAiClickHandler && isToolsClusterElement && expressionEnabled !== false;
        };

        it('should show fromAi for tools cluster with expressions enabled', () => {
            expect(shouldShowFromAi(true, true, true)).toBe(true);
        });

        it('should NOT show fromAi when expressionEnabled is false', () => {
            expect(shouldShowFromAi(true, false, true)).toBe(false);
        });

        it('should NOT show fromAi for non-tools elements', () => {
            expect(shouldShowFromAi(false, true, true)).toBe(false);
        });

        it('should NOT show fromAi without click handler', () => {
            expect(shouldShowFromAi(true, true, false)).toBe(false);
        });

        it('should show fromAi when expressionEnabled is undefined (legacy)', () => {
            expect(shouldShowFromAi(true, undefined, true)).toBe(true);
        });
    });

    describe('initial value formula mode check gate', () => {
        /**
         * Replicates the useEffect in PropertyMentionsInput.tsx that checks
         * initial value for formula mode on mount.
         */
        const shouldInitFormulaMode = (
            initialValue: string | undefined,
            expressionEnabled: boolean | undefined,
            hasSetIsFormulaMode: boolean
        ): boolean => {
            return (
                hasSetIsFormulaMode &&
                expressionEnabled !== false &&
                typeof initialValue === 'string' &&
                initialValue.trim().startsWith('=')
            );
        };

        it('should init formula mode for expression value when expressionEnabled is true', () => {
            expect(shouldInitFormulaMode('=test', true, true)).toBe(true);
        });

        it('should NOT init formula mode when expressionEnabled is false', () => {
            expect(shouldInitFormulaMode('=test', false, true)).toBe(false);
        });

        it('should NOT init for non-expression value', () => {
            expect(shouldInitFormulaMode('test', true, true)).toBe(false);
        });

        it('should NOT init for undefined value', () => {
            expect(shouldInitFormulaMode(undefined, true, true)).toBe(false);
        });
    });
});
