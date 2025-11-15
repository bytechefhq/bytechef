import {ModeType, useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Outlet, useLocation} from 'react-router-dom';

interface SettingsProps {
    sidebarNavItems: {
        href?: string;
        title: string;
    }[];
    title?: string;
}

const Settings = ({sidebarNavItems, title = 'Settings'}: SettingsProps) => {
    const currentType = useModeTypeStore((state) => state.currentType);

    const isFeatureFlagEnabled = useFeatureFlagsStore();

    const location = useLocation();

    sidebarNavItems = sidebarNavItems.filter((navItem) => {
        if (navItem.href === 'api-connectors') {
            return isFeatureFlagEnabled('ff-207');
        }

        if (navItem.href?.includes('/account/appearance')) {
            return isFeatureFlagEnabled('ff-445');
        }

        if (navItem.href === 'custom-components') {
            return isFeatureFlagEnabled('ff-1024');
        }

        if (navItem.href === 'api-keys') {
            return (
                (currentType === ModeType.AUTOMATION &&
                    (isFeatureFlagEnabled('ff-1025') || isFeatureFlagEnabled('ff-1039'))) ||
                (currentType === ModeType.EMBEDDED && isFeatureFlagEnabled('ff-520'))
            );
        }

        if (navItem.href === 'admin-api-keys') {
            return isFeatureFlagEnabled('ff-1024');
        }

        if (navItem.href === 'mcp-server') {
            return isFeatureFlagEnabled('ff-2197');
        }

        return true;
    });

    return (
        <LayoutContainer
            leftSidebarBody={
                <LeftSidebarNav
                    body={sidebarNavItems.map((navItem) =>
                        navItem.href ? (
                            <LeftSidebarNavItem
                                item={{
                                    current: location.pathname.includes(navItem.href),
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
