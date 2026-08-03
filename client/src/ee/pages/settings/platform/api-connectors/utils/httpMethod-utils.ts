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
            return 'text-orange-700 dark:text-orange-400';
        case HttpMethod.Delete:
            return 'text-content-destructive-primary';
        default:
            return 'text-content-neutral-secondary';
    }
};

/**
 * Returns the Tailwind CSS background and text color classes for an HTTP method pill.
 * Used for filled-style badges in endpoint selection lists.
 */
export const getHttpMethodPillColor = (method?: string | null): string => {
    switch (method?.toUpperCase()) {
        case 'GET':
            return 'bg-surface-success-secondary text-content-success-primary';
        case 'POST':
            return 'bg-surface-brand-secondary text-content-brand-primary';
        case 'PUT':
            return 'bg-yellow-100 text-yellow-800';
        case 'PATCH':
            return 'bg-surface-warning-secondary text-content-warning-primary';
        case 'DELETE':
            return 'bg-surface-destructive-secondary text-content-destructive';
        default:
            return 'bg-surface-neutral-secondary text-content-neutral-primary';
    }
};
