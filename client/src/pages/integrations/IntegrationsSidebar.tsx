import React, {useState} from 'react';
import {
    useGetIntegrationCategoriesQuery,
    useGetIntegrationTagsQuery,
} from '../../queries/integrations.queries';
import IntegrationsSidebarItem, {Type} from './IntegrationsSidebarItem';
import {useSearchParams} from 'react-router-dom';

const IntegrationsSidebar: React.FC = () => {
    const [searchParams] = useSearchParams();
    const [current, setCurrent] = useState<{id?: number; type: Type}>({
        id: searchParams.get('categoryId')
            ? +searchParams.get('categoryId')!
            : searchParams.get('tagId')
            ? +searchParams.get('tagId')!
            : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    });
    const {isLoading: categoriesIsLoading, data: categories} =
        useGetIntegrationCategoriesQuery();
    const {isLoading: tagsIsLoading, data: tags} = useGetIntegrationTagsQuery();

    const onItemClick = (type: Type, id?: number) => {
        setCurrent({id, type});
    };

    return (
        <>
            <div className="mb-4 space-y-1" aria-label="Categories">
                <SidebarSubtitle title="Categories" />

                <IntegrationsSidebarItem
                    item={{
                        name: 'All Categories',
                        type: Type.Category,
                        current: !current?.id && current.type === Type.Category,
                        onItemClick,
                    }}
                />

                {!categoriesIsLoading &&
                    categories?.map((item) => (
                        <IntegrationsSidebarItem
                            key={item.id}
                            item={{
                                id: item.id,
                                name: item.name,
                                type: Type.Category,
                                current:
                                    current?.id === item.id &&
                                    current.type === Type.Category,
                                onItemClick,
                            }}
                        />
                    ))}
            </div>
            <div className="space-y-1" aria-label="Categories">
                <SidebarSubtitle title="Tags" />

                {!tagsIsLoading && tags?.length === 0 ? (
                    <p className="px-3 text-xs">No tags.</p>
                ) : (
                    tags?.map((item) => (
                        <IntegrationsSidebarItem
                            key={item.id}
                            item={{
                                id: item.id,
                                name: item.name,
                                type: Type.Tag,
                                current:
                                    current?.id === item.id &&
                                    current.type === Type.Tag,
                                onItemClick,
                            }}
                        />
                    ))
                )}
            </div>
        </>
    );
};

const SidebarSubtitle: React.FC<{title: string}> = ({title}) => {
    return (
        <div>
            <h4 className="py-1 px-3 pr-4 text-sm font-medium tracking-tight text-gray-900 dark:text-gray-200">
                {title}
            </h4>
        </div>
    );
};

export default IntegrationsSidebar;
