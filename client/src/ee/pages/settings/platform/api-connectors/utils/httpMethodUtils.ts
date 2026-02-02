import {HttpMethod} from '@/shared/middleware/graphql';

/**
 * Returns the Tailwind CSS text color class for an HTTP method badge.
 * Used for outline-style badges across API connector components.
 */
export const getHttpMethodBadgeColor = (method?: HttpMethod | null): string => {
    switch (method) {
        case HttpMethod.Get:
            return 'text-content-brand-primary';
        case HttpMethod.Post:
            return 'text-content-success-primary';
        case HttpMethod.Put:
            return 'text-content-warning-primary';
        case HttpMethod.Patch:
            return 'text-orange-700';
        case HttpMethod.Delete:
            return 'text-content-destructive-primary';
        default:
            return 'text-gray-700';
    }
};

/**
 * Returns the Tailwind CSS background and text color classes for an HTTP method pill.
 * Used for filled-style badges in endpoint selection lists.
 */
export const getHttpMethodPillColor = (method?: string | null): string => {
    switch (method?.toUpperCase()) {
        case 'GET':
            return 'bg-green-100 text-green-800';
        case 'POST':
            return 'bg-blue-100 text-blue-800';
        case 'PUT':
            return 'bg-yellow-100 text-yellow-800';
        case 'PATCH':
            return 'bg-orange-100 text-orange-800';
        case 'DELETE':
            return 'bg-red-100 text-red-800';
        default:
            return 'bg-gray-100 text-gray-800';
    }
};
