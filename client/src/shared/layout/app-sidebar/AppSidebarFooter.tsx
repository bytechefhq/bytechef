import {Avatar, AvatarFallback} from '@/components/ui/avatar';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuPortal,
    DropdownMenuSeparator,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import shouldShowEmbeddedGettingStartedHelpLink from '@/ee/pages/embedded/getting-started/shouldShowEmbeddedGettingStartedHelpLink';
import {useUserGuidingLauncher} from '@/hooks/useUserGuiding';
import shouldShowAutomationGettingStartedHelpLink from '@/pages/automation/getting-started/shouldShowAutomationGettingStartedHelpLink';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AUTOMATION_GETTING_STARTED_PATH, EMBEDDED_GETTING_STARTED_PATH} from '@/pages/home/constants/platformModes';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {AUTHORITIES, DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    BookOpenIcon,
    BugIcon,
    ChevronsUpDownIcon,
    ClipboardCheckIcon,
    EyeIcon,
    EyeOffIcon,
    HelpCircleIcon,
    HistoryIcon,
    MessagesSquareIcon,
    RocketIcon,
    SettingsIcon,
    User2Icon,
    UserRoundCogIcon,
} from 'lucide-react';
import {useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const openExternalLink = (url: string) => window.open(url, '_blank', 'noopener,noreferrer');

export function AppSidebarFooter() {
    const {docs, edition} = useApplicationInfoStore(
        useShallow((state) => ({
            docs: state.docs,
            edition: state.application?.edition,
        }))
    );
    const {account, logout} = useAuthenticationStore(
        useShallow((state) => ({
            account: state.account,
            logout: state.logout,
        }))
    );
    const currentType = usePlatformTypeStore((state) => state.currentType);
    const {currentEnvironmentId, setCurrentEnvironmentId} = useEnvironmentStore(
        useShallow((state) => ({
            currentEnvironmentId: state.currentEnvironmentId,
            setCurrentEnvironmentId: state.setCurrentEnvironmentId,
        }))
    );
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const canCreateProject = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_CREATE');

    const userGuidingLauncher = useUserGuidingLauncher();

    const {pathname} = useLocation();

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const showEmbeddedGettingStartedHelpLink = shouldShowEmbeddedGettingStartedHelpLink({
        edition,
        environmentId: currentEnvironmentId,
        isAdmin: !!account?.authorities?.includes(AUTHORITIES.ADMIN),
        pathname,
    });

    const showAutomationGettingStartedHelpLink = shouldShowAutomationGettingStartedHelpLink({
        canCreateProject,
        development: currentEnvironmentId === DEVELOPMENT_ENVIRONMENT,
        pathname,
    });

    const gettingStartedHelpPath = showEmbeddedGettingStartedHelpLink
        ? EMBEDDED_GETTING_STARTED_PATH
        : showAutomationGettingStartedHelpLink
          ? AUTOMATION_GETTING_STARTED_PATH
          : undefined;

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: environmentsQuery} = useEnvironmentsQuery();

    const handleLogOutClick = async () => {
        await queryClient.cancelQueries();

        await logout();

        queryClient.clear();
    };

    useEffect(() => {
        const environments = environmentsQuery?.environments;

        if (environments && environments.length > 0) {
            if (currentEnvironmentId) {
                if (!environments.map((environment) => environment?.id!).find((id) => +id === currentEnvironmentId)) {
                    if (environments[0]?.id) {
                        setCurrentEnvironmentId(+environments[0]?.id);
                    }
                }
            } else if (environments[0]?.id && !currentEnvironmentId) {
                setCurrentEnvironmentId(+environments[0]?.id);
            }
        }
    }, [currentEnvironmentId, environmentsQuery?.environments, setCurrentEnvironmentId]);

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <button
                    aria-label="User menu"
                    className="flex h-12 w-full items-center gap-2 rounded-md px-[3px] py-2 text-left hover:bg-sidebar-accent"
                    type="button"
                >
                    <Avatar className="shrink-0">
                        <AvatarFallback className="bg-surface-neutral-primary text-primary">
                            <User2Icon className="size-7" />
                        </AvatarFallback>
                    </Avatar>

                    <div className="flex w-full min-w-0 items-center justify-between group-data-[collapsible=icon]:hidden">
                        <div className="flex flex-1 flex-col">
                            <span className="text-xs text-muted-foreground">Signed in as</span>

                            <span className="truncate text-sm font-medium">{account?.email}</span>
                        </div>

                        <ChevronsUpDownIcon className="size-4" />
                    </div>
                </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="start" className="w-72 space-y-2 p-2">
                <div className="flex items-center space-x-2">
                    <Avatar className="cursor-pointer">
                        <AvatarFallback className="bg-muted">
                            <User2Icon className="size-6" />
                        </AvatarFallback>
                    </Avatar>

                    <div className="min-w-0">
                        <div className="text-sm text-muted-foreground">Signed in as</div>

                        <div className="text-sm break-all">{account?.email}</div>
                    </div>
                </div>

                <DropdownMenuSeparator />

                <div className="space-y-1">
                    {currentType === PlatformType.AUTOMATION && (
                        <DropdownMenuItem
                            className="cursor-pointer font-semibold"
                            onClick={() => navigate('/automation/approval-tasks')}
                        >
                            <div className="flex items-center space-x-1">
                                <ClipboardCheckIcon className="size-5" />

                                <span>Approval Tasks</span>
                            </div>
                        </DropdownMenuItem>
                    )}

                    <DropdownMenuItem
                        className="cursor-pointer font-semibold"
                        onClick={() =>
                            navigate(`${pathname.startsWith('/automation') ? '/automation' : '/embedded'}/settings`)
                        }
                    >
                        <div className="flex items-center space-x-1">
                            <SettingsIcon className="size-5" />

                            <span>Settings</span>
                        </div>
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        className="cursor-pointer font-semibold"
                        onClick={() =>
                            navigate(`${pathname.startsWith('/automation') ? '/automation' : '/embedded'}/account`)
                        }
                    >
                        <div className="flex items-center space-x-1">
                            <UserRoundCogIcon className="size-5" />

                            <span>Your account</span>
                        </div>
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuSub>
                        <DropdownMenuSubTrigger className="cursor-pointer font-semibold">
                            <div className="flex items-center space-x-1">
                                <HelpCircleIcon className="size-5" />

                                <span>Help & Feedback</span>
                            </div>
                        </DropdownMenuSubTrigger>

                        <DropdownMenuPortal>
                            <DropdownMenuSubContent className="w-56">
                                {gettingStartedHelpPath && (
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        onClick={() => navigate(gettingStartedHelpPath)}
                                    >
                                        <RocketIcon className="size-4" />

                                        <span>Getting started</span>
                                    </DropdownMenuItem>
                                )}

                                {docs.documentationUrl && (
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        onClick={() => openExternalLink(docs.documentationUrl!)}
                                    >
                                        <BookOpenIcon className="size-4" />

                                        <span>Documentation</span>
                                    </DropdownMenuItem>
                                )}

                                {docs.changelogUrl && (
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        onClick={() => openExternalLink(docs.changelogUrl!)}
                                    >
                                        <HistoryIcon className="size-4" />

                                        <span>Changelog</span>
                                    </DropdownMenuItem>
                                )}

                                {(docs.communitySupportUrl || docs.reportBugUrl) && (
                                    <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
                                        Need Help?
                                    </DropdownMenuLabel>
                                )}

                                {docs.communitySupportUrl && (
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        onClick={() => openExternalLink(docs.communitySupportUrl!)}
                                    >
                                        <MessagesSquareIcon className="size-4" />

                                        <span>Community Support</span>
                                    </DropdownMenuItem>
                                )}

                                {docs.reportBugUrl && (
                                    <DropdownMenuItem
                                        className="cursor-pointer"
                                        onClick={() => openExternalLink(docs.reportBugUrl!)}
                                    >
                                        <BugIcon className="size-4" />

                                        <span>Report a Bug</span>
                                    </DropdownMenuItem>
                                )}

                                {userGuidingLauncher.available && (
                                    <>
                                        <DropdownMenuSeparator />

                                        <DropdownMenuItem
                                            className="cursor-pointer"
                                            onClick={
                                                userGuidingLauncher.launcherHidden
                                                    ? userGuidingLauncher.showLauncher
                                                    : userGuidingLauncher.hideLauncher
                                            }
                                        >
                                            {userGuidingLauncher.launcherHidden ? (
                                                <EyeIcon className="size-4" />
                                            ) : (
                                                <EyeOffIcon className="size-4" />
                                            )}

                                            <span>
                                                {userGuidingLauncher.launcherHidden ? 'Show Guide' : 'Hide Guide'}
                                            </span>
                                        </DropdownMenuItem>
                                    </>
                                )}
                            </DropdownMenuSubContent>
                        </DropdownMenuPortal>
                    </DropdownMenuSub>
                </div>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="cursor-pointer font-semibold" onClick={handleLogOutClick}>
                    Log Out
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
