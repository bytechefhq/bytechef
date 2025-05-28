import {
    PATH_CLOSING_PARENTHESIS_REPLACEMENT,
    PATH_DASH_REPLACEMENT,
    PATH_DIGIT_PREFIX,
    PATH_HASH_REPLACEMENT,
    PATH_OPENING_PARENTHESIS_REPLACEMENT,
    PATH_SPACE_REPLACEMENT,
    PATH_UNICODE_REPLACEMENT_PREFIX,
} from '@/shared/constants';
import isObject from 'isobject';

interface EncodeParametersGenericProps {
    matchPattern: RegExp;
    parameters: {[key: string]: unknown};
    replacement?: string;
    replacementFn?: (match: string) => string;
}

function encodeParametersGeneric({
    matchPattern,
    parameters,
    replacement,
    replacementFn,
}: EncodeParametersGenericProps): {
    [key: string]: unknown;
} {
    const encodedParameters = {...parameters};

    Object.keys(encodedParameters).forEach((key) => {
        if (key.match(matchPattern)) {
            const newKey = replacementFn
                ? key.replace(matchPattern, replacementFn(key))
                : key.replace(matchPattern, replacement!);

            encodedParameters[newKey] = encodedParameters[key];

            delete encodedParameters[key];
        }

        if (isObject(encodedParameters[key]) && encodedParameters[key] !== null) {
            encodedParameters[key] = encodeParametersGeneric({
                matchPattern,
                parameters: encodedParameters[key] as {[key: string]: unknown},
                replacement,
            });
        }
    });

    return encodedParameters;
}

function encodePathGeneric(path: string, matchPattern: RegExp, replacement: string): string {
    let encodedPath = path;

    if (encodedPath.match(matchPattern)) {
        encodedPath = encodedPath.replace(matchPattern, replacement);
    }

    return encodedPath;
}

export function encodeParameters(parameters: {[key: string]: unknown}): {[key: string]: unknown} {
    let encodedParameters = encodeParametersGeneric({
        matchPattern: /\s/g,
        parameters,
        replacement: PATH_SPACE_REPLACEMENT,
    });

    encodedParameters = encodeParametersGeneric({
        matchPattern: /^\d/,
        parameters: encodedParameters,
        replacement: PATH_DIGIT_PREFIX,
    });

    encodedParameters = encodeParametersGeneric({
        matchPattern: /-/g,
        parameters: encodedParameters,
        replacement: PATH_DASH_REPLACEMENT,
    });

    encodedParameters = encodeParametersGeneric({
        matchPattern: /#/g,
        parameters: encodedParameters,
        replacement: PATH_HASH_REPLACEMENT,
    });

    encodedParameters = encodeParametersGeneric({
        matchPattern: /\(/g,
        parameters: encodedParameters,
        replacement: PATH_OPENING_PARENTHESIS_REPLACEMENT,
    });

    encodedParameters = encodeParametersGeneric({
        matchPattern: /\)/g,
        parameters: encodedParameters,
        replacement: PATH_CLOSING_PARENTHESIS_REPLACEMENT,
    });

    encodedParameters = encodeParametersGeneric({
        // eslint-disable-next-line no-control-regex
        matchPattern: /[^\x00-\x7F]/g,
        parameters: encodedParameters,
        replacementFn: (match) => `${PATH_UNICODE_REPLACEMENT_PREFIX}${match.charCodeAt(0)}_`,
    });

    return encodedParameters;
}

export function decodePath(path: string): string {
    let decodedPath = path;

    if (decodedPath.includes(PATH_SPACE_REPLACEMENT)) {
        decodedPath = decodedPath.replace(new RegExp(PATH_SPACE_REPLACEMENT, 'g'), ' ');
    }

    if (decodedPath.includes(PATH_DIGIT_PREFIX)) {
        decodedPath = decodedPath.replace(new RegExp(PATH_DIGIT_PREFIX, 'g'), '');
    }

    if (decodedPath.includes(PATH_DASH_REPLACEMENT)) {
        decodedPath = decodedPath.replace(new RegExp(PATH_DASH_REPLACEMENT, 'g'), '-');
    }

    if (decodedPath.includes(PATH_HASH_REPLACEMENT)) {
        decodedPath = decodedPath.replace(new RegExp(PATH_HASH_REPLACEMENT, 'g'), '#');
    }

    if (decodedPath.includes(PATH_OPENING_PARENTHESIS_REPLACEMENT)) {
        decodedPath = decodedPath.replace(new RegExp(PATH_OPENING_PARENTHESIS_REPLACEMENT, 'g'), '(');
    }

    if (decodedPath.includes(PATH_CLOSING_PARENTHESIS_REPLACEMENT)) {
        decodedPath = decodedPath.replace(new RegExp(PATH_CLOSING_PARENTHESIS_REPLACEMENT, 'g'), ')');
    }

    if (decodedPath.includes(PATH_UNICODE_REPLACEMENT_PREFIX)) {
        decodedPath = decodeNonAsciiCharacters(decodedPath);
    }

    return decodedPath;
}

export function encodePath(path: string): string {
    let encodedPath = encodePathGeneric(path, /\s/g, PATH_SPACE_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /^\d/, PATH_DIGIT_PREFIX);

    encodedPath = encodePathGeneric(encodedPath, /-/g, PATH_DASH_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /#/g, PATH_HASH_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /\(/g, PATH_OPENING_PARENTHESIS_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /\)/g, PATH_CLOSING_PARENTHESIS_REPLACEMENT);

    encodedPath = encodeNonAsciiCharacters(encodedPath);

    return encodedPath;
}

function encodeNonAsciiCharacters(string: string): string {
    // eslint-disable-next-line no-control-regex
    return string.replace(/[^\x00-\x7F]/g, (char) => `${PATH_UNICODE_REPLACEMENT_PREFIX}${char.charCodeAt(0)}_`);
}

function decodeNonAsciiCharacters(string: string): string {
    return string.replace(new RegExp(`${PATH_UNICODE_REPLACEMENT_PREFIX}(\\d+)_`, 'g'), (_, code) =>
        String.fromCharCode(Number(code))
    );
}

// Transform: "user.first-name" → "user['first-name']"
export function transformPathForObjectAccess(path: string): string {
    if (!path || !path.includes('.')) {
        return path;
    }

    const segments = path.split('.').filter(Boolean);

    if (segments.length === 0) {
        return path;
    }

    const firstSegment = segments[0];

    const formattedSegments = segments.slice(1).map((segment) => {
        const hasArrayNotation = /\[\d+\]$/.test(segment);

        if (hasArrayNotation) {
            const propertyMatch = segment.match(/^([^[]+)(\[\d+\])$/);

            if (propertyMatch) {
                const [, propertyName, arrayAccess] = propertyMatch;

                const propertyNeedsBrackets = /[^a-zA-Z0-9_$]/.test(propertyName);

                return propertyNeedsBrackets ? `['${propertyName}']${arrayAccess}` : `.${propertyName}${arrayAccess}`;
            }
        }

        const hasSpecialChars = /[^a-zA-Z0-9_$]/.test(segment);
        const startsWithDigit = /^\d/.test(segment);

        const needsBrackets = hasSpecialChars || startsWithDigit;

        return needsBrackets ? `['${segment}']` : `.${segment}`;
    });

    return `${firstSegment}${formattedSegments.join('')}`;
}

// Transform: "Value from ${user.first-name}" → "Value from ${user['first-name']}"
export function transformValueForObjectAccess(value: string): string {
    if (typeof value !== 'string') {
        return value;
    }

    if (value.includes('${')) {
        return value.replace(/\${([^}]*)}/g, (_match, expression) => `\${${transformPathForObjectAccess(expression)}}`);
    }

    return value;
}
