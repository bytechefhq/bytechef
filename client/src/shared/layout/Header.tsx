import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface HeaderProps {
    centerTitle?: boolean;
    className?: string;
    description?: string;
    position?: 'main' | 'sidebar';
    right?: ReactNode;
    title: string | ReactNode;
    titleClassName?: string;
}

const Header = ({
    centerTitle = false,
    className,
    description,
    position = 'sidebar',
    right,
    title,
    titleClassName,
}: HeaderProps) => (
    <header className={twMerge('px-4 py-3', centerTitle ? '2xl:mx-auto 2xl:w-4/5' : '3xl:w-4/5', className)}>
        <div className="flex w-full items-center justify-between">
            <div className="flex flex-col">
                <div
                    className={twMerge(
                        'flex h-header-height flex-col justify-center text-lg tracking-tight text-foreground',
                        position === 'sidebar' ? 'font-semibold' : '',
                        titleClassName
                    )}
                >
                    {title}
                </div>

                <div className="text-sm text-muted-foreground">{description}</div>
            </div>

            {right && <div>{right}</div>}
        </div>
    </header>
);

export default Header;
