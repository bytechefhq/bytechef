import { Fragment, useState } from 'react'
import { Dialog, Transition } from '@headlessui/react'
import { CogIcon, XMarkIcon } from '@heroicons/react/24/outline'
import ThemeSwitcher from '../../components/ThemeSwitcher/ThemeSwitcher'
import cx from 'classnames'
import { Bars3BottomLeftIcon } from '@heroicons/react/24/solid'

const navigation = [
  { name: 'Display', href: '#', icon: CogIcon, current: true }
]

export default function Settings() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <>
      <div>
        <Transition.Root show={sidebarOpen} as={Fragment}>
          <Dialog
            as="div"
            className="relative z-40 md:hidden"
            onClose={setSidebarOpen}
          >
            <Transition.Child
              as={Fragment}
              enter="transition-opacity ease-linear duration-300"
              enterFrom="opacity-0"
              enterTo="opacity-100"
              leave="transition-opacity ease-linear duration-300"
              leaveFrom="opacity-100"
              leaveTo="opacity-0"
            >
              <div className="fixed inset-0 bg-gray-600 bg-opacity-75" />
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
                <Dialog.Panel className="relative flex w-full max-w-xs flex-1 flex-col bg-white pt-5 pb-4">
                  <Transition.Child
                    as={Fragment}
                    enter="ease-in-out duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in-out duration-300"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                  >
                    <div className="absolute top-0 right-0 -mr-14 p-1">
                      <button
                        type="button"
                        className="flex h-12 w-12 items-center justify-center rounded-full focus:bg-gray-600 focus:outline-none"
                        onClick={() => setSidebarOpen(false)}
                      >
                        <XMarkIcon
                          className="h-6 w-6 text-white"
                          aria-hidden="true"
                        />
                        <span className="sr-only">Close sidebar</span>
                      </button>
                    </div>
                  </Transition.Child>

                  <div className="mt-5 h-0 flex-1 overflow-y-auto">
                    <nav className="flex h-full flex-col">
                      <div className="space-y-1">
                        {navigation.map((item) => (
                          <a
                            key={item.name}
                            href={item.href}
                            className={cx(
                              item.current
                                ? 'border-blue-600 bg-blue-50 text-blue-600'
                                : 'border-transparent text-gray-600 hover:bg-gray-50 hover:text-gray-900',
                              'group flex items-center border-l-4 py-2 px-3 text-base font-medium'
                            )}
                            aria-current={item.current ? 'page' : undefined}
                          >
                            {/*<item.icon*/}
                            {/*  className={cx(*/}
                            {/*    item.current*/}
                            {/*      ? 'text-blue-500'*/}
                            {/*      : 'text-gray-400 group-hover:text-gray-500',*/}
                            {/*    'mr-4 h-6 w-6 flex-shrink-0'*/}
                            {/*  )}*/}
                            {/*  aria-hidden="true"*/}
                            {/*/>*/}
                            {item.name}
                          </a>
                        ))}
                      </div>
                    </nav>
                  </div>
                </Dialog.Panel>
              </Transition.Child>
              <div className="w-14 flex-shrink-0" aria-hidden="true">
                {/* Dummy element to force sidebar to shrink to fit close icon */}
              </div>
            </div>
          </Dialog>
        </Transition.Root>

        {/* Static sidebar for desktop */}
        <div className="hidden md:fixed md:inset-y-0 md:flex md:w-64 md:flex-col">
          {/* Sidebar component, swap this element with another sidebar if you like */}
          <nav className="flex flex-grow flex-col overflow-y-auto border-gray-200 bg-gray-50 pt-4 pb-4 dark:bg-gray-700">
            <div className="flex flex-shrink-0 items-center px-4">
              <h1 className="text-3xl font-bold tracking-tight text-gray-900 dark:text-gray-200">
                Settings
              </h1>
            </div>
            <div className="mt-5 flex-grow">
              <div className="space-y-1">
                {navigation.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    className={cx(
                      item.current
                        ? 'bg-gray-200 text-black dark:bg-gray-600 dark:text-gray-300'
                        : 'border-transparent text-gray-600 hover:bg-gray-50 hover:text-gray-900',
                      'group flex items-center py-2 px-3 text-sm font-medium',
                      'rounded-md',
                      'ml-2 mr-2'
                    )}
                  >
                    {/*<item.icon*/}
                    {/*  className={cx(*/}
                    {/*    item.current*/}
                    {/*      ? 'text-black dark:text-gray-300'*/}
                    {/*      : 'text-gray-400 group-hover:text-gray-500',*/}
                    {/*    'mr-3 h-6 w-6 flex-shrink-0'*/}
                    {/*  )}*/}
                    {/*  aria-hidden="true"*/}
                    {/*/>*/}
                    {item.name}
                  </a>
                ))}
              </div>
            </div>
          </nav>
        </div>

        {/* Content area */}
        <div className="md:pl-64">
          <div className="mx-auto flex flex-col sm:px-6 md:px-8">
            {/*<div className="sticky top-0 z-10 flex h-16 flex-shrink-0 border-b border-gray-200 bg-white">*/}
            {/*  <button*/}
            {/*    type="button"*/}
            {/*    className="border-gray-200 px-4 text-gray-500 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-purple-500 md:hidden"*/}
            {/*    onClick={() => setSidebarOpen(true)}*/}
            {/*  >*/}
            {/*    <span className="sr-only">Open sidebar</span>*/}
            {/*    <Bars3BottomLeftIcon className="h-6 w-6" aria-hidden="true" />*/}
            {/*  </button>*/}
            {/*</div>*/}

            <main className="flex-1">
              <div className="relative mx-auto">
                <div className="pt-4 pb-16">
                  <div className="px-4 sm:px-6 md:px-0">
                    <h1 className="text-3xl font-bold tracking-tight text-gray-900 dark:text-gray-200">
                      Display
                    </h1>
                  </div>
                  <div className="px-4 sm:px-6 md:px-0">
                    <div className="py-6">
                      {/* Description list with inline editing */}
                      <div className="divide-y divide-gray-200">
                        <div>
                          <dl className="divide-y divide-gray-200">
                            <div className="items-center py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:py-5">
                              <dt className="text-sm font-medium text-gray-500 dark:text-gray-300">
                                Appearance
                              </dt>
                              <dd className="mt-1 flex text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                <span className="flex-grow">
                                  <ThemeSwitcher />
                                </span>
                              </dd>
                            </div>
                          </dl>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </main>
          </div>
        </div>
      </div>
    </>
  )
}
