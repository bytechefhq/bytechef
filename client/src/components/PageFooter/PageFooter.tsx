import React, {PropsWithChildren, ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface PageFooterProps {
    leftSidebar?: boolean;
    position?: 'main' | 'sidebar';
    right?: ReactNode;
}

const PageFooter = ({
    children,
    position = 'sidebar',
    leftSidebar = false,
    right,
}: PropsWithChildren<PageFooterProps>) => (
    <footer
        className={twMerge(
            'p-4',
            position === 'main' ? 'flex w-full place-self-center 2xl:w-4/5' : ''
        )}
    >
        <div className="flex w-full items-center justify-between">
            <div
                className={twMerge(
                    'flex h-[34px] w-full items-center text-lg tracking-tight text-gray-900 dark:text-gray-200',
                    leftSidebar && 'font-semibold'
                )}
            >
                {children}
            </div>

            {right && <div>{right}</div>}
        </div>
    </footer>
);

export default PageFooter;
