import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import EmptyList from '@/components/EmptyList';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {WorkflowIcon} from 'lucide-react';
import {useState} from 'react';

import AutomationWorkflowProjectWorkflowListItem from './AutomationWorkflowProjectWorkflowListItem';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

interface AutomationWorkflowProjectWorkflowListProps {
    onCreateWorkflow: (projectId: string) => void;
    onDeleteWorkflow: (workflowUuid: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    project: AutomationWorkflowProjectType;
}

const AutomationWorkflowProjectWorkflowList = ({
    onCreateWorkflow,
    onDeleteWorkflow,
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
        <div className="border-b border-b-gray-100 py-3 pl-4">
            {workflows.length > 0 ? (
                <>
                    <h3 className="flex justify-start pl-2 text-sm heading-tertiary">Workflows</h3>

                    <ul className="divide-y divide-gray-100">
                        {workflows.map((workflow) => (
                            <AutomationWorkflowProjectWorkflowListItem
                                key={workflow.workflowUuid}
                                onDeleteWorkflow={setWorkflowUuidToDelete}
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
                        icon={<WorkflowIcon className="size-24 text-gray-300" />}
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
