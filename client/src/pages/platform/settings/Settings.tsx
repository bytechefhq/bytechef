import LayoutContainer from '@/layouts/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import PageHeader from '@/layouts/PageHeader';
import {Outlet, useLocation} from 'react-router-dom';

const sidebarNavItems = [
    {
        href: '/settings/account',
        title: 'Account',
    },
    {
        href: '/settings/appearance',
        title: 'Appearance',
    },
    {
        title: 'Embedded',
    },
    {
        href: '/settings/e/api-keys',
        title: 'API Keys',
    },
    {
        href: '/settings/e/signing-keys',
        title: 'Signing Keys',
    },
];

export default function Settings() {
    const location = useLocation();

    return (
        <LayoutContainer
            leftSidebarBody={
                <LeftSidebarNav
                    body={sidebarNavItems.map((navItem) =>
                        navItem.href ? (
                            <LeftSidebarNavItem
                                item={{
                                    filterData: location.pathname === navItem.href,
                                    name: navItem.title,
                                }}
                                key={navItem.href}
                                toLink={navItem.href}
                            />
                        ) : (
                            <h3 className="px-2 py-3 font-semibold" key={navItem.title}>
                                {navItem.title}
                            </h3>
                        )
                    )}
                />
            }
            leftSidebarHeader={<PageHeader position="sidebar" title="Settings" />}
        >
            <div className="size-full">
                <Outlet />
            </div>
        </LayoutContainer>
    );
}
