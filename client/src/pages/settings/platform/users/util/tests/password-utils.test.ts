import {describe, expect, it} from 'vitest';

import {generatePassword, isValidPassword} from '../password-utils';

describe('password-utils', () => {
    describe('generatePassword', () => {
        it('generates a password with default length of 12 characters', () => {
            const password = generatePassword();

            expect(password.length).toBe(12);
        });

        it('generates a password with specified length', () => {
            const password = generatePassword(16);

            expect(password.length).toBe(16);
        });

        it('generates a password with minimum length of 8 characters even if lower is specified', () => {
            const password = generatePassword(4);

            expect(password.length).toBe(8);
        });

        it('generates a password containing at least one uppercase letter', () => {
            const password = generatePassword();

            expect(/[A-Z]/.test(password)).toBe(true);
        });

        it('generates a password containing at least one number', () => {
            const password = generatePassword();

            expect(/\d/.test(password)).toBe(true);
        });

        it('generates different passwords on each call', () => {
            const password1 = generatePassword();
            const password2 = generatePassword();

            expect(password1).not.toBe(password2);
        });

        it('only contains alphanumeric characters', () => {
            const password = generatePassword(20);

            expect(/^[A-Za-z0-9]+$/.test(password)).toBe(true);
        });

        it('generates valid passwords consistently (100 iterations)', () => {
            for (let i = 0; i < 100; i++) {
                const password = generatePassword();

                expect(isValidPassword(password)).toBe(true);
            }
        });
    });

    describe('isValidPassword', () => {
        it('returns true for a valid password with 8+ chars, uppercase, and number', () => {
            expect(isValidPassword('Password1')).toBe(true);
        });

        it('returns true for a long valid password', () => {
            expect(isValidPassword('MySecurePassword123')).toBe(true);
        });

        it('returns false for a password shorter than 8 characters', () => {
            expect(isValidPassword('Pass1')).toBe(false);
        });

        it('returns false for a password with exactly 7 characters', () => {
            expect(isValidPassword('Passwo1')).toBe(false);
        });

        it('returns true for a password with exactly 8 characters', () => {
            expect(isValidPassword('Passwor1')).toBe(true);
        });

        it('returns false for a password without uppercase letters', () => {
            expect(isValidPassword('password123')).toBe(false);
        });

        it('returns false for a password without numbers', () => {
            expect(isValidPassword('PasswordOnly')).toBe(false);
        });

        it('returns false for an empty string', () => {
            expect(isValidPassword('')).toBe(false);
        });

        it('returns false for a password with only lowercase letters', () => {
            expect(isValidPassword('abcdefgh')).toBe(false);
        });

        it('returns false for a password with only uppercase letters', () => {
            expect(isValidPassword('ABCDEFGH')).toBe(false);
        });

        it('returns false for a password with only numbers', () => {
            expect(isValidPassword('12345678')).toBe(false);
        });

        it('returns true for a password with uppercase at the end', () => {
            expect(isValidPassword('password1A')).toBe(true);
        });

        it('returns true for a password with number at the end', () => {
            expect(isValidPassword('PasswordA1')).toBe(true);
        });
    });
});
