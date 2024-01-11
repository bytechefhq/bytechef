import {Avatar, AvatarFallback, AvatarImage} from '@/components/ui/avatar';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Link, To} from 'react-router-dom';

import './DesktopSidebar.css';

import {
    NavigationMenu,
    NavigationMenuContent,
    NavigationMenuIndicator,
    NavigationMenuItem,
    NavigationMenuLink,
    NavigationMenuList,
    NavigationMenuTrigger,
} from '@/components/ui/navigation-menu';
import React, {PropsWithChildren} from 'react';
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
    return (
        <aside className={twMerge('hidden border-r bg-muted lg:flex lg:shrink-0', className)}>
            <div className="flex w-[56px]">
                <div className="flex min-h-0 flex-1 flex-col">
                    <div className="flex-1">
                        <NavigationMenu className="my-4">
                            <NavigationMenuList>
                                <NavigationMenuItem>
                                    <NavigationMenuTrigger className="bg-transparent px-3 hover:bg-transparent focus:bg-transparent data-[active]:bg-transparent data-[state=open]:bg-transparent">
                                        <div className="flex items-center justify-center">
                                            <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                                        </div>
                                    </NavigationMenuTrigger>

                                    <NavigationMenuContent>
                                        <ul className="grid gap-3 p-4 md:w-[400px] lg:w-[500px] lg:grid-cols-[.75fr_1fr]">
                                            <li className="row-span-3">
                                                <NavigationMenuLink asChild>
                                                    <a
                                                        className="flex size-full w-full select-none flex-col justify-center rounded-md bg-gradient-to-b from-muted/50 to-muted p-4 no-underline outline-none focus:shadow-md"
                                                        href="/"
                                                    >
                                                        <div className="flex gap-3">
                                                            <div className="mb-2">
                                                                <img
                                                                    alt="ByteChef"
                                                                    className="h-8 w-auto"
                                                                    src={reactLogo}
                                                                />
                                                            </div>

                                                            <div className="mb-2 text-lg font-medium">ByteChef</div>
                                                        </div>

                                                        <p className="text-sm leading-tight text-muted-foreground">
                                                            The API Integration & Workflow Automation Platform.
                                                        </p>
                                                    </a>
                                                </NavigationMenuLink>
                                            </li>

                                            <ListItem title="Embedded" to="/embedded">
                                                Allow your users to integrate your product with applications they use.
                                            </ListItem>

                                            <ListItem title="Automation" to="/automation">
                                                Integrate applications and automate processes inside your organization.
                                            </ListItem>
                                        </ul>
                                    </NavigationMenuContent>
                                </NavigationMenuItem>

                                {/*eslint-disable tailwindcss/no-custom-classname*/}

                                <NavigationMenuIndicator className="NavigationMenuIndicator" />
                            </NavigationMenuList>
                        </NavigationMenu>

                        <nav aria-label="Sidebar" className="flex flex-col items-center overflow-y-auto py-3">
                            {navigation.map((item) => (
                                <Link
                                    className="flex items-center rounded-lg py-3 hover:text-blue-600"
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
                        <Link className="flex" to="/settings">
                            <Avatar>
                                <AvatarImage alt="@shadcn" src="https://github.com/shadcn.png" />

                                <AvatarFallback>CN</AvatarFallback>
                            </Avatar>
                        </Link>
                    </div>
                </div>
            </div>
        </aside>
    );
}

const ListItem = ({children, title, to, ...props}: PropsWithChildren<{title: string; to: To}>) => {
    return (
        <li>
            <NavigationMenuLink asChild>
                <Link
                    className="block select-none space-y-1 rounded-md p-3 leading-none no-underline outline-none transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground"
                    to={to}
                    {...props}
                >
                    <div className="text-sm font-medium leading-none">{title}</div>

                    <p className="line-clamp-2 text-sm leading-snug text-muted-foreground">{children}</p>
                </Link>
            </NavigationMenuLink>
        </li>
    );
};
