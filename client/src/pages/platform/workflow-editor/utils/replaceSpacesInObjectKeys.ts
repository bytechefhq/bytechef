import {PATH_SPACE_REPLACEMENT} from '@/shared/constants';

export default function replaceSpacesInKeys(parameters: {[key: string]: unknown}): {
    [key: string]: unknown;
} {
    Object.keys(parameters).forEach((key) => {
        if (key.includes(' ')) {
            const newKey = key.replace(/\s/g, PATH_SPACE_REPLACEMENT);

            parameters[newKey] = parameters[key];

            delete parameters[key];
        }

        if (typeof parameters[key] === 'object' && parameters[key] !== null) {
            replaceSpacesInKeys(parameters[key] as {[key: string]: unknown});
        }
    });

    return parameters;
}
