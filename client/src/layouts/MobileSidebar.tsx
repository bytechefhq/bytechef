import {Dialog, DialogContent} from '@/components/ui/dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

import reactLogo from '../assets/logo.svg';

type Props = {
    user: {name: string; email: string; imageUrl: string};
    navigation: {
        name: string;
        href: string;
        icon: React.ForwardRefExoticComponent<Omit<React.SVGProps<SVGSVGElement>, 'ref'>>;
    }[];
    mobileMenuOpen: boolean;
    setMobileMenuOpen: (value: boolean) => void;
};

export function MobileSidebar({mobileMenuOpen, navigation, setMobileMenuOpen, user}: Props) {
    return (
        <Dialog onOpenChange={setMobileMenuOpen} open={mobileMenuOpen}>
            <DialogContent className="flex h-full flex-col bg-white focus:outline-none">
                <div className="absolute right-4 top-0 pt-4">
                    <button
                        className="ml-1 items-center justify-center rounded-full p-2 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white"
                        onClick={() => setMobileMenuOpen(false)}
                    >
                        <span className="sr-only">Close sidebar</span>

                        <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
                    </button>
                </div>

                <div className="pb-4 pt-5">
                    <div className="flex shrink-0 items-center px-4">
                        <img alt="ByteChef" className="h-8 w-auto" src={reactLogo} />
                    </div>

                    <nav aria-label="Sidebar" className="mt-5">
                        <div className="space-y-1 px-2">
                            {navigation.map((item) => (
                                <a
                                    className="group flex items-center rounded-md p-2 text-base font-medium text-gray-600 hover:bg-gray-100 hover:text-gray-900"
                                    href={item.href}
                                    key={item.name}
                                >
                                    <item.icon
                                        aria-hidden="true"
                                        className="mr-4 size-6 text-gray-400 group-hover:text-gray-500"
                                    />

                                    {item.name}
                                </a>
                            ))}
                        </div>
                    </nav>
                </div>

                <div className="flex shrink-0 border-t border-gray-200 p-4">
                    <a className="group block shrink-0" href="#">
                        <div className="flex items-center">
                            <div>
                                <img alt="" className="inline-block size-10 rounded-full" src={user.imageUrl} />
                            </div>

                            <div className="ml-3">
                                <p className="text-base font-medium text-gray-700 group-hover:text-gray-900">
                                    {user.name}
                                </p>

                                <p className="text-sm font-medium text-gray-500 group-hover:text-gray-700">
                                    Account Settings
                                </p>
                            </div>
                        </div>
                    </a>
                </div>
            </DialogContent>
        </Dialog>
    );
}
