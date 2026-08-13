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
    // The 3xl width cap matches the main content's `3xl:mx-auto 3xl:w-4/5` containers, so it belongs to the
    // main header only. Inside a fixed-width sidebar it shrinks the header to 80% of the rail and pulls any
    // right-aligned action away from the edge the search box and rows below still reach.
    <header
        className={twMerge(
            'py-3',
            // px-3 in a sidebar so the title lines up with the rows beneath it; the main header keeps px-4,
            // where there is nothing below to align to.
            position === 'sidebar' ? 'px-3' : 'px-4',
            position === 'main' && (centerTitle ? '3xl:mx-auto 3xl:w-4/5' : '3xl:w-4/5'),
            className
        )}
    >
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
