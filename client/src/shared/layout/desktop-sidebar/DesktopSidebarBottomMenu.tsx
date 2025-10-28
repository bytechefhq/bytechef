import {Avatar, AvatarFallback} from '@/components/ui/avatar';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuPortal,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ModeType, useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useGetUserWorkspacesQuery} from '@/shared/queries/automation/workspaces.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    AudioLinesIcon,
    BlendIcon,
    DiamondIcon,
    HelpCircleIcon,
    PlusIcon,
    SettingsIcon,
    User2Icon,
    UserRoundCogIcon,
} from 'lucide-react';
import {useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const DesktopSidebarBottomMenu = () => {
    const application = useApplicationInfoStore((state) => state.application);
    const {account, logout} = useAuthenticationStore(
        useShallow((state) => ({
            account: state.account,
            logout: state.logout,
        }))
    );
    const {currentType, setCurrentType} = useModeTypeStore(
        useShallow((state) => ({
            currentType: state.currentType,
            setCurrentType: state.setCurrentType,
        }))
    );
    const {currentEnvironmentId, setCurrentEnvironmentId} = useEnvironmentStore(
        useShallow((state) => ({
            currentEnvironmentId: state.currentEnvironmentId,
            setCurrentEnvironmentId: state.setCurrentEnvironmentId,
        }))
    );
    const {currentWorkspaceId, setCurrentWorkspaceId} = useWorkspaceStore(
        useShallow((state) => ({
            currentWorkspaceId: state.currentWorkspaceId,
            setCurrentWorkspaceId: state.setCurrentWorkspaceId,
        }))
    );

    const analytics = useAnalytics();

    const {pathname} = useLocation();

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    const ff_520 = useFeatureFlagsStore()('ff-520');

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: environmentsQuery} = useEnvironmentsQuery();

    const {data: workspaces} = useGetUserWorkspacesQuery(account?.id!, !!account);

    const handleLogOutClick = () => {
        analytics.reset();
        queryClient.resetQueries();
        logout();
    };

    const handleModeTypeChange = (value: string) => {
        const selectedType = +value;

        setCurrentType(selectedType);

        if (selectedType === ModeType.AUTOMATION) {
            navigate(`/automation${currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? '/projects' : '/deployments'}`);
        } else if (selectedType === ModeType.EMBEDDED) {
            navigate(
                `/embedded${currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? '/integrations' : '/configurations'}`
            );
        }
    };

    const handleEnvironmentValueChange = (value: string) => {
        setCurrentEnvironmentId(+value);

        if (currentType === ModeType.AUTOMATION) {
            navigate(`/automation${+value === DEVELOPMENT_ENVIRONMENT ? '/projects' : '/deployments'}`);
        } else if (currentType === ModeType.EMBEDDED) {
            navigate(`/embedded${+value === DEVELOPMENT_ENVIRONMENT ? '/integrations' : '/configurations'}`);
        }
    };

    const handleWorkspaceValueChange = (value: string) => {
        setCurrentWorkspaceId(+value);

        if (currentType === ModeType.AUTOMATION) {
            navigate(`/automation${currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? '/projects' : '/deployments'}`);
        }
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

    useEffect(() => {
        if (workspaces && workspaces.length > 0) {
            if (currentWorkspaceId) {
                if (!workspaces.map((workspace) => workspace.id!).find((id) => id === currentWorkspaceId)) {
                    if (workspaces[0]?.id) {
                        setCurrentWorkspaceId(workspaces[0]?.id);
                    }
                }
            } else if (workspaces[0]?.id && !currentWorkspaceId) {
                setCurrentWorkspaceId(workspaces[0]?.id);
            }
        }
    }, [currentWorkspaceId, workspaces, setCurrentWorkspaceId]);

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Avatar className="cursor-pointer">
                    <AvatarFallback className="bg-white">
                        <User2Icon className="size-6" />
                    </AvatarFallback>
                </Avatar>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="start" className="w-72 space-y-2 p-2">
                <div className="flex items-center space-x-2">
                    <Avatar className="cursor-pointer">
                        <AvatarFallback className="bg-muted">
                            <User2Icon className="size-6" />
                        </AvatarFallback>
                    </Avatar>

                    <div>
                        <div className="text-sm text-muted-foreground">Signed in as</div>

                        <div>{account?.email}</div>
                    </div>
                </div>

                <DropdownMenuSeparator />

                {ff_520 && application?.edition === 'EE' && (
                    <>
                        <DropdownMenuSub>
                            <DropdownMenuSubTrigger className="cursor-pointer font-semibold">
                                <BlendIcon className="size-5" />

                                <span>{`Mode: ${currentType === ModeType.AUTOMATION ? 'Automation' : 'Embedded'}`}</span>
                            </DropdownMenuSubTrigger>

                            <DropdownMenuPortal>
                                <DropdownMenuSubContent>
                                    <DropdownMenuRadioGroup
                                        onValueChange={handleModeTypeChange}
                                        value={currentType?.toString()}
                                    >
                                        <DropdownMenuRadioItem value="0">Automation</DropdownMenuRadioItem>

                                        <DropdownMenuRadioItem value="1">Embedded</DropdownMenuRadioItem>
                                    </DropdownMenuRadioGroup>
                                </DropdownMenuSubContent>
                            </DropdownMenuPortal>
                        </DropdownMenuSub>

                        <DropdownMenuSeparator />
                    </>
                )}

                {pathname.startsWith('/automation') && application?.edition === 'EE' && workspaces && (
                    <>
                        <DropdownMenuSub>
                            <DropdownMenuSubTrigger className="cursor-pointer font-semibold">
                                <DiamondIcon className="size-5" />

                                {`Workspace: ${workspaces.find((w) => w.id === currentWorkspaceId)?.name}`}
                            </DropdownMenuSubTrigger>

                            <DropdownMenuPortal>
                                <DropdownMenuSubContent>
                                    <DropdownMenuRadioGroup
                                        onValueChange={handleWorkspaceValueChange}
                                        value={currentWorkspaceId?.toString()}
                                    >
                                        {workspaces.map((workspace) => (
                                            <DropdownMenuRadioItem key={workspace.id} value={workspace.id!.toString()}>
                                                {workspace.name}
                                            </DropdownMenuRadioItem>
                                        ))}
                                    </DropdownMenuRadioGroup>

                                    <DropdownMenuSeparator />

                                    <DropdownMenuItem
                                        className="flex space-x-2"
                                        onClick={() => navigate('/automation/settings/workspaces')}
                                    >
                                        <PlusIcon /> <span>New Workspace</span>
                                    </DropdownMenuItem>
                                </DropdownMenuSubContent>
                            </DropdownMenuPortal>
                        </DropdownMenuSub>

                        <DropdownMenuSeparator />
                    </>
                )}

                {application?.edition === 'EE' && environmentsQuery?.environments && (
                    <>
                        <DropdownMenuSub>
                            <DropdownMenuSubTrigger className="cursor-pointer font-semibold">
                                <AudioLinesIcon className="size-5" />

                                {`Environment: ${environmentsQuery?.environments.find((w) => +w?.id! === currentEnvironmentId)?.name}`}
                            </DropdownMenuSubTrigger>

                            <DropdownMenuPortal>
                                <DropdownMenuSubContent>
                                    <DropdownMenuRadioGroup
                                        onValueChange={handleEnvironmentValueChange}
                                        value={currentEnvironmentId?.toString()}
                                    >
                                        {environmentsQuery?.environments.map((environment) => (
                                            <DropdownMenuRadioItem key={environment?.id} value={environment?.id!}>
                                                {environment?.name}
                                            </DropdownMenuRadioItem>
                                        ))}
                                    </DropdownMenuRadioGroup>
                                </DropdownMenuSubContent>
                            </DropdownMenuPortal>
                        </DropdownMenuSub>

                        <DropdownMenuSeparator />
                    </>
                )}

                <div className="min-h-40 space-y-1">
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

                    <DropdownMenuItem
                        className="cursor-pointer font-semibold"
                        onClick={() => window.open('https://docs.bytechef.io', '_blank')}
                    >
                        <div className="flex items-center space-x-1">
                            <HelpCircleIcon className="size-5" />

                            <span>Documentation</span>
                        </div>
                    </DropdownMenuItem>
                </div>

                <DropdownMenuSeparator />

                <DropdownMenuItem className="cursor-pointer font-semibold" onClick={handleLogOutClick}>
                    Log Out
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default DesktopSidebarBottomMenu;
