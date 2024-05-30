import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface PageHeaderProps {
    centerTitle?: boolean;
    className?: string;
    position?: 'main' | 'sidebar';
    right?: ReactNode;
    title: string;
    titleClassName?: string;
}

const PageHeader = ({
    centerTitle = false,
    className,
    position = 'sidebar',
    right,
    title,
    titleClassName,
}: PageHeaderProps) => (
    <header className={twMerge('p-4', centerTitle && '2xl:mx-auto 2xl:w-4/5', className)}>
        <div className="flex w-full items-center justify-between">
            <div
                className={twMerge(
                    'flex h-[34px] items-center text-lg tracking-tight text-foreground',
                    position === 'sidebar' ? 'font-semibold' : '',
                    titleClassName
                )}
            >
                {title}
            </div>

            {right && <div>{right}</div>}
        </div>
    </header>
);

export default PageHeader;
