import {
    PATH_DASH_REPLACEMENT,
    PATH_DIGIT_PREFIX,
    PATH_HASH_REPLACEMENT,
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

    return decodedPath;
}

export function encodePath(path: string): string {
    let encodedPath = encodePathGeneric(path, /\s/g, PATH_SPACE_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /^\d/, PATH_DIGIT_PREFIX);

    encodedPath = encodePathGeneric(encodedPath, /-/g, PATH_DASH_REPLACEMENT);

    encodedPath = encodePathGeneric(encodedPath, /#/g, PATH_HASH_REPLACEMENT);

    return encodedPath;
}
