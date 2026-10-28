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
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {ChevronDownIcon, PlusIcon, UploadIcon, WorkflowIcon} from 'lucide-react';
import {useState} from 'react';

import AutomationWorkflowProjectWorkflowListItem from './AutomationWorkflowProjectWorkflowListItem';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectWorkflowTemplateType =
    AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number]['workflowTemplates'][number];

interface AutomationWorkflowProjectWorkflowListProps {
    onCreateWorkflow: (projectId: string) => void;
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditWorkflow: (workflow: AutomationWorkflowProjectWorkflowTemplateType) => void;
    onImportWorkflow: (projectId: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    project: AutomationWorkflowProjectType;
}

const AutomationWorkflowProjectWorkflowList = ({
    onCreateWorkflow,
    onDeleteWorkflow,
    onEditWorkflow,
    onImportWorkflow,
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
                    <div className="flex items-center justify-between pl-3">
                        <h3 className="text-sm heading-tertiary">Workflows</h3>

                        <ButtonGroup aria-label="Workflow Creation Actions">
                            <Button
                                aria-label="Create Workflow"
                                onClick={(event) => {
                                    event.stopPropagation();

                                    onCreateWorkflow(project.id);
                                }}
                                size="sm"
                                variant="secondary"
                            >
                                <PlusIcon />
                                New Workflow
                            </Button>

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        aria-label="More Workflow Creation Actions"
                                        icon={<ChevronDownIcon />}
                                        size="sm"
                                        variant="secondary"
                                    >
                                        <> </>
                                    </Button>
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem
                                        aria-label="Import Workflow"
                                        icon={<UploadIcon />}
                                        label="Import Workflow"
                                        onClick={(event) => {
                                            event.stopPropagation();

                                            onImportWorkflow(project.id);
                                        }}
                                    />
                                </DropdownMenuContent>
                            </DropdownMenu>
                        </ButtonGroup>
                    </div>

                    <ul className="divide-y divide-stroke-neutral-primary">
                        {workflows.map((workflow) => (
                            <AutomationWorkflowProjectWorkflowListItem
                                key={workflow.workflowUuid}
                                onDeleteWorkflow={setWorkflowUuidToDelete}
                                onEditWorkflow={onEditWorkflow}
                                onSelectWorkflow={onSelectWorkflow}
                                projectName={project.name}
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

export default AutomationWorkflowProjectWorkflowList;
