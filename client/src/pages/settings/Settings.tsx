import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Outlet, useLocation} from 'react-router-dom';

interface SettingsProps {
    sidebarNavItems: {
        href: string;
        title: string;
    }[];
}

export default function Settings({sidebarNavItems}: SettingsProps) {
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
                            <h3 className="px-2 py-1 text-sm font-semibold text-muted-foreground" key={navItem.title}>
                                {navItem.title}
                            </h3>
                        )
                    )}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Settings" />}
        >
            <div className="size-full">
                <Outlet />
            </div>
        </LayoutContainer>
    );
}
