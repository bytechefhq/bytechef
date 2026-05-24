import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {AutomationWorkflowProjectTagsQuery, AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {useState} from 'react';

import AutomationWorkflowProjectListItem from './AutomationWorkflowProjectListItem';
import AutomationWorkflowProjectWorkflowList from './AutomationWorkflowProjectWorkflowList';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type EmbeddedTagType = AutomationWorkflowProjectTagsQuery['automationWorkflowProjectTags'][number];

interface AutomationWorkflowProjectListProps {
    onCreateWorkflow: (projectId: string) => void;
    onDeleteProject: (projectId: string) => void;
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditProject: (project: AutomationWorkflowProjectType) => void;
    onImportWorkflow: (projectId: string) => void;
    onPublishProject: (projectId: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    onUpdateTags: (project: AutomationWorkflowProjectType, tagNames: string[]) => void;
    projects: Array<AutomationWorkflowProjectType>;
    tags: EmbeddedTagType[];
}

const AutomationWorkflowProjectList = ({
    onCreateWorkflow,
    onDeleteProject,
    onDeleteWorkflow,
    onEditProject,
    onImportWorkflow,
    onPublishProject,
    onSelectWorkflow,
    onUpdateTags,
    projects,
    tags,
}: AutomationWorkflowProjectListProps) => {
    const [openProjectIds, setOpenProjectIds] = useState<Set<string>>(new Set());

    return (
        <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
            {projects.map((project) => (
                <Collapsible
                    className="group"
                    key={project.id}
                    onOpenChange={(open) => {
                        setOpenProjectIds((previousOpenProjectIds) => {
                            const nextOpenProjectIds = new Set(previousOpenProjectIds);

                            if (open) {
                                nextOpenProjectIds.add(project.id);
                            } else {
                                nextOpenProjectIds.delete(project.id);
                            }

                            return nextOpenProjectIds;
                        });
                    }}
                    open={openProjectIds.has(project.id)}
                >
                    <AutomationWorkflowProjectListItem
                        onCreateWorkflow={onCreateWorkflow}
                        onDeleteProject={onDeleteProject}
                        onEditProject={onEditProject}
                        onImportWorkflow={onImportWorkflow}
                        onPublishProject={onPublishProject}
                        onSelectWorkflow={onSelectWorkflow}
                        onUpdateTags={onUpdateTags}
                        project={project}
                        tags={tags}
                    />

                    <CollapsibleContent>
                        <AutomationWorkflowProjectWorkflowList
                            onCreateWorkflow={onCreateWorkflow}
                            onDeleteWorkflow={onDeleteWorkflow}
                            onSelectWorkflow={onSelectWorkflow}
                            project={project}
                        />
                    </CollapsibleContent>
                </Collapsible>
            ))}
        </div>
    );
};

export default AutomationWorkflowProjectList;
