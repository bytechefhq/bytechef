import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Link, useLocation} from 'react-router-dom';

import './DesktopSidebar.css';

import reactLogo from '@/assets/logo.svg';
import DesktopSidebarBottomMenu from '@/shared/layout/desktop-sidebar/DesktopSidebarBottomMenu';
import {ForwardRefExoticComponent, SVGProps} from 'react';
import {twMerge} from 'tailwind-merge';

export function DesktopSidebar({
    className,
    navigation,
}: {
    className?: string;
    navigation: {
        name: string;
        href: string;
        icon: ForwardRefExoticComponent<Omit<SVGProps<SVGSVGElement>, 'ref'>>;
    }[];
}) {
    const {pathname} = useLocation();

    return (
        <aside className={twMerge('hidden bg-muted lg:flex lg:shrink-0', className)}>
            <div className="flex w-sidebar-width border-r border-r-border/50 bg-muted">
                <div className="flex min-h-0 flex-1 flex-col">
                    <div className="flex-1">
                        <div className="flex items-center justify-center py-4">
                            <Link to="/">
                                <img alt="ByteChef" className="h-8 w-auto cursor-pointer" src={reactLogo} />
                            </Link>
                        </div>

                        <nav aria-label="Sidebar" className="flex flex-col items-center overflow-y-auto">
                            {navigation.map((item) => (
                                <div className="p-0.5" key={item.name}>
                                    <Link
                                        className={twMerge(
                                            'flex items-center rounded-lg p-2 hover:text-blue-600',
                                            pathname.includes(item.href) && 'text-blue-600'
                                        )}
                                        to={item.href}
                                    >
                                        <Tooltip>
                                            <TooltipTrigger>
                                                <item.icon aria-hidden="true" className="size-6" />
                                            </TooltipTrigger>

                                            <TooltipContent side="right">{item.name}</TooltipContent>
                                        </Tooltip>

                                        <span className="sr-only">{item.name}</span>
                                    </Link>
                                </div>
                            ))}
                        </nav>
                    </div>

                    <div className="flex shrink-0 flex-col items-center justify-center gap-4 py-4">
                        <DesktopSidebarBottomMenu />
                    </div>
                </div>
            </div>
        </aside>
    );
}
