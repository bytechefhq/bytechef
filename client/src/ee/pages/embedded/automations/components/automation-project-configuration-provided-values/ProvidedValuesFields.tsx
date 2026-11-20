import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Input} from '@/components/ui/input';
import {REQUIRED_INTERNAL_ONLY_INPUT_HINT} from '@/ee/pages/embedded/shared/internalOnlyInputs';
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {useAutomationProjectConfigurationWorkflowInputOptionsQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useMemo, useState} from 'react';

import {
    BOOLEAN_OPTIONS,
    type ConnectionSlotType,
    type GroupValuesType,
    LET_EACH_USER_CONNECT,
    NUMERIC_INPUT_TYPES,
    type ProvidableInputPropertyI,
    type ProvidableInputType,
    type ProvidableValuesType,
    type ProvidedConnectionsType,
    type ProvidedInputValueType,
    type ProvidedInputsType,
    type ValueOptionI,
    getInternalOnlyInputs,
    getProvidedConnectionId,
    isComponentReferenceInput,
} from './providedValues-utils';

const SET_A_VALUE_PLACEHOLDER = 'Set a value';

const isBlankText = (text?: string): boolean => !text || text.trim() === '';

const isEmptyInputValue = (value?: ProvidedInputValueType): boolean => {
    if (value === undefined || typeof value === 'string') {
        return isBlankText(value);
    }

    return Object.values(value).every((memberValue) => isBlankText(memberValue));
};

interface RequiredInternalOnlyHintProps {
    input: ProvidableInputType;
    value?: ProvidedInputValueType;
}

const RequiredInternalOnlyHint = ({input, value}: RequiredInternalOnlyHintProps) => {
    if (!input.required || !isEmptyInputValue(value)) {
        return null;
    }

    return <span className="text-xs text-content-destructive-primary">{REQUIRED_INTERNAL_ONLY_INPUT_HINT}</span>;
};

const getInputLabelText = (input: ProvidableInputType): string => {
    const inputLabel = input.label || input.name;

    return input.required ? `${inputLabel} *` : inputLabel;
};

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
        <Select onValueChange={onValueChange} value={value}>
            <SelectTrigger aria-label={label}>
                <SelectValue placeholder={placeholder || SET_A_VALUE_PLACEHOLDER} />
            </SelectTrigger>

            <SelectContent>
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
                {getInputLabelText(input)}
            </label>

            {normalizedType === 'BOOLEAN' ? (
                <ValueSelect label={inputLabel} onValueChange={onValueChange} options={BOOLEAN_OPTIONS} value={value} />
            ) : (
                <Input
                    id={`provided-input-${input.name}`}
                    inputMode={normalizedType && NUMERIC_INPUT_TYPES.has(normalizedType) ? 'decimal' : undefined}
                    onChange={(event) => onValueChange(event.target.value)}
                    placeholder={SET_A_VALUE_PLACEHOLDER}
                    value={value}
                />
            )}

            <RequiredInternalOnlyHint input={input} value={value} />
        </fieldset>
    );
};

interface DynamicOptionsSelectProps {
    configurationId: string;
    input: ProvidableInputType;
    label: string;
    lookupDependsOnValues: Record<string, string>;
    onValueChange: (value: string) => void;
    projectVersion?: number;
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
    projectVersion,
    property,
    providedConnectionId,
    value,
    workflowUuid,
}: DynamicOptionsSelectProps) => {
    const [optionConnectionId, setOptionConnectionId] = useState<string | undefined>(
        () => input.optionConnections[0]?.id
    );

    const connectionId = providedConnectionId || optionConnectionId;

    const {data, isError, isLoading} = useAutomationProjectConfigurationWorkflowInputOptionsQuery(
        {
            connectionId: connectionId ?? '',
            id: configurationId,
            inputName: input.name,
            lookupDependsOnValues,
            projectVersion,
            propertyName: property.name ?? '',
            workflowUuid,
        },
        {enabled: !!connectionId && !!property.name}
    );

    const options = useMemo(
        () =>
            (data?.automationProjectConfigurationWorkflowInputOptions ?? []).map((option) => ({
                label: option.label || option.value,
                value: option.value,
            })),
        [data]
    );

    const optionsConnectionLabel = `Load ${label} options with`;

    let placeholder = SET_A_VALUE_PLACEHOLDER;

    if (!connectionId) {
        placeholder = `${SET_A_VALUE_PLACEHOLDER} (no connection to load options)`;
    } else if (isLoading) {
        placeholder = 'Loading options...';
    } else if (isError) {
        placeholder = `${SET_A_VALUE_PLACEHOLDER} (options could not be loaded)`;
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
    projectVersion?: number;
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
    projectVersion,
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
                projectVersion={projectVersion}
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
            placeholder={SET_A_VALUE_PLACEHOLDER}
            value={value}
        />
    );
};

interface ComponentReferenceInputProps {
    configurationId: string;
    input: ProvidableInputType;
    onValueChange: (value: ProvidedInputValueType) => void;
    projectVersion?: number;
    providedConnectionId?: string;
    value?: ProvidedInputValueType;
    workflowUuid: string;
}

const ComponentReferenceInput = ({
    configurationId,
    input,
    onValueChange,
    projectVersion,
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
                <span className="text-sm font-medium">{getInputLabelText(input)}</span>

                <ComponentReferenceMember
                    configurationId={configurationId}
                    input={input}
                    label={inputLabel}
                    lookupDependsOnValues={{}}
                    onValueChange={onValueChange}
                    projectVersion={projectVersion}
                    property={members[0]}
                    providedConnectionId={providedConnectionId}
                    value={typeof value === 'string' ? value : ''}
                    workflowUuid={workflowUuid}
                />

                <RequiredInternalOnlyHint input={input} value={value} />
            </fieldset>
        );
    }

    const groupValues: GroupValuesType = value && typeof value === 'object' ? value : {};

    return (
        <fieldset className="flex flex-col gap-3 rounded-md border border-stroke-neutral-secondary p-3">
            <legend className="px-1 text-sm font-medium">{getInputLabelText(input)}</legend>

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
                            projectVersion={projectVersion}
                            property={member}
                            providedConnectionId={providedConnectionId}
                            value={groupValues[memberName] ?? ''}
                            workflowUuid={workflowUuid}
                        />
                    </fieldset>
                );
            })}

            <RequiredInternalOnlyHint input={input} value={value} />
        </fieldset>
    );
};

interface ProvidedValuesFieldsProps {
    configurationId: string;
    enabled?: boolean;
    label?: string;
    onEnabledChange?: (enabled: boolean) => void;
    onProvidedConnectionsChange: (providedConnections: ProvidedConnectionsType) => void;
    onProvidedInputsChange: (providedInputs: ProvidedInputsType) => void;
    projectVersion?: number;
    providableValues: ProvidableValuesType;
    providedConnections: ProvidedConnectionsType;
    providedInputs: ProvidedInputsType;
    showWorkflowToggle?: boolean;
    workflowUuid: string;
}

const ProvidedValuesFields = ({
    configurationId,
    enabled = true,
    label,
    onEnabledChange = () => undefined,
    onProvidedConnectionsChange,
    onProvidedInputsChange,
    projectVersion,
    providableValues,
    providedConnections,
    providedInputs,
    showWorkflowToggle = false,
    workflowUuid,
}: ProvidedValuesFieldsProps) => {
    const internalOnlyInputs = getInternalOnlyInputs(providableValues);

    return (
        <WorkflowConfigurationCard
            connectionCount={providableValues.connectionSlots.length}
            connectionsContent={
                providableValues.connectionSlots.length > 0 ? (
                    providableValues.connectionSlots.map((connectionSlot) => (
                        <ConnectionSlot
                            connectionSlot={connectionSlot}
                            key={connectionSlot.componentName}
                            onValueChange={(connectionId) =>
                                onProvidedConnectionsChange({
                                    ...providedConnections,
                                    [connectionSlot.componentName]: connectionId,
                                })
                            }
                            value={providedConnections[connectionSlot.componentName] || LET_EACH_USER_CONNECT}
                        />
                    ))
                ) : (
                    <p className="text-sm">No Connections yet</p>
                )
            }
            enabled={enabled}
            inputCount={internalOnlyInputs.length}
            inputsContent={
                internalOnlyInputs.length > 0 ? (
                    <div className="space-y-4">
                        {internalOnlyInputs.map((input) => {
                            const value = providedInputs[input.name];

                            const handleInputValueChange = (inputValue: ProvidedInputValueType) =>
                                onProvidedInputsChange({...providedInputs, [input.name]: inputValue});

                            return isComponentReferenceInput(input) ? (
                                <ComponentReferenceInput
                                    configurationId={configurationId}
                                    input={input}
                                    key={input.name}
                                    onValueChange={handleInputValueChange}
                                    projectVersion={projectVersion}
                                    providedConnectionId={getProvidedConnectionId(
                                        providedConnections,
                                        input.componentName
                                    )}
                                    value={value}
                                    workflowUuid={workflowUuid}
                                />
                            ) : (
                                <PlainInput
                                    input={input}
                                    key={input.name}
                                    onValueChange={handleInputValueChange}
                                    value={typeof value === 'string' ? value : ''}
                                />
                            );
                        })}
                    </div>
                ) : (
                    <p className="text-sm">No Inputs yet</p>
                )
            }
            label={label}
            onEnabledChange={onEnabledChange}
            showWorkflowToggle={showWorkflowToggle}
            switchId={`provided-values-${workflowUuid}`}
        />
    );
};

export default ProvidedValuesFields;
