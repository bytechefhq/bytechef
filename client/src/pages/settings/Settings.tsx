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
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Settings" />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    topBody={sidebarNavItems.map((item) => (
                        <LeftSidebarNavItem
                            key={item.href}
                            item={{
                                filterData: location.pathname === item.href,
                                name: item.title,
                            }}
                            toLink={item.href}
                        />
                    ))}
                />
            }
        >
            <div className="px-4">
                <Outlet />
            </div>
        </LayoutContainer>
    );
}
