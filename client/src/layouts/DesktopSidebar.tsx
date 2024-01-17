import {Avatar, AvatarFallback, AvatarImage} from '@/components/ui/avatar';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Link} from 'react-router-dom';

import reactLogo from '../assets/logo.svg';

export function DesktopSidebar({
    navigation,
}: {
    navigation: {
        name: string;
        href: string;
        icon: React.ForwardRefExoticComponent<Omit<React.SVGProps<SVGSVGElement>, 'ref'>>;
    }[];
}) {
    return (
        <aside className="hidden border-r bg-muted lg:flex lg:shrink-0">
            <div className="flex w-[56px]">
                <div className="flex min-h-0 flex-1 flex-col">
                    <div className="flex-1">
                        <Link to="">
                            <div className="flex items-center justify-center py-4">
                                <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                            </div>
                        </Link>

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
