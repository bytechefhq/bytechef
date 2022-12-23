import reactLogo from '../../assets/logo.svg';
import {Bars3Icon} from '@heroicons/react/24/outline';

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
                    <button
                        type="button"
                        className="-mr-3 inline-flex h-12 w-12 items-center justify-center rounded-md bg-white text-black hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white"
                        onClick={() => setMobileMenuOpen(true)}
                    >
                        <span className="sr-only">Open sidebar</span>

                        <Bars3Icon className="h-6 w-6" aria-hidden="true" />
                    </button>
                </div>
            </div>
        </div>
    );
}
