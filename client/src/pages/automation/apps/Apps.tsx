import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import AppDialog from '@/pages/automation/apps/components/AppDialog';
import HostedAppInstances from '@/pages/automation/apps/components/HostedAppInstances';
import AppList from '@/pages/automation/apps/components/app-list/AppList';
import {APP_MANAGE_SCOPE} from '@/pages/automation/apps/constants';
import useAppOwnerNames from '@/pages/automation/apps/hooks/useAppOwnerNames';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import FilterTitle from '@/shared/components/filters/FilterTitle';
import CategoryTagLeftSidebarNav from '@/shared/layout/CategoryTagLeftSidebarNav';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {
    App,
    useAppCategoriesQuery,
    useAppTagsQuery,
    useAppsQuery,
    useMyWorkspaceScopesQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {AppWindowIcon, FilterXIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

interface AppFilterItemI {
    id: number;
    name: string;
}

const toFilterItems = (items?: Array<{id?: string | null; name?: string | null}>): AppFilterItemI[] =>
    (items || []).flatMap((item) => (item.id && item.name ? [{id: Number(item.id), name: item.name}] : []));

const Apps = () => {
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    const [searchParams] = useSearchParams();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const environmentId = String(currentEnvironmentId);
    const workspaceId = String(currentWorkspaceId);

    const getOwnerName = useAppOwnerNames();

    const {data, error, isLoading} = useAppsQuery({workspaceId});

    const {
        data: appCategoriesData,
        error: appCategoriesError,
        isLoading: appCategoriesLoading,
    } = useAppCategoriesQuery({workspaceId});

    const {data: appTagsData, error: appTagsError, isLoading: appTagsLoading} = useAppTagsQuery({workspaceId});

    const {data: scopesData} = useMyWorkspaceScopesQuery({workspaceId});

    const canManage = (scopesData?.myWorkspaceScopes ?? []).includes(APP_MANAGE_SCOPE);

    const apps = useMemo(() => (data?.apps ?? []) as App[], [data?.apps]);

    const categoryIdParam = searchParams.get('categoryId');
    const tagIdParam = searchParams.get('tagId');

    const currentCategoryId = categoryIdParam ? Number(categoryIdParam) : undefined;
    const currentTagId = tagIdParam ? Number(tagIdParam) : undefined;

    const categories = useMemo(() => toFilterItems(appCategoriesData?.appCategories), [appCategoriesData]);

    const tags = useMemo(() => toFilterItems(appTagsData?.appTags), [appTagsData]);

    const filteredApps = useMemo(
        () =>
            apps.filter(
                (app) =>
                    (currentCategoryId === undefined || Number(app.categoryId) === currentCategoryId) &&
                    (currentTagId === undefined || (app.tagIds || []).some((tagId) => Number(tagId) === currentTagId))
            ),
        [apps, currentCategoryId, currentTagId]
    );

    const isFiltered = currentCategoryId !== undefined || currentTagId !== undefined;

    const appTags = useMemo(() => tags.map((tag) => ({id: String(tag.id), name: tag.name})), [tags]);

    const filters = [
        {label: 'Category', value: categories.find((category) => category.id === currentCategoryId)?.name},
        {label: 'Tag', value: tags.find((tag) => tag.id === currentTagId)?.name},
    ];

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        apps.length > 0 &&
                        canManage && <Button label="New App" onClick={() => setShowCreateDialog(true)} />
                    }
                    title={apps.length > 0 || isFiltered ? <FilterTitle filters={filters} /> : 'Apps'}
                />
            }
            leftSidebarBody={
                <CategoryTagLeftSidebarNav
                    categories={categories}
                    categoriesIsLoading={appCategoriesLoading}
                    currentCategoryId={currentCategoryId}
                    currentTagId={currentTagId}
                    tags={tags}
                    tagsClassName="mb-0"
                    tagsEmptyMessage="No defined tags."
                    tagsIsLoading={appTagsLoading}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Apps" />}
            leftSidebarHidden={isLoading || (apps.length === 0 && !isFiltered)}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[error as Error | null, appCategoriesError as Error | null, appTagsError as Error | null]}
                loading={isLoading}
            >
                {filteredApps.length > 0 ? (
                    <AppList apps={filteredApps} canManage={canManage} getOwnerName={getOwnerName} tags={appTags} />
                ) : isFiltered ? (
                    <EmptyState description="Try a different filter." icon={<FilterXIcon />} title="No matching apps" />
                ) : (
                    <EmptyList
                        button={
                            canManage ? <Button label="New App" onClick={() => setShowCreateDialog(true)} /> : undefined
                        }
                        icon={<AppWindowIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Register an app to let it call connections through ByteChef."
                        title="No Apps"
                    />
                )}
            </PageLoader>

            <HostedAppInstances canManage={canManage} environmentId={environmentId} workspaceId={workspaceId} />

            {/* One instance for the whole page: it holds the one-time key, so it must outlive the switch between
                the empty state and the list that the first created app causes. */}

            {canManage && <AppDialog onOpenChange={setShowCreateDialog} open={showCreateDialog} />}
        </LayoutContainer>
    );
};

export default Apps;
