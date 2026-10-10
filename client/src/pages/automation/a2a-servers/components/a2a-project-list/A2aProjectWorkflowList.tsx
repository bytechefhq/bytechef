import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {
    useA2aProjectWorkflowsByA2aProjectIdQuery,
    useDeleteA2aProjectMutation,
    useUpdateA2aProjectMutation,
    useUpdateA2aProjectWorkflowEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PencilIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

import {A2aProjectItemType} from './hooks/useA2aProjectList';

interface A2aProjectWorkflowListProps {
    a2aProject: A2aProjectItemType;
    onEditClick: () => void;
}

const A2aProjectWorkflowList = ({a2aProject, onEditClick}: A2aProjectWorkflowListProps) => {
    const [workflowIdToRemove, setWorkflowIdToRemove] = useState<string>();

    const queryClient = useQueryClient();

    const {data, isLoading} = useA2aProjectWorkflowsByA2aProjectIdQuery({a2aProjectId: a2aProject.id});

    const handleRemoveSuccess = () => {
        void queryClient.invalidateQueries({queryKey: ['a2aProjectsByServerId']});
        void queryClient.invalidateQueries({queryKey: ['a2aProjectWorkflowsByA2aProjectId']});

        setWorkflowIdToRemove(undefined);
    };

    const deleteA2aProjectMutation = useDeleteA2aProjectMutation({onSuccess: handleRemoveSuccess});
    const updateA2aProjectMutation = useUpdateA2aProjectMutation({onSuccess: handleRemoveSuccess});

    const updateA2aProjectWorkflowEnabledMutation = useUpdateA2aProjectWorkflowEnabledMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['a2aProjectWorkflowsByA2aProjectId']});
        },
    });

    const a2aProjectWorkflows =
        data?.a2aProjectWorkflowsByA2aProjectId?.filter(
            (a2aProjectWorkflow): a2aProjectWorkflow is NonNullable<typeof a2aProjectWorkflow> =>
                a2aProjectWorkflow !== null
        ) || [];

    const handleRemoveConfirm = () => {
        const remainingWorkflowIds = a2aProject.workflowIds.filter((workflowId) => workflowId !== workflowIdToRemove);

        if (remainingWorkflowIds.length === 0) {
            deleteA2aProjectMutation.mutate({id: a2aProject.id});
        } else {
            updateA2aProjectMutation.mutate({
                id: a2aProject.id,
                input: {selectedWorkflowIds: remainingWorkflowIds},
            });
        }
    };

    if (isLoading) {
        return <p className="text-xs text-muted-foreground">Loading workflows...</p>;
    }

    return (
        <>
            <div className="flex flex-col gap-1">
                {a2aProjectWorkflows.map((a2aProjectWorkflow) => {
                    const workflowLabel =
                        a2aProjectWorkflow.workflowLabel || a2aProjectWorkflow.workflowId || 'Unnamed Workflow';

                    return (
                        <div className="flex items-center gap-2 py-0.5" key={a2aProjectWorkflow.id}>
                            <div className="flex min-w-0 flex-1 flex-col">
                                <span className="truncate text-sm font-medium">{workflowLabel}</span>

                                {a2aProjectWorkflow.skillName && a2aProjectWorkflow.skillName !== workflowLabel && (
                                    <span className="truncate text-xs text-muted-foreground">
                                        {`Skill: ${a2aProjectWorkflow.skillName}`}
                                    </span>
                                )}
                            </div>

                            <div className="flex shrink-0 items-center gap-2">
                                <Switch
                                    aria-label={`Enable ${workflowLabel}`}
                                    checked={a2aProjectWorkflow.enabled}
                                    className="mr-2"
                                    disabled={updateA2aProjectWorkflowEnabledMutation.isPending}
                                    onCheckedChange={(enabled) =>
                                        updateA2aProjectWorkflowEnabledMutation.mutate({
                                            enabled,
                                            id: a2aProjectWorkflow.id,
                                        })
                                    }
                                />

                                <Button
                                    aria-label={`Edit ${workflowLabel}`}
                                    className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
                                    icon={<PencilIcon className="size-4" />}
                                    onClick={onEditClick}
                                    size="iconSm"
                                    title="Edit"
                                    variant="ghost"
                                />

                                <Button
                                    aria-label={`Remove ${workflowLabel}`}
                                    className="rounded p-1"
                                    icon={<Trash2Icon className="size-4" />}
                                    onClick={() => setWorkflowIdToRemove(a2aProjectWorkflow.workflowId ?? undefined)}
                                    size="iconSm"
                                    title="Remove"
                                    variant="destructiveGhost"
                                />
                            </div>
                        </div>
                    );
                })}
            </div>

            <AlertDialog
                isPending={deleteA2aProjectMutation.isPending || updateA2aProjectMutation.isPending}
                onCancel={() => setWorkflowIdToRemove(undefined)}
                onConfirm={handleRemoveConfirm}
                open={workflowIdToRemove !== undefined}
            />
        </>
    );
};

export default A2aProjectWorkflowList;
