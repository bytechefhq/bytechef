import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import SettingsNavGroup, {SettingsNavGroupItemI} from '@/shared/layout/SettingsNavGroup';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {Outlet, useLocation} from 'react-router-dom';

export interface SettingsNavItemI {
    href?: string;
    items?: SettingsNavItemI[];
    title: string;
}

interface SettingsProps {
    sidebarNavItems: SettingsNavItemI[];
    title?: string;
}

const Settings = ({sidebarNavItems, title = 'Settings'}: SettingsProps) => {
    const currentType = usePlatformTypeStore((state) => state.currentType);

    const billingEnabled = useApplicationInfoStore((state) => state.billing.enabled);
    const isFeatureFlagEnabled = useFeatureFlagsStore();

    const location = useLocation();

    const isNavItemVisible = (navItem: SettingsNavItemI) => {
        if (navItem.href === 'api-connectors') {
            return isFeatureFlagEnabled('ff-207');
        }

        if (navItem.href?.includes('/account/appearance')) {
            return isFeatureFlagEnabled('ff-445');
        }

        if (navItem.href === 'custom-components') {
            return isFeatureFlagEnabled('ff-1024');
        }

        if (navItem.href === 'git-configuration') {
            return isFeatureFlagEnabled('ff-1039');
        }

        if (navItem.href === 'workspace-api-keys') {
            return (
                (currentType === PlatformType.AUTOMATION &&
                    (isFeatureFlagEnabled('ff-1025') ||
                        isFeatureFlagEnabled('ff-1039') ||
                        isFeatureFlagEnabled('ff-4814'))) ||
                currentType === PlatformType.EMBEDDED
            );
        }

        if (navItem.href === 'mcp-server') {
            return isFeatureFlagEnabled('ff-2197');
        }

        if (navItem.href === 'admin-api-keys') {
            return isFeatureFlagEnabled('ff-1024');
        }

        if (navItem.href === 'identity-providers') {
            return isFeatureFlagEnabled('ff-1040');
        }

        if (navItem.href === 'billing') {
            return billingEnabled;
        }

        return true;
    };

    sidebarNavItems = sidebarNavItems
        .filter(isNavItemVisible)
        .map((navItem) => (navItem.items ? {...navItem, items: navItem.items.filter(isNavItemVisible)} : navItem))
        .filter((navItem) => !navItem.items || navItem.items.length > 0);

    const isHeading = (navItem: SettingsNavItemI) => navItem.href === undefined && navItem.items === undefined;

    sidebarNavItems = sidebarNavItems.filter(
        (navItem, index, visibleNavItems) =>
            !isHeading(navItem) || (visibleNavItems[index + 1] !== undefined && !isHeading(visibleNavItems[index + 1]))
    );

    return (
        <LayoutContainer
            leftSidebarBody={
                <LeftSidebarNav
                    body={sidebarNavItems.map((navItem) => {
                        if (navItem.items) {
                            return (
                                <SettingsNavGroup
                                    isCurrent={(href) => location.pathname.includes(href)}
                                    items={navItem.items as SettingsNavGroupItemI[]}
                                    key={navItem.title}
                                    title={navItem.title}
                                />
                            );
                        }

                        if (navItem.href) {
                            return (
                                <LeftSidebarNavItem
                                    item={{
                                        current: location.pathname.includes(navItem.href),
                                        name: navItem.title,
                                    }}
                                    key={navItem.href}
                                    toLink={navItem.href}
                                />
                            );
                        }

                        return (
                            <h3
                                className="px-2 pt-4 pb-1 text-sm font-semibold text-muted-foreground first:pt-0"
                                key={navItem.title}
                            >
                                {navItem.title}
                            </h3>
                        );
                    })}
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
