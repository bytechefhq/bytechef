import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {useGetWorkspaceProjectGitConfigurationsQuery} from '@/ee/queries/projectGit.queries';
import ProjectsFilterTitle from '@/pages/automation/projects/components/ProjectsFilterTitle';
import ProjectsLeftSidebarNav from '@/pages/automation/projects/components/ProjectsLeftSidebarNav';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {FolderIcon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

import ProjectDialog from './components/ProjectDialog';
import ProjectList from './components/project-list/ProjectList';

export enum Type {
    Category,
    Tag,
}

const Projects = () => {
    const application = useApplicationInfoStore((state) => state.application);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [searchParams] = useSearchParams();

    const ff_1039 = useFeatureFlagsStore()('ff-1039');

    const categoryId = searchParams.get('categoryId');
    const tagId = searchParams.get('tagId');

    const filterData = {
        id: categoryId ? parseInt(categoryId) : tagId ? parseInt(tagId) : undefined,
        type: tagId ? Type.Tag : Type.Category,
    };

    const {data: categories, error: categoriesError, isLoading: categoriesIsLoading} = useGetProjectCategoriesQuery();

    const {
        data: projectGitConfigurations,
        error: projectGitConfigurationsError,
        isLoading: projectGitConfigurationsIsLoading,
    } = useGetWorkspaceProjectGitConfigurationsQuery(currentWorkspaceId!, ff_1039 && application?.edition === 'EE');

    const {
        data: projects,
        error: projectsError,
        isLoading: projectsIsLoading,
    } = useGetWorkspaceProjectsQuery({
        categoryId: searchParams.get('categoryId') ? parseInt(searchParams.get('categoryId')!) : undefined,
        id: currentWorkspaceId!,
        tagId: searchParams.get('tagId') ? parseInt(searchParams.get('tagId')!) : undefined,
    });

    const {data: tags, error: tagsError, isLoading: tagsIsLoading} = useGetProjectTagsQuery();

    return (
        <LayoutContainer
            header={
                projects &&
                projects.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={<ProjectDialog project={undefined} triggerNode={<Button>New Project</Button>} />}
                        title={<ProjectsFilterTitle categories={categories} filterData={filterData} tags={tags} />}
                    />
                )
            }
            leftSidebarBody={<ProjectsLeftSidebarNav categories={categories} filterData={filterData} tags={tags} />}
            leftSidebarHeader={<Header position="sidebar" title="Projects" />}
            leftSidebarWidth="64"
        >
            <PageLoader
                errors={[categoriesError, projectGitConfigurationsError, projectsError, tagsError]}
                loading={categoriesIsLoading || projectGitConfigurationsIsLoading || projectsIsLoading || tagsIsLoading}
            >
                {projects && projects?.length > 0 && tags ? (
                    <ProjectList
                        projectGitConfigurations={projectGitConfigurations ?? []}
                        projects={projects}
                        tags={tags}
                    />
                ) : (
                    <EmptyList
                        button={<ProjectDialog project={undefined} triggerNode={<Button>Create Project</Button>} />}
                        icon={<FolderIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new project."
                        title="No Projects"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Projects;
