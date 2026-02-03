import {ParameterLocationType} from '../types/api-connector-wizard.types';

/**
 * Returns the display label for a parameter location type.
 */
export const getLocationLabel = (location: ParameterLocationType): string => {
    switch (location) {
        case 'query':
            return 'Query';
        case 'path':
            return 'Path';
        case 'header':
            return 'Header';
        default:
            return location;
    }
};

/**
 * Returns the Tailwind CSS background and text color classes for an HTTP status code.
 */
export const getStatusCodeColor = (statusCode: string): string => {
    const code = parseInt(statusCode, 10);

    if (code >= 200 && code < 300) {
        return 'bg-green-100 text-green-800';
    }

    if (code >= 300 && code < 400) {
        return 'bg-blue-100 text-blue-800';
    }

    if (code >= 400 && code < 500) {
        return 'bg-yellow-100 text-yellow-800';
    }

    if (code >= 500) {
        return 'bg-red-100 text-red-800';
    }

    return 'bg-gray-100 text-gray-800';
};
