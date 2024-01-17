import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {ProjectModel, TagModel} from '@/middleware/automation/configuration';
import ProjectListItem from 'pages/automation/projects/components/ProjectListItem';

import ProjectWorkflowList from './ProjectWorkflowList';

const ProjectList = ({projects, tags}: {projects: ProjectModel[]; tags: TagModel[]}) => {
    return (
        <div className="w-full divide-y divide-gray-100 px-2 2xl:mx-auto 2xl:w-4/5">
            {projects.map((project) => {
                const projectTagIds = project.tags?.map((tag) => tag.id);

                return (
                    <Collapsible key={project.id}>
                        <ProjectListItem
                            key={project.id}
                            project={project}
                            remainingTags={tags?.filter((tag) => !projectTagIds?.includes(tag.id))}
                        />

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
