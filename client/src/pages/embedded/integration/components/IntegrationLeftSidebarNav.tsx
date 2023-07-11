import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';

const sidebarNavItems = [
    {
        href: 'configuration',
        title: 'Configuration',
    },
    {
        href: 'connection',
        title: 'Connection',
    },
    {
        href: 'workflows',
        title: 'Workflows',
    },
    {
        href: 'user-metadata',
        title: 'User Metadata',
    },
];

const IntegrationLeftSidebarNav = () => {
    return (
        <LeftSidebarNav
            topBody={sidebarNavItems.map((item) => (
                <LeftSidebarNavItem
                    key={item.href}
                    item={{
                        current: location.pathname === item.href,
                        name: item.title,
                    }}
                    toLink={`/embedded/integrations/1/${item.href}`}
                />
            ))}
        />
    );
};

export default IntegrationLeftSidebarNav;
