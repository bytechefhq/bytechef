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
];

export default function Settings() {
    const location = useLocation();

    return (
        <LayoutContainer
            header={<PageHeader title="Account" />}
            leftSidebarBody={
                <LeftSidebarNav
                    topBody={sidebarNavItems.map((item) => (
                        <LeftSidebarNavItem
                            item={{
                                filterData: location.pathname === item.href,
                                name: item.title,
                            }}
                            key={item.href}
                            toLink={item.href}
                        />
                    ))}
                />
            }
            leftSidebarHeader={<PageHeader position="sidebar" title="Settings" />}
        >
            <div className="px-4">
                <Outlet />
            </div>
        </LayoutContainer>
    );
}
