import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {SettingsNavItemI, useVisibleSettingsNavItems} from '@/shared/layout/Settings';
import {Navigate} from 'react-router-dom';

interface SettingsIndexRedirectProps {
    fallbackHref: string;
    sidebarNavItems: SettingsNavItemI[];
    tenantAdminHref: string;
}

const SettingsIndexRedirect = ({fallbackHref, sidebarNavItems, tenantAdminHref}: SettingsIndexRedirectProps) => {
    const isTenantAdmin = useIsTenantAdmin();
    const visibleNavItems = useVisibleSettingsNavItems(sidebarNavItems);

    const firstVisibleHref = visibleNavItems
        .flatMap((navItem) => navItem.items ?? [navItem])
        .find((navItem) => navItem.href !== undefined)?.href;

    return <Navigate replace to={isTenantAdmin ? tenantAdminHref : firstVisibleHref || fallbackHref} />;
};

export default SettingsIndexRedirect;
