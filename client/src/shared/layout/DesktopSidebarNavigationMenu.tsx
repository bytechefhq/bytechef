'use client';

import {
    NavigationMenu,
    NavigationMenuContent,
    NavigationMenuItem,
    NavigationMenuLink,
    NavigationMenuList,
    NavigationMenuTrigger,
    NavigationMenuViewport,
} from '@/components/ui/navigation-menu';
import {cn} from '@/shared/util/cn-utils';
import {ComponentPropsWithoutRef, ElementRef, ReactNode, forwardRef} from 'react';
import {Link} from 'react-router-dom';

import reactLogo from '../../assets/logo.svg';

const DesktopSidebarNavigationMenu = ({children}: {children: ReactNode}) => {
    return (
        <NavigationMenu>
            <NavigationMenuList>
                <NavigationMenuItem>
                    <NavigationMenuTrigger
                        className="m-0 bg-transparent p-0"
                        onPointerLeave={(event) => event.preventDefault()}
                        onPointerMove={(event) => event.preventDefault()}
                    >
                        {children}
                    </NavigationMenuTrigger>

                    <NavigationMenuContent>
                        <ul className="grid w-[450px] gap-3 p-4 lg:grid-cols-[.60fr_1fr]">
                            <li className="row-span-3">
                                <div className=" flex size-full select-none items-center gap-2 rounded-md bg-gradient-to-b from-muted/50 to-muted p-4 no-underline outline-none focus:shadow-md">
                                    <img alt="ByteChef" className="size-8" src={reactLogo} />

                                    <div className="my-2 text-lg font-medium">ByteChef</div>
                                </div>
                            </li>

                            <ListItem href="/embedded" title="Embedded">
                                Build integrations for your product.
                            </ListItem>

                            <ListItem href="/automation" title="Automation">
                                Automate your daily work.
                            </ListItem>
                        </ul>
                    </NavigationMenuContent>
                </NavigationMenuItem>
            </NavigationMenuList>

            <NavigationMenuViewport />
        </NavigationMenu>
    );
};

const ListItem = forwardRef<ElementRef<'a'>, ComponentPropsWithoutRef<'a'>>(
    ({children, className, href, title, ...props}, ref) => {
        return (
            <li>
                <NavigationMenuLink asChild>
                    <Link
                        className={cn(
                            'block select-none space-y-1 rounded-md p-3 leading-none no-underline outline-none transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground',
                            className
                        )}
                        ref={ref}
                        to={href!}
                        {...props}
                    >
                        <div className="text-sm font-medium leading-none">{title}</div>

                        <p className="line-clamp-2 text-sm leading-snug text-muted-foreground">{children}</p>
                    </Link>
                </NavigationMenuLink>
            </li>
        );
    }
);

ListItem.displayName = 'ListItem';

export default DesktopSidebarNavigationMenu;
