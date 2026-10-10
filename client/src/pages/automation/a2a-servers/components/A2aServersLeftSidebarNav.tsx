import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {Tag} from '@/shared/middleware/graphql';
import {TagIcon} from 'lucide-react';

import {A2aServersFilterType} from '../hooks/useA2aServers';

interface A2aServersLeftSidebarNavProps {
    a2aProjectsIsLoading: boolean;
    filterData: {id?: string; type: A2aServersFilterType};
    tags?: Tag[];
    tagsIsLoading: boolean;
    uniqueProjects: {id: string; name: string}[];
}

const A2aServersLeftSidebarNav = ({
    a2aProjectsIsLoading,
    filterData,
    tags,
    tagsIsLoading,
    uniqueProjects,
}: A2aServersLeftSidebarNavProps) => (
    <>
        <LeftSidebarFilterNav
            emptyMessage="No projects."
            items={uniqueProjects.map((project) => ({
                current: filterData.id === project.id && filterData.type === A2aServersFilterType.Project,
                id: project.id,
                name: project.name,
                toLink: `?projectId=${project.id}`,
            }))}
            leadItem={{
                current: !filterData.id,
                name: 'All Projects',
                toLink: '',
            }}
            loading={a2aProjectsIsLoading}
            title="Projects"
        />

        <LeftSidebarFilterNav
            emptyMessage="No defined tags."
            icon={<TagIcon className="mr-1 size-4" />}
            items={(tags ?? []).map((tag) => ({
                current: filterData.id === tag.id && filterData.type === A2aServersFilterType.Tag,
                id: tag.id,
                name: tag.name,
                toLink: `?tagId=${tag.id}`,
            }))}
            loading={tagsIsLoading}
            title="Tags"
        />
    </>
);

export default A2aServersLeftSidebarNav;
