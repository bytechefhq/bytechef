import '@/shared/styles/dropdownMenu.css';
import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Popover} from '@/components/ui/popover';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import {
    useA2aProjectWorkflowsByA2aProjectIdQuery,
    useDeleteA2aProjectMutation,
    useUpdateA2aProjectMutation,
    useUpdateA2aProjectWorkflowEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';

import A2aProjectWorkflowEditDialog from './A2aProjectWorkflowEditDialog';
import A2aProjectWorkflowSkillPopover from './A2aProjectWorkflowSkillPopover';
import {A2aProjectItemType} from './hooks/useA2aProjectList';

interface A2aProjectWorkflowListProps {
    a2aProject: A2aProjectItemType;
}

const A2aProjectWorkflowList = ({a2aProject}: A2aProjectWorkflowListProps) => {
    const [a2aProjectWorkflowIdToConfigure, setA2aProjectWorkflowIdToConfigure] = useState<string>();
    const [workflowIdToEdit, setWorkflowIdToEdit] = useState<string>();
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

                    const isSkillPopoverOpen = a2aProjectWorkflowIdToConfigure === a2aProjectWorkflow.id;

                    return (
                        <Popover
                            key={a2aProjectWorkflow.id}
                            onOpenChange={(open) => !open && setA2aProjectWorkflowIdToConfigure(undefined)}
                            open={isSkillPopoverOpen}
                        >
                            <div className="flex items-center gap-2 py-0.5">
                                <div className="flex min-w-0 flex-1 flex-col">
                                    <span className="truncate text-sm font-medium">{workflowLabel}</span>

                                    {a2aProjectWorkflow.skillName && a2aProjectWorkflow.skillName !== workflowLabel && (
                                        <span className="truncate text-xs text-muted-foreground">
                                            {`Skill: ${a2aProjectWorkflow.skillName}`}
                                        </span>
                                    )}
                                </div>

                                <div className="flex shrink-0 items-center gap-2">
                                    <McpToolConfigureButton
                                        anchorsPopover
                                        label={workflowLabel}
                                        onClick={() => setA2aProjectWorkflowIdToConfigure(a2aProjectWorkflow.id)}
                                    />

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

                                    <DropdownMenu>
                                        <DropdownMenuTrigger asChild>
                                            <Button
                                                aria-label={`${workflowLabel} actions`}
                                                icon={<EllipsisVerticalIcon />}
                                                size="iconSm"
                                                variant="ghost"
                                            />
                                        </DropdownMenuTrigger>

                                        <DropdownMenuContent align="end">
                                            <DropdownMenuItem
                                                className="dropdown-menu-item"
                                                onClick={() =>
                                                    setWorkflowIdToEdit(a2aProjectWorkflow.workflowId ?? undefined)
                                                }
                                            >
                                                <EditIcon /> Edit
                                            </DropdownMenuItem>

                                            <DropdownMenuSeparator />

                                            <DropdownMenuItem
                                                className="dropdown-menu-item-destructive"
                                                onClick={() =>
                                                    setWorkflowIdToRemove(a2aProjectWorkflow.workflowId ?? undefined)
                                                }
                                                variant="destructive"
                                            >
                                                <Trash2Icon /> Remove
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </div>
                            </div>

                            {isSkillPopoverOpen && (
                                <A2aProjectWorkflowSkillPopover
                                    a2aProjectWorkflowId={a2aProjectWorkflow.id}
                                    onClose={() => setA2aProjectWorkflowIdToConfigure(undefined)}
                                    skillDescription={a2aProjectWorkflow.skillDescription}
                                    skillName={a2aProjectWorkflow.skillName}
                                    workflowLabel={workflowLabel}
                                />
                            )}
                        </Popover>
                    );
                })}
            </div>

            {workflowIdToEdit && (
                <A2aProjectWorkflowEditDialog
                    a2aProject={a2aProject}
                    onClose={() => setWorkflowIdToEdit(undefined)}
                    workflowId={workflowIdToEdit}
                />
            )}

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
