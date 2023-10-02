import Avatar from '@/components/Avatar/Avatar';
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip';
import {Link} from 'react-router-dom';

import reactLogo from '../assets/logo.svg';

export function DesktopSidebar({
    navigation,
}: {
    navigation: {
        name: string;
        href: string;
        icon: React.ForwardRefExoticComponent<
            Omit<React.SVGProps<SVGSVGElement>, 'ref'>
        >;
    }[];
}) {
    return (
        <aside className="hidden bg-muted lg:flex lg:shrink-0">
            <div className="flex w-[64px]">
                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
                    <div className="flex-1">
                        <Link to="">
                            <div className="flex items-center justify-center py-4">
                                <img
                                    className="h-8 w-auto"
                                    src={reactLogo}
                                    alt="ByteChef"
                                />
                            </div>
                        </Link>

                        <nav
                            aria-label="Sidebar"
                            className="flex flex-col items-center py-3"
                        >
                            {navigation.map((item) => (
                                <Link
                                    key={item.name}
                                    to={item.href}
                                    className="flex items-center rounded-lg p-4 hover:text-blue-600"
                                >
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <item.icon
                                                className="h-7 w-7"
                                                aria-hidden="true"
                                            />
                                        </TooltipTrigger>

                                        <TooltipContent side="right">
                                            {item.name}
                                        </TooltipContent>
                                    </Tooltip>

                                    <span className="sr-only">{item.name}</span>
                                </Link>
                            ))}
                        </nav>
                    </div>

                    <div className="flex shrink-0 justify-center py-4">
                        <Link className="flex" to="/settings">
                            <Avatar size="small" />
                        </Link>
                    </div>
                </div>
            </div>
        </aside>
    );
}
