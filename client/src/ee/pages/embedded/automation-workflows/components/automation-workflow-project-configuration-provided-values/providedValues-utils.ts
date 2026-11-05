import {AutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery} from '@/shared/middleware/graphql';
import {Option} from '@/shared/middleware/platform/configuration';

export type ProvidableValuesType =
    AutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery['automationWorkflowProjectConfigurationWorkflowProvidableValues'];
export type ConnectionSlotType = ProvidableValuesType['connectionSlots'][number];
export type ProvidableInputType = ProvidableValuesType['inputs'][number];
export type GroupValuesType = Record<string, string>;
export type ProvidedInputValueType = GroupValuesType | string;
export type ProvidedConnectionsType = Record<string, string>;
export type ProvidedInputsType = Record<string, ProvidedInputValueType>;

export interface ProvidableInputPropertyI {
    label?: string;
    name?: string;
    options?: Option[];
    optionsDataSource?: {optionsLookupDependsOn?: string[]};
    type?: string;
}

export interface ProvidedValuesPayloadI {
    providedConnections: Record<string, string>;
    providedInputs: Record<string, boolean | number | string | GroupValuesType>;
}

export interface ValueOptionI {
    label: string;
    value: string;
}

export const LET_EACH_USER_CONNECT = 'let-each-user-connect';

export const LET_EACH_USER_FILL = 'let-each-user-fill';

export const NUMERIC_INPUT_TYPES = new Set(['FLOAT', 'INTEGER', 'NUMBER']);

export const BOOLEAN_OPTIONS: ValueOptionI[] = [
    {label: 'True', value: 'true'},
    {label: 'False', value: 'false'},
];

export const coerceInputValue = (type: string | null | undefined, value: string): boolean | number | string => {
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

export const isComponentReferenceInput = (input: ProvidableInputType): boolean =>
    !!input.componentName && input.componentVersion != null && !!input.groupName;

export const getInitialProvidedConnections = (providableValues: ProvidableValuesType): ProvidedConnectionsType =>
    Object.fromEntries(
        Object.entries((providableValues.providedConnections ?? {}) as Record<string, unknown>).map(
            ([componentName, connectionId]) => [componentName, String(connectionId)]
        )
    );

export const getInitialProvidedInputs = (providableValues: ProvidableValuesType): ProvidedInputsType =>
    Object.fromEntries(
        Object.entries((providableValues.providedInputs ?? {}) as Record<string, unknown>)
            .filter(([, value]) => value != null)
            .map(([inputName, value]) => [inputName, toProvidedInputValue(value)])
    );

export const getProvidedConnectionId = (
    providedConnections: ProvidedConnectionsType,
    componentName?: string | null
): string | undefined => {
    const connectionId = componentName ? providedConnections[componentName] : undefined;

    return connectionId && connectionId !== LET_EACH_USER_CONNECT ? connectionId : undefined;
};

export const buildProvidedValuesPayload = (
    providableValues: ProvidableValuesType,
    providedConnections: ProvidedConnectionsType,
    providedInputs: ProvidedInputsType
): ProvidedValuesPayloadI => {
    const connectionsToSave: Record<string, string> = {};

    for (const connectionSlot of providableValues.connectionSlots) {
        const connectionId = getProvidedConnectionId(providedConnections, connectionSlot.componentName);

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

    return {providedConnections: connectionsToSave, providedInputs: inputsToSave};
};
