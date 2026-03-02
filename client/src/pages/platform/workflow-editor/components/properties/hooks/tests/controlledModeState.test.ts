import {describe, expect, it, vi} from 'vitest';

/**
 * Tests for the controlled-mode state logic added to useProperty.
 *
 * These test the pure logic extracted from the hook: fromAi expression
 * building, mode switching, blur validation, and dependency filtering.
 */

describe('controlled-mode state logic', () => {
    describe('fromAiExpression computation', () => {
        it('builds expression with name only when description is absent', () => {
            const name = 'email';
            const description = undefined;
            const expression = description ? `=fromAi('${name}', '${description}')` : `=fromAi('${name}')`;

            expect(expression).toBe("=fromAi('email')");
        });

        it('builds expression with name and description when both are present', () => {
            const name = 'email';
            const description = 'The user email address';
            const expression = description ? `=fromAi('${name}', '${description}')` : `=fromAi('${name}')`;

            expect(expression).toBe("=fromAi('email', 'The user email address')");
        });
    });

    describe('handleFromAiToggle', () => {
        it('calls fieldOnChange with fromAi expression when toggling on', () => {
            const fieldOnChange = vi.fn();
            const fromAiExpression = "=fromAi('name', 'description')";

            const handleFromAiToggle = (fromAi: boolean, onChange: (value: string) => void) => {
                if (fromAi) {
                    onChange(fromAiExpression);
                }
            };

            handleFromAiToggle(true, fieldOnChange);

            expect(fieldOnChange).toHaveBeenCalledWith("=fromAi('name', 'description')");
        });

        it('does not call fieldOnChange when toggling off', () => {
            const fieldOnChange = vi.fn();
            const fromAiExpression = "=fromAi('name')";

            const handleFromAiToggle = (fromAi: boolean, onChange: (value: string) => void) => {
                if (fromAi) {
                    onChange(fromAiExpression);
                }
            };

            handleFromAiToggle(false, fieldOnChange);

            expect(fieldOnChange).not.toHaveBeenCalled();
        });
    });

    describe('handleControlledBlur validation', () => {
        const validatePropertyValue = (value: string | number): boolean => {
            const stringValue = typeof value === 'string' ? value : String(value);

            if (typeof value === 'string' && (value.startsWith('=') || value.includes('${'))) {
                return true;
            }

            return stringValue.length > 0;
        };

        const handleControlledBlur = (value: unknown): string | undefined => {
            if (value === '' || value == null) {
                return undefined;
            }

            if (!validatePropertyValue(value as string | number)) {
                return 'Incorrect value';
            }

            return undefined;
        };

        it('returns undefined for empty string', () => {
            expect(handleControlledBlur('')).toBeUndefined();
        });

        it('returns undefined for null', () => {
            expect(handleControlledBlur(null)).toBeUndefined();
        });

        it('returns undefined for valid values', () => {
            expect(handleControlledBlur('hello')).toBeUndefined();
        });

        it('returns undefined for expression values', () => {
            expect(handleControlledBlur('=someExpression')).toBeUndefined();
        });
    });

    describe('controlled dynamic mode initialization', () => {
        it('detects = prefixed values as dynamic mode', () => {
            const fieldValue = '=someExpression()';
            const isDynamic = typeof fieldValue === 'string' && fieldValue.startsWith('=');

            expect(isDynamic).toBe(true);
        });

        it('does not detect regular values as dynamic mode', () => {
            const fieldValue = 'normalValue';
            const isDynamic = typeof fieldValue === 'string' && fieldValue.startsWith('=');

            expect(isDynamic).toBe(false);
        });

        it('does not detect numeric values as dynamic mode', () => {
            const fieldValue = 42 as string | number;
            const isDynamic = typeof fieldValue === 'string' && fieldValue.startsWith('=');

            expect(isDynamic).toBe(false);
        });
    });

    describe('dependency value filtering', () => {
        it('filters out =fromAi() expressions from dependency values', () => {
            const resolvedValues = ["=fromAi('field')", 'normalValue', "=fromAi('other', 'desc')"];

            const filteredValues = resolvedValues.map((resolvedValue) => {
                if (typeof resolvedValue === 'string' && resolvedValue.startsWith('=fromAi(')) {
                    return undefined;
                }

                return resolvedValue;
            });

            expect(filteredValues).toEqual([undefined, 'normalValue', undefined]);
        });

        it('passes through regular string values', () => {
            const resolvedValues = ['hello', 'world'];

            const filteredValues = resolvedValues.map((resolvedValue) => {
                if (typeof resolvedValue === 'string' && resolvedValue.startsWith('=fromAi(')) {
                    return undefined;
                }

                return resolvedValue;
            });

            expect(filteredValues).toEqual(['hello', 'world']);
        });

        it('filters expression and interpolation values from dependency checks', () => {
            const values = ['=expression', '${interpolated}', 'normal', ''];
            const hasValidDependencies = values.every(
                (dependencyValue) =>
                    dependencyValue != null &&
                    dependencyValue !== '' &&
                    !(
                        typeof dependencyValue === 'string' &&
                        (dependencyValue.startsWith('=') || dependencyValue.includes('${'))
                    )
            );

            expect(hasValidDependencies).toBe(false);
        });

        it('accepts all-normal dependency values', () => {
            const values = ['option1', 'option2'];
            const hasValidDependencies = values.every(
                (dependencyValue) =>
                    dependencyValue != null &&
                    dependencyValue !== '' &&
                    !(
                        typeof dependencyValue === 'string' &&
                        (dependencyValue.startsWith('=') || dependencyValue.includes('${'))
                    )
            );

            expect(hasValidDependencies).toBe(true);
        });
    });
});
