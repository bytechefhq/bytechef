import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AppWorkflowBindingDialog from '@/pages/automation/apps/components/AppWorkflowBindingDialog';
import {useAppWorkflowsQuery, useUnbindAppWorkflowMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon, Trash2Icon, WorkflowIcon} from 'lucide-react';
import {useState} from 'react';

interface AppWorkflowsTabProps {
    appInstanceId: string;
    canManage: boolean;
}

const AppWorkflowsTab = ({appInstanceId, canManage}: AppWorkflowsTabProps) => {
    const [workflowKeyToUnbind, setWorkflowKeyToUnbind] = useState<string | undefined>();

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useAppWorkflowsQuery({appInstanceId});

    const unbindAppWorkflowMutation = useUnbindAppWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appWorkflows']});

            setWorkflowKeyToUnbind(undefined);
        },
    });

    const appWorkflows = data?.appWorkflows ?? [];

    const empty = !isLoading && !error && appWorkflows.length === 0;

    const bindWorkflowButton = canManage && (
        <AppWorkflowBindingDialog appInstanceId={appInstanceId} triggerNode={<Button label="Bind Workflow" />} />
    );

    return (
        <div className="flex flex-1 flex-col space-y-4">
            {!empty && (
                <div className="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <p className="text-sm text-content-neutral-secondary">
                        The deployed workflows this app can run. Only workflows triggered by Call workflow can be bound.
                    </p>

                    {bindWorkflowButton}
                </div>
            )}

            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {appWorkflows.length > 0 ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Workflow key</TableHead>

                                <TableHead>Workflow</TableHead>

                                <TableHead className="max-sm:hidden">Deployment</TableHead>

                                <TableHead />
                            </TableRow>
                        </TableHeader>

                        <TableBody>
                            {appWorkflows.map((appWorkflow) => (
                                <TableRow key={appWorkflow.id}>
                                    <TableCell className="font-medium">{appWorkflow.workflowKey}</TableCell>

                                    <TableCell>
                                        {appWorkflow.available ? (
                                            appWorkflow.workflowName
                                        ) : (
                                            <span
                                                className="text-content-warning-primary"
                                                title="This workflow's deployment or workflow is gone or disabled, so runs through this binding fail."
                                            >
                                                Unavailable
                                            </span>
                                        )}
                                    </TableCell>

                                    <TableCell className="max-sm:hidden">{appWorkflow.projectDeploymentName}</TableCell>

                                    <TableCell className="text-right">
                                        {canManage && (
                                            <DropdownMenu>
                                                <DropdownMenuTrigger asChild>
                                                    <Button
                                                        aria-label={`${appWorkflow.workflowKey} actions`}
                                                        icon={<EllipsisVerticalIcon />}
                                                        size="icon"
                                                        variant="ghost"
                                                    />
                                                </DropdownMenuTrigger>

                                                <DropdownMenuContent align="end" className="p-0">
                                                    <DropdownMenuItem
                                                        className="dropdown-menu-item-destructive"
                                                        onClick={() => setWorkflowKeyToUnbind(appWorkflow.workflowKey)}
                                                        variant="destructive"
                                                    >
                                                        <Trash2Icon /> Unbind
                                                    </DropdownMenuItem>
                                                </DropdownMenuContent>
                                            </DropdownMenu>
                                        )}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                ) : (
                    <EmptyState
                        actions={bindWorkflowButton}
                        description="Bind a deployed workflow so the app can run it. Only workflows triggered by Call workflow can be bound."
                        icon={<WorkflowIcon />}
                        title="No workflows bound"
                    />
                )}
            </PageLoader>

            <AlertDialog
                confirmLabel="Unbind"
                description={`Unbind "${workflowKeyToUnbind}"? The app can no longer run this workflow.`}
                isPending={unbindAppWorkflowMutation.isPending}
                onCancel={() => setWorkflowKeyToUnbind(undefined)}
                onConfirm={() => {
                    if (workflowKeyToUnbind) {
                        unbindAppWorkflowMutation.mutate({appInstanceId, workflowKey: workflowKeyToUnbind});
                    }
                }}
                open={workflowKeyToUnbind !== undefined}
                title="Unbind workflow?"
            />
        </div>
    );
};

export default AppWorkflowsTab;
