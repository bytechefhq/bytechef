import AppListItem from '@/pages/automation/apps/components/app-list/AppListItem';
import {App} from '@/shared/middleware/graphql';

interface AppListProps {
    apps: App[];
    canManage: boolean;
    getOwnerName: (ownerUserId: string) => string;
}

const AppList = ({apps, canManage, getOwnerName}: AppListProps) => (
    <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
        {apps.map((app) => (
            <AppListItem app={app} canManage={canManage} key={app.id} ownerName={getOwnerName(app.ownerUserId)} />
        ))}
    </div>
);

export default AppList;
