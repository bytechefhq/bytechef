import {Form} from '@/components/ui/form';
import ProjectDeploymentDialogWorkflowsStepItem from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItem';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {Connection, ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {UseFormReturn} from 'react-hook-form';

interface ServerProjectWorkflowsListProps {
    connections?: Connection[];
    form: UseFormReturn<ProjectDeployment>;
    onSelectedWorkflowIdsChange: (selectedWorkflowIds: string[]) => void;
    selectedWorkflowIds: string[];
    workflows: Workflow[];
}

const ServerProjectWorkflowsList = ({
    connections,
    form,
    onSelectedWorkflowIdsChange,
    selectedWorkflowIds,
    workflows,
}: ServerProjectWorkflowsListProps) => (
    <WorkflowMockProvider>
        <Form {...form}>
            <div className="min-h-full space-y-5">
                {workflows.map((workflow, workflowIndex) => (
                    <ProjectDeploymentDialogWorkflowsStepItem
                        connections={connections}
                        control={form.control}
                        enabled={selectedWorkflowIds.includes(workflow.id!)}
                        formState={form.formState}
                        key={workflow.id}
                        label={workflow.label || workflow.id}
                        onEnabledChange={(enabled) =>
                            onSelectedWorkflowIdsChange(
                                enabled
                                    ? [...selectedWorkflowIds, workflow.id!]
                                    : selectedWorkflowIds.filter((workflowId) => workflowId !== workflow.id)
                            )
                        }
                        setValue={form.setValue}
                        showWorkflowToggle
                        workflow={workflow}
                        workflowIndex={workflowIndex}
                        workflows={[workflow]}
                    />
                ))}
            </div>
        </Form>
    </WorkflowMockProvider>
);

export default ServerProjectWorkflowsList;
