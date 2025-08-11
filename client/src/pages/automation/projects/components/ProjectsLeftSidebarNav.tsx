import {Type} from '@/pages/automation/projects/Projects';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Category, Tag} from '@/shared/middleware/automation/configuration';
import {TagIcon} from 'lucide-react';

interface ProjectsLeftSidebarNavProps {
    categories?: Category[];
    filterData: {id?: number; type: Type};
    tags?: Tag[];
}

const ProjectsLeftSidebarNav = ({categories, filterData, tags}: ProjectsLeftSidebarNavProps) => (
    <LeftSidebarNav
        body={
            <>
                <div className="mb-4">
                    <h4 className="px-2 py-1 pr-4 text-sm font-medium tracking-tight text-muted-foreground">
                        Categories
                    </h4>

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
                </div>

                <div>
                    <h4 className="px-2 py-1 pr-4 text-sm font-medium tracking-tight text-muted-foreground">Tags</h4>

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
                </div>
            </>
        }
        className="mb-0"
    />
);

export default ProjectsLeftSidebarNav;
