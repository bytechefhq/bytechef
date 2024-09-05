import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import ProjectListItem from 'pages/automation/projects/components/ProjectListItem';

import ProjectWorkflowList from './ProjectWorkflowList';

const ProjectList = ({projects, tags}: {projects: Project[]; tags: Tag[]}) => {
    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {projects.map((project) => {
                const projectTagIds = project.tags?.map((tag) => tag.id);

                return (
                    <Collapsible className="group" key={project.id}>
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
