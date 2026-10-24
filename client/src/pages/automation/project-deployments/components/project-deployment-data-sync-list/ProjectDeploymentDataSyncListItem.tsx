import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {describeTrigger} from '@/pages/automation/data-syncs/utils/dataSyncTrigger';
import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {
    DataSyncDeploymentsQuery,
    DataSyncTriggerType,
    useRunDataSyncDeploymentMutation,
} from '@/shared/middleware/graphql';
import {useEnableProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ArrowLeftRightIcon, PlayIcon} from 'lucide-react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

export type ProjectDeploymentDataSyncType = DataSyncDeploymentsQuery['dataSyncDeployments'][number];

interface ProjectDeploymentDataSyncListItemProps {
    dataSyncDeployment: ProjectDeploymentDataSyncType;
    projectDeploymentWorkflow?: ProjectDeploymentWorkflow;
}

const ProjectDeploymentDataSyncListItem = ({
    dataSyncDeployment,
    projectDeploymentWorkflow,
}: ProjectDeploymentDataSyncListItemProps) => {
    const queryClient = useQueryClient();

    const enableProjectDeploymentWorkflowMutation = useEnableProjectDeploymentWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
            queryClient.invalidateQueries({queryKey: ['dataSyncDeployments']});
        },
    });

    const runDataSyncDeploymentMutation = useRunDataSyncDeploymentMutation({
        onSuccess: () => {
            toast('Sync run requested.');
        },
    });

    const {dataSyncTitle} = dataSyncDeployment;
    const enabled = projectDeploymentWorkflow?.enabled ?? false;

    const triggerLabel =
        dataSyncDeployment.triggerType === DataSyncTriggerType.Manual
            ? describeTrigger(dataSyncDeployment.triggerType, undefined)
            : 'Scheduled';

    const lastExecutionDate = dataSyncDeployment.lastExecutionDate
        ? new Date(dataSyncDeployment.lastExecutionDate)
        : undefined;

    const canRun = dataSyncDeployment.enabled && enabled;

    return (
        <li className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <div className="flex min-w-0 flex-1 items-center gap-2">
                <ArrowLeftRightIcon className="size-4 shrink-0 text-content-neutral-secondary" />

                <Tooltip>
                    <TooltipTrigger className="line-clamp-1 min-w-0 flex-1 truncate text-start">
                        <span
                            className={twMerge(
                                'block truncate text-sm font-semibold',
                                !enabled && 'text-content-neutral-secondary'
                            )}
                        >
                            {dataSyncTitle}
                        </span>
                    </TooltipTrigger>

                    <TooltipContent align="start" className="max-w-md break-all">
                        {dataSyncTitle}
                    </TooltipContent>
                </Tooltip>

                <span className="shrink-0 text-xs font-normal text-content-neutral-secondary">{triggerLabel}</span>
            </div>

            <div className="flex shrink-0 items-center gap-x-6">
                {lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-content-neutral-secondary">
                            <span className="text-xs">
                                {`Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent side="left">Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    <span className="text-xs text-content-neutral-secondary">No executions</span>
                )}

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            aria-label="Run now"
                            disabled={!canRun || runDataSyncDeploymentMutation.isPending}
                            icon={<PlayIcon className="text-success" />}
                            onClick={() =>
                                runDataSyncDeploymentMutation.mutate({
                                    id: dataSyncDeployment.dataSyncId,
                                    projectDeploymentId: dataSyncDeployment.id,
                                })
                            }
                            size="icon"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Run the sync now</TooltipContent>
                </Tooltip>

                {projectDeploymentWorkflow && (
                    <div className="relative flex items-center">
                        {enableProjectDeploymentWorkflowMutation.isPending && (
                            <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                        )}

                        <Switch
                            aria-label={`Enable data sync ${dataSyncTitle}`}
                            checked={enabled}
                            className="mr-2"
                            disabled={enableProjectDeploymentWorkflowMutation.isPending}
                            onCheckedChange={(value) =>
                                enableProjectDeploymentWorkflowMutation.mutate({
                                    enable: value,
                                    id: +dataSyncDeployment.id,
                                    workflowId: projectDeploymentWorkflow.workflowId!,
                                })
                            }
                        />
                    </div>
                )}
            </div>
        </li>
    );
};

export default ProjectDeploymentDataSyncListItem;
