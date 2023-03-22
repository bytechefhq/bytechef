import {
    useGetProjectsQuery,
    useGetProjectTagsQuery,
} from '../../../queries/projects.queries';
import ProjectListItem from 'pages/automation/projects/ProjectListItem';
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
                'w-full px-2 2xl:mx-auto 2xl:w-4/5',
                projects?.length === 0 ? 'place-self-center' : ''
            )}
        >
            <ul role="list">
                {isLoading && <span className="px-2">Loading...</span>}

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
                                <li key={project.id}>
                                    <div className="group rounded-md border-b border-b-gray-100 bg-white p-2 py-3 hover:bg-gray-50">
                                        <ProjectListItem
                                            project={project}
                                            key={project.id}
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
export default ProjectList;
