import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import EmptyList from '@/components/EmptyList';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';
import {useState} from 'react';

import AutomationWorkflowProjectWorkflowListItem from './AutomationWorkflowProjectWorkflowListItem';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectWorkflowTemplateType =
    AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number];

interface AutomationWorkflowProjectWorkflowListProps {
    onCreateWorkflow: (projectId: string) => void;
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditWorkflow: (workflow: AutomationWorkflowProjectWorkflowTemplateType) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    project: AutomationWorkflowProjectType;
}

const AutomationWorkflowProjectWorkflowList = ({
    onCreateWorkflow,
    onDeleteWorkflow,
    onEditWorkflow,
    onSelectWorkflow,
    project,
}: AutomationWorkflowProjectWorkflowListProps) => {
    const [workflowUuidToDelete, setWorkflowUuidToDelete] = useState<string | null>(null);

    const workflows = [...project.workflowTemplates].sort((firstWorkflow, secondWorkflow) =>
        (firstWorkflow.label || firstWorkflow.workflowUuid).localeCompare(
            secondWorkflow.label || secondWorkflow.workflowUuid
        )
    );

    return (
        <div className="pt-3">
            {workflows.length > 0 ? (
                <>
                    <h3 className="flex justify-start pl-3 text-sm heading-tertiary">Workflows</h3>

                    <ul className="divide-y divide-stroke-neutral-primary">
                        {workflows.map((workflow) => (
                            <AutomationWorkflowProjectWorkflowListItem
                                key={workflow.workflowUuid}
                                onDeleteWorkflow={setWorkflowUuidToDelete}
                                onEditWorkflow={onEditWorkflow}
                                onSelectWorkflow={onSelectWorkflow}
                                workflow={workflow}
                            />
                        ))}
                    </ul>
                </>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={
                            <Button
                                label="Create Workflow"
                                onClick={(event) => {
                                    event.stopPropagation();

                                    onCreateWorkflow(project.id);
                                }}
                            />
                        }
                        icon={<WorkflowIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new workflow."
                        title="No Workflows"
                    />
                </div>
            )}

            <DeleteAlertDialog
                onCancel={() => setWorkflowUuidToDelete(null)}
                onDelete={() => {
                    if (workflowUuidToDelete) {
                        onDeleteWorkflow(workflowUuidToDelete);
                    }

                    setWorkflowUuidToDelete(null);
                }}
                open={workflowUuidToDelete !== null}
            />
        </div>
    );
};

export default AutomationWorkflowProjectWorkflowList;
