import {ReactNode} from 'react';

import {TemplatesLayoutContainerCategoryFilters} from './TemplatesLayoutContainerCategoryFilters';
import {TemplatesLayoutContainerSearchBar} from './TemplatesLayoutContainerSearchBar';

interface TemplateImportsProps {
    children?: ReactNode;
    searchPlaceholder: string;
    title: string;
}

const TemplatesLayoutContainer = ({children, searchPlaceholder, title}: TemplateImportsProps) => {
    return (
        <div className="size-full">
            <main className="mx-auto mt-14 flex h-full max-w-7xl flex-col overflow-auto px-4 py-8 sm:px-6 lg:px-8">
                <div className="mb-8 text-center">
                    <h1 className="mb-10 text-4xl font-bold text-foreground">{title}</h1>

                    <div className="mb-6">
                        <TemplatesLayoutContainerSearchBar placeholder={searchPlaceholder} />
                    </div>

                    <div className="flex justify-center">
                        <TemplatesLayoutContainerCategoryFilters />
                    </div>
                </div>

                <div className="mt-12 flex-1">{children}</div>
            </main>
        </div>
    );
};

export default TemplatesLayoutContainer;
