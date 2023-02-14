import React, {ReactNode} from 'react';
import cx from 'classnames';

interface PageHeaderProps {
    leftSidebar?: boolean;
    className?: string;
    right?: ReactNode;
    title: string;
}

const PageHeader = ({
    title,
    className,
    leftSidebar = false,
    right,
}: PageHeaderProps) => (
    <header className={cx('p-4', className)}>
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
