import reactLogo from '@/assets/logo.svg';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useGetUserWorkspacesQuery} from '@/shared/queries/automation/workspaces.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ChevronsUpDownIcon, SettingsIcon} from 'lucide-react';
import {useEffect} from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const Logo = () => (
    <span className="flex size-10 shrink-0 items-center justify-center">
        <img alt="ByteChef" className="size-8 max-w-none shrink-0" src={reactLogo} />
    </span>
);

export function AppSidebarWorkspaceSelect() {
    const account = useAuthenticationStore((state) => state.account);
    const application = useApplicationInfoStore((state) => state.application);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentType = usePlatformTypeStore((state) => state.currentType);
    const {currentWorkspaceId, setCurrentWorkspaceId} = useWorkspaceStore(
        useShallow((state) => ({
            currentWorkspaceId: state.currentWorkspaceId,
            setCurrentWorkspaceId: state.setCurrentWorkspaceId,
        }))
    );

    const {pathname} = useLocation();

    const navigate = useNavigate();

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workspaces} = useGetUserWorkspacesQuery(account?.id!, !!account);

    const currentWorkspace = workspaces?.find((workspace) => workspace.id === currentWorkspaceId);

    const handleWorkspaceValueChange = (value: string) => {
        setCurrentWorkspaceId(+value);

        if (currentType === PlatformType.AUTOMATION) {
            void navigate(
                `/automation${currentEnvironmentId === DEVELOPMENT_ENVIRONMENT ? '/projects' : '/deployments'}`
            );
        }
    };

    useEffect(() => {
        const firstWorkspaceId = workspaces?.[0]?.id;

        if (!firstWorkspaceId) {
            return;
        }

        if (!workspaces.some((workspace) => workspace.id === currentWorkspaceId)) {
            setCurrentWorkspaceId(firstWorkspaceId);
        }
    }, [currentWorkspaceId, workspaces, setCurrentWorkspaceId]);

    if (!pathname.startsWith('/automation') || application?.edition !== 'EE' || !workspaces?.length) {
        return (
            <Link className="flex min-w-0 flex-1 items-center gap-1 group-data-[collapsible=icon]:flex-none" to="/">
                <Logo />

                <span className="px-2 text-base font-medium group-data-[collapsible=icon]:hidden">ByteChef</span>
            </Link>
        );
    }

    return (
        <div className="flex min-w-0 flex-1 items-center gap-1 group-data-[collapsible=icon]:flex-none">
            <Link className="shrink-0" to="/">
                <Logo />
            </Link>

            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <button
                        aria-label="Workspace menu"
                        className="flex h-9 min-w-0 flex-1 items-center gap-2 rounded-md px-2 text-left group-data-[collapsible=icon]:hidden hover:bg-sidebar-accent data-[state=open]:bg-sidebar-accent"
                        type="button"
                    >
                        <span className="truncate text-base font-medium">{currentWorkspace?.name}</span>

                        <ChevronsUpDownIcon className="ml-auto size-4 shrink-0 text-muted-foreground" />
                    </button>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="start" className="w-64">
                    <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
                        Workspaces
                    </DropdownMenuLabel>

                    <DropdownMenuRadioGroup
                        onValueChange={handleWorkspaceValueChange}
                        value={currentWorkspaceId?.toString()}
                    >
                        {workspaces.map((workspace) => (
                            <DropdownMenuRadioItem
                                className="cursor-pointer"
                                key={workspace.id}
                                value={workspace.id!.toString()}
                            >
                                {workspace.name}
                            </DropdownMenuRadioItem>
                        ))}
                    </DropdownMenuRadioGroup>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        className="cursor-pointer pl-8"
                        onClick={() => void navigate('/automation/settings/workspaces')}
                    >
                        <SettingsIcon className="absolute left-2 size-3.5" />

                        <span>Manage Workspaces</span>
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}
