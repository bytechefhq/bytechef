import {Button} from '@/components/ui/button';
import {Dialog, Transition} from '@headlessui/react';
import {Cross1Icon} from '@radix-ui/react-icons';
import {Fragment} from 'react';

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
        <Transition.Root as={Fragment} show={mobileMenuOpen}>
            <Dialog as="div" className="relative z-40 lg:hidden" onClose={setMobileMenuOpen}>
                <Transition.Child
                    as={Fragment}
                    enter="transition-opacity ease-linear duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="transition-opacity ease-linear duration-300"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    <div className="fixed inset-0 bg-gray-600" />
                </Transition.Child>

                <div className="fixed inset-0 z-40 flex">
                    <Transition.Child
                        as={Fragment}
                        enter="transition ease-in-out duration-300 transform"
                        enterFrom="-translate-x-full"
                        enterTo="translate-x-0"
                        leave="transition ease-in-out duration-300 transform"
                        leaveFrom="translate-x-0"
                        leaveTo="-translate-x-full"
                    >
                        <Dialog.Panel className="relative flex w-full max-w-xs flex-1 flex-col bg-white focus:outline-none">
                            <Transition.Child
                                as={Fragment}
                                enter="ease-in-out duration-300"
                                enterFrom="opacity-0"
                                enterTo="opacity-100"
                                leave="ease-in-out duration-300"
                                leaveFrom="opacity-100"
                                leaveTo="opacity-0"
                            >
                                <div className="absolute right-0 top-0 -mr-12 pt-4">
                                    <Button
                                        className="ml-1 items-center justify-center rounded-full p-2 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white"
                                        onClick={() => setMobileMenuOpen(false)}
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <span className="sr-only">Close sidebar</span>

                                        <Cross1Icon aria-hidden="true" className="h-6 w-6 text-white" />
                                    </Button>
                                </div>
                            </Transition.Child>

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
                                                    className="mr-4 h-6 w-6 text-gray-400 group-hover:text-gray-500"
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
                                            <img
                                                alt=""
                                                className="inline-block h-10 w-10 rounded-full"
                                                src={user.imageUrl}
                                            />
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
                        </Dialog.Panel>
                    </Transition.Child>

                    <div aria-hidden="true" className="w-14 shrink-0" />
                </div>
            </Dialog>
        </Transition.Root>
    );
}
