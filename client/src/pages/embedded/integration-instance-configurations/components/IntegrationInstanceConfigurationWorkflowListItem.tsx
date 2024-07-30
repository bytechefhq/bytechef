import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useToast} from '@/components/ui/use-toast';
import IntegrationInstanceConfigurationEditWorkflowDialog from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationEditWorkflowDialog';
import IntegrationInstanceConfigurationWorkflowListItemDropDownMenuProps from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationWorkflowListItemDropDownMenu';
import useIntegrationInstanceConfigurationWorkflowSheetStore from '@/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationWorkflowSheetStore';
import {IntegrationInstanceConfigurationWorkflowModel, WorkflowModel} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/shared/middleware/platform/configuration';
import {useEnableIntegrationInstanceConfigurationWorkflowMutation} from '@/shared/mutations/embedded/integrationInstanceConfigurations.mutations';
import {IntegrationInstanceConfigurationKeys} from '@/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useCopyToClipboard} from '@uidotdev/usehooks';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

const IntegrationInstanceConfigurationWorkflowListItem = ({
    filteredComponentNames,
    integrationInstanceConfigurationEnabled,
    integrationInstanceConfigurationId,
    integrationInstanceConfigurationWorkflow,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    filteredComponentNames?: string[];
    integrationInstanceConfigurationEnabled: boolean;
    integrationInstanceConfigurationId: number;
    integrationInstanceConfigurationWorkflow: IntegrationInstanceConfigurationWorkflowModel;
    workflow: WorkflowModel;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
}) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const {setIntegrationInstanceConfigurationWorkflowSheetOpen, setWorkflowId} =
        useIntegrationInstanceConfigurationWorkflowSheetStore();

    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();
    const {toast} = useToast();

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

    const handleWorkflowLabelClick = () => {
        setWorkflowId(workflow.id!);
        setIntegrationInstanceConfigurationWorkflowSheetOpen(true);
    };

    return (
        <li className="flex items-center justify-between rounded-md px-2 py-1 hover:bg-gray-50" key={workflow.id}>
            <div
                className={twMerge(
                    'w-80 text-sm font-semibold',
                    !integrationInstanceConfigurationWorkflow.enabled && 'text-muted-foreground'
                )}
            >
                <button onClick={handleWorkflowLabelClick}>{workflow.label}</button>
            </div>

            <div className="ml-6 flex">
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

            <div className="flex items-center justify-end gap-x-6">
                {integrationInstanceConfigurationWorkflow?.lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <span>
                                {`Executed at ${integrationInstanceConfigurationWorkflow.lastExecutionDate?.toLocaleDateString()} ${integrationInstanceConfigurationWorkflow.lastExecutionDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    '-'
                )}

                {integrationInstanceConfigurationWorkflow && (
                    <div className="flex items-center gap-x-4">
                        {/*        {workflow.manualTrigger && (*/}

                        {/*            <Button*/}

                        {/*                disabled={!integrationInstanceConfigurationEnabled || !integrationInstanceConfigurationWorkflow.enabled}*/}

                        {/*                onClick={() => handleWorkflowRun()}*/}

                        {/*                size="icon"*/}

                        {/*                variant="ghost"*/}

                        {/*            >*/}

                        {/*                <Tooltip>*/}

                        {/*                    <TooltipTrigger asChild>*/}

                        {/*                        <PlayIcon className="h-5 text-success" />*/}

                        {/*                    </TooltipTrigger>*/}

                        {/*                    <TooltipContent>Run workflow manually</TooltipContent>*/}

                        {/*                </Tooltip>*/}

                        {/*            </Button>*/}

                        {/*        )}*/}

                        {/*        {integrationInstanceConfigurationWorkflow.staticWebhookUrl && (*/}

                        {/*            <Button*/}

                        {/*                disabled={!integrationInstanceConfigurationWorkflow.enabled}*/}

                        {/*                onClick={() => copyToClipboard(integrationInstanceConfigurationWorkflow.staticWebhookUrl!)}*/}

                        {/*                size="icon"*/}

                        {/*                variant="ghost"*/}

                        {/*            >*/}

                        {/*                <Tooltip>*/}

                        {/*                    <TooltipTrigger asChild>*/}

                        {/*                        <ClipboardIcon className="h-5" />*/}

                        {/*                    </TooltipTrigger>*/}

                        {/*                    <TooltipContent>Copy static workflow webhook trigger url</TooltipContent>*/}

                        {/*                </Tooltip>*/}

                        {/*            </Button>*/}

                        {/*        )}*/}

                        {/*        {!workflow.manualTrigger && !integrationInstanceConfigurationWorkflow.staticWebhookUrl && (*/}

                        {/*            <Switch*/}

                        {/*                checked={integrationInstanceConfigurationWorkflow.enabled}*/}

                        {/*                disabled={integrationInstanceConfigurationEnabled}*/}

                        {/*                onCheckedChange={(value) => {*/}

                        {/*                    enableIntegrationInstanceConfigurationWorkflowMutation.mutate(*/}

                        {/*                        {*/}

                        {/*                            enable: value,*/}

                        {/*                            id: integartionInstanceConfigurationId,*/}

                        {/*                            workflowId: workflow.id!,*/}

                        {/*                        },*/}

                        {/*                        {*/}

                        {/*                            onSuccess: () => {*/}

                        {/*                                integrationInstanceConfigurationWorkflow.enabled = !integrationInstanceConfigurationWorkflow?.enabled;*/}

                        {/*                            },*/}

                        {/*                        }*/}

                        {/*                    );*/}

                        {/*                }}*/}

                        {/*            />*/}

                        {/*        )}*/}

                        {/*{workflow.triggers?.[0]?.name !== 'manual' && !projectInstanceWorkflow.staticWebhookUrl && (*/}

                        {/*    <div className="w-9"></div>*/}

                        {/*)}*/}

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
