import {Button} from '@/components/ui/button';
import {Menu} from 'lucide-react';

import reactLogo from '../assets/logo.svg';

export function MobileTopNavigation({setMobileMenuOpen}: {setMobileMenuOpen: (value: boolean) => void}) {
    return (
        <div className="lg:hidden">
            <div className="flex items-center justify-between bg-white px-4 py-2 sm:px-6 lg:px-8">
                <div>
                    <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                </div>

                <div>
                    <Button onClick={() => setMobileMenuOpen(true)} size="icon" variant="ghost">
                        <span className="sr-only">Open sidebar</span>

                        <Menu aria-hidden="true" className="size-6" />
                    </Button>
                </div>
            </div>
        </div>
    );
}
