import React, {useState} from 'react';
import {
    useGetIntegrationCategoriesQuery,
    useGetIntegrationTagsQuery,
} from '../../queries/integrations';
import LeftSidebarItem, {Type} from './LeftSidebarItem';
import {useSearchParams} from 'react-router-dom';
import {TagIcon} from '@heroicons/react/20/solid';

const SidebarSubtitle: React.FC<{title: string}> = ({title}) => (
    <h4 className="py-1 px-2 pr-4 text-sm font-medium tracking-tight text-gray-900 dark:text-gray-200">
        {title}
    </h4>
);

const LeftSidebar: React.FC = () => {
    const [searchParams] = useSearchParams();
    const [current, setCurrent] = useState<{id?: number; type: Type}>({
        id: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : searchParams.get('tagId')
            ? parseInt(searchParams.get('tagId')!)
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
        <div className="px-2">
            <div className="mb-4 space-y-1" aria-label="Categories">
                <SidebarSubtitle title="Categories" />

                <LeftSidebarItem
                    item={{
                        name: 'All Categories',
                        type: Type.Category,
                        current: !current?.id && current.type === Type.Category,
                        onItemClick,
                    }}
                />

                {!categoriesIsLoading &&
                    categories?.map((item) => (
                        <LeftSidebarItem
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

            <div className="mb-4 space-y-1" aria-label="Categories">
                <SidebarSubtitle title="Tags" />

                {!tagsIsLoading &&
                    (!tags?.length ? (
                        <p className="px-3 text-xs">No tags.</p>
                    ) : (
                        tags?.map((item) => (
                            <LeftSidebarItem
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
                                icon={<TagIcon className="mr-1 h-4 w-4" />}
                            />
                        ))
                    ))}
            </div>
        </div>
    );
};

export default LeftSidebar;
