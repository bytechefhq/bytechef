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
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {
    BlendIcon,
    ChevronsUpDownIcon,
    ClipboardCheckIcon,
    HelpCircleIcon,
    SettingsIcon,
    User2Icon,
    UserRoundCogIcon,
} from 'lucide-react';
import {useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

export function AppSidebarFooter() {
    const application = useApplicationInfoStore((state) => state.application);
    const {account, logout} = useAuthenticationStore(
        useShallow((state) => ({
            account: state.account,
            logout: state.logout,
        }))
    );
    const {currentType, setCurrentType} = usePlatformTypeStore(
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

    const analytics = useAnalytics();

    const {pathname} = useLocation();

    const navigate = useNavigate();

    const queryClient = useQueryClient();

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: environmentsQuery} = useEnvironmentsQuery();

    const handleLogOutClick = async () => {
        analytics.reset();

        await queryClient.cancelQueries();

        await logout();

        queryClient.clear();
    };

    const handlePlatformTypeChange = (value: string) => {
        const selectedType = +value;

        setCurrentType(selectedType);

        if (selectedType === PlatformType.AUTOMATION) {
            navigate(`/automation${currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? '/projects' : '/deployments'}`);
        } else if (selectedType === PlatformType.EMBEDDED) {
            navigate(
                `/embedded${currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? '/integrations' : '/configurations'}`
            );
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

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <button
                    aria-label="User menu"
                    className="flex h-12 w-full items-center gap-2 rounded-md px-[3px] py-2 text-left hover:bg-sidebar-accent"
                    type="button"
                >
                    <Avatar className="shrink-0">
                        <AvatarFallback className="bg-white text-primary">
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

                {application?.edition === 'EE' && (
                    <>
                        <DropdownMenuSub>
                            <DropdownMenuSubTrigger className="cursor-pointer font-semibold">
                                <BlendIcon className="size-5" />

                                <span>{`Mode: ${currentType === PlatformType.AUTOMATION ? 'Automation' : 'Embedded'}`}</span>
                            </DropdownMenuSubTrigger>

                            <DropdownMenuPortal>
                                <DropdownMenuSubContent>
                                    <DropdownMenuRadioGroup
                                        onValueChange={handlePlatformTypeChange}
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

                <div className="min-h-40 space-y-1">
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
}
