import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

import LeftSidebarToggle from './LeftSidebarToggle';
import {useLeftSidebarToggle} from './LeftSidebarToggleContext';

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
}: HeaderProps) => {
    // LeftSidebarToggle renders nothing when the page has no sidebar, so the box that holds it cannot be
    // rendered unconditionally either — an empty box would still claim the flex gap beside the title.
    const {hasLeftSidebar} = useLeftSidebarToggle();

    return (
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
                {/* The toggle sits BESIDE the title-and-description block rather than inside the title row,
                    so the description starts under the title rather than under the toggle. Its own box is
                    one title-row tall and the group is top-aligned, which keeps the toggle level with the
                    title rather than sliding down between the two lines.
                    self-start is what makes the position absolute rather than relative: the row centres its
                    children, so without it a `right` slot taller than the title row (an EnvironmentSelect, a
                    button group) would push the toggle down by half the difference — a page-by-page drift of
                    a few pixels depending on what its header happens to carry. */}

                <div className="flex items-start gap-2 self-start">
                    {/* Sidebar headers never carry the toggle: it belongs beside the content, not inside the
                        panel it hides. */}

                    {position === 'main' && hasLeftSidebar && (
                        <div className="flex h-header-height items-center">
                            <LeftSidebarToggle />
                        </div>
                    )}

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
                </div>

                {right && <div>{right}</div>}
            </div>
        </header>
    );
};

export default Header;
