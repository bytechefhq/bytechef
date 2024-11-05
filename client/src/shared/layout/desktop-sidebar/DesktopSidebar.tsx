import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Link, useLocation} from 'react-router-dom';

import './DesktopSidebar.css';

import {Button} from '@/components/ui/button';
import {useCopilotStore} from '@/pages/platform/copilot/stores/useCopilotStore';
import DesktopSidebarBottomMenu from '@/shared/layout/desktop-sidebar/DesktopSidebarBottomMenu';
import DesktopSidebarTopMenu from '@/shared/layout/desktop-sidebar/DesktopSidebarTopMenu';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {BotMessageSquareIcon} from 'lucide-react';
import React from 'react';
import {twMerge} from 'tailwind-merge';

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
    const {pathname} = useLocation();

    const {ai} = useApplicationInfoStore();
    const {setShowCopilot, showCopilot} = useCopilotStore();

    const ff_1570 = useFeatureFlagsStore()('ff-1570');

    return (
        <aside className={twMerge('hidden bg-muted lg:flex lg:shrink-0', className)}>
            <div className="w-sidebar-width flex bg-muted border-r">
                <div className="flex min-h-0 flex-1 flex-col">
                    <div className="flex-1">
                        <div className="flex items-center justify-center py-4">
                            <DesktopSidebarTopMenu />
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
                        {ai.enabled && ff_1570 && (
                            <Button onClick={() => setShowCopilot(!showCopilot)} size="icon" variant="ghost">
                                <BotMessageSquareIcon className="size-6" />
                            </Button>
                        )}

                        <DesktopSidebarBottomMenu />
                    </div>
                </div>
            </div>
        </aside>
    );
}
