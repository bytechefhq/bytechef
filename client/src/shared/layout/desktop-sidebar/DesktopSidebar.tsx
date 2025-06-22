import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Link, useLocation} from 'react-router-dom';

import './DesktopSidebar.css';

import reactLogo from '@/assets/logo.svg';
import DesktopSidebarBottomMenu from '@/shared/layout/desktop-sidebar/DesktopSidebarBottomMenu';
import {useConnectDialog} from '@bytechef/embedded-react';
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
    const {openDialog} = useConnectDialog({
        integrationId: '1052',
        baseUrl: 'http://127.0.0.1:5173',
        environment: 'DEVELOPMENT',
        // integrationInstanceId: '',
        jwtToken:
            'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNIVmliR2xqT2tWeE9HNW5Sa3BUUW1kTmJsaE1iVkZpTWtkcGRUWkVVRXg0TlcxS1dGbHAifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.IOUDS8b5jp7OxLn-Wcw3e5X6S_zZ7KdHTII3CZ-_OK46JjMEmERKfE_86ZIW0RckConFh6O_V_4tXj1GXJgNH0Zj6VdEjpwNBRXzsh_G_qtFt1cNdvAJbhi1LupOeM9F8aRTyvHci7OD5b5VzYNig7M8FZLx14e5V4AIUscwyi0XOSvud9LaOHU_-sb-K0jLUGEproLYkzXRjkxrP4dxbJGNOK92-9g7ap8AWnrLhYuivTpimkQstf5dWp1lBn-M4t_evu9KyAWYkVCVLVLBmo5mMHc694-OaGv8-zbVxaCXd0BMO68t7FdtA_zZGD1aeFZTVmIqJmVrDthHvAokZw',
    });

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
                        <button onClick={openDialog}>open dialog</button>

                        <DesktopSidebarBottomMenu />
                    </div>
                </div>
            </div>
        </aside>
    );
}
