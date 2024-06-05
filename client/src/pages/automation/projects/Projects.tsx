import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useGetProjectCategoriesQuery} from '@/shared/queries/automation/projectCategories.queries';
import {useGetProjectTagsQuery} from '@/shared/queries/automation/projectTags.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {FolderIcon, TagIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

import ProjectDialog from './components/ProjectDialog';
import ProjectList from './components/ProjectList';

export enum Type {
    Category,
    Tag,
}

const Projects = () => {
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

    let pageTitle: string | undefined;

    if (filterData.type === Type.Category) {
        pageTitle = categories?.find((category) => category.id === filterData.id)?.name;
    } else {
        pageTitle = tags?.find((tag) => tag.id === filterData.id)?.name;
    }

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
                        title={
                            !pageTitle
                                ? 'All Projects'
                                : `Filter by ${searchParams.get('tagId') ? 'tag' : 'category'}: ${pageTitle}`
                        }
                    />
                )
            }
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

                                {!categoriesIsLoading &&
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
                                {!tagsIsLoading &&
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
                                        <span className="px-3 text-xs">No defined tags.</span>
                                    ))}
                            </>
                        }
                        title="Tags"
                    />
                </>
            }
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
                        icon={<FolderIcon className="size-12 text-gray-400" />}
                        message="Get started by creating a new project."
                        title="No Projects"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default Projects;
