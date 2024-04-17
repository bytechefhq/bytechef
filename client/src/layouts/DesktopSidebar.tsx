import {Avatar, AvatarFallback, AvatarImage} from '@/components/ui/avatar';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Link, useNavigate} from 'react-router-dom';

import './DesktopSidebar.css';

import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import React from 'react';
import {twMerge} from 'tailwind-merge';

import reactLogo from '../assets/logo.svg';

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
    const navigate = useNavigate();

    return (
        <aside className={twMerge('hidden border-r bg-muted lg:flex lg:shrink-0', className)}>
            <div className="flex w-[56px]">
                <div className="flex min-h-0 flex-1 flex-col">
                    <div className="flex-1">
                        <Link to="">
                            <div className="flex items-center justify-center py-4">
                                <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                            </div>
                        </Link>

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
                                    <AvatarImage alt="@shadcn" src="https://github.com/shadcn.png" />

                                    <AvatarFallback>CN</AvatarFallback>
                                </Avatar>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="start" className="w-64 space-y-2 p-2">
                                <DropdownMenuItem
                                    className="cursor-pointer font-semibold"
                                    onClick={() => navigate('/embedded')}
                                >
                                    Embedded
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    className="cursor-pointer font-semibold"
                                    onClick={() => navigate('/automation')}
                                >
                                    Automation
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <div className="min-h-52">
                                    <DropdownMenuItem
                                        className="cursor-pointer font-semibold"
                                        onClick={() => navigate('/settings')}
                                    >
                                        Settings
                                    </DropdownMenuItem>
                                </div>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem className="cursor-pointer font-semibold">Log Out</DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>
        </aside>
    );
}
