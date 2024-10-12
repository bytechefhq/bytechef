import {Type} from '@/pages/embedded/integrations/Integrations';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Category, Tag} from '@/shared/middleware/automation/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {TagIcon} from 'lucide-react';

const IntegrationsLeftSidebarNav = ({
    categories,
    filterData,
    tags,
}: {
    categories: Category[] | undefined;
    filterData: {id?: number | string; type: Type};
    tags: Tag[] | undefined;
}) => {
    const ff_743 = useFeatureFlagsStore()('ff-743');

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem
                            item={{
                                current: !filterData?.id && filterData.type === Type.Category,
                                name: 'All Categories',
                            }}
                        />

                        {categories &&
                            categories?.map((item) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current: filterData?.id === item.id && filterData.type === Type.Category,
                                        id: item.id,
                                        name: item.name,
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
                        {tags &&
                            (tags?.length ? (
                                tags?.map((item) => (
                                    <LeftSidebarNavItem
                                        icon={<TagIcon className="mr-1 size-4" />}
                                        item={{
                                            current: filterData?.id === item.id && filterData.type === Type.Tag,
                                            id: item.id!,
                                            name: item.name,
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
                className={!ff_743 ? 'mb-0' : ''}
                title="Tags"
            />

            {ff_743 && (
                <LeftSidebarNav
                    body={
                        <>
                            <LeftSidebarNavItem
                                item={{
                                    current: filterData?.id === 'accounting' && filterData.type === Type.UnifiedAPI,
                                    id: 'accounting',
                                    name: 'Accounting',
                                }}
                                toLink="?unifiedApiCategory=accounting"
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: filterData?.id === 'commerce' && filterData.type === Type.UnifiedAPI,
                                    id: 'commerce',
                                    name: 'Commerce',
                                }}
                                toLink="?unifiedApiCategory=commerce"
                            />

                            <LeftSidebarNavItem
                                item={{
                                    current: filterData?.id === 'crm' && filterData.type === Type.UnifiedAPI,
                                    id: 'crm',
                                    name: 'CRM',
                                }}
                                toLink="?unifiedApiCategory=crm"
                            />
                        </>
                    }
                    title="Unified API"
                />
            )}
        </>
    );
};

export default IntegrationsLeftSidebarNav;
