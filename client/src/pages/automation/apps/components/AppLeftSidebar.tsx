import {Input} from '@/components/Input/Input';
import PageLoader from '@/components/PageLoader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useAppsQuery} from '@/shared/middleware/graphql';
import {useMemo, useState} from 'react';

interface AppLeftSidebarProps {
    currentId?: string;
}

const AppLeftSidebar = ({currentId}: AppLeftSidebarProps) => {
    const [search, setSearch] = useState('');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data, error, isLoading} = useAppsQuery({workspaceId: String(currentWorkspaceId)});

    const filteredApps = useMemo(() => {
        const searchText = search.trim().toLowerCase();

        return (data?.apps ?? [])
            .filter((app) => !searchText || app.name.toLowerCase().includes(searchText))
            .sort((firstApp, secondApp) =>
                firstApp.name.localeCompare(secondApp.name, undefined, {sensitivity: 'base'})
            );
    }, [data?.apps, search]);

    return (
        <div className="flex h-full flex-col">
            <div className="space-y-2 pt-0.5 pb-3">
                <Input
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Search apps..."
                    value={search}
                />
            </div>

            <div className="flex-1 overflow-y-auto">
                <PageLoader errors={[error as Error | null]} loading={false}>
                    <LeftSidebarNav
                        body={
                            <>
                                {filteredApps.map((app) => (
                                    <LeftSidebarNavItem
                                        item={{current: app.id === currentId, id: Number(app.id), name: app.name}}
                                        key={app.id}
                                        toLink={`/automation/apps/${app.id}`}
                                    />
                                ))}

                                {filteredApps.length === 0 && (
                                    <div className="px-2 py-2 text-sm text-muted-foreground">No apps found</div>
                                )}
                            </>
                        }
                        loading={isLoading}
                    />
                </PageLoader>
            </div>
        </div>
    );
};

export default AppLeftSidebar;
