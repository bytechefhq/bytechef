import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import LayoutContainer from '@/layouts/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/layouts/LeftSidebarNav';
import PageHeader from '@/layouts/PageHeader';
import {useGetIntegrationCategoriesQuery} from '@/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/queries/embedded/integrationTags.quries';
import {useGetIntegrationsQuery} from '@/queries/embedded/integrations.queries';
import {SquareIcon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import IntegrationDialog from './components/IntegrationDialog';
import IntegrationList from './components/IntegrationList';

export enum Type {
    Category,
    Tag,
}

const Integrations = () => {
    const [searchParams] = useSearchParams();

    const defaultCurrentState = {
        id: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

    const [filterData, setFilterData] = useState<{id?: number; type: Type}>(defaultCurrentState);

    const {
        data: categories,
        error: categoriesError,
        isLoading: categoriesIsLoading,
    } = useGetIntegrationCategoriesQuery();

    const {
        data: integrations,
        error: integrationsError,
        isLoading: integrationsIsLoading,
    } = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetIntegrationTagsQuery();

    let pageTitle: string | undefined;

    if (filterData.type === Type.Category) {
        pageTitle = categories?.find((category) => category.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <LayoutContainer
            header={
                <PageHeader
                    centerTitle={true}
                    position="main"
                    right={
                        <IntegrationDialog integration={undefined} triggerNode={<Button>Create Integration</Button>} />
                    }
                    title={`${searchParams.get('tagId') ? 'Tags' : 'Categories'}: ${pageTitle || 'All'}`}
                />
            }
            leftSidebarBody={
                <LeftSidebarNav
                    bottomBody={
                        <>
                            {!tagsIsLoading &&
                                (!tags?.length ? (
                                    <p className="px-3 text-xs">No tags.</p>
                                ) : (
                                    tags?.map((item) => (
                                        <LeftSidebarNavItem
                                            icon={<TagIcon className="mr-1 size-4" />}
                                            item={{
                                                filterData: filterData?.id === item.id && filterData.type === Type.Tag,
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
                                ))}
                        </>
                    }
                    bottomTitle="Tags"
                    topBody={
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

                            {!categoriesIsLoading &&
                                categories?.map((item) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            filterData: filterData?.id === item.id && filterData.type === Type.Category,
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
                    topTitle="Categories"
                />
            }
            leftSidebarHeader={<PageHeader position="sidebar" title="Integrations" />}
        >
            <PageLoader
                errors={[categoriesError, integrationsError, tagsError]}
                loading={categoriesIsLoading || integrationsIsLoading || tagsIsLoading}
            >
                {integrations && integrations?.length > 0 ? (
                    integrations && tags && <IntegrationList integrations={integrations} tags={tags} />
                ) : (
                    <EmptyList
                        button={
                            <IntegrationDialog
                                integration={undefined}
                                triggerNode={<Button>Create Integration</Button>}
                            />
                        }
                        icon={<SquareIcon className="size-12 text-gray-400" />}
                        message="Get started by creating a new integrations."
                        title="No integrations"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Integrations;
