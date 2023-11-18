import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {ProjectModel, TagModel} from '@/middleware/helios/configuration';
import ProjectListItem from 'pages/automation/projects/ProjectListItem';

import ProjectWorkflowList from './ProjectWorkflowList';

const ProjectList = ({
    projects,
    tags,
}: {
    projects: ProjectModel[];
    tags: TagModel[];
}) => {
    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {projects.map((project) => {
                const projectTagIds = project.tags?.map((tag) => tag.id);

                return (
                    <Collapsible
                        className="data-[state=closed]:border-b data-[state=closed]:border-b-gray-100"
                        key={project.id}
                    >
                        <div className="w-full rounded-md px-2 py-5 hover:bg-gray-50">
                            <ProjectListItem
                                key={project.id}
                                project={project}
                                remainingTags={tags?.filter(
                                    (tag) => !projectTagIds?.includes(tag.id)
                                )}
                            />
                        </div>

                        <CollapsibleContent>
                            <ProjectWorkflowList project={project} />
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </div>
    );
};
export default ProjectList;
