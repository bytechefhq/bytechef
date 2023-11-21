import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import * as React from 'react';

export function RightSidebar({
    navigation,
}: {
    navigation: {
        name: string;
        icon: React.ForwardRefExoticComponent<
            Omit<React.SVGProps<SVGSVGElement>, 'ref'>
        >;
        onClick?: () => void;
    }[];
}) {
    return (
        <aside className="hidden bg-muted lg:flex lg:shrink-0">
            <div className="flex w-[48px]">
                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
                    <div className="flex-1">
                        <nav
                            aria-label="Sidebar"
                            className="flex flex-col items-center py-3"
                        >
                            {navigation.map((item) => (
                                <a
                                    className="flex items-center rounded-lg p-3 hover:text-blue-600"
                                    key={item.name}
                                    onClick={item.onClick}
                                >
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <item.icon
                                                aria-hidden="true"
                                                className="h-6 w-6"
                                            />
                                        </TooltipTrigger>

                                        <TooltipContent side="left">
                                            {item.name}
                                        </TooltipContent>
                                    </Tooltip>

                                    <span className="sr-only">{item.name}</span>
                                </a>
                            ))}
                        </nav>
                    </div>
                </div>
            </div>
        </aside>
    );
}
