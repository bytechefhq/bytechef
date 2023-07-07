import Button from '@/components/Button/Button';
import {Bars3Icon} from '@heroicons/react/24/outline';

import reactLogo from '../assets/logo.svg';

export function MobileTopNavigation({
    setMobileMenuOpen,
}: {
    setMobileMenuOpen: (value: boolean) => void;
}) {
    return (
        <div className="lg:hidden">
            <div className="flex items-center justify-between bg-white px-4 py-2 sm:px-6 lg:px-8">
                <div>
                    <img
                        className="h-8 w-auto"
                        src={reactLogo}
                        alt="ByteChef"
                    />
                </div>

                <div>
                    <Button
                        displayType="icon"
                        icon={
                            <Bars3Icon className="h-6 w-6" aria-hidden="true" />
                        }
                        onClick={() => setMobileMenuOpen(true)}
                    >
                        <span className="sr-only">Open sidebar</span>
                    </Button>
                </div>
            </div>
        </div>
    );
}
