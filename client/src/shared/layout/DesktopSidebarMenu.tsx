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
import {useGetUserWorkspacesQuery} from '@/shared/queries/automation/workspaces.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {PlusIcon} from '@radix-ui/react-icons';
import {SettingsIcon, User2Icon, UserRoundCog} from 'lucide-react';
import React, {useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';

const DesktopSidebarMenu = () => {
    const {application} = useApplicationInfoStore();
    const {account, logout} = useAuthenticationStore();
    const {currentWorkspaceId, setCurrentWorkspaceId} = useWorkspaceStore();

    const {pathname} = useLocation();

    const navigate = useNavigate();

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workspaces} = useGetUserWorkspacesQuery(account?.id!, !!account);

    const handleLogOutClick = () => {
        logout();
    };

    const handleWorkflowValueChange = (value: string) => {
        setCurrentWorkspaceId(+value);

        navigate('/automation/projects');
    };

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

            <DropdownMenuContent align="start" className="w-64 space-y-2 p-2">
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

                <div className="min-h-52 space-y-1">
                    {pathname.startsWith('/automation') && application?.edition === 'ee' && workspaces && (
                        <>
                            <DropdownMenuSub>
                                <DropdownMenuSubTrigger className="cursor-pointer font-semibold">
                                    Workspaces
                                </DropdownMenuSubTrigger>

                                <DropdownMenuPortal>
                                    <DropdownMenuSubContent>
                                        <DropdownMenuRadioGroup
                                            onValueChange={handleWorkflowValueChange}
                                            value={currentWorkspaceId?.toString()}
                                        >
                                            {workspaces.map((workspace) => (
                                                <DropdownMenuRadioItem
                                                    key={workspace.id}
                                                    value={workspace.id!.toString()}
                                                >
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
                            <UserRoundCog className="size-5" />

                            <span>Your account</span>
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

export default DesktopSidebarMenu;
