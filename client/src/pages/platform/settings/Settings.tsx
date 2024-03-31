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
        href: '/settings/api-keys',
        title: 'API Keys',
    },
    {
        title: 'Embedded',
    },
    {
        href: '/settings/signing-keys',
        title: 'Signing Keys',
    }
];

export default function Settings() {
    const location = useLocation();

    const curNavItem = sidebarNavItems.find(navItem => location.pathname === navItem.href)

    return (
        <LayoutContainer
            leftSidebarBody={
                <LeftSidebarNav
                    body={sidebarNavItems.map((navItem) => (
                        navItem.href ? <LeftSidebarNavItem
                            item={{
                                filterData: location.pathname === navItem.href,
                                name: navItem.title,
                            }}
                            key={navItem.href}
                            toLink={navItem.href}
                        /> : <h3 className="py-3 px-2 font-semibold">{navItem.title}</h3>
                    ))}
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
