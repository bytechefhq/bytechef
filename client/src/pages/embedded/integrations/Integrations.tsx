import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import IntegrationDialog from '@/pages/embedded/integrations/components/IntegrationDialog';
import IntegrationList from '@/pages/embedded/integrations/components/IntegrationList';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Category, Tag} from '@/shared/middleware/embedded/configuration';
import {useGetIntegrationCategoriesQuery} from '@/shared/queries/embedded/integrationCategories.queries';
import {useGetIntegrationTagsQuery} from '@/shared/queries/embedded/integrationTags.quries';
import {useGetIntegrationsQuery} from '@/shared/queries/embedded/integrations.queries';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SquareIcon, TagIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

export enum Type {
    Category,
    Tag,
    UnifiedAPI,
}

const FilterTitle = ({
    categories,
    filterData,
    tags,
}: {
    categories: Category[] | undefined;
    filterData: {id?: number | string; type: Type};
    tags: Tag[] | undefined;
}) => {
    const [searchParams] = useSearchParams();

    let pageTitle: string | ReactNode | undefined;

    if (filterData.type === Type.Category) {
        pageTitle = categories?.find((category) => category.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

    return (
        <div className="space-x-1">
            <span className="text-sm uppercase text-muted-foreground">{`Filter by ${searchParams.get('tagId') ? 'tag' : 'category'}:`}</span>

            <span className="text-base">{pageTitle ?? 'All Categories'}</span>
        </div>
    );
};

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

    const [filterData, setFilterData] = useState<{id?: number | string; type: Type}>(defaultCurrentState);

    const navigate = useNavigate();

    const ff_743 = useFeatureFlagsStore()('ff-743');

    const {
        data: integrations,
        error: integrationsError,
        isLoading: integrationsLoading,
    } = useGetIntegrationsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetIntegrationCategoriesQuery();

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    return (
        <LayoutContainer
            leftSidebarBody={
                <>
                    <LeftSidebarNav
                        body={
                            <>
                                <LeftSidebarNavItem
                                    item={{
                                        current: !filterData?.id && filterData.type === Type.Category,
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
                                                current:
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
                                                    current: filterData?.id === item.id && filterData.type === Type.Tag,
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

                    {ff_743 && (
                        <LeftSidebarNav
                            body={
                                <>
                                    <LeftSidebarNavItem
                                        item={{
                                            current:
                                                filterData?.id === 'accounting' && filterData.type === Type.UnifiedAPI,
                                            id: 'accounting',
                                            name: 'Accounting',
                                            onItemClick: (id?: number | string) => {
                                                setFilterData({
                                                    id: id as number,
                                                    type: Type.UnifiedAPI,
                                                });
                                            },
                                        }}
                                        toLink="?unifiedApiCategory=accounting"
                                    />

                                    <LeftSidebarNavItem
                                        item={{
                                            current:
                                                filterData?.id === 'commerce' && filterData.type === Type.UnifiedAPI,
                                            id: 'commerce',
                                            name: 'Commerce',
                                            onItemClick: (id?: number | string) => {
                                                setFilterData({
                                                    id: id as number,
                                                    type: Type.UnifiedAPI,
                                                });
                                            },
                                        }}
                                        toLink="?unifiedApiCategory=commerce"
                                    />

                                    <LeftSidebarNavItem
                                        item={{
                                            current: filterData?.id === 'crm' && filterData.type === Type.UnifiedAPI,
                                            id: 'crm',
                                            name: 'CRM',
                                            onItemClick: (id?: number | string) => {
                                                setFilterData({
                                                    id: id as string,
                                                    type: Type.UnifiedAPI,
                                                });
                                            },
                                        }}
                                        toLink="?unifiedApiCategory=crm"
                                    />
                                </>
                            }
                            title="Unified API"
                        />
                    )}
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Integrations" />}
        >
            <PageLoader
                errors={[categoriesError, integrationsError, tagsError]}
                loading={categoriesLoading || integrationsLoading || tagsLoading}
            >
                {integrations && integrations?.length > 0 && tags ? (
                    <div className="flex size-full flex-col">
                        <Header
                            centerTitle={true}
                            position="main"
                            right={
                                integrations &&
                                integrations.length > 0 && (
                                    <IntegrationDialog
                                        integration={undefined}
                                        onClose={(integration) => {
                                            if (integration) {
                                                navigate(
                                                    `/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}`
                                                );
                                            }
                                        }}
                                        triggerNode={<Button>New Integration</Button>}
                                    />
                                )
                            }
                            title={<FilterTitle categories={categories} filterData={filterData} tags={tags} />}
                        />

                        <div className="flex-1 overflow-y-auto">
                            <IntegrationList integrations={integrations} tags={tags} />
                        </div>
                    </div>
                ) : (
                    <EmptyList
                        button={
                            <IntegrationDialog
                                integration={undefined}
                                onClose={(integration) => {
                                    if (integration) {
                                        navigate(
                                            `/embedded/integrations/${integration?.id}/integration-workflows/${integration?.integrationWorkflowIds![0]}`
                                        );
                                    }
                                }}
                                triggerNode={<Button>Create Integration</Button>}
                            />
                        }
                        icon={<SquareIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new integrations."
                        title="No Integrations"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Integrations;
