import React, {ReactNode} from 'react';
import cx from 'classnames';

interface PageHeaderProps {
    leftSidebar?: boolean;
    position?: 'main' | 'sidebar';
    right?: ReactNode;
    title: string;
}

const PageHeader = ({
    title,
    position = 'sidebar',
    leftSidebar = false,
    right,
}: PageHeaderProps) => (
    <header
        className={cx(
            'p-4',
            position === 'main'
                ? 'flex place-self-center sm:w-full 2xl:w-4/5'
                : ''
        )}
    >
        <div className="flex w-full items-center justify-between">
            <div
                className={cx(
                    'flex h-[34px] items-center text-lg tracking-tight text-gray-900 dark:text-gray-200',
                    leftSidebar ? 'font-semibold' : ''
                )}
            >
                {title}
            </div>

            {right && <div>{right}</div>}
        </div>
    </header>
);

export default PageHeader;
