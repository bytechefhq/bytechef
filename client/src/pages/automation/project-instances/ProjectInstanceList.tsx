import {FolderPlusIcon} from '@heroicons/react/24/outline';
import {useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import EmptyList from '../../../components/EmptyList/EmptyList';
import {
    useGetProjectInstanceTagsQuery,
    useGetProjectInstancesQuery,
} from '../../../queries/projects.queries';
import ProjectInstanceDialog from './ProjectInstanceDialog';
import ProjectInstanceListItem from './ProjectInstanceListItem';

const ProjectInstanceList = () => {
    const [searchParams] = useSearchParams();

    const {
        isLoading,
        error,
        data: projectsInstances,
    } = useGetProjectInstancesQuery({
        projectIds: searchParams.get('projectId')
            ? [parseInt(searchParams.get('projectId')!)]
            : undefined,
        tagIds: searchParams.get('tagId')
            ? [parseInt(searchParams.get('tagId')!)]
            : undefined,
    });

    const {data: tags} = useGetProjectInstanceTagsQuery();

    return (
        <div
            className={twMerge(
                'w-full px-2 2xl:mx-auto 2xl:w-4/5',
                projectsInstances?.length === 0 ? 'place-self-center' : ''
            )}
        >
            <ul role="list">
                {isLoading && <span className="px-2">Loading...</span>}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (projectsInstances?.length === 0 ? (
                        <EmptyList
                            button={
                                <ProjectInstanceDialog
                                    projectInstance={undefined}
                                />
                            }
                            icon={
                                <FolderPlusIcon className="h-12 w-12 text-gray-400" />
                            }
                            message="Get started by creating a new project instance."
                            title="No instances of projects"
                        />
                    ) : (
                        projectsInstances.map((projectInstance) => {
                            const projectTagIds = projectInstance.tags?.map(
                                (tag) => tag.id
                            );

                            return (
                                <li key={projectInstance.id}>
                                    <div className="group rounded-md border-b border-b-gray-100 bg-white p-2 py-3 hover:bg-gray-50">
                                        <ProjectInstanceListItem
                                            projectInstance={projectInstance}
                                            key={projectInstance.id}
                                            remainingTags={tags?.filter(
                                                (tag) =>
                                                    !projectTagIds?.includes(
                                                        tag.id
                                                    )
                                            )}
                                        />
                                    </div>
                                </li>
                            );
                        })
                    ))}
            </ul>
        </div>
    );
};
export default ProjectInstanceList;
