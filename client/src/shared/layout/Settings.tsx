import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import SettingsNavGroup, {SettingsNavGroupItemI} from '@/shared/layout/SettingsNavGroup';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useEffect, useMemo} from 'react';
import {Outlet, useLocation} from 'react-router-dom';

export interface SettingsNavItemI {
    href?: string;
    items?: SettingsNavItemI[];
    title: string;
}

const isCurrentHref = (pathname: string, href: string) => {
    if (href.startsWith('/')) {
        return pathname === href || pathname.startsWith(`${href}/`);
    }

    return pathname.endsWith(`/${href}`) || pathname.includes(`/${href}/`);
};

const SETTINGS_PATH_SEGMENT = '/settings/';

const getSettingsPageKey = (href: string | undefined) => {
    if (!href) {
        return href;
    }

    const segmentIndex = href.lastIndexOf(SETTINGS_PATH_SEGMENT);

    return segmentIndex === -1 ? href : href.slice(segmentIndex + SETTINGS_PATH_SEGMENT.length);
};

interface SettingsProps {
    sidebarNavItems: SettingsNavItemI[];
    title?: string;
}

const Settings = ({sidebarNavItems, title = 'Settings'}: SettingsProps) => {
    const billingEnabled = useApplicationInfoStore((state) => state.billing.enabled);
    const isFeatureFlagEnabled = useFeatureFlagsStore();

    const location = useLocation();

    const isNavItemVisible = (navItem: SettingsNavItemI) => {
        const pageKey = getSettingsPageKey(navItem.href);

        if (pageKey === 'api-connectors') {
            return isFeatureFlagEnabled('ff-207');
        }

        if (pageKey === 'custom-components') {
            return isFeatureFlagEnabled('ff-1024');
        }

        if (pageKey === 'git-configuration') {
            return isFeatureFlagEnabled('ff-1039');
        }

        if (pageKey === 'mcp-server') {
            return isFeatureFlagEnabled('ff-2197');
        }

        if (pageKey === 'admin-api-keys') {
            return isFeatureFlagEnabled('ff-1024');
        }

        if (pageKey === 'identity-providers') {
            return isFeatureFlagEnabled('ff-1040');
        }

        if (pageKey === 'billing') {
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

    const currentNavItem = useMemo(
        () =>
            sidebarNavItems
                .flatMap((navItem) => navItem.items ?? [navItem])
                .filter((navItem) => navItem.href && isCurrentHref(location.pathname, navItem.href))
                .sort((navItem, otherNavItem) => otherNavItem.href!.length - navItem.href!.length)[0],
        [location.pathname, sidebarNavItems]
    );

    const currentNavItemTitle = currentNavItem?.title;

    useEffect(() => {
        if (currentNavItemTitle) {
            document.title = currentNavItemTitle;
        }
    }, [currentNavItemTitle]);

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
                                    key={navItem.items.map((groupItem) => groupItem.href).join()}
                                    title={navItem.title}
                                />
                            );
                        }

                        if (navItem.href) {
                            return (
                                <LeftSidebarNavItem
                                    item={{
                                        current: navItem.href === currentNavItem?.href,
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
