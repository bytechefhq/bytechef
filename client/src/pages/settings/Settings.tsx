import {CogIcon} from '@heroicons/react/24/outline';
import ThemeSwitcher from '../../components/ThemeSwitcher/ThemeSwitcher';
import cx from 'classnames';
import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';

const navigation = [{name: 'Display', href: '#', icon: CogIcon, current: true}];

export default function Settings() {
    return (
        <SidebarContentLayout
            headerProps={{
                subTitle: 'Display',
            }}
            title={'Settings'}
            sidebar={
                <>
                    {navigation.map((item) => (
                        <a
                            key={item.name}
                            href={item.href}
                            className={cx(
                                item.current
                                    ? 'bg-gray-200 text-black dark:bg-gray-600 dark:text-gray-300'
                                    : 'border-transparent text-gray-600 hover:bg-gray-50 hover:text-gray-900',
                                'group flex items-center py-2 px-3 text-sm font-medium',
                                'rounded-md'
                            )}
                        >
                            {item.name}
                        </a>
                    ))}
                </>
            }
        >
            <div className="divide-y divide-gray-200">
                <div>
                    <dl className="divide-y divide-gray-200">
                        <div className="items-center py-4 sm:grid sm:grid-cols-3 sm:gap-4 sm:py-5">
                            <dt className="text-sm font-medium text-gray-500 dark:text-gray-300">
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
        </SidebarContentLayout>
    );
}
