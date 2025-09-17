import {
    PATH_CLOSING_PARENTHESIS_REPLACEMENT,
    PATH_COLON_REPLACEMENT,
    PATH_DASH_REPLACEMENT,
    PATH_DIGIT_PREFIX,
    PATH_HASH_REPLACEMENT,
    PATH_OPENING_PARENTHESIS_REPLACEMENT,
    PATH_SLASH_REPLACEMENT,
    PATH_SPACE_REPLACEMENT,
    PATH_UNICODE_REPLACEMENT_PREFIX,
} from '@/shared/constants';

import {decodePath, encodeParameters, encodePath} from '../utils/encodingUtils';

describe('encodingUtils', () => {
    describe('encodePath and decodePath', () => {
        it('should encode and decode spaces', () => {
            const path = 'user name';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_SPACE_REPLACEMENT);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode dashes', () => {
            const path = 'user-name';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_DASH_REPLACEMENT);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode hashes', () => {
            const path = 'user#id';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_HASH_REPLACEMENT);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode opening parenthesis', () => {
            const path = 'user(name';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_OPENING_PARENTHESIS_REPLACEMENT);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode closing parenthesis', () => {
            const path = 'user)name';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_CLOSING_PARENTHESIS_REPLACEMENT);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode unicode characters', () => {
            const path = 'userŽname';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_UNICODE_REPLACEMENT_PREFIX);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode slashes', () => {
            const path = 'user/name';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_SLASH_REPLACEMENT);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode colons', () => {
            const path = 'https://www.example.com';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_COLON_REPLACEMENT);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode digits at the start', () => {
            const path = '1user';
            const encoded = encodePath(path);
            expect(encoded).toContain(PATH_DIGIT_PREFIX);
            expect(decodePath(encoded)).toBe(path);
        });

        it('should encode and decode digits at the start of any segment', () => {
            const paths = [
                '1user',
                'row.values.4broj telefona',
                '1user.2foo.3bar',
                'foo.4bar.5baz.6qux',
                'row.5values.6other.7thing',
            ];
            for (const path of paths) {
                const encoded = encodePath(path);
                expect(decodePath(encoded)).toBe(path);
            }
        });
    });

    describe('encodeParameters', () => {
        it('should encode keys with special characters including slash and colon', () => {
            const params = {
                '1user': 8,
                'user name': 1,
                'user#id': 3,
                'user(name': 4,
                'user)name': 5,
                'user/name': 7,
                'user:name': 9,
                'user-name': 2,
                userŽname: 6,
            };
            const encoded = encodeParameters(params);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_SPACE_REPLACEMENT))).toBe(true);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_DASH_REPLACEMENT))).toBe(true);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_HASH_REPLACEMENT))).toBe(true);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_OPENING_PARENTHESIS_REPLACEMENT))).toBe(true);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_CLOSING_PARENTHESIS_REPLACEMENT))).toBe(true);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_UNICODE_REPLACEMENT_PREFIX))).toBe(true);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_SLASH_REPLACEMENT))).toBe(true);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_COLON_REPLACEMENT))).toBe(true);
            expect(Object.keys(encoded).some((k) => k.includes(PATH_DIGIT_PREFIX))).toBe(true);
        });
    });
});
