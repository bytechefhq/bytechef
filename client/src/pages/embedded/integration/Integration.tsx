import LayoutContainer from '@/layouts/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import PageHeader from '@/layouts/PageHeader';
import {Outlet} from 'react-router-dom';

const sidebarNavItems = [
    {
        href: 'configuration',
        title: 'Configuration',
    },
    {
        href: 'workflows',
        title: 'Workflows',
    },
    {
        href: 'user-metadata',
        title: 'User Metadata',
    },
    {
        href: 'settings',
        title: 'Settings',
    },
];

const IntegrationLeftSidebarNav = () => {
    return (
        <LeftSidebarNav
            topBody={sidebarNavItems.map(
                (item) =>
                    item && (
                        <LeftSidebarNavItem
                            item={{
                                filterData: location.pathname === item.href,
                                name: item.title,
                            }}
                            key={item.href}
                            toLink={`/embedded/integrations/1/${item.href}`}
                        />
                    )
            )}
        />
    );
};

const Integration = () => {
    return (
        <>
            <LayoutContainer
                leftSidebarBody={<IntegrationLeftSidebarNav />}
                leftSidebarHeader={<PageHeader position="sidebar" title="Pipedrive" />}
                leftSidebarWidth="56"
            >
                <Outlet />
            </LayoutContainer>
        </>
    );
};

export default Integration;
