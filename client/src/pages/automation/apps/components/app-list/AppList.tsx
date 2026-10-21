import AppListItem, {AppListItemTagI} from '@/pages/automation/apps/components/app-list/AppListItem';
import {App} from '@/shared/middleware/graphql';

interface AppListProps {
    apps: App[];
    canManage: boolean;
    getOwnerName: (ownerUserId: string) => string;
    tags: AppListItemTagI[];
}

const AppList = ({apps, canManage, getOwnerName, tags}: AppListProps) => (
    <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
        {apps.map((app) => (
            <div className="mb-2 rounded border border-border/50" key={app.id}>
                <AppListItem app={app} canManage={canManage} ownerName={getOwnerName(app.ownerUserId)} tags={tags} />
            </div>
        ))}
    </div>
);

export default AppList;
