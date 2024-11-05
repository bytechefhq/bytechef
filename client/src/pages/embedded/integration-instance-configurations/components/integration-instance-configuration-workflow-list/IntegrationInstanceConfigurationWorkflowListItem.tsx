import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurationEditWorkflowDialog from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog';
import IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps from '@/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowListItemDropDownMenu';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {IntegrationInstanceConfigurationWorkflow, Workflow} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useEnableIntegrationInstanceConfigurationWorkflowMutation} from '@/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationKeys} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const IntegrationInstanceConfigurationWorkflowListItem = ({
    componentName,
    filteredComponentNames,
    integrationInstanceConfigurationEnabled,
    integrationInstanceConfigurationId,
    integrationInstanceConfigurationWorkflow,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    componentName: string;
    filteredComponentNames?: string[];
    integrationInstanceConfigurationEnabled: boolean;
    integrationInstanceConfigurationId: number;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflow;
    workflow: Workflow;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    };
}) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const {setIntegrationInstanceConfigurationWorkflowSheetOpen, setWorkflowId} =
        useIntegrationInstanceConfigurationWorkflowSheetStore();

    const queryClient = useQueryClient();

    const enableIntegrationInstanceConfigurationWorkflowMutation =
        useEnableIntegrationInstanceConfigurationWorkflowMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: IntegrationInstanceConfigurationKeys.integrationInstanceConfigurations,
                });
            },
        });

    const handleIntegrationInstanceConfigurationEnable = () => {
        enableIntegrationInstanceConfigurationWorkflowMutation.mutate(
            {
                enable: !integrationInstanceConfigurationWorkflow.enabled,
                id: integrationInstanceConfigurationId,
                workflowId: workflow.id!,
            },
            {
                onSuccess: () => {
                    integrationInstanceConfigurationWorkflow = {
                        ...integrationInstanceConfigurationWorkflow,
                        enabled: !integrationInstanceConfigurationWorkflow?.enabled,
                    };
                },
            }
        );
    };

    const handleWorkflowClick = () => {
        setWorkflowId(workflow.id!);
        setIntegrationInstanceConfigurationWorkflowSheetOpen(true);
    };

    return (
        <li className="flex items-center justify-between rounded-md px-2 py-1 hover:bg-gray-50" key={workflow.id}>
            <div className="flex flex-1 cursor-pointer items-center" onClick={handleWorkflowClick}>
                <div
                    className={twMerge(
                        'w-80 text-sm font-semibold',
                        !integrationInstanceConfigurationWorkflow.enabled && 'text-muted-foreground'
                    )}
                >
                    {workflow.label}
                </div>

                <div className="ml-6 flex space-x-1">
                    {filteredComponentNames?.map((name) => {
                        const componentDefinition = workflowComponentDefinitions[name];
                        const taskDispatcherDefinition = workflowTaskDispatcherDefinitions[name];

                        return (
                            <div className="mr-0.5 flex items-center justify-center rounded-full border p-1" key={name}>
                                <Tooltip>
                                    <TooltipTrigger>
                                        <InlineSVG
                                            className="size-5 flex-none"
                                            key={name}
                                            src={
                                                componentDefinition?.icon
                                                    ? componentDefinition?.icon
                                                    : (taskDispatcherDefinition?.icon ?? '')
                                            }
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent side="top">{componentDefinition?.title}</TooltipContent>
                                </Tooltip>
                            </div>
                        );
                    })}
                </div>
            </div>

            <div className="flex items-center justify-end gap-x-6">
                {integrationInstanceConfigurationWorkflow?.lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <span className="text-xs">
                                {`Executed at ${integrationInstanceConfigurationWorkflow.lastExecutionDate?.toLocaleDateString()} ${integrationInstanceConfigurationWorkflow.lastExecutionDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    <span className="text-xs">No executions</span>
                )}

                {integrationInstanceConfigurationWorkflow && (
                    <div className="flex items-center gap-x-4">
                        <Switch
                            checked={integrationInstanceConfigurationWorkflow.enabled}
                            className="mr-2"
                            disabled={integrationInstanceConfigurationEnabled}
                            onCheckedChange={(value) => {
                                enableIntegrationInstanceConfigurationWorkflowMutation.mutate(
                                    {
                                        enable: value,
                                        id: integrationInstanceConfigurationId,
                                        workflowId: workflow.id!,
                                    },
                                    {
                                        onSuccess: () => {
                                            integrationInstanceConfigurationWorkflow.enabled =
                                                !integrationInstanceConfigurationWorkflow?.enabled;
                                        },
                                    }
                                );
                            }}
                        />

                        <IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps
                            integrationInstanceConfigurationEnabled={integrationInstanceConfigurationEnabled}
                            integrationInstanceConfigurationWorkflowEnabled={
                                integrationInstanceConfigurationWorkflow.enabled!
                            }
                            onEditClick={() => setShowEditWorkflowDialog(true)}
                            onEnableClick={() => handleIntegrationInstanceConfigurationEnable()}
                            workflow={workflow}
                        />
                    </div>
                )}
            </div>

            {showEditWorkflowDialog && integrationInstanceConfigurationWorkflow && (
                <IntegrationInstanceConfigurationEditWorkflowDialog
                    componentName={componentName}
                    integrationInstanceConfigurationEnabled={integrationInstanceConfigurationEnabled}
                    integrationInstanceConfigurationWorkflow={integrationInstanceConfigurationWorkflow}
                    onClose={() => setShowEditWorkflowDialog(false)}
                    workflow={workflow}
                />
            )}
        </li>
    );
};

export default IntegrationInstanceConfigurationWorkflowListItem;
