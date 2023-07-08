import {
    useGetProjectInstanceTagsQuery,
    useGetProjectInstancesQuery,
} from '@/queries/projects.queries';
import {FolderPlusIcon} from '@heroicons/react/24/outline';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
} from '@radix-ui/react-accordion';
import {ProjectModel} from 'middleware/automation/configuration';
import {useSearchParams} from 'react-router-dom';

import EmptyList from '../../../components/EmptyList/EmptyList';
import ProjectInstanceDialog from './ProjectInstanceDialog';
import ProjectInstanceListItem from './ProjectInstanceListItem';
import ProjectInstanceWorkflowList from './ProjectInstanceWorkflowList';

const ProjectInstanceList = ({project}: {project: ProjectModel}) => {
    const [searchParams] = useSearchParams();

    const {
        data: projectInstances,
        error,
        isLoading,
    } = useGetProjectInstancesQuery({
        projectId: project.id !== undefined ? project.id! : undefined,
        tagId: searchParams.get('tagId')
            ? parseInt(searchParams.get('tagId')!)
            : undefined,
    });

    const {data: tags} = useGetProjectInstanceTagsQuery();

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {isLoading && <span className="px-2">Loading...</span>}

            {error && !isLoading && `An error has occurred: ${error.message}`}

            {!isLoading &&
                !error &&
                (projectInstances?.length === 0 ? (
                    <EmptyList
                        button={<ProjectInstanceDialog />}
                        icon={
                            <FolderPlusIcon className="h-12 w-12 text-gray-400" />
                        }
                        message="Get started by creating a new project instance."
                        title="No instances of projects"
                    />
                ) : (
                    <Accordion type="multiple" className="mb-8">
                        <h3 className="mb-1 px-2 text-xl font-semibold text-gray-900">
                            {project.name}
                        </h3>

                        {projectInstances.map((projectInstance) => {
                            const projectTagIds = projectInstance.tags?.map(
                                (tag) => tag.id
                            );

                            if (!project.id) {
                                return;
                            }

                            return (
                                <AccordionItem
                                    value={projectInstance.id!.toString()}
                                    key={projectInstance.id}
                                >
                                    <div className="w-full rounded-md px-2 py-3 hover:bg-gray-50 data-[state=closed]:border-b data-[state=closed]:border-b-gray-100">
                                        <ProjectInstanceListItem
                                            projectInstance={projectInstance}
                                            key={projectInstance.id}
                                            remainingTags={tags?.filter(
                                                (tag) =>
                                                    !projectTagIds?.includes(
                                                        tag.id
                                                    )
                                            )}
                                            project={project}
                                        />
                                    </div>

                                    <AccordionContent>
                                        <ProjectInstanceWorkflowList
                                            projectId={project.id}
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
export default ProjectInstanceList;
