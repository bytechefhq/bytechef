import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
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

    const {isFeatureFlagEnabled} = useFeatureFlagsStore();

    const location = useLocation();

    sidebarNavItems = sidebarNavItems.filter((navItem) => {
        if (navItem.href === 'api-connectors') {
            return isFeatureFlagEnabled('ff-207');
        }

        if (navItem.href?.includes('/account/appearance')) {
            return isFeatureFlagEnabled('ff-445');
        }

        if (navItem.href === '/automation/settings/api-keys' && location.pathname.includes('automation/settings')) {
            return isFeatureFlagEnabled('ff-1023');
        }

        if (navItem.href === 'custom-components') {
            return isFeatureFlagEnabled('ff-1024');
        }

        if (navItem.href === 'admin-api-keys' || navItem.title === 'Organization') {
            return isFeatureFlagEnabled('ff-1024') || isFeatureFlagEnabled('ff-1025');
        }

        return true;
    });

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
