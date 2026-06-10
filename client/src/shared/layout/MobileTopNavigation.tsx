import {SidebarTrigger} from '@/components/ui/sidebar';

import reactLogo from '../../assets/logo.svg';

export function MobileTopNavigation() {
    return (
        <div className="lg:hidden">
            <div className="flex items-center justify-between bg-white px-4 py-2">
                <div>
                    <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                </div>

                <SidebarTrigger aria-label="Open sidebar" />
            </div>
        </div>
    );
}
