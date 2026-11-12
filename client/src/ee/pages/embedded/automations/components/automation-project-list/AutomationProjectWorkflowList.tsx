import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import EmptyList from '@/components/EmptyList';
import {ButtonGroup} from '@/components/ui/button-group';
import DeleteWorkflowAlertDialog from '@/shared/components/DeleteWorkflowAlertDialog';
import {AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {ChevronDownIcon, UploadIcon, WorkflowIcon} from 'lucide-react';
import {useState} from 'react';

import AutomationProjectWorkflowListItem from './AutomationProjectWorkflowListItem';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type AutomationProjectWorkflowTemplateType =
    AutomationProjectsQuery['automationProjects'][number]['workflowTemplates'][number];

interface AutomationProjectWorkflowListProps {
    onCreateWorkflow: (projectId: string) => void;
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditWorkflow: (workflow: AutomationProjectWorkflowTemplateType) => void;
    onImportWorkflow: (projectId: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    project: AutomationProjectType;
}

const AutomationProjectWorkflowList = ({
    onCreateWorkflow,
    onDeleteWorkflow,
    onEditWorkflow,
    onImportWorkflow,
    onSelectWorkflow,
    project,
}: AutomationProjectWorkflowListProps) => {
    const [workflowUuidToDelete, setWorkflowUuidToDelete] = useState<string | null>(null);

    const workflows = [...project.workflowTemplates].sort((firstWorkflow, secondWorkflow) =>
        (firstWorkflow.label || firstWorkflow.workflowUuid).localeCompare(
            secondWorkflow.label || secondWorkflow.workflowUuid
        )
    );

    return (
        <div>
            {workflows.length > 0 ? (
                <>
                    <ul className="divide-y divide-stroke-neutral-primary">
                        {workflows.map((workflow) => (
                            <AutomationProjectWorkflowListItem
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
                            <ButtonGroup aria-label="Workflow Creation Actions" className="mx-auto">
                                <Button label="Create Workflow" onClick={() => onCreateWorkflow(project.id)} />

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button
                                            aria-label="More Workflow Creation Actions"
                                            icon={<ChevronDownIcon />}
                                        />
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end">
                                        <DropdownMenuItem
                                            aria-label="Import Workflow"
                                            icon={<UploadIcon />}
                                            label="Import Workflow"
                                            onClick={() => onImportWorkflow(project.id)}
                                        />
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </ButtonGroup>
                        }
                        icon={<WorkflowIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new workflow."
                        title="No Workflows"
                    />
                </div>
            )}

            {workflowUuidToDelete !== null && (
                <DeleteWorkflowAlertDialog
                    onClose={() => setWorkflowUuidToDelete(null)}
                    onDelete={() => {
                        onDeleteWorkflow(workflowUuidToDelete);

                        setWorkflowUuidToDelete(null);
                    }}
                />
            )}
        </div>
    );
};

export default AutomationProjectWorkflowList;
