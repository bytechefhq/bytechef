import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {
    useProvisionConnectedUserProjectWorkflowsMutation,
    useProvisionableConnectedUserProjectsQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';
import {useForm} from 'react-hook-form';

interface ProvisionProjectWorkflowFormI {
    projectId: string;
    workflows: Array<{inputs: Record<string, string>; selected: boolean; workflowUuid: string}>;
}

interface ProvisionProjectWorkflowDialogProps {
    connectedUserId: number;
    onClose: () => void;
}

const ProvisionProjectWorkflowDialog = ({connectedUserId, onClose}: ProvisionProjectWorkflowDialogProps) => {
    const queryClient = useQueryClient();

    const {handleSubmit, setValue, watch} = useForm<ProvisionProjectWorkflowFormI>({
        defaultValues: {projectId: '', workflows: []},
    });

    const {data} = useProvisionableConnectedUserProjectsQuery({connectedUserId: connectedUserId.toString()});

    const provisionConnectedUserProjectWorkflowsMutation = useProvisionConnectedUserProjectWorkflowsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUserId)});
            void queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
            void queryClient.invalidateQueries({queryKey: ['provisionableConnectedUserProjects']});

            onClose();
        },
    });

    const projectId = watch('projectId');
    const formWorkflows = watch('workflows');

    const provisionableProjects = data?.provisionableConnectedUserProjects;

    const provisionableProject = useMemo(
        () => provisionableProjects?.find((current) => current.projectId === projectId),
        [provisionableProjects, projectId]
    );

    const canSave =
        !!provisionableProject &&
        formWorkflows.some((formWorkflow) => formWorkflow.selected) &&
        formWorkflows.every((formWorkflow) => {
            if (!formWorkflow.selected) {
                return true;
            }

            const workflow = provisionableProject.workflows.find(
                (current) => current.workflowUuid === formWorkflow.workflowUuid
            );

            return (workflow?.inputs ?? []).every(
                (input) => !input.required || !!formWorkflow.inputs?.[input.name]?.trim()
            );
        });

    const handleProjectChange = (value: string) => {
        const selectedProject = provisionableProjects?.find((current) => current.projectId === value);

        setValue('projectId', value);
        setValue(
            'workflows',
            (selectedProject?.workflows ?? []).map((workflow) => ({
                inputs: {},
                selected: false,
                workflowUuid: workflow.workflowUuid,
            }))
        );
    };

    const handleSave = ({workflows}: ProvisionProjectWorkflowFormI) => {
        provisionConnectedUserProjectWorkflowsMutation.mutate({
            connectedUserId: connectedUserId.toString(),
            workflows: workflows
                .filter((workflow) => workflow.selected)
                .map(({inputs, workflowUuid}) => {
                    const hasInputs = !!provisionableProject?.workflows.find(
                        (current) => current.workflowUuid === workflowUuid
                    )?.inputs.length;

                    return hasInputs ? {inputs, workflowUuid} : {workflowUuid};
                }),
        });
    };

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-xl" showCloseButton>
                <DialogHeader>
                    <DialogTitle>New Workflow</DialogTitle>

                    <DialogDescription>
                        Provision automation workflows from a released configuration for this connected user.
                    </DialogDescription>
                </DialogHeader>

                <div className="flex flex-col gap-4">
                    {provisionableProjects?.length === 0 ? (
                        <p className="text-sm text-content-neutral-secondary">
                            No project has a configuration released to this user&apos;s environment.
                        </p>
                    ) : (
                        <fieldset className="space-y-2 border-0 p-0">
                            <Label htmlFor="provision-project-id">Project</Label>

                            <select
                                className="flex h-9 w-full rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary px-3 py-1 text-sm"
                                id="provision-project-id"
                                onChange={(event) => handleProjectChange(event.target.value)}
                                value={projectId}
                            >
                                <option value="">Select...</option>

                                {provisionableProjects?.map((current) => (
                                    <option key={current.projectId} value={current.projectId}>
                                        {current.name}
                                    </option>
                                ))}
                            </select>
                        </fieldset>
                    )}

                    {provisionableProject?.workflows.map((workflow, workflowIndex) => {
                        const label = workflow.label || workflow.workflowUuid;
                        const locked = workflow.provisioned || workflow.dangling;
                        const selected = !!formWorkflows[workflowIndex]?.selected;

                        return (
                            <div
                                className="space-y-3 rounded border border-border/50 px-3 py-2"
                                key={workflow.workflowUuid}
                            >
                                <div className="flex items-center justify-between gap-2">
                                    <div className="flex items-center gap-2">
                                        <Checkbox
                                            aria-label={label}
                                            checked={locked || selected}
                                            disabled={locked}
                                            onCheckedChange={(checked) =>
                                                setValue(`workflows.${workflowIndex}.selected`, checked === true)
                                            }
                                        />

                                        <span className="text-sm font-medium">{label}</span>
                                    </div>

                                    {workflow.dangling ? (
                                        <Badge label="Needs attention" styleType="warning-filled" />
                                    ) : (
                                        workflow.provisioned && <Badge label="Provisioned" styleType="success-filled" />
                                    )}
                                </div>

                                {selected &&
                                    !locked &&
                                    workflow.inputs.map((input) => (
                                        <fieldset className="space-y-1 border-0 p-0" key={input.name}>
                                            <Label htmlFor={`provision-workflow-${workflowIndex}-${input.name}`}>
                                                {input.label || input.name}

                                                {input.required && ' *'}
                                            </Label>

                                            <Input
                                                id={`provision-workflow-${workflowIndex}-${input.name}`}
                                                onChange={(event) =>
                                                    setValue(`workflows.${workflowIndex}.inputs`, {
                                                        ...formWorkflows[workflowIndex]?.inputs,
                                                        [input.name]: event.target.value,
                                                    })
                                                }
                                                value={formWorkflows[workflowIndex]?.inputs?.[input.name] || ''}
                                            />
                                        </fieldset>
                                    ))}
                            </div>
                        );
                    })}
                </div>

                <DialogFooter>
                    {provisionableProjects?.length === 0 ? (
                        <Button label="Close" onClick={onClose} variant="outline" />
                    ) : (
                        <>
                            <Button label="Cancel" onClick={onClose} variant="outline" />

                            <Button
                                disabled={!canSave || provisionConnectedUserProjectWorkflowsMutation.isPending}
                                label="Save"
                                onClick={handleSubmit(handleSave)}
                            />
                        </>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ProvisionProjectWorkflowDialog;
