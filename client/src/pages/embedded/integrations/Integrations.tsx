import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useGetIntegrationCategoriesQuery} from '@/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/shared/queries/embedded/integrationTags.quries';
import {TagIcon} from 'lucide-react';
import {useState} from 'react';
import {Outlet, useLocation, useSearchParams} from 'react-router-dom';

export enum Type {
    Category,
    Tag,
}

const Integrations = () => {
    const {pathname} = useLocation();
    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: pathname.includes('unified')
            ? searchParams.get('categoryId')!
            : searchParams.get('categoryId')
              ? parseInt(searchParams.get('categoryId')!)
              : searchParams.get('tagId')
                ? parseInt(searchParams.get('tagId')!)
                : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

    const [filterData, setFilterData] = useState<{id?: number | string; type: Type}>(defaultCurrentState);

    const {data: categories, isLoading: categoriesLoading} = useGetIntegrationCategoriesQuery();

    const {data: tags, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    return (
        <LayoutContainer
            leftSidebarBody={
                <>
                    <div className="pl-0">
                        <LeftSidebarNav
                            body={
                                <>
                                    <LeftSidebarNavItem
                                        item={{
                                            filterData: !filterData?.id && filterData.type === Type.Category,
                                            name: 'All Categories',
                                            onItemClick: (id?: number | string) => {
                                                setFilterData({
                                                    id: id as number,
                                                    type: Type.Category,
                                                });
                                            },
                                        }}
                                    />

                                    {!categoriesLoading &&
                                        categories?.map((item) => (
                                            <LeftSidebarNavItem
                                                item={{
                                                    filterData:
                                                        filterData?.id === item.id && filterData.type === Type.Category,
                                                    id: item.id,
                                                    name: item.name,
                                                    onItemClick: (id?: number | string) => {
                                                        setFilterData({
                                                            id: id as number,
                                                            type: Type.Category,
                                                        });
                                                    },
                                                }}
                                                key={item.name}
                                                toLink={`?categoryId=${item.id}`}
                                            />
                                        ))}
                                </>
                            }
                            title="Categories"
                        />

                        <LeftSidebarNav
                            body={
                                <>
                                    {!tagsLoading &&
                                        (tags?.length ? (
                                            tags?.map((item) => (
                                                <LeftSidebarNavItem
                                                    icon={<TagIcon className="mr-1 size-4" />}
                                                    item={{
                                                        filterData:
                                                            filterData?.id === item.id && filterData.type === Type.Tag,
                                                        id: item.id!,
                                                        name: item.name,
                                                        onItemClick: (id?: number | string) => {
                                                            setFilterData({
                                                                id: id as number,
                                                                type: Type.Tag,
                                                            });
                                                        },
                                                    }}
                                                    key={item.id}
                                                    toLink={`?tagId=${item.id}`}
                                                />
                                            ))
                                        ) : (
                                            <p className="px-3 text-xs">No tags.</p>
                                        ))}
                                </>
                            }
                            title="Tags"
                        />
                    </div>
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Integrations" />}
        >
            <Outlet />
        </LayoutContainer>
    );
};

export default Integrations;
