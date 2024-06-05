import {Avatar, AvatarFallback} from '@/components/ui/avatar';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Link, useLocation, useNavigate} from 'react-router-dom';

import './DesktopSidebar.css';

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
import DesktopSidebarNavigationMenu from '@/shared/layout/DesktopSidebarNavigationMenu';
import {useGetUserWorkspacesQuery} from '@/shared/queries/automation/workspaces.queries';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {SettingsIcon, UserCircle2Icon, UserRoundCog} from 'lucide-react';
import React, {useEffect} from 'react';
import {twMerge} from 'tailwind-merge';

import reactLogo from '../../assets/logo.svg';

export function DesktopSidebar({
    className,
    navigation,
}: {
    className?: string;
    navigation: {
        name: string;
        href: string;
        icon: React.ForwardRefExoticComponent<Omit<React.SVGProps<SVGSVGElement>, 'ref'>>;
    }[];
}) {
    const {account, logout} = useAuthenticationStore();
    const {currentWorkspaceId, setCurrentWorkspaceId} = useWorkspaceStore();

    const {pathname} = useLocation();

    const navigate = useNavigate();

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workspaces} = useGetUserWorkspacesQuery(account?.id!, !!account);

    const handleLogOutClick = () => {
        logout();
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
        <aside className={twMerge('hidden border-r bg-muted lg:flex lg:shrink-0', className)}>
            <div className="flex w-[56px]">
                <div className="flex min-h-0 flex-1 flex-col">
                    <div className="flex-1">
                        <div className="flex items-center justify-center py-4">
                            <DesktopSidebarNavigationMenu>
                                <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                            </DesktopSidebarNavigationMenu>
                        </div>

                        <nav aria-label="Sidebar" className="flex flex-col items-center overflow-y-auto">
                            {navigation.map((item) => (
                                <Link
                                    className="flex items-center rounded-lg p-3 hover:text-blue-600"
                                    key={item.name}
                                    to={item.href}
                                >
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <item.icon aria-hidden="true" className="size-7" />
                                        </TooltipTrigger>

                                        <TooltipContent side="right">{item.name}</TooltipContent>
                                    </Tooltip>

                                    <span className="sr-only">{item.name}</span>
                                </Link>
                            ))}
                        </nav>
                    </div>

                    <div className="flex shrink-0 justify-center py-4">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Avatar className="cursor-pointer">
                                    <AvatarFallback>
                                        <UserCircle2Icon className="size-8" />
                                    </AvatarFallback>
                                </Avatar>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="start" className="w-64 space-y-2 p-2">
                                <div className="flex items-center space-x-2">
                                    <div>
                                        <UserCircle2Icon className="size-8" />
                                    </div>

                                    <div>
                                        <div className="text-sm text-muted-foreground">Signed in as</div>

                                        <div>{account?.email}</div>
                                    </div>
                                </div>

                                <DropdownMenuSeparator />

                                <div className="min-h-52 space-y-1">
                                    {pathname.startsWith('/automation') && workspaces && workspaces.length > 1 && (
                                        <>
                                            <DropdownMenuSub>
                                                <DropdownMenuSubTrigger className="cursor-pointer font-semibold">
                                                    Workspaces
                                                </DropdownMenuSubTrigger>

                                                <DropdownMenuPortal>
                                                    <DropdownMenuSubContent>
                                                        <DropdownMenuRadioGroup
                                                            onValueChange={(value) => setCurrentWorkspaceId(+value)}
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
                                                    </DropdownMenuSubContent>
                                                </DropdownMenuPortal>
                                            </DropdownMenuSub>

                                            <DropdownMenuSeparator />
                                        </>
                                    )}

                                    <DropdownMenuItem
                                        className="cursor-pointer font-semibold"
                                        onClick={() =>
                                            navigate(
                                                `${pathname.startsWith('/automation') ? '/automation' : '/embedded'}/settings`
                                            )
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
                                            navigate(
                                                `${pathname.startsWith('/automation') ? '/automation' : '/embedded'}/account`
                                            )
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
                    </div>
                </div>
            </div>
        </aside>
    );
}
