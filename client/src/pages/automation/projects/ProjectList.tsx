import {
    useGetProjectsQuery,
    useGetProjectTagsQuery,
} from '../../../queries/projects.queries';
import ProjectItem from 'pages/automation/projects/ProjectItem';
import {useSearchParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import EmptyList from '../../../components/EmptyList/EmptyList';
import ProjectDialog from './ProjectDialog';
import {FolderPlusIcon} from '@heroicons/react/24/outline';

const ProjectList = () => {
    const [searchParams] = useSearchParams();

    const {
        isLoading,
        error,
        data: projects,
    } = useGetProjectsQuery({
        categoryIds: searchParams.get('categoryId')
            ? [parseInt(searchParams.get('categoryId')!)]
            : undefined,
        tagIds: searchParams.get('tagId')
            ? [parseInt(searchParams.get('tagId')!)]
            : undefined,
    });

    const {data: tags} = useGetProjectTagsQuery();

    return (
        <div
            className={twMerge(
                'flex place-self-center px-2 sm:w-full 2xl:w-4/5',
                projects?.length === 0 ? 'h-full items-center' : ''
            )}
        >
            <ul role="list" className="w-full divide-y divide-gray-100">
                {isLoading && 'Loading...'}

                {error &&
                    !isLoading &&
                    `An error has occurred: ${error.message}`}

                {!isLoading &&
                    !error &&
                    (projects?.length === 0 ? (
                        <EmptyList
                            button={<ProjectDialog project={undefined} />}
                            icon={
                                <FolderPlusIcon className="h-12 w-12 text-gray-400" />
                            }
                            message="Get started by creating a new project."
                            title="No projects"
                        />
                    ) : (
                        projects.map((project) => {
                            const projectTagIds = project.tags?.map(
                                (tag) => tag.id
                            );

                            return (
                                <div key={project.id}>
                                    <li className="group my-3 rounded-md bg-white p-2 hover:bg-gray-50">
                                        <ProjectItem
                                            project={project}
                                            projectNames={projects.map(
                                                (project) => project.name
                                            )}
                                            key={project.id}
                                            remainingTags={tags?.filter(
                                                (tag) =>
                                                    !projectTagIds?.includes(
                                                        tag.id
                                                    )
                                            )}
                                        />
                                    </li>
                                </div>
                            );
                        })
                    ))}
            </ul>
        </div>
    );
};
export default ProjectList;
