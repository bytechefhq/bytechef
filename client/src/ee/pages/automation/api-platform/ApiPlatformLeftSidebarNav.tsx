import {Type} from '@/ee/pages/automation/api-platform/api-collections/ApiCollections';
import {ProjectBasic} from '@/ee/shared/middleware/automation/api-platform';
import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {Tag} from '@/shared/middleware/automation/configuration';
import {TagIcon} from 'lucide-react';

interface ApiPlatformLeftSidebarNavProps {
    environment?: number;
    filterData?: {id?: number; type: Type};
    projects: ProjectBasic[] | undefined;
    projectsIsLoading?: boolean;
    tags: Tag[] | undefined;
    tagsIsLoading?: boolean;
}

const ApiPlatformLeftSidebarNav = ({
    environment,
    filterData,
    projects,
    projectsIsLoading = false,
    tags,
    tagsIsLoading = false,
}: ApiPlatformLeftSidebarNavProps) => {
    return (
        <>
            <LeftSidebarFilterNav
                items={(projects ?? []).map((project) => ({
                    current: filterData?.id === project.id && filterData?.type === Type.Project,
                    id: project.id!,
                    name: project.name,
                    toLink: `../api-collections?projectId=${project.id}&environment=${environment ?? ''}`,
                }))}
                leadItem={{
                    current: !filterData?.id && filterData?.type === Type.Project,
                    name: 'All Projects',
                    toLink: `../api-collections?environment=${environment ?? ''}`,
                }}
                loading={projectsIsLoading}
                title="Projects"
            />

            <LeftSidebarFilterNav
                emptyMessage="No defined tags."
                icon={<TagIcon className="mr-1 size-4" />}
                items={(tags ?? []).map((tag) => ({
                    current: filterData?.id === tag.id && filterData?.type === Type.Tag,
                    id: tag.id!,
                    name: tag.name,
                    toLink: `../api-collections?tagId=${tag.id}&environment=${environment ?? ''}`,
                }))}
                loading={tagsIsLoading}
                title="Tags"
            />
        </>
    );
};

export default ApiPlatformLeftSidebarNav;
