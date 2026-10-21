import {
    ALL_CATEGORIES,
    PortalAppInstanceType,
    PortalAppType,
    getCategoryOptions,
    matchesCategory,
    matchesSearch,
    selectRecentlyUsed,
    sortByName,
    toLaunchableUrl,
} from '@/pages/automation/portal/utils';
import {
    PortalAppsQuery,
    SetAppFavoriteMutationVariables,
    usePortalAppsQuery,
    useRecordAppLaunchMutation,
    useSetAppFavoriteMutation,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo, useState} from 'react';

interface FavoriteContextI {
    previousData?: PortalAppsQuery;
    queryKey: unknown[];
}

const PORTAL_APPS_QUERY_KEY = 'portalApps';

export default function usePortal() {
    const [categoryFilter, setCategoryFilter] = useState(ALL_CATEGORIES);
    const [search, setSearch] = useState('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();

    const environmentId = String(currentEnvironmentId);

    const portalAppsQueryKey = [PORTAL_APPS_QUERY_KEY, {environmentId}];

    const {data, error, isLoading} = usePortalAppsQuery({environmentId});

    const recordAppLaunchMutation = useRecordAppLaunchMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: [PORTAL_APPS_QUERY_KEY]});
        },
    });

    const setAppFavoriteMutation = useSetAppFavoriteMutation<unknown, FavoriteContextI>({
        onError: (_error, _variables, context) => {
            if (context?.previousData) {
                queryClient.setQueryData(context.queryKey, context.previousData);
            }
        },
        onMutate: async (variables: SetAppFavoriteMutationVariables) => {
            await queryClient.cancelQueries({queryKey: portalAppsQueryKey});

            const previousData = queryClient.getQueryData<PortalAppsQuery>(portalAppsQueryKey);

            if (previousData) {
                queryClient.setQueryData<PortalAppsQuery>(portalAppsQueryKey, {
                    portalApps: previousData.portalApps.map((portalApp) =>
                        portalApp.id === variables.appId ? {...portalApp, favorite: variables.favorite} : portalApp
                    ),
                });
            }

            return {previousData, queryKey: portalAppsQueryKey};
        },
        onSettled: () => {
            void queryClient.invalidateQueries({queryKey: [PORTAL_APPS_QUERY_KEY]});
        },
    });

    const portalApps = useMemo(() => data?.portalApps || [], [data?.portalApps]);

    const categoryOptions = useMemo(() => getCategoryOptions(portalApps), [portalApps]);

    const effectiveCategoryFilter = categoryOptions.some((categoryOption) => categoryOption.value === categoryFilter)
        ? categoryFilter
        : ALL_CATEGORIES;

    const matchingApps = useMemo(
        () =>
            portalApps.filter(
                (portalApp) => matchesSearch(portalApp, search) && matchesCategory(portalApp, effectiveCategoryFilter)
            ),
        [effectiveCategoryFilter, portalApps, search]
    );

    const allApps = useMemo(() => sortByName(matchingApps), [matchingApps]);

    const favoriteApps = useMemo(() => allApps.filter((portalApp) => portalApp.favorite), [allApps]);

    const recentlyUsedApps = useMemo(() => selectRecentlyUsed(matchingApps), [matchingApps]);

    const {mutate: recordAppLaunch} = recordAppLaunchMutation;

    const launchInstance = useCallback(
        (portalAppInstance: PortalAppInstanceType) => {
            const launchableUrl = toLaunchableUrl(portalAppInstance.url);

            if (!launchableUrl) {
                return;
            }

            window.open(launchableUrl, '_blank', 'noopener,noreferrer');

            recordAppLaunch({appInstanceId: portalAppInstance.id});
        },
        [recordAppLaunch]
    );

    const {mutate: setAppFavorite} = setAppFavoriteMutation;

    const toggleFavorite = useCallback(
        (portalApp: PortalAppType) => {
            setAppFavorite({appId: portalApp.id, favorite: !portalApp.favorite});
        },
        [setAppFavorite]
    );

    const clearFilters = useCallback(() => {
        setCategoryFilter(ALL_CATEGORIES);
        setSearch('');
    }, []);

    return {
        allApps,
        categoryFilter: effectiveCategoryFilter,
        categoryOptions,
        clearFilters,
        error,
        favoriteApps,
        hasApps: portalApps.length > 0,
        isLoading,
        launchInstance,
        recentlyUsedApps,
        search,
        setCategoryFilter,
        setSearch,
        toggleFavorite,
    };
}
