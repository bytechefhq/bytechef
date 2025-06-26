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
        baseUrl: 'http://127.0.0.1:5173',
        environment: 'DEVELOPMENT',
        integrationId: '1050',
        // integrationInstanceId: '',
        jwtToken:
            'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNIVmliR2xqT21kUWNFMHZjVXRpZVRKcVFsWldlSHBHYTFNeFMxTkpTRTVRVERCRU9WcEwifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.T7lg33lpEDgdN16K5_EeqEutzhkPT5K2FouLz2LnrCi3cGcu-jJPjWz6ROy7VD-kIp0sp0MMmHmIyId_W-hyNtNsgpVWv8TIiEYySCAJy6xk15d6TgPA-1WvitdP1_1h_mPvT188rFRKQeRb7XmcKC3d8meOTAM7PDsbfa9h9Xhj2zyFp4TAktaR1Paevf0WkaIrC11aHK-7oiE_YiNGI4Il56DFXaDg-gqw6wGWuTyzIYyeMkewNozy8SzMS0ET1C1O8Z-uF7wh4UMOqVPGsf7htmBw3WZi663SlTGv4V0HfDOUlg5QF2ITYRnC0jPskqxCf4sVC3HS6WOvl-C0_w',
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
