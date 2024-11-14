import {Type} from '@/ee/pages/automation/api-platform/api-collections/ApiCollections';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {TagIcon} from 'lucide-react';
import {useLocation} from 'react-router-dom';

interface ApiPlatformLeftSidebarNavProps {
    environment?: number;
    filterData?: {id?: number; type: Type};
    projects: Project[] | undefined;
    tags: Tag[] | undefined;
}

const ApiPlatformLeftSidebarNav = ({environment, filterData, projects, tags}: ApiPlatformLeftSidebarNavProps) => {
    const location = useLocation();

    return (
        <>
            <LeftSidebarNav
                body={
                    <>
                        {[{label: 'All Environments'}, {label: 'Test', value: 1}, {label: 'Production', value: 2}]?.map(
                            (item) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current:
                                            environment === item.value && !location.pathname.includes('api-clients'),
                                        id: item.value,
                                        name: item.label,
                                    }}
                                    key={item.value ?? ''}
                                    toLink={`../api-collections?environment=${item.value ?? ''}${filterData?.id ? `&${filterData.type === Type.Project ? 'projectId' : 'tagId'}=${filterData.id}` : ''}`}
                                />
                            )
                        )}
                    </>
                }
                title="Environments"
            />

            <LeftSidebarNav
                body={
                    <>
                        <LeftSidebarNavItem
                            item={{
                                current: !filterData?.id && filterData?.type === Type.Project,
                                name: 'All Projects',
                            }}
                            toLink={`../api-collections?environment=${environment ?? ''}`}
                        />

                        {projects &&
                            projects?.map((item) => (
                                <LeftSidebarNavItem
                                    item={{
                                        current: filterData?.id === item.id && filterData?.type === Type.Project,
                                        id: item.id,
                                        name: item.name,
                                    }}
                                    key={item.name}
                                    toLink={`../api-collections?projectId=${item.id}&environment=${environment ?? ''}`}
                                />
                            ))}
                    </>
                }
                title="Projects"
            />

            <LeftSidebarNav
                body={
                    <>
                        {tags && !!tags.length ? (
                            tags?.map((item) => (
                                <LeftSidebarNavItem
                                    icon={<TagIcon className="mr-1 size-4" />}
                                    item={{
                                        current: filterData?.id === item.id && filterData?.type === Type.Tag,
                                        id: item.id!,
                                        name: item.name,
                                    }}
                                    key={item.id}
                                    toLink={`?tagId=${item.id}&environment=${environment ?? ''}`}
                                />
                            ))
                        ) : (
                            <span className="px-3 text-xs">No defined tags.</span>
                        )}
                    </>
                }
                title="Tags"
            />

            <LeftSidebarNav
                body={
                    <LeftSidebarNavItem
                        item={{
                            current: location.pathname.includes('api-clients'),
                            id: 'api-clients',
                            name: 'API Clients',
                        }}
                        toLink="../api-clients"
                    />
                }
            />
        </>
    );
};

export default ApiPlatformLeftSidebarNav;
