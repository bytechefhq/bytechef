import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {useGetWorkspaceProjectGitConfigurationsQuery} from '@/ee/shared/mutations/automation/projectGit.queries';
import handleImportProject from '@/pages/automation/project/utils/handleImportProject';
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
import {useQueryClient} from '@tanstack/react-query';
import {FolderIcon, LayoutTemplateIcon, PlusIcon, UploadIcon} from 'lucide-react';
import {useRef} from 'react';
import {useNavigate, useSearchParams} from 'react-router-dom';

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
    const fileInputRef = useRef<HTMLInputElement>(null);
    const navigate = useNavigate();

    const ff_1039 = useFeatureFlagsStore()('ff-1039');
    const ff_1041 = useFeatureFlagsStore()('ff-1041');
    const ff_2482 = useFeatureFlagsStore()('ff-2482');

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

    const queryClient = useQueryClient();

    return (
        <LayoutContainer
            header={
                projects &&
                projects.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={
                            ff_2482 ? (
                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button label="New Project" />
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
                                        <ProjectDialog
                                            project={undefined}
                                            triggerNode={
                                                <DropdownMenuItem onSelect={(event) => event.preventDefault()}>
                                                    <PlusIcon className="mr-2 size-4" />
                                                    From Scratch
                                                </DropdownMenuItem>
                                            }
                                        />

                                        {ff_1041 && (
                                            <DropdownMenuItem onClick={() => navigate(`templates`)}>
                                                <LayoutTemplateIcon className="mr-2 size-4" />
                                                From Template
                                            </DropdownMenuItem>
                                        )}

                                        <DropdownMenuItem onClick={() => fileInputRef.current?.click()}>
                                            <UploadIcon className="mr-2 size-4" />
                                            Import Project
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            ) : (
                                <ProjectDialog project={undefined} triggerNode={<Button label="New Project" />} />
                            )
                        }
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
                        button={
                            ff_2482 ? (
                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button label="Create Project" />
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
                                        <ProjectDialog
                                            project={undefined}
                                            triggerNode={
                                                <DropdownMenuItem onSelect={(event) => event.preventDefault()}>
                                                    <PlusIcon className="mr-2 size-4" /> From Scratch
                                                </DropdownMenuItem>
                                            }
                                        />

                                        {ff_1041 && (
                                            <DropdownMenuItem onClick={() => navigate(`templates`)}>
                                                <LayoutTemplateIcon className="mr-2 size-4" />
                                                From Template
                                            </DropdownMenuItem>
                                        )}

                                        <DropdownMenuItem onClick={() => fileInputRef.current?.click()}>
                                            <UploadIcon className="mr-2 size-4" /> Import Project
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            ) : (
                                <ProjectDialog triggerNode={<Button label="Create Project" />} />
                            )
                        }
                        icon={<FolderIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new project."
                        title="No Projects"
                    />
                )}
            </PageLoader>

            <input
                accept=".zip"
                onChange={(event) => handleImportProject(event, currentWorkspaceId, queryClient)}
                ref={fileInputRef}
                style={{display: 'none'}}
                type="file"
            />
        </LayoutContainer>
    );
};

export default Projects;
