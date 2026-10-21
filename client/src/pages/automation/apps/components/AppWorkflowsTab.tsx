import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AppWorkflowBindingDialog from '@/pages/automation/apps/components/AppWorkflowBindingDialog';
import {App, useAppWorkflowsQuery, useUnbindAppWorkflowMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface AppWorkflowsTabProps {
    app: App;
    canManage: boolean;
}

const AppWorkflowsTab = ({app, canManage}: AppWorkflowsTabProps) => {
    const [workflowKeyToUnbind, setWorkflowKeyToUnbind] = useState<string | undefined>();

    const queryClient = useQueryClient();

    const {data, error, isLoading} = useAppWorkflowsQuery({appId: app.id});

    const unbindAppWorkflowMutation = useUnbindAppWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['appWorkflows']});

            setWorkflowKeyToUnbind(undefined);
        },
    });

    const appWorkflows = data?.appWorkflows ?? [];

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <p className="text-sm text-content-neutral-secondary">
                    The deployed workflows this app can run. Only workflows triggered by Call workflow can be bound.
                </p>

                {canManage && <AppWorkflowBindingDialog app={app} triggerNode={<Button label="Bind Workflow" />} />}
            </div>

            <PageLoader errors={[error as Error | null]} loading={isLoading}>
                {appWorkflows.length > 0 ? (
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>Workflow key</TableHead>

                                <TableHead>Workflow</TableHead>

                                <TableHead>Deployment</TableHead>

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

                                    <TableCell>{appWorkflow.projectDeploymentName}</TableCell>

                                    <TableCell className="text-right">
                                        {canManage && (
                                            <Button
                                                aria-label={`Unbind ${appWorkflow.workflowKey}`}
                                                label="Unbind"
                                                onClick={() => setWorkflowKeyToUnbind(appWorkflow.workflowKey)}
                                                size="sm"
                                                variant="destructiveGhost"
                                            />
                                        )}
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                ) : (
                    <p className="py-8 text-center text-sm text-content-neutral-secondary">
                        No workflows are bound to this app yet.
                    </p>
                )}
            </PageLoader>

            <AlertDialog
                confirmLabel="Unbind"
                description={`Unbind "${workflowKeyToUnbind}"? The app can no longer run this workflow.`}
                isPending={unbindAppWorkflowMutation.isPending}
                onCancel={() => setWorkflowKeyToUnbind(undefined)}
                onConfirm={() => {
                    if (workflowKeyToUnbind) {
                        unbindAppWorkflowMutation.mutate({appId: app.id, workflowKey: workflowKeyToUnbind});
                    }
                }}
                open={workflowKeyToUnbind !== undefined}
                title="Unbind workflow?"
            />
        </div>
    );
};

export default AppWorkflowsTab;
