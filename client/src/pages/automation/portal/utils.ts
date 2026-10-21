import {PortalAppsQuery} from '@/shared/middleware/graphql';

export type PortalAppType = PortalAppsQuery['portalApps'][number];

export type PortalAppInstanceType = PortalAppType['instances'][number];

export const RECENTLY_USED_LIMIT = 6;

export const getPortalAppIconUrl = (appId: string) =>
    `${import.meta.env.VITE_API_BASE_PATH || ''}/api/automation/internal/portal/apps/${encodeURIComponent(appId)}/icon`;

export const ALL_CATEGORIES = 'all';

export const UNCATEGORIZED = 'uncategorized';

export interface PortalCategoryOptionI {
    label: string;
    value: string;
}

export const toLaunchableUrl = (url: string | null | undefined): string | undefined => {
    if (!url) {
        return undefined;
    }

    try {
        const parsedUrl = new URL(url);

        return parsedUrl.protocol === 'http:' || parsedUrl.protocol === 'https:' ? parsedUrl.href : undefined;
    } catch {
        return undefined;
    }
};

export const isLaunchableUrl = (url: string | null | undefined): boolean => toLaunchableUrl(url) !== undefined;

export const getCategoryOptions = (portalApps: PortalAppType[]): PortalCategoryOptionI[] => {
    const categoryNamesById = new Map<string, string>();

    portalApps.forEach((portalApp) => {
        if (portalApp.categoryId) {
            categoryNamesById.set(portalApp.categoryId, portalApp.categoryName || 'Unnamed category');
        }
    });

    if (categoryNamesById.size === 0) {
        return [];
    }

    const categoryOptions = [...categoryNamesById.entries()]
        .map(([categoryId, categoryName]) => ({label: categoryName, value: categoryId}))
        .sort((firstOption, secondOption) =>
            firstOption.label.localeCompare(secondOption.label, undefined, {sensitivity: 'base'})
        );

    const hasUncategorized = portalApps.some((portalApp) => !portalApp.categoryId);

    return [
        {label: 'All categories', value: ALL_CATEGORIES},
        ...categoryOptions,
        ...(hasUncategorized ? [{label: 'Uncategorized', value: UNCATEGORIZED}] : []),
    ];
};

export const matchesCategory = (portalApp: PortalAppType, categoryFilter: string): boolean => {
    if (categoryFilter === ALL_CATEGORIES) {
        return true;
    }

    if (categoryFilter === UNCATEGORIZED) {
        return !portalApp.categoryId;
    }

    return portalApp.categoryId === categoryFilter;
};

export const matchesSearch = (portalApp: PortalAppType, search: string): boolean => {
    const normalizedSearch = search.trim().toLowerCase();

    if (!normalizedSearch) {
        return true;
    }

    return (
        portalApp.name.toLowerCase().includes(normalizedSearch) ||
        (portalApp.description || '').toLowerCase().includes(normalizedSearch)
    );
};

const toTimestamp = (date: string | null) => {
    const timestamp = date ? Date.parse(date) : NaN;

    return Number.isNaN(timestamp) ? 0 : timestamp;
};

export const sortByName = (portalApps: PortalAppType[]) =>
    [...portalApps].sort((firstApp, secondApp) =>
        firstApp.name.localeCompare(secondApp.name, undefined, {sensitivity: 'base'})
    );

export const selectRecentlyUsed = (portalApps: PortalAppType[]) =>
    portalApps
        .filter((portalApp) => !!portalApp.lastUsedDate)
        .sort((firstApp, secondApp) => toTimestamp(secondApp.lastUsedDate) - toTimestamp(firstApp.lastUsedDate))
        .slice(0, RECENTLY_USED_LIMIT);
