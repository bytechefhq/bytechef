import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import * as React from 'react';
import {twMerge} from 'tailwind-merge';

export function RightSidebar({
    className,
    navigation,
}: {
    className?: string;
    navigation: {
        name?: string;
        icon?: React.ForwardRefExoticComponent<Omit<React.SVGProps<SVGSVGElement>, 'ref'>>;
        onClick?: () => void;
        separator?: boolean;
    }[];
}) {
    return (
        <div className={twMerge('hidden lg:flex lg:shrink-0', className)}>
            <div className="flex min-h-0 flex-col overflow-y-auto">
                <nav aria-label="Sidebar" className="flex flex-col items-center rounded-lg py-3">
                    {navigation.map((item, index) =>
                        item.separator ? (
                            <div className="my-2 w-8/12 border-b" key={`right-sidebar-separator-${index}`} />
                        ) : (
                            <button
                                className="flex items-center rounded-lg p-3 hover:text-blue-600 [&_svg]:size-5"
                                key={item.name}
                                onClick={item.onClick}
                            >
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        {item.icon && <item.icon aria-hidden="true" />}
                                    </TooltipTrigger>

                                    <TooltipContent side="left">{item.name}</TooltipContent>
                                </Tooltip>

                                <span className="sr-only">{item.name}</span>
                            </button>
                        )
                    )}
                </nav>
            </div>
        </div>
    );
}
