import {describe, expect, it} from 'vitest';

import resolveExpressionValue from './resolveExpressionValue';

describe('resolveExpressionValue', () => {
    describe('Branch 1: empty input in expression mode → preserves "="', () => {
        it('returns "=" when rawValue is empty and current field starts with "="', () => {
            expect(resolveExpressionValue('', '=someExpression')).toBe('=');
        });

        it('returns "=" when rawValue is empty and current field is bare "="', () => {
            expect(resolveExpressionValue('', '=')).toBe('=');
        });

        it('returns "=" when rawValue is empty and current field is a fromAi expression', () => {
            expect(resolveExpressionValue('', "=fromAi('field')")).toBe('=');
        });
    });

    describe('Branch 2: empty input outside expression mode → clears to ""', () => {
        it('returns "" when rawValue is empty and current field is a plain string', () => {
            expect(resolveExpressionValue('', 'hello')).toBe('');
        });

        it('returns "" when rawValue is empty and current field is also empty', () => {
            expect(resolveExpressionValue('', '')).toBe('');
        });

        it('returns "" when rawValue is empty and current field is a number', () => {
            expect(resolveExpressionValue('', 42)).toBe('');
        });

        it('returns "" when rawValue is empty and current field is null', () => {
            expect(resolveExpressionValue('', null)).toBe('');
        });

        it('returns "" when rawValue is empty and current field is undefined', () => {
            expect(resolveExpressionValue('', undefined)).toBe('');
        });
    });

    describe('Branch 3: input starts with "=" → passes through as-is', () => {
        it('passes through an expression value', () => {
            expect(resolveExpressionValue('=myExpression', 'anything')).toBe('=myExpression');
        });

        it('passes through bare "=" even outside expression mode', () => {
            expect(resolveExpressionValue('=', 'plainValue')).toBe('=');
        });

        it('passes through expression with interpolation', () => {
            expect(resolveExpressionValue('=${variable}', 'current')).toBe('=${variable}');
        });

        it('passes through expression when current field is also an expression', () => {
            expect(resolveExpressionValue('=newExpr', '=oldExpr')).toBe('=newExpr');
        });
    });

    describe('Branch 4: non-"=" input in expression mode → prepends "="', () => {
        it('prepends "=" to a plain value when in expression mode', () => {
            expect(resolveExpressionValue('someValue', '=currentExpression')).toBe('=someValue');
        });

        it('prepends "=" to a numeric string when in expression mode', () => {
            expect(resolveExpressionValue('123', '=otherExpr')).toBe('=123');
        });

        it('prepends "=" to a value with spaces when in expression mode', () => {
            expect(resolveExpressionValue('hello world', '=expr')).toBe('=hello world');
        });
    });

    describe('Branch 5: non-"=" input outside expression mode → passes through as-is', () => {
        it('passes through a plain value when not in expression mode', () => {
            expect(resolveExpressionValue('hello', 'world')).toBe('hello');
        });

        it('passes through a numeric string when current field is a number', () => {
            expect(resolveExpressionValue('42', 100)).toBe('42');
        });

        it('passes through when current field is null', () => {
            expect(resolveExpressionValue('value', null)).toBe('value');
        });

        it('passes through when current field is undefined', () => {
            expect(resolveExpressionValue('value', undefined)).toBe('value');
        });

        it('passes through when current field is an empty string', () => {
            expect(resolveExpressionValue('newValue', '')).toBe('newValue');
        });
    });

    describe('edge cases', () => {
        it('handles current field being a boolean (not in expression mode)', () => {
            expect(resolveExpressionValue('test', true)).toBe('test');
        });

        it('handles current field being an object (not in expression mode)', () => {
            expect(resolveExpressionValue('test', {key: 'value'})).toBe('test');
        });

        it('handles current field being an array (not in expression mode)', () => {
            expect(resolveExpressionValue('test', ['a', 'b'])).toBe('test');
        });

        it('handles rawValue with only whitespace outside expression mode', () => {
            expect(resolveExpressionValue('  ', 'current')).toBe('  ');
        });

        it('handles rawValue with only whitespace in expression mode', () => {
            expect(resolveExpressionValue('  ', '=expr')).toBe('=  ');
        });

        it('handles rawValue with special characters', () => {
            expect(resolveExpressionValue('<script>alert(1)</script>', 'plain')).toBe('<script>alert(1)</script>');
        });
    });
});
