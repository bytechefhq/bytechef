import reactLogo from '../../assets/logo.svg';
import {Bars3Icon} from '@heroicons/react/24/outline';
import Button from 'components/Button/Button';

export function MobileTopNavigation({
    setMobileMenuOpen,
}: {
    setMobileMenuOpen: (value: boolean) => void;
}) {
    return (
        <div className="lg:hidden">
            <div className="flex items-center justify-between bg-white py-2 px-4 sm:px-6 lg:px-8">
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
