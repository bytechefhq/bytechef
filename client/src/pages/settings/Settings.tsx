import {CogIcon} from '@heroicons/react/24/outline';
import ThemeSwitcher from '../../components/ThemeSwitcher/ThemeSwitcher';
import {twMerge} from 'tailwind-merge';
import LayoutContainer from '../../layouts/LayoutContainer/LayoutContainer';
import PageHeader from '../../components/PageHeader/PageHeader';
import React from 'react';

const navigation = [{name: 'Display', href: '#', icon: CogIcon, current: true}];

export default function Settings() {
    return (
        <LayoutContainer
            header={<PageHeader title="Display" />}
            leftSidebarHeader={
                <h1
                    aria-labelledby="primary-heading"
                    className="px-2 py-4 text-xl font-semibold tracking-tight text-gray-900 dark:text-gray-200"
                >
                    Settings
                </h1>
            }
            leftSidebarBody={
                <div className="px-2">
                    {navigation.map((item) => (
                        <a
                            key={item.name}
                            href={item.href}
                            className={twMerge(
                                item.current
                                    ? 'bg-gray-100 text-black dark:bg-gray-600 dark:text-gray-300'
                                    : 'border-transparent text-gray-600 hover:bg-gray-50 hover:text-gray-900',
                                'group flex items-center py-2 px-2 text-sm font-medium',
                                'rounded-md'
                            )}
                        >
                            {item.name}
                        </a>
                    ))}
                </div>
            }
        >
            <div className="divide-y divide-gray-200 px-4">
                <div>
                    <dl className="divide-y divide-gray-200">
                        <div className="items-center py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:py-5">
                            <dt className="text-sm font-medium text-gray-900 dark:text-gray-300">
                                Appearance
                            </dt>

                            <dd className="mt-1 flex text-sm text-gray-900 sm:col-span-2 sm:mt-0">
                                <span className="grow">
                                    <ThemeSwitcher />
                                </span>
                            </dd>
                        </div>
                    </dl>
                </div>
            </div>
        </LayoutContainer>
    );
}
