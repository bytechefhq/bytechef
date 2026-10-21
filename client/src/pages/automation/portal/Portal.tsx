import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import PortalAppCard from '@/pages/automation/portal/components/PortalAppCard';
import PortalFilters from '@/pages/automation/portal/components/PortalFilters';
import PortalInstanceSelect from '@/pages/automation/portal/components/PortalInstanceSelect';
import usePortal from '@/pages/automation/portal/hooks/usePortal';
import {PortalAppInstanceType, PortalAppType} from '@/pages/automation/portal/utils';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LayoutGridIcon} from 'lucide-react';
import {useId, useMemo, useState} from 'react';

interface PortalSectionProps {
    onLaunch: (portalAppInstance: PortalAppInstanceType) => void;
    onSelectInstance: (portalApp: PortalAppType) => void;
    onToggleFavorite: (portalApp: PortalAppType) => void;
    portalApps: PortalAppType[];
    title: string;
}

const PortalSection = ({onLaunch, onSelectInstance, onToggleFavorite, portalApps, title}: PortalSectionProps) => {
    const headingId = useId();

    return (
        <section aria-labelledby={headingId} className="space-y-3">
            <h2 className="text-sm font-semibold text-content-neutral-secondary" id={headingId}>
                {title}
            </h2>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
                {portalApps.map((portalApp) => (
                    <PortalAppCard
                        key={portalApp.id}
                        onLaunch={onLaunch}
                        onSelectInstance={onSelectInstance}
                        onToggleFavorite={onToggleFavorite}
                        portalApp={portalApp}
                    />
                ))}
            </div>
        </section>
    );
};

const Portal = () => {
    const [selectedAppId, setSelectedAppId] = useState<string | undefined>();

    const {
        allApps,
        categoryFilter,
        categoryOptions,
        clearFilters,
        error,
        favoriteApps,
        hasApps,
        isLoading,
        launchInstance,
        recentlyUsedApps,
        search,
        setCategoryFilter,
        setSearch,
        toggleFavorite,
    } = usePortal();

    const selectedApp = useMemo(
        () => allApps.find((portalApp) => portalApp.id === selectedAppId),
        [allApps, selectedAppId]
    );

    const sectionProps = {
        onLaunch: launchInstance,
        onSelectInstance: (portalApp: PortalAppType) => setSelectedAppId(portalApp.id),
        onToggleFavorite: toggleFavorite,
    };

    return (
        <LayoutContainer header={<Header centerTitle={true} position="main" title="App Portal" />}>
            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {hasApps ? (
                    <div className="w-full space-y-6 px-4 py-4 3xl:mx-auto 3xl:w-4/5">
                        <PortalFilters
                            categoryFilter={categoryFilter}
                            categoryOptions={categoryOptions}
                            onCategoryFilterChange={setCategoryFilter}
                            onSearchChange={setSearch}
                            search={search}
                        />

                        {allApps.length > 0 ? (
                            <>
                                {favoriteApps.length > 0 && (
                                    <PortalSection {...sectionProps} portalApps={favoriteApps} title="Favorites" />
                                )}

                                {recentlyUsedApps.length > 0 && (
                                    <PortalSection
                                        {...sectionProps}
                                        portalApps={recentlyUsedApps}
                                        title="Recently used"
                                    />
                                )}

                                <PortalSection {...sectionProps} portalApps={allApps} title="All apps" />
                            </>
                        ) : (
                            <EmptyFilterResult entityName="apps" entityTitle="Apps" onClear={clearFilters} />
                        )}
                    </div>
                ) : (
                    <EmptyList
                        icon={<LayoutGridIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Apps appear here once an administrator marks them discoverable and gives you access to one of their instances."
                        title="No Apps Yet"
                    />
                )}
            </PageLoader>

            {selectedApp && (
                <PortalInstanceSelect
                    onLaunch={launchInstance}
                    onOpenChange={(open) => {
                        if (!open) {
                            setSelectedAppId(undefined);
                        }
                    }}
                    portalApp={selectedApp}
                />
            )}
        </LayoutContainer>
    );
};

export default Portal;
