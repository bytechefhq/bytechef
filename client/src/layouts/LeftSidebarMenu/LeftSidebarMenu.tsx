import React, {ReactNode} from 'react';

const SidebarSubtitle = ({title}: {title: string}): JSX.Element => (
    <h4 className="py-1 px-2 pr-4 text-sm font-medium tracking-tight text-gray-900 dark:text-gray-200">
        {title}
    </h4>
);

export interface LeftSidebarProps {
    bottomBody?: ReactNode;
    bottomTitle: string;
    topBody: ReactNode;
    topTitle: string;
}

const LeftSidebarMenu = ({
    bottomBody,
    bottomTitle,
    topBody,
    topTitle,
}: LeftSidebarProps): JSX.Element => {
    return (
        <div className="px-2">
            <div className="mb-4 space-y-1" aria-label={topTitle}>
                <SidebarSubtitle title={topTitle} />

                {topBody}
            </div>

            <div className="mb-4 space-y-1" aria-label={bottomTitle}>
                <SidebarSubtitle title={bottomTitle} />

                {bottomBody}
            </div>
        </div>
    );
};

export default LeftSidebarMenu;
