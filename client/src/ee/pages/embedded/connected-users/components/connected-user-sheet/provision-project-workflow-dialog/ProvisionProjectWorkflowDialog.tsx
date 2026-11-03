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

interface ProvisionProjectWorkflowStateI {
    inputs: Record<string, string>;
    selected: boolean;
}

interface ProvisionProjectWorkflowFormI {
    configurationId: string;
    projectId: string;
    workflows: Record<string, ProvisionProjectWorkflowStateI>;
}

interface AssignedConfigurationI {
    configurationId: string;
    configurationName: string;
    projectId: string;
}

interface ProvisionProjectWorkflowDialogProps {
    assignedConfigurations?: AssignedConfigurationI[];
    connectedUserId: number;
    onClose: () => void;
}

const ASSIGNED_CONFIGURATION_DISABLED_LABEL = 'Assigned configuration (disabled)';

const ProvisionProjectWorkflowDialog = ({
    assignedConfigurations,
    connectedUserId,
    onClose,
}: ProvisionProjectWorkflowDialogProps) => {
    const queryClient = useQueryClient();

    const {handleSubmit, setValue, watch} = useForm<ProvisionProjectWorkflowFormI>({
        defaultValues: {configurationId: '', projectId: '', workflows: {}},
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

    const configurationId = watch('configurationId');
    const projectId = watch('projectId');
    const formWorkflows = watch('workflows');

    const provisionableProjects = data?.provisionableConnectedUserProjects;

    const provisionableProject = useMemo(
        () => provisionableProjects?.find((project) => project.projectId === projectId),
        [provisionableProjects, projectId]
    );

    const assignedConfigurationId = provisionableProject?.assignedConfigurationId;

    const selectedConfiguration = useMemo(
        () =>
            provisionableProject?.configurations.find(
                (configuration) => configuration.id === (assignedConfigurationId || configurationId)
            ),
        [assignedConfigurationId, configurationId, provisionableProject]
    );

    const assignedConfigurationLabel = useMemo(() => {
        if (!assignedConfigurationId) {
            return undefined;
        }

        if (selectedConfiguration) {
            return `${selectedConfiguration.name} · v${selectedConfiguration.projectVersion}`;
        }

        return (
            assignedConfigurations?.find(
                (assignedConfiguration) =>
                    assignedConfiguration.projectId === projectId &&
                    assignedConfiguration.configurationId === assignedConfigurationId
            )?.configurationName || ASSIGNED_CONFIGURATION_DISABLED_LABEL
        );
    }, [assignedConfigurationId, assignedConfigurations, projectId, selectedConfiguration]);

    const selectableWorkflows = useMemo(
        () =>
            (selectedConfiguration?.workflows || []).filter(
                (workflow) =>
                    !workflow.provisioned && !workflow.dangling && formWorkflows[workflow.workflowUuid]?.selected
            ),
        [formWorkflows, selectedConfiguration]
    );

    const canSave =
        !!selectedConfiguration &&
        selectableWorkflows.length > 0 &&
        selectableWorkflows.every((workflow) =>
            workflow.inputs.every(
                (input) => !input.required || !!formWorkflows[workflow.workflowUuid]?.inputs[input.name]?.trim()
            )
        );

    const handleProjectChange = (value: string) => {
        const project = provisionableProjects?.find((provisionable) => provisionable.projectId === value);

        const defaultConfiguration =
            project?.configurations.find((configuration) => configuration.selfServe) ||
            (project?.configurations.length === 1 ? project.configurations[0] : undefined);

        setValue('configurationId', defaultConfiguration?.id || '');
        setValue('projectId', value);
        setValue('workflows', {});
    };

    const handleWorkflowChange = (workflowUuid: string, workflowState: Partial<ProvisionProjectWorkflowStateI>) => {
        setValue('workflows', {
            ...formWorkflows,
            [workflowUuid]: {
                inputs: formWorkflows[workflowUuid]?.inputs || {},
                selected: formWorkflows[workflowUuid]?.selected || false,
                ...workflowState,
            },
        });
    };

    const handleConfigurationChange = (value: string) => {
        setValue('configurationId', value);
        setValue('workflows', {});
    };

    const handleSave = ({workflows}: ProvisionProjectWorkflowFormI) => {
        if (!selectedConfiguration) {
            return;
        }

        provisionConnectedUserProjectWorkflowsMutation.mutate({
            configurationId: selectedConfiguration.id,
            connectedUserId: connectedUserId.toString(),
            workflows: selectableWorkflows.map((workflow) => {
                const inputs = workflows[workflow.workflowUuid]?.inputs || {};

                return workflow.inputs.length > 0
                    ? {inputs, workflowUuid: workflow.workflowUuid}
                    : {workflowUuid: workflow.workflowUuid};
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

                                {provisionableProjects?.map((project) => (
                                    <option key={project.projectId} value={project.projectId}>
                                        {`${project.name} V${project.projectVersion}`}
                                    </option>
                                ))}
                            </select>
                        </fieldset>
                    )}

                    {provisionableProject &&
                        (assignedConfigurationId ? (
                            <fieldset className="space-y-2 border-0 p-0">
                                <Label htmlFor="provision-configuration-id">Configuration</Label>

                                <Input id="provision-configuration-id" readOnly value={assignedConfigurationLabel} />

                                <p className="text-xs text-content-neutral-secondary">
                                    {selectedConfiguration
                                        ? 'Use Change configuration to move this user.'
                                        : 'This configuration is disabled, so it cannot provision workflows. Use Change configuration to move this user.'}
                                </p>
                            </fieldset>
                        ) : provisionableProject.configurations.length === 0 ? (
                            <p className="text-sm text-content-neutral-secondary">
                                No configuration available for this user
                            </p>
                        ) : (
                            <fieldset className="space-y-2 border-0 p-0">
                                <Label htmlFor="provision-configuration-id">Configuration</Label>

                                <select
                                    className="flex h-9 w-full rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary px-3 py-1 text-sm"
                                    id="provision-configuration-id"
                                    onChange={(event) => handleConfigurationChange(event.target.value)}
                                    value={configurationId}
                                >
                                    <option value="">Select...</option>

                                    {provisionableProject.configurations.map((configuration) => (
                                        <option key={configuration.id} value={configuration.id}>
                                            {`${configuration.name} · v${configuration.projectVersion}`}
                                        </option>
                                    ))}
                                </select>
                            </fieldset>
                        ))}

                    {selectedConfiguration?.workflows.map((workflow) => {
                        const label = workflow.label || workflow.workflowUuid;
                        const locked = workflow.provisioned || workflow.dangling;
                        const workflowState = formWorkflows[workflow.workflowUuid];
                        const selected = !!workflowState?.selected;

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
                                                handleWorkflowChange(workflow.workflowUuid, {
                                                    selected: checked === true,
                                                })
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
                                            <Label
                                                htmlFor={`provision-workflow-${workflow.workflowUuid}-${input.name}`}
                                            >
                                                {input.label || input.name}

                                                {input.required && ' *'}
                                            </Label>

                                            <Input
                                                id={`provision-workflow-${workflow.workflowUuid}-${input.name}`}
                                                onChange={(event) =>
                                                    handleWorkflowChange(workflow.workflowUuid, {
                                                        inputs: {
                                                            ...workflowState?.inputs,
                                                            [input.name]: event.target.value,
                                                        },
                                                    })
                                                }
                                                value={workflowState?.inputs[input.name] || ''}
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
