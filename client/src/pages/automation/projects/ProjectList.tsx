import {
    ChevronDownIcon,
    ChevronRightIcon,
    FolderPlusIcon,
} from '@heroicons/react/24/outline';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from '@radix-ui/react-accordion';
import ProjectListItem from 'pages/automation/projects/ProjectListItem';
import {useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import EmptyList from '../../../components/EmptyList/EmptyList';
import {
    useGetProjectTagsQuery,
    useGetProjectsQuery,
} from '../../../queries/projects.queries';
import ProjectDialog from './ProjectDialog';
import ProjectWorkflowList from './ProjectWorkflowList';

const ProjectList = () => {
    const [searchParams] = useSearchParams();

    const {
        data: projects,
        error,
        isLoading,
    } = useGetProjectsQuery({
        categoryIds: searchParams.get('categoryId')
            ? [parseInt(searchParams.get('categoryId')!)]
            : undefined,
        tagIds: searchParams.get('tagId')
            ? [parseInt(searchParams.get('tagId')!)]
            : undefined,
    });

    const {data: tags} = useGetProjectTagsQuery();

    const [projectIdOpened, setProjectIdOpened] = useState<string | null>(null);

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            <ul role="list">
                {isLoading && <span className="px-2">Loading...</span>}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (!projects?.length ? (
                        <EmptyList
                            button={<ProjectDialog project={undefined} />}
                            icon={
                                <FolderPlusIcon className="h-12 w-12 text-gray-400" />
                            }
                            message="Get started by creating a new project."
                            title="No projects"
                        />
                    ) : (
                        <Accordion
                            type="single"
                            defaultValue={projectIdOpened || undefined}
                            collapsible
                        >
                            {projects.map((project) => {
                                const projectTagIds = project.tags?.map(
                                    (tag) => tag.id
                                );

                                return (
                                    <AccordionItem
                                        value={project.id!.toString()}
                                        key={project.id}
                                    >
                                        <AccordionTrigger
                                            className="w-full"
                                            onClick={() => {
                                                if (
                                                    projectIdOpened ===
                                                    project.id!.toString()
                                                ) {
                                                    setProjectIdOpened(null);
                                                } else {
                                                    setProjectIdOpened(
                                                        project.id!.toString()
                                                    );
                                                }
                                            }}
                                        >
                                            {projectIdOpened ===
                                            project.id!.toString() ? (
                                                <ChevronDownIcon className="mr-2 h-5 w-5 text-gray-500" />
                                            ) : (
                                                <ChevronRightIcon className="mr-2 h-5 w-5 text-gray-500" />
                                            )}

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
                                        </AccordionTrigger>

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
            </ul>
        </div>
    );
};
export default ProjectList;
