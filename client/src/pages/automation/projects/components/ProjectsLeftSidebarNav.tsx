import {Type} from '@/pages/automation/projects/Projects';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Category, Tag} from '@/shared/middleware/automation/configuration';
import {TagIcon} from 'lucide-react';

const ProjectsLeftSidebarNav = ({
    categories,
    filterData,
    tags,
}: {
    categories: Category[] | undefined;
    filterData: {id?: number; type: Type};
    tags: Tag[] | undefined;
}) => {
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
                                <span className="px-3 text-xs">No defined tags.</span>
                            ))}
                    </>
                }
                className="mb-0"
                title="Tags"
            />
        </>
    );
};

export default ProjectsLeftSidebarNav;
