import {Form} from '@/components/ui/form';
import IntegrationInstanceConfigurationDialogWorkflowsStepItem from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem';
import {IntegrationInstanceConfiguration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {UseFormReturn} from 'react-hook-form';

interface McpIntegrationWorkflowsListProps {
    componentName: string;
    enabledWorkflowIds: string[];
    form: UseFormReturn<IntegrationInstanceConfiguration>;
    onSelectedWorkflowIdsChange: (selectedWorkflowIds: string[]) => void;
    selectedWorkflowIds: string[];
    workflows: Workflow[];
}

const McpIntegrationWorkflowsList = ({
    componentName,
    enabledWorkflowIds,
    form,
    onSelectedWorkflowIdsChange,
    selectedWorkflowIds,
    workflows,
}: McpIntegrationWorkflowsListProps) => (
    <WorkflowMockProvider>
        <Form {...form}>
            <div className="min-h-full space-y-5">
                {workflows.map((workflow, workflowIndex) => (
                    <IntegrationInstanceConfigurationDialogWorkflowsStepItem
                        componentName={componentName}
                        control={form.control}
                        disabled={
                            !enabledWorkflowIds.includes(workflow.id!) && !selectedWorkflowIds.includes(workflow.id!)
                        }
                        disabledMessage="Turned off in the configuration"
                        enabled={selectedWorkflowIds.includes(workflow.id!)}
                        formState={form.formState}
                        key={workflow.id}
                        label={workflow.label || workflow.id!}
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
                    />
                ))}
            </div>
        </Form>
    </WorkflowMockProvider>
);

export default McpIntegrationWorkflowsList;
