import Button from '@/components/Button/Button';
import {MenuIcon} from 'lucide-react';

import reactLogo from '../../assets/logo.svg';

export function MobileTopNavigation({setMobileMenuOpen}: {setMobileMenuOpen: (value: boolean) => void}) {
    return (
        <div className="lg:hidden">
            <div className="flex items-center justify-between bg-white px-4 py-2">
                <div>
                    <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                </div>

                <div>
                    <Button
                        aria-label="Open sidebar"
                        icon={<MenuIcon aria-hidden="true" className="size-6" />}
                        onClick={() => setMobileMenuOpen(true)}
                        size="icon"
                        variant="ghost"
                    />
                </div>
            </div>
        </div>
    );
}
