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
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {
    AutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery,
    AutomationWorkflowProjectConfigurationsQuery,
    useAutomationWorkflowProjectConfigurationWorkflowInputOptionsQuery,
    useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery,
    useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {Option} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo, useState} from 'react';

type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type ProvidableValuesType =
    AutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery['automationWorkflowProjectConfigurationWorkflowProvidableValues'];
type ConnectionSlotType = ProvidableValuesType['connectionSlots'][number];
type ProvidableInputType = ProvidableValuesType['inputs'][number];
type GroupValuesType = Record<string, string>;
type ProvidedInputValueType = GroupValuesType | string;

interface ProvidableInputPropertyI {
    label?: string;
    name?: string;
    options?: Option[];
    optionsDataSource?: {optionsLookupDependsOn?: string[]};
    type?: string;
}

interface ValueOptionI {
    label: string;
    value: string;
}

const LET_EACH_USER_CONNECT = 'let-each-user-connect';

const LET_EACH_USER_FILL = 'let-each-user-fill';

const NUMERIC_INPUT_TYPES = new Set(['FLOAT', 'INTEGER', 'NUMBER']);

const BOOLEAN_OPTIONS: ValueOptionI[] = [
    {label: 'True', value: 'true'},
    {label: 'False', value: 'false'},
];

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

const toProvidedInputValue = (value: unknown): ProvidedInputValueType => {
    if (value !== null && typeof value === 'object') {
        return Object.fromEntries(
            Object.entries(value as Record<string, unknown>).map(([memberName, memberValue]) => [
                memberName,
                String(memberValue),
            ])
        );
    }

    return String(value);
};

const isComponentReferenceInput = (input: ProvidableInputType): boolean =>
    !!input.componentName && input.componentVersion != null && !!input.groupName;

interface ValueSelectProps {
    label: string;
    onValueChange: (value: string) => void;
    options: ValueOptionI[];
    placeholder?: string;
    value: string;
}

const ValueSelect = ({label, onValueChange, options, placeholder, value}: ValueSelectProps) => {
    const isCurrentValueMissing = !!value && !options.some((option) => option.value === value);

    return (
        <Select
            onValueChange={(selectedValue) => onValueChange(selectedValue === LET_EACH_USER_FILL ? '' : selectedValue)}
            value={value || LET_EACH_USER_FILL}
        >
            <SelectTrigger aria-label={label}>
                <SelectValue />
            </SelectTrigger>

            <SelectContent>
                <SelectItem value={LET_EACH_USER_FILL}>{placeholder || 'Let each user fill it'}</SelectItem>

                {isCurrentValueMissing && <SelectItem value={value}>{value}</SelectItem>}

                {options.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                        {option.label}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
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

interface PlainInputProps {
    input: ProvidableInputType;
    onValueChange: (value: string) => void;
    value: string;
}

const PlainInput = ({input, onValueChange, value}: PlainInputProps) => {
    const inputLabel = input.label || input.name;
    const normalizedType = input.type?.toUpperCase();

    return (
        <fieldset className="flex flex-col gap-2 border-0 p-0">
            <label className="text-sm font-medium" htmlFor={`provided-input-${input.name}`}>
                {inputLabel}
            </label>

            {normalizedType === 'BOOLEAN' ? (
                <ValueSelect label={inputLabel} onValueChange={onValueChange} options={BOOLEAN_OPTIONS} value={value} />
            ) : (
                <Input
                    id={`provided-input-${input.name}`}
                    inputMode={normalizedType && NUMERIC_INPUT_TYPES.has(normalizedType) ? 'decimal' : undefined}
                    onChange={(event) => onValueChange(event.target.value)}
                    placeholder="Let each user fill it"
                    value={value}
                />
            )}
        </fieldset>
    );
};

interface DynamicOptionsSelectProps {
    configurationId: string;
    input: ProvidableInputType;
    label: string;
    lookupDependsOnValues: Record<string, string>;
    onValueChange: (value: string) => void;
    property: ProvidableInputPropertyI;
    providedConnectionId?: string;
    value: string;
    workflowUuid: string;
}

const DynamicOptionsSelect = ({
    configurationId,
    input,
    label,
    lookupDependsOnValues,
    onValueChange,
    property,
    providedConnectionId,
    value,
    workflowUuid,
}: DynamicOptionsSelectProps) => {
    const [optionConnectionId, setOptionConnectionId] = useState<string | undefined>(
        () => input.optionConnections[0]?.id
    );

    const connectionId = providedConnectionId || optionConnectionId;

    const {data, isError, isLoading} = useAutomationWorkflowProjectConfigurationWorkflowInputOptionsQuery(
        {
            connectionId: connectionId ?? '',
            id: configurationId,
            inputName: input.name,
            lookupDependsOnValues,
            propertyName: property.name ?? '',
            workflowUuid,
        },
        {enabled: !!connectionId && !!property.name}
    );

    const options = useMemo(
        () =>
            (data?.automationWorkflowProjectConfigurationWorkflowInputOptions ?? []).map((option) => ({
                label: option.label || option.value,
                value: option.value,
            })),
        [data]
    );

    const optionsConnectionLabel = `Load ${label} options with`;

    let placeholder = 'Let each user fill it';

    if (!connectionId) {
        placeholder = 'Let each user fill it (no connection to load options)';
    } else if (isLoading) {
        placeholder = 'Loading options...';
    } else if (isError) {
        placeholder = 'Let each user fill it (options could not be loaded)';
    }

    return (
        <div className="flex flex-col gap-2">
            {!providedConnectionId && input.optionConnections.length > 0 && (
                <div className="flex flex-col gap-1">
                    <span className="text-xs text-content-neutral-secondary">{optionsConnectionLabel}</span>

                    <Select onValueChange={setOptionConnectionId} value={optionConnectionId}>
                        <SelectTrigger aria-label={optionsConnectionLabel}>
                            <SelectValue />
                        </SelectTrigger>

                        <SelectContent>
                            {input.optionConnections.map((optionConnection) => (
                                <SelectItem key={optionConnection.id} value={optionConnection.id}>
                                    {optionConnection.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            )}

            <ValueSelect
                label={label}
                onValueChange={onValueChange}
                options={options}
                placeholder={placeholder}
                value={value}
            />
        </div>
    );
};

interface ComponentReferenceMemberProps {
    configurationId: string;
    input: ProvidableInputType;
    label: string;
    lookupDependsOnValues: Record<string, string>;
    onValueChange: (value: string) => void;
    property: ProvidableInputPropertyI;
    providedConnectionId?: string;
    value: string;
    workflowUuid: string;
}

const ComponentReferenceMember = ({
    configurationId,
    input,
    label,
    lookupDependsOnValues,
    onValueChange,
    property,
    providedConnectionId,
    value,
    workflowUuid,
}: ComponentReferenceMemberProps) => {
    const staticOptions = useMemo(
        () =>
            (property.options ?? [])
                .filter((option) => option.value != null)
                .map((option) => ({label: option.label || String(option.value), value: String(option.value)})),
        [property.options]
    );

    if (property.optionsDataSource) {
        return (
            <DynamicOptionsSelect
                configurationId={configurationId}
                input={input}
                label={label}
                lookupDependsOnValues={lookupDependsOnValues}
                onValueChange={onValueChange}
                property={property}
                providedConnectionId={providedConnectionId}
                value={value}
                workflowUuid={workflowUuid}
            />
        );
    }

    if (staticOptions.length > 0) {
        return <ValueSelect label={label} onValueChange={onValueChange} options={staticOptions} value={value} />;
    }

    if (property.type === 'BOOLEAN') {
        return <ValueSelect label={label} onValueChange={onValueChange} options={BOOLEAN_OPTIONS} value={value} />;
    }

    return (
        <Input
            aria-label={label}
            onChange={(event) => onValueChange(event.target.value)}
            placeholder="Let each user fill it"
            value={value}
        />
    );
};

interface ComponentReferenceInputProps {
    configurationId: string;
    input: ProvidableInputType;
    onValueChange: (value: ProvidedInputValueType) => void;
    providedConnectionId?: string;
    value?: ProvidedInputValueType;
    workflowUuid: string;
}

const ComponentReferenceInput = ({
    configurationId,
    input,
    onValueChange,
    providedConnectionId,
    value,
    workflowUuid,
}: ComponentReferenceInputProps) => {
    const inputLabel = input.label || input.name;

    const {data: componentDefinition, isLoading} = useGetComponentDefinitionQuery({
        componentName: input.componentName!,
        componentVersion: input.componentVersion!,
    });

    const members = useMemo(
        () =>
            (componentDefinition?.inputs?.find((group) => group.name === input.groupName)?.properties ??
                []) as ProvidableInputPropertyI[],
        [componentDefinition, input.groupName]
    );

    if (isLoading || members.length === 0) {
        return (
            <PlainInput input={input} onValueChange={onValueChange} value={typeof value === 'string' ? value : ''} />
        );
    }

    if (members.length === 1) {
        return (
            <fieldset className="flex flex-col gap-2 border-0 p-0">
                <span className="text-sm font-medium">{inputLabel}</span>

                <ComponentReferenceMember
                    configurationId={configurationId}
                    input={input}
                    label={inputLabel}
                    lookupDependsOnValues={{}}
                    onValueChange={onValueChange}
                    property={members[0]}
                    providedConnectionId={providedConnectionId}
                    value={typeof value === 'string' ? value : ''}
                    workflowUuid={workflowUuid}
                />
            </fieldset>
        );
    }

    const groupValues: GroupValuesType = value && typeof value === 'object' ? value : {};

    return (
        <fieldset className="flex flex-col gap-3 rounded-md border border-stroke-neutral-secondary p-3">
            <legend className="px-1 text-sm font-medium">{inputLabel}</legend>

            {members.map((member) => {
                const memberName = member.name ?? '';
                const memberLabel = member.label || memberName;

                const lookupDependsOnValues = Object.fromEntries(
                    (member.optionsDataSource?.optionsLookupDependsOn ?? []).map((dependencyPath) => {
                        const dependencyName = dependencyPath.split('.').pop() ?? dependencyPath;

                        return [dependencyName, groupValues[dependencyName] ?? ''];
                    })
                );

                return (
                    <fieldset className="flex flex-col gap-2 border-0 p-0" key={memberName}>
                        <span className="text-sm">{memberLabel}</span>

                        <ComponentReferenceMember
                            configurationId={configurationId}
                            input={input}
                            label={memberLabel}
                            lookupDependsOnValues={lookupDependsOnValues}
                            onValueChange={(memberValue) => onValueChange({...groupValues, [memberName]: memberValue})}
                            property={member}
                            providedConnectionId={providedConnectionId}
                            value={groupValues[memberName] ?? ''}
                            workflowUuid={workflowUuid}
                        />
                    </fieldset>
                );
            })}
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
    const [providedInputs, setProvidedInputs] = useState<Record<string, ProvidedInputValueType>>(() =>
        Object.fromEntries(
            Object.entries((providableValues.providedInputs ?? {}) as Record<string, unknown>)
                .filter(([, value]) => value != null)
                .map(([inputName, value]) => [inputName, toProvidedInputValue(value)])
        )
    );

    const queryClient = useQueryClient();

    const updateWorkflowMutation = useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation();

    const setProvidedInput = (inputName: string, value: ProvidedInputValueType) =>
        setProvidedInputs((previous) => ({...previous, [inputName]: value}));

    const getProvidedConnectionId = (componentName?: string | null) => {
        const connectionId = componentName ? providedConnections[componentName] : undefined;

        return connectionId && connectionId !== LET_EACH_USER_CONNECT ? connectionId : undefined;
    };

    const handleSubmit = () => {
        const connectionsToSave: Record<string, string> = {};

        for (const connectionSlot of providableValues.connectionSlots) {
            const connectionId = getProvidedConnectionId(connectionSlot.componentName);

            if (connectionId) {
                connectionsToSave[connectionSlot.componentName] = connectionId;
            }
        }

        const inputsToSave: Record<string, boolean | number | string | GroupValuesType> = {};

        for (const input of providableValues.inputs) {
            const value = providedInputs[input.name];

            if (value === undefined) {
                continue;
            }

            if (typeof value === 'object') {
                const filledMembers = Object.fromEntries(
                    Object.entries(value).filter(([, memberValue]) => memberValue.trim() !== '')
                );

                if (Object.keys(filledMembers).length > 0) {
                    inputsToSave[input.name] = filledMembers;
                }

                continue;
            }

            if (value.trim() === '') {
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
                <WorkflowConfigurationCard
                    connectionCount={providableValues.connectionSlots.length}
                    connectionsContent={
                        providableValues.connectionSlots.length > 0 ? (
                            providableValues.connectionSlots.map((connectionSlot) => (
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
                            ))
                        ) : (
                            <p className="p-4 text-center text-sm text-content-neutral-secondary">
                                This workflow has no connections to provide.
                            </p>
                        )
                    }
                    enabled
                    inputCount={providableValues.inputs.length}
                    inputsContent={
                        providableValues.inputs.length > 0 ? (
                            <div className="space-y-4">
                                {providableValues.inputs.map((input) => {
                                    const value = providedInputs[input.name];

                                    return isComponentReferenceInput(input) ? (
                                        <ComponentReferenceInput
                                            configurationId={configuration.id}
                                            input={input}
                                            key={input.name}
                                            onValueChange={(inputValue) => setProvidedInput(input.name, inputValue)}
                                            providedConnectionId={getProvidedConnectionId(input.componentName)}
                                            value={value}
                                            workflowUuid={workflowUuid}
                                        />
                                    ) : (
                                        <PlainInput
                                            input={input}
                                            key={input.name}
                                            onValueChange={(inputValue) => setProvidedInput(input.name, inputValue)}
                                            value={typeof value === 'string' ? value : ''}
                                        />
                                    );
                                })}
                            </div>
                        ) : (
                            <p className="p-4 text-center text-sm text-content-neutral-secondary">
                                This workflow has no inputs to provide.
                            </p>
                        )
                    }
                    onEnabledChange={() => undefined}
                    showWorkflowToggle={false}
                    switchId={`provided-values-${workflowUuid}`}
                />
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
