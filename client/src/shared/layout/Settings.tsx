import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ReactNode} from 'react';
import {Outlet, useLocation} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

interface SettingsProps {
    sidebarNavItems: {
        href?: string;
        subgroup?: boolean;
        title: string;
    }[];
    title?: string;
}

// Matched on whole path segments rather than a bare substring: `users` is a substring of
// `workspace-users`, so on the workspace page both the workspace and the organization entry lit up
// at once. A nested route still counts as inside its nav item, which is what the substring match
// was giving us by accident.
export const isNavItemCurrent = (pathname: string, href: string): boolean => {
    const segmentPath = href.startsWith('/') ? href : `/${href}`;

    return pathname === segmentPath || pathname.endsWith(segmentPath) || pathname.includes(`${segmentPath}/`);
};

const Settings = ({sidebarNavItems, title = 'Settings'}: SettingsProps) => {
    const currentType = usePlatformTypeStore((state) => state.currentType);

    const billingEnabled = useApplicationInfoStore((state) => state.billing.enabled);
    const mcpServerEnabled = useApplicationInfoStore((state) => state.ai.mcp.server.enabled);
    const isFeatureFlagEnabled = useFeatureFlagsStore();

    const location = useLocation();

    sidebarNavItems = sidebarNavItems.filter((navItem) => {
        if (navItem.href === 'components') {
            return isFeatureFlagEnabled('ff-1024') || isFeatureFlagEnabled('ff-207');
        }

        if (navItem.href?.includes('/account/appearance')) {
            return isFeatureFlagEnabled('ff-445');
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
            return isFeatureFlagEnabled('ff-2197') && mcpServerEnabled;
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
    });

    // A subgroup heading sits at the same level as a top-level one — same type scale, same
    // indent — so a group reads consistently wherever it appears. `subgroup` only tightens the
    // space above it, since a subgroup follows items it belongs with rather than opening a new
    // section.
    const navigationElements: ReactNode[] = sidebarNavItems.map((navItem) =>
        navItem.href ? (
            <LeftSidebarNavItem
                item={{
                    current: isNavItemCurrent(location.pathname, navItem.href),
                    name: navItem.title,
                }}
                key={navItem.href}
                toLink={navItem.href}
            />
        ) : (
            <h3
                className={twMerge(
                    'px-2 pb-1 text-sm font-semibold text-muted-foreground',
                    navItem.subgroup ? 'pt-3' : 'pt-4'
                )}
                key={navItem.title}
            >
                {navItem.title}
            </h3>
        )
    );

    return (
        <LayoutContainer
            leftSidebarBody={<LeftSidebarNav body={navigationElements} />}
            leftSidebarHeader={<Header position="sidebar" title={title} />}
        >
            <div className="size-full">
                <Outlet />
            </div>
        </LayoutContainer>
    );
};

export default Settings;
