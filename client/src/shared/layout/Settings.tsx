import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEffect} from 'react';
import {Outlet, useLocation} from 'react-router-dom';

interface SettingsProps {
    sidebarNavItems: {
        href?: string;
        title: string;
    }[];
    title?: string;
}

const Settings = ({sidebarNavItems, title = 'Settings'}: SettingsProps) => {
    const {getApplicationInfo} = useApplicationInfoStore();

    const location = useLocation();

    useEffect(() => {
        getApplicationInfo();
    }, [getApplicationInfo]);

    return (
        <LayoutContainer
            leftSidebarBody={
                <LeftSidebarNav
                    body={sidebarNavItems.map((navItem) =>
                        navItem.href ? (
                            <LeftSidebarNavItem
                                item={{
                                    current: location.pathname === navItem.href,
                                    name: navItem.title,
                                }}
                                key={navItem.href}
                                toLink={navItem.href}
                            />
                        ) : (
                            <h3
                                className="px-2 pb-1 pt-4 text-sm font-semibold text-muted-foreground"
                                key={navItem.title}
                            >
                                {navItem.title}
                            </h3>
                        )
                    )}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title={title} />}
        >
            <div className="size-full">
                <Outlet />
            </div>
        </LayoutContainer>
    );
};

export default Settings;
