import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Input} from '@/components/ui/input';
import {
    AutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery,
    AutomationWorkflowProjectConfigurationsQuery,
    useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery,
    useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type ProvidableValuesType =
    AutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery['automationWorkflowProjectConfigurationWorkflowProvidableValues'];
type ConnectionSlotType = ProvidableValuesType['connectionSlots'][number];

const LET_EACH_USER_CONNECT = 'let-each-user-connect';

const NUMERIC_INPUT_TYPES = new Set(['FLOAT', 'INTEGER', 'NUMBER']);

const coerceInputValue = (type: string | null | undefined, value: string): boolean | number | string => {
    const normalizedType = type?.toUpperCase();

    if (normalizedType && NUMERIC_INPUT_TYPES.has(normalizedType) && Number.isFinite(Number(value))) {
        return Number(value);
    }

    if (normalizedType === 'BOOLEAN' && (value === 'true' || value === 'false')) {
        return value === 'true';
    }

    return value;
};

interface ConnectionSlotProps {
    connectionSlot: ConnectionSlotType;
    onValueChange: (connectionId: string) => void;
    value: string;
}

const ConnectionSlot = ({connectionSlot, onValueChange, value}: ConnectionSlotProps) => {
    const connectionLabel = `${connectionSlot.title || connectionSlot.componentName} Connection`;

    const isUnavailable =
        value !== LET_EACH_USER_CONNECT &&
        !connectionSlot.providableConnections.some((providableConnection) => providableConnection.id === value);

    return (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <span className="text-sm font-medium">{connectionLabel}</span>

            <Select onValueChange={onValueChange} value={value}>
                <SelectTrigger aria-label={connectionLabel}>
                    <SelectValue />
                </SelectTrigger>

                <SelectContent>
                    <SelectItem value={LET_EACH_USER_CONNECT}>Let each user connect</SelectItem>

                    {isUnavailable && <SelectItem value={value}>{`Connection ${value} (unavailable)`}</SelectItem>}

                    {connectionSlot.providableConnections.map((providableConnection) => (
                        <SelectItem key={providableConnection.id} value={providableConnection.id}>
                            {providableConnection.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </fieldset>
    );
};

interface ProvidedValuesFormProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    onClose: () => void;
    providableValues: ProvidableValuesType;
    workflowUuid: string;
}

const ProvidedValuesForm = ({configuration, onClose, providableValues, workflowUuid}: ProvidedValuesFormProps) => {
    const [providedConnections, setProvidedConnections] = useState<Record<string, string>>(() =>
        Object.fromEntries(
            Object.entries((providableValues.providedConnections ?? {}) as Record<string, unknown>).map(
                ([componentName, connectionId]) => [componentName, String(connectionId)]
            )
        )
    );
    const [providedInputs, setProvidedInputs] = useState<Record<string, string>>(() =>
        Object.fromEntries(
            Object.entries((providableValues.providedInputs ?? {}) as Record<string, unknown>).map(
                ([inputName, value]) => [inputName, String(value)]
            )
        )
    );

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation();

    const handleSubmit = () => {
        const connectionsToSave: Record<string, string> = {};

        for (const connectionSlot of providableValues.connectionSlots) {
            const connectionId = providedConnections[connectionSlot.componentName];

            if (connectionId && connectionId !== LET_EACH_USER_CONNECT) {
                connectionsToSave[connectionSlot.componentName] = connectionId;
            }
        }

        const inputsToSave: Record<string, boolean | number | string> = {};

        for (const input of providableValues.inputs) {
            const value = providedInputs[input.name];

            if (value === undefined || value.trim() === '') {
                continue;
            }

            inputsToSave[input.name] = coerceInputValue(input.type, value);
        }

        updateWorkflowMutation.mutate(
            {
                id: configuration.id,
                providedConnections: connectionsToSave,
                providedInputs: inputsToSave,
                workflowUuid,
            },
            {
                onSuccess: () => {
                    void queryClient.invalidateQueries({queryKey: ['automationWorkflowProjectConfigurations']});
                    void queryClient.invalidateQueries({
                        queryKey: ['automationWorkflowProjectConfigurationWorkflowProvidableValues'],
                    });

                    onClose();
                },
            }
        );
    };

    return (
        <form
            className="flex min-h-0 flex-1 flex-col"
            onSubmit={(event) => {
                event.preventDefault();

                handleSubmit();
            }}
        >
            <DialogHeader
                description="Connections and inputs set here are used by every connected user and cannot be changed by them."
                title="Provided Values"
            />

            <DialogBody>
                {providableValues.connectionSlots.map((connectionSlot) => (
                    <ConnectionSlot
                        connectionSlot={connectionSlot}
                        key={connectionSlot.componentName}
                        onValueChange={(connectionId) =>
                            setProvidedConnections((previous) => ({
                                ...previous,
                                [connectionSlot.componentName]: connectionId,
                            }))
                        }
                        value={providedConnections[connectionSlot.componentName] || LET_EACH_USER_CONNECT}
                    />
                ))}

                {providableValues.inputs.map((input) => (
                    <fieldset className="flex flex-col gap-2 border-0 p-0" key={input.name}>
                        <label className="text-sm font-medium" htmlFor={`provided-input-${input.name}`}>
                            {input.label || input.name}
                        </label>

                        <Input
                            id={`provided-input-${input.name}`}
                            onChange={(event) =>
                                setProvidedInputs((previous) => ({
                                    ...previous,
                                    [input.name]: event.target.value,
                                }))
                            }
                            placeholder="Let each user fill it"
                            value={providedInputs[input.name] || ''}
                        />
                    </fieldset>
                ))}

                {providableValues.connectionSlots.length === 0 && providableValues.inputs.length === 0 && (
                    <p className="text-sm text-content-neutral-secondary">
                        This workflow has no connections or inputs to provide.
                    </p>
                )}
            </DialogBody>

            <DialogFooter>
                <DialogCancelButton />

                <Button disabled={updateWorkflowMutation.isPending} label="Save" type="submit" />
            </DialogFooter>
        </form>
    );
};

interface AutomationWorkflowProjectConfigurationWorkflowDialogProps {
    configuration: AutomationWorkflowProjectConfigurationType;
    onClose: () => void;
    workflow: AutomationWorkflowProjectConfigurationType['workflows'][number];
}

const AutomationWorkflowProjectConfigurationWorkflowDialog = ({
    configuration,
    onClose,
    workflow,
}: AutomationWorkflowProjectConfigurationWorkflowDialogProps) => {
    const {data} = useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery({
        id: configuration.id,
        workflowUuid: workflow.workflowUuid,
    });

    const providableValues = data?.automationWorkflowProjectConfigurationWorkflowProvidableValues;

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-label="Provided Values Dialog" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    {providableValues ? (
                        <ProvidedValuesForm
                            configuration={configuration}
                            onClose={onClose}
                            providableValues={providableValues}
                            workflowUuid={workflow.workflowUuid}
                        />
                    ) : (
                        <>
                            <DialogHeader
                                description="Connections and inputs set here are used by every connected user and cannot be changed by them."
                                title="Provided Values"
                            />

                            <DialogBody>
                                <p className="text-sm text-content-neutral-secondary">Loading...</p>
                            </DialogBody>
                        </>
                    )}
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AutomationWorkflowProjectConfigurationWorkflowDialog;
