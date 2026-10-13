import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {ComponentConnection, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {FileInputIcon, Link2Icon, WorkflowIcon} from 'lucide-react';
import {useEffect} from 'react';
import {Control, FieldValues, FormState, UseFormSetValue, useWatch} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

export interface IntegrationInstanceConfigurationDialogWorkflowListItemProps {
    componentName: string;
    control: Control<IntegrationInstanceConfiguration>;
    enabled?: boolean;
    formState: FormState<IntegrationInstanceConfiguration>;
    label: string;
    onEnabledChange?: (enabled: boolean) => void;
    setValue: UseFormSetValue<IntegrationInstanceConfiguration>;
    showWorkflowToggle?: boolean;
    workflow: Workflow;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItem = ({
    componentName,
    control,
    enabled,
    formState,
    label,
    onEnabledChange,
    setValue,
    showWorkflowToggle = false,
    workflow,
    workflowIndex,
}: IntegrationInstanceConfigurationDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    const componentConnections: ComponentConnection[] = (workflow?.tasks ?? [])
        .flatMap((task) => task.connections ?? [])
        .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
        .filter((connection) => connection.componentName !== componentName);

    const inputs = (workflow.inputs ?? []).filter(
        (input) => input.componentReference?.componentName !== componentName && input.internalOnly
    );

    const watchedConnections = useWatch({
        control,
        name: `integrationInstanceConfigurationWorkflows.${workflowIndex}.connections`,
    });

    const configuredConnectionIds = componentConnections.map(
        (componentConnection, connectionIndex) => watchedConnections?.[connectionIndex]?.connectionId
    );

    const workflowEnabled = enabled ?? workflowEnabledMap.get(workflow.id!) ?? false;

    const handleEnabledChange = (value: boolean) => {
        if (onEnabledChange) {
            onEnabledChange(value);

            return;
        }

        setValue(`integrationInstanceConfigurationWorkflows.${workflowIndex!}.enabled`, value);

        setWorkflowEnabled(workflow.id!, value);
    };

    useEffect(() => {
        setValue(`integrationInstanceConfigurationWorkflows.${workflowIndex!}.workflowId`, workflow.id!);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div
            className={twMerge(
                showWorkflowToggle &&
                    'flex flex-col gap-3 rounded-lg border border-stroke-neutral-secondary bg-surface-main p-2'
            )}
        >
            {showWorkflowToggle && (
                <div className="flex items-center rounded-lg">
                    <Label
                        className={twMerge(
                            'flex w-full cursor-pointer items-center gap-2 text-base font-normal',
                            workflowEnabled && 'font-semibold'
                        )}
                        htmlFor={`integrationInstanceConfigurationWorkflows.${workflowIndex}`}
                    >
                        <div
                            className={twMerge(
                                'rounded-lg bg-surface-neutral-tertiary p-2',
                                workflowEnabled && 'bg-surface-brand-secondary'
                            )}
                        >
                            <WorkflowIcon
                                className={twMerge(
                                    'size-4 text-content-neutral-secondary transition-colors',
                                    workflowEnabled && 'text-content-brand-primary'
                                )}
                            />
                        </div>

                        {label}
                    </Label>

                    <Switch
                        aria-label={label}
                        checked={workflowEnabled}
                        id={`integrationInstanceConfigurationWorkflows.${workflowIndex}`}
                        onCheckedChange={handleEnabledChange}
                    />
                </div>
            )}

            {(workflowEnabled || !showWorkflowToggle) && (
                <Tabs className="flex flex-col gap-2.5" defaultValue="connections">
                    <TabsList className="flex w-full">
                        <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="connections">
                            <Link2Icon className="mr-2 size-4" />

                            <span>Connections</span>

                            <span className="ml-1">({componentConnections.length})</span>
                        </TabsTrigger>

                        <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="inputs">
                            <FileInputIcon className="mr-2 size-4" />

                            <span>Inputs</span>

                            <span className="ml-1">({inputs.length})</span>
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent className="mt-0 space-y-4" tabIndex={-1} value="connections">
                        <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections
                            componentConnections={componentConnections}
                            control={control}
                            workflowIndex={workflowIndex}
                        />
                    </TabsContent>

                    <TabsContent className="mt-0" tabIndex={-1} value="inputs">
                        <InputConfigurationList
                            componentConnections={componentConnections}
                            configuredConnectionIds={configuredConnectionIds}
                            control={control as unknown as Control<FieldValues>}
                            controlPath={`integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs`}
                            formState={formState as unknown as FormState<FieldValues>}
                            inputs={inputs}
                            workflowId={workflow.id}
                        />
                    </TabsContent>
                </Tabs>
            )}
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItem;
