import {FolderPlusIcon} from '@heroicons/react/24/outline';
import ProjectListItem from 'pages/automation/projects/ProjectListItem';
import {useSearchParams} from 'react-router-dom';

import EmptyList from '../../../components/EmptyList/EmptyList';
import {
    useGetProjectTagsQuery,
    useGetProjectsQuery,
} from '../../../queries/projects.queries';
import ProjectDialog from './ProjectDialog';

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
                        projects.map((project) => {
                            const projectTagIds = project.tags?.map(
                                (tag) => tag.id
                            );

                            return (
                                <ProjectListItem
                                    key={project.id}
                                    project={project}
                                    remainingTags={tags?.filter(
                                        (tag) =>
                                            !projectTagIds?.includes(tag.id)
                                    )}
                                />
                            );
                        })
                    ))}
            </ul>
        </div>
    );
};
export default ProjectList;
