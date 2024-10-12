import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import ProjectsLeftSidebarNav from '@/pages/automation/projects/components/ProjectsLeftSidebarNav';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {Category, Tag} from '@/shared/middleware/automation/configuration';
import {useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {FolderIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import ProjectDialog from './components/ProjectDialog';
import ProjectList from './components/project-list/ProjectList';

export enum Type {
    Category,
    Tag,
}

const FilterTitle = ({
    categories,
    filterData,
    tags,
}: {
    categories: Category[] | undefined;
    filterData: {id?: number; type: Type};
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

const Projects = () => {
    const [searchParams] = useSearchParams();

    const filterData = {
        id: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : searchParams.get('tagId')
              ? parseInt(searchParams.get('tagId')!)
              : undefined,
        type: searchParams.get('tagId') ? Type.Tag : Type.Category,
    };

    const {currentWorkspaceId} = useWorkspaceStore();

    const navigate = useNavigate();

    const {data: categories, error: categoriesError, isLoading: categoriesIsLoading} = useGetProjectCategoriesQuery();

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
                        right={
                            <ProjectDialog
                                onClose={(project) => {
                                    if (project) {
                                        navigate(
                                            `/automation/projects/${project?.id}/project-workflows/${project?.projectWorkflowIds![0]}`
                                        );
                                    }
                                }}
                                project={undefined}
                                triggerNode={<Button>New Project</Button>}
                            />
                        }
                        title={<FilterTitle categories={categories} filterData={filterData} tags={tags} />}
                    />
                )
            }
            leftSidebarBody={<ProjectsLeftSidebarNav categories={categories} filterData={filterData} tags={tags} />}
            leftSidebarHeader={<Header position="sidebar" title="Projects" />}
        >
            <PageLoader
                errors={[categoriesError, projectsError, tagsError]}
                loading={categoriesIsLoading || projectsIsLoading || tagsIsLoading}
            >
                {projects && projects?.length > 0 ? (
                    projects && tags && <ProjectList projects={projects} tags={tags} />
                ) : (
                    <EmptyList
                        button={
                            <ProjectDialog
                                onClose={(project) => {
                                    if (project) {
                                        navigate(
                                            `/automation/projects/${project?.id}/project-workflows/${project?.projectWorkflowIds![0]}`
                                        );
                                    }
                                }}
                                project={undefined}
                                triggerNode={<Button>Create Project</Button>}
                            />
                        }
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
