import {
    useGetProjectTagsQuery,
    useGetProjectsQuery,
} from '@/queries/projects.queries';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
} from '@radix-ui/react-accordion';
import {FolderIcon} from 'lucide-react';
import ProjectListItem from 'pages/automation/projects/ProjectListItem';
import {useSearchParams} from 'react-router-dom';

import EmptyList from '../../../components/EmptyList/EmptyList';
import ProjectDialog from './ProjectDialog';
import ProjectWorkflowList from './ProjectWorkflowList';

const ProjectList = () => {
    const [searchParams] = useSearchParams();

    const {
        data: projects,
        error,
        isLoading,
    } = useGetProjectsQuery({
        categoryId: searchParams.get('categoryId')
            ? parseInt(searchParams.get('categoryId')!)
            : undefined,
        tagId: searchParams.get('tagId')
            ? parseInt(searchParams.get('tagId')!)
            : undefined,
    });

    const {data: tags} = useGetProjectTagsQuery();

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {isLoading && <span className="px-2">Loading...</span>}

            {error && !isLoading && `An error has occurred: ${error.message}`}

            {!isLoading &&
                !error &&
                (!projects?.length ? (
                    <EmptyList
                        button={<ProjectDialog project={undefined} />}
                        icon={
                            <FolderIcon className="h-12 w-12 text-gray-400" />
                        }
                        message="Get started by creating a new project."
                        title="No projects"
                    />
                ) : (
                    <Accordion type="multiple" className="mb-8">
                        {projects.map((project) => {
                            const projectTagIds = project.tags?.map(
                                (tag) => tag.id
                            );

                            return (
                                <AccordionItem
                                    value={project.id!.toString()}
                                    key={project.id}
                                    className="data-[state=closed]:border-b data-[state=closed]:border-b-gray-100"
                                >
                                    <div className="w-full rounded-md px-2 py-5 hover:bg-gray-50">
                                        <ProjectListItem
                                            key={project.id}
                                            project={project}
                                            remainingTags={tags?.filter(
                                                (tag) =>
                                                    !projectTagIds?.includes(
                                                        tag.id
                                                    )
                                            )}
                                        />
                                    </div>

                                    <AccordionContent>
                                        <ProjectWorkflowList
                                            project={project}
                                        />
                                    </AccordionContent>
                                </AccordionItem>
                            );
                        })}
                    </Accordion>
                ))}
        </div>
    );
};
export default ProjectList;
