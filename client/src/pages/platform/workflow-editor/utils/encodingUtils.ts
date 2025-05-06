import {
    PATH_CLOSING_PARENTHESIS_REPLACEMENT,
    PATH_DASH_REPLACEMENT,
    PATH_DIGIT_PREFIX,
    PATH_HASH_REPLACEMENT,
    PATH_OPENING_PARENTHESIS_REPLACEMENT,
    PATH_SPACE_REPLACEMENT,
} from '@/shared/constants';
import isObject from 'isobject';

function encodeParametersGeneric(
    parameters: {[key: string]: unknown},
    matchPattern: RegExp,
    replacement: string
): {[key: string]: unknown} {
    const encodedParameters = {...parameters};

    Object.keys(encodedParameters).forEach((key) => {
        if (key.match(matchPattern)) {
            const newKey = key.replace(matchPattern, replacement);

            encodedParameters[newKey] = encodedParameters[key];

            delete encodedParameters[key];
        }

        if (isObject(encodedParameters[key]) && encodedParameters[key] !== null) {
            encodedParameters[key] = encodeParametersGeneric(
                encodedParameters[key] as {[key: string]: unknown},
                matchPattern,
                replacement
            );
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
    let encodedParameters = encodeParametersGeneric(parameters, /\s/g, PATH_SPACE_REPLACEMENT);

    encodedParameters = encodeParametersGeneric(encodedParameters, /^\d/, PATH_DIGIT_PREFIX);

    encodedParameters = encodeParametersGeneric(encodedParameters, /-/g, PATH_DASH_REPLACEMENT);

    encodedParameters = encodeParametersGeneric(encodedParameters, /#/g, PATH_HASH_REPLACEMENT);

    encodedParameters = encodeParametersGeneric(encodedParameters, /\(/g, PATH_OPENING_PARENTHESIS_REPLACEMENT);

    encodedParameters = encodeParametersGeneric(encodedParameters, /\)/g, PATH_CLOSING_PARENTHESIS_REPLACEMENT);

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

    return decodedPath;
}

export function encodePath(path: string): string {
    let encodedPath = encodePathGeneric(path, /\s/g, PATH_SPACE_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /^\d/, PATH_DIGIT_PREFIX);

    encodedPath = encodePathGeneric(encodedPath, /-/g, PATH_DASH_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /#/g, PATH_HASH_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /\(/g, PATH_OPENING_PARENTHESIS_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /\)/g, PATH_CLOSING_PARENTHESIS_REPLACEMENT);

    return encodedPath;
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
        return value.replace(/\${([^}]*)}/g, (match, expression) => `\${${transformPathForObjectAccess(expression)}}`);
    }

    return transformPathForObjectAccess(value);
}
