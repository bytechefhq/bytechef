import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';

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
        href: 'settings',
        title: 'Settings',
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
