import reactLogo from '@/assets/logo.svg';
import Button from '@/components/Button/Button';
import {useSidebar} from '@/components/ui/sidebar';
import {MenuIcon} from 'lucide-react';

export function MobileTopNavigation() {
    const {toggleSidebar} = useSidebar();

    return (
        <div className="lg:hidden">
            <div className="flex items-center justify-between bg-surface-neutral-primary px-4 py-2">
                <div>
                    <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                </div>

                <Button
                    aria-label="Open sidebar"
                    className="size-10 [&_svg]:size-7"
                    icon={<MenuIcon className="size-7" />}
                    onClick={toggleSidebar}
                    size="icon"
                    variant="ghost"
                />
            </div>
        </div>
    );
}
