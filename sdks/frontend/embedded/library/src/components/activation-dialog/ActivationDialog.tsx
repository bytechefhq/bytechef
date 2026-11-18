import {RefObject, useCallback, useEffect, useMemo, useReducer, useRef, useState} from 'react';
import {ApiResponseError, createApiClient} from '../shared/apiClient';
import dialogStyles from '../shared/dialog.module.css';
import DialogPoweredBy from '../shared/DialogPoweredBy';
import WebhookUrl from '../shared/WebhookUrl';
import XIcon from '../connect-dialog/assets/x.svg';
import {
    activationReducer,
    canProceed,
    hasEditableInputs,
    initialActivationState,
    toInputValueText,
} from './activationReducer';
import ComponentInputField from './ComponentInputField';
import ConnectDialogOpener from './ConnectDialogOpener';
import styles from './ActivationDialog.module.css';
import {
    AutomationTemplateType,
    ConnectedUserWorkflowType,
    ConnectionType,
    ExistingReferenceType,
    MissingConnectionErrorType,
    ProvidedValueLockedErrorType,
    TemplateComponentType,
    TemplateInputType,
} from './types';

export interface ActivationDialogProps {
    baseUrl?: string;
    connectionCreationAllowed?: boolean;
    environment?: string;
    jwtToken: string;
    mode?: 'dark' | 'light';
    onActivated?: (workflowUuid: string) => void;
    onClose: () => void;
    onDeactivated?: (workflowTemplateId: string) => void;
    onEditWorkflow?: (workflowUuid: string) => void;
    onSaved?: (workflowUuid: string) => void;
    workflowTemplateId: string;
}

function findExistingCopiedWorkflowUuid(error: unknown): string | undefined {
    if (!(error instanceof ApiResponseError) || error.status !== 400) {
        return undefined;
    }

    const body = error.body as
        | {entityClass?: unknown; errorKey?: unknown; inputParameters?: {copiedWorkflowUuid?: unknown} | null}
        | null
        | undefined;

    if (body?.entityClass !== 'ConnectedUserProjectWorkflow' || body.errorKey !== 100) {
        return undefined;
    }

    const copiedWorkflowUuid = body.inputParameters?.copiedWorkflowUuid;

    return typeof copiedWorkflowUuid === 'string' && copiedWorkflowUuid !== '' ? copiedWorkflowUuid : undefined;
}

const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.';
const TEMPLATE_NOT_FOUND_MESSAGE = 'This automation could not be found.';
const SESSION_EXPIRED_MESSAGE = 'Your session has expired. Reload the page and try again.';
const FORBIDDEN_MESSAGE = "You don't have permission to do this.";
const STATUS_UNKNOWN_MESSAGE = 'Could not check your automations. Please try again.';
const ROLLBACK_FAILED_MESSAGE =
    'Activation failed and could not be fully undone. Check your automations before trying again.';
const PROVIDED_BY_ADMINISTRATOR_LABEL = 'Provided by your administrator';
const PROVIDED_VALUE_LOCKED_MESSAGE =
    'Your administrator already provides a connection for this automation, so it cannot be changed. Reload the page and try again.';
const AI_AGENTS_SWITCH_LABEL = 'Available to AI agents';
const AI_AGENTS_SWITCH_FAILED_MESSAGE = 'Could not change whether AI agents can use this automation. Please try again.';
const NOT_ENABLED_MESSAGE = 'Your settings were saved, but the automation could not be switched on. Please try again.';

const INPUT_TYPE_ATTRIBUTES: Record<string, string> = {
    DATE: 'date',
    DATE_TIME: 'datetime-local',
    EMAIL: 'email',
    INTEGER: 'number',
    NUMBER: 'number',
    TIME: 'time',
    URL: 'url',
};

const describeError = (error: unknown): string => {
    if (error instanceof ApiResponseError) {
        if (error.status === 401) {
            return SESSION_EXPIRED_MESSAGE;
        }

        if (error.status === 403) {
            return FORBIDDEN_MESSAGE;
        }

        if (error.status === 404) {
            return TEMPLATE_NOT_FOUND_MESSAGE;
        }
    }

    return GENERIC_ERROR_MESSAGE;
};

const runHostCallback = (name: string, callback: () => void) => {
    try {
        callback();
    } catch (error) {
        console.error(`The ${name} callback failed:`, error);
    }
};

const readMissingConnectionComponentName = (error: unknown): string | undefined => {
    if (error instanceof ApiResponseError && error.status === 409) {
        return (error.body as MissingConnectionErrorType | undefined)?.missingConnectionComponentName;
    }

    return undefined;
};

const isProvidedValueLockedError = (error: unknown): boolean => {
    if (error instanceof ApiResponseError && error.status === 409) {
        const body = error.body as ProvidedValueLockedErrorType | undefined;

        return !!body?.providedComponentName;
    }

    return false;
};

const findAiAgentsWorkflow = (
    workflows: ConnectedUserWorkflowType[] | undefined,
    automationUuid: string
): ConnectedUserWorkflowType | undefined =>
    (workflows ?? []).find((workflow) => workflow.kind === 'REFERENCE' && workflow.automationUuid === automationUuid);

const templateComponents = (template: AutomationTemplateType): TemplateComponentType[] => [
    ...(template.triggers ?? []),
    ...(template.components ?? []),
];

const templateInputs = (template: AutomationTemplateType): TemplateInputType[] =>
    (template.inputs ?? []).filter((input) => !!input.name);

const findExistingReference = (
    workflows: ConnectedUserWorkflowType[] | undefined,
    workflowTemplateId: string
): ExistingReferenceType | undefined => {
    const reference = (workflows ?? []).find(
        (workflow) => workflow.kind === 'REFERENCE' && workflow.automationUuid === workflowTemplateId
    );

    return reference ? {connections: reference.connections ?? {}, inputValues: reference.inputValues ?? {}} : undefined;
};

interface LoadResultType {
    aiAgentsWorkflow?: ConnectedUserWorkflowType;
    error?: string;
    existingReference?: ExistingReferenceType;
    key: string;
    template?: AutomationTemplateType;
}

interface ConnectRowProps {
    component: TemplateComponentType;
    connectionCreationAllowed: boolean;
    connections: ConnectionType[];
    highlighted: boolean;
    loadFailed: boolean;
    onConnect: () => void;
    onRetry: () => void;
    onSelect: (connectionId: number) => void;
    selectedConnectionId?: number;
}

const ConnectRow = ({
    component,
    connectionCreationAllowed,
    connections,
    highlighted,
    loadFailed,
    onConnect,
    onRetry,
    onSelect,
    selectedConnectionId,
}: ConnectRowProps) => {
    const title = component.title || component.name;
    const available = component.integrationId !== null && component.integrationId !== undefined;

    return (
        <div
            className={highlighted ? styles.componentRowHighlighted : styles.componentRow}
            data-testid={`connect-row-${component.name}`}
        >
            <div className={styles.componentTitle}>
                {component.icon && (
                    <span className={styles.componentIcon} dangerouslySetInnerHTML={{__html: component.icon}} />
                )}

                <span>{title}</span>
            </div>

            {available ? (
                <div className={styles.controls}>
                    <select
                        aria-label={`${title} connection`}
                        className={styles.select}
                        onChange={(event) => onSelect(Number(event.target.value))}
                        value={selectedConnectionId === undefined ? '' : String(selectedConnectionId)}
                    >
                        <option value="" disabled>
                            Select a {title} account
                        </option>

                        {connections.map((connection) => (
                            <option key={connection.id} value={String(connection.id)}>
                                {connection.name}
                            </option>
                        ))}
                    </select>

                    {connectionCreationAllowed && (
                        <button
                            aria-label="Connect"
                            className={`${dialogStyles.buttonSecondary} ${styles.connectButton}`}
                            onClick={onConnect}
                            title={`Connect a new ${title} account`}
                            type="button"
                        >
                            +
                        </button>
                    )}
                </div>
            ) : (
                <p className={styles.notice}>{title} isn't set up yet — ask your administrator.</p>
            )}

            {available && loadFailed && (
                <div className={styles.retryRow}>
                    <p className={styles.warning}>Couldn't load your {title} accounts.</p>

                    <button className={dialogStyles.buttonSecondary} onClick={onRetry} type="button">
                        Retry
                    </button>
                </div>
            )}

            {highlighted && <p className={styles.warning}>Connect {title} to continue</p>}
        </div>
    );
};

interface ActivationBodyProps {
    connectOpenRef: RefObject<boolean>;
    baseUrl: string;
    connectionCreationAllowed: boolean;
    editable: boolean;
    environment: string;
    existingReference?: ExistingReferenceType;
    fetch: ReturnType<typeof createApiClient>['fetch'];
    initialAiAgentsWorkflow?: ConnectedUserWorkflowType;
    jwtToken: string;
    mode?: 'dark' | 'light';
    onActivated?: (workflowUuid: string) => void;
    onClose: () => void;
    onDeactivated?: (workflowTemplateId: string) => void;
    onEditWorkflow?: (workflowUuid: string) => void;
    onSaved?: (workflowUuid: string) => void;
    template: AutomationTemplateType;
}

const ActivationBody = ({
    connectOpenRef,
    baseUrl,
    connectionCreationAllowed,
    editable,
    environment,
    existingReference,
    fetch,
    initialAiAgentsWorkflow,
    jwtToken,
    mode,
    onActivated,
    onClose,
    onDeactivated,
    onEditWorkflow,
    onSaved,
    template,
}: ActivationBodyProps) => {
    const [busy, setBusy] = useState(false);
    const [confirmingDeactivate, setConfirmingDeactivate] = useState(false);
    const [connectingComponent, setConnectingComponent] = useState<TemplateComponentType>();
    const [connectionLoadFailures, setConnectionLoadFailures] = useState<Record<string, boolean>>({});
    const [connectionsByComponent, setConnectionsByComponent] = useState<Record<string, ConnectionType[]>>({});
    const [aiAgentsError, setAiAgentsError] = useState<string>();
    const [aiAgentsWorkflow, setAiAgentsWorkflow] = useState<ConnectedUserWorkflowType | undefined>(
        initialAiAgentsWorkflow
    );

    const [state, dispatch] = useReducer(activationReducer, template, (initialTemplate) =>
        initialActivationState(templateComponents(initialTemplate), templateInputs(initialTemplate), existingReference)
    );

    const toolOnly = aiAgentsWorkflow?.toolOnly === true;

    const components = useMemo(() => templateComponents(template), [template]);
    const inputs = useMemo(() => templateInputs(template), [template]);

    const providedComponents = useMemo(
        () =>
            components.filter(
                (component, index) =>
                    component.provided &&
                    component.connectionRequired &&
                    components.findIndex((candidate) => candidate.name === component.name) === index
            ),
        [components]
    );
    const providedComponentNames = useMemo(
        () => components.filter((component) => component.provided).map((component) => component.name),
        [components]
    );

    const requiredComponents = useMemo(
        () => state.requiredComponents.map((name) => components.find((component) => component.name === name) ?? {name}),
        [components, state.requiredComponents]
    );

    const componentTitle = useCallback(
        (componentName: string) =>
            components.find((component) => component.name === componentName)?.title || componentName,
        [components]
    );

    const loadConnections = useCallback(
        async (componentName: string): Promise<boolean> => {
            try {
                const connections: ConnectionType[] = await fetch(
                    `/api/embedded/v1/components/${componentName}/connections`
                );

                setConnectionsByComponent((previous) => ({...previous, [componentName]: connections ?? []}));
                setConnectionLoadFailures((previous) => ({...previous, [componentName]: false}));

                return true;
            } catch (error) {
                console.error(`Failed to load the ${componentName} connections:`, error);

                setConnectionLoadFailures((previous) => ({...previous, [componentName]: true}));

                return false;
            }
        },
        [fetch]
    );

    const rollBackProvision = useCallback(async (): Promise<boolean> => {
        try {
            await fetch(`/api/embedded/v1/automation/workflow-templates/${template.id}/provision`, {
                method: 'DELETE',
            });

            return true;
        } catch (error) {
            console.error(`Failed to undo the provisioning of template ${template.id}:`, error);

            return false;
        }
    }, [fetch, template.id]);

    const loadAiAgentsWorkflow = useCallback(async () => {
        const workflows = await fetch<ConnectedUserWorkflowType[]>('/api/embedded/v1/automation/workflows');

        setAiAgentsWorkflow(findAiAgentsWorkflow(workflows, template.id));
    }, [fetch, template.id]);

    const activate = useCallback(async () => {
        setBusy(true);

        const connections = Object.fromEntries(
            Object.entries(state.selections).filter(
                (selection): selection is [string, number] =>
                    selection[1] != null && !providedComponentNames.includes(selection[0])
            )
        );

        let activatedBefore: boolean;

        try {
            const workflows = await fetch<ConnectedUserWorkflowType[]>('/api/embedded/v1/automation/workflows');

            activatedBefore = (workflows ?? []).some(
                (workflow) => workflow.kind === 'REFERENCE' && workflow.automationUuid === template.id
            );
        } catch (error) {
            console.error('Failed to check whether the automation is already active:', error);

            dispatch({error: STATUS_UNKNOWN_MESSAGE, type: 'FAILED'});
            setBusy(false);

            return;
        }

        let provisioned = false;

        try {
            await fetch(`/api/embedded/v1/automation/workflow-templates/${template.id}/provision`, {
                body: {connections, inputs: hasEditableInputs(inputs) ? state.inputValues : undefined},
                method: 'POST',
            });

            provisioned = true;

            if (!toolOnly) {
                await fetch(`/api/embedded/v1/automation/workflows/${template.id}/enable`, {method: 'POST'});
            }
        } catch (error) {
            console.error(`Failed to activate template ${template.id}:`, error);

            const rolledBack = activatedBefore || (await rollBackProvision());

            const missingConnectionComponentName = readMissingConnectionComponentName(error);
            const providedValueLocked = isProvidedValueLockedError(error);

            if (!rolledBack) {
                dispatch({error: ROLLBACK_FAILED_MESSAGE, type: 'FAILED'});
            } else if (providedValueLocked) {
                dispatch({error: PROVIDED_VALUE_LOCKED_MESSAGE, type: 'FAILED'});
            } else if (missingConnectionComponentName) {
                dispatch({componentName: missingConnectionComponentName, type: 'MISSING_CONNECTION'});
            } else if (activatedBefore && provisioned) {
                dispatch({error: NOT_ENABLED_MESSAGE, type: 'FAILED'});
            } else {
                dispatch({error: describeError(error), type: 'FAILED'});
            }

            setBusy(false);

            return;
        }

        dispatch({type: 'ACTIVATED', workflowUuid: template.id});
        setBusy(false);

        loadAiAgentsWorkflow().catch((error: unknown) => {
            console.error('Failed to load the AI agents availability:', error);
        });

        if (onActivated) {
            runHostCallback('onActivated', () => onActivated(template.id));
        }

        onClose();
    }, [
        fetch,
        inputs,
        loadAiAgentsWorkflow,
        onActivated,
        onClose,
        providedComponentNames,
        rollBackProvision,
        state.inputValues,
        state.selections,
        template.id,
        toolOnly,
    ]);

    const save = useCallback(async () => {
        if (!existingReference) {
            return;
        }

        const changedConnections = Object.fromEntries(
            Object.entries(state.selections).filter(
                (selection): selection is [string, number] =>
                    selection[1] != null &&
                    !providedComponentNames.includes(selection[0]) &&
                    existingReference.connections[selection[0]] !== selection[1]
            )
        );

        const inputsChanged = inputs.some(
            (input) =>
                toInputValueText(state.inputValues[input.name]) !==
                toInputValueText(existingReference.inputValues[input.name])
        );

        if (Object.keys(changedConnections).length === 0 && !inputsChanged) {
            onClose();

            return;
        }

        setBusy(true);

        try {
            await fetch(`/api/embedded/v1/automation/workflow-templates/${template.id}/provision`, {
                body: {
                    connections: Object.keys(changedConnections).length > 0 ? changedConnections : undefined,
                    inputs: inputsChanged ? state.inputValues : undefined,
                },
                method: 'POST',
            });
        } catch (error) {
            console.error(`Failed to save the settings of template ${template.id}:`, error);

            const missingConnectionComponentName = readMissingConnectionComponentName(error);

            if (isProvidedValueLockedError(error)) {
                dispatch({error: PROVIDED_VALUE_LOCKED_MESSAGE, type: 'FAILED'});
            } else if (missingConnectionComponentName) {
                dispatch({componentName: missingConnectionComponentName, type: 'MISSING_CONNECTION'});
            } else {
                dispatch({error: describeError(error), type: 'FAILED'});
            }

            setBusy(false);

            return;
        }

        dispatch({type: 'ACTIVATED', workflowUuid: template.id});
        setBusy(false);

        if (onSaved) {
            runHostCallback('onSaved', () => onSaved(template.id));
        }

        onClose();
    }, [
        existingReference,
        fetch,
        inputs,
        onClose,
        onSaved,
        providedComponentNames,
        state.inputValues,
        state.selections,
        template.id,
    ]);

    const deactivate = useCallback(async () => {
        if (!confirmingDeactivate) {
            setConfirmingDeactivate(true);

            return;
        }

        setBusy(true);

        try {
            await fetch(`/api/embedded/v1/automation/workflow-templates/${template.id}/provision`, {
                method: 'DELETE',
            });
        } catch (error) {
            console.error(`Failed to deactivate template ${template.id}:`, error);

            dispatch({error: describeError(error), type: 'FAILED'});
            setConfirmingDeactivate(false);
            setBusy(false);

            return;
        }

        setBusy(false);

        if (onDeactivated) {
            runHostCallback('onDeactivated', () => onDeactivated(template.id));
        }

        onClose();
    }, [confirmingDeactivate, fetch, onClose, onDeactivated, template.id]);

    const editWorkflow = useCallback(async () => {
        setBusy(true);

        let copiedWorkflowUuid: string;

        try {
            copiedWorkflowUuid = await fetch(`/api/embedded/v1/automation/workflow-templates/${template.id}/copy`, {
                method: 'POST',
            });
        } catch (error) {
            const existingCopiedWorkflowUuid = findExistingCopiedWorkflowUuid(error);

            if (existingCopiedWorkflowUuid === undefined) {
                console.error('Failed to copy the workflow template:', error);

                dispatch({error: describeError(error), type: 'FAILED'});
                setBusy(false);

                return;
            }

            copiedWorkflowUuid = existingCopiedWorkflowUuid;
        }

        setBusy(false);

        if (onEditWorkflow) {
            runHostCallback('onEditWorkflow', () => onEditWorkflow(copiedWorkflowUuid));
        }

        onClose();
    }, [fetch, onClose, onEditWorkflow, template.id]);

    const handleConnected = useCallback(
        (componentName: string, connectionId: number | undefined) => {
            void loadConnections(componentName).then((loaded) => {
                if (loaded && connectionId !== undefined) {
                    dispatch({componentName, connectionId, type: 'SELECT_CONNECTION'});
                }
            });
        },
        [loadConnections]
    );

    const toggleAiAgents = useCallback(
        async (enabled: boolean) => {
            setAiAgentsError(undefined);
            setBusy(true);

            try {
                await fetch(`/api/embedded/v1/automation/workflows/${template.id}/mcp/enable`, {
                    method: enabled ? 'POST' : 'DELETE',
                });

                await loadAiAgentsWorkflow();
            } catch (error) {
                console.error(`Failed to change the AI agents availability of ${template.id}:`, error);

                setAiAgentsError(AI_AGENTS_SWITCH_FAILED_MESSAGE);
            }

            setBusy(false);
        },
        [fetch, loadAiAgentsWorkflow, template.id]
    );

    const nothingToEdit = !!existingReference && state.requiredComponents.length === 0 && !hasEditableInputs(inputs);

    const backShown =
        state.step !== 'connect' &&
        state.step !== 'done' &&
        (state.requiredComponents.length > 0 || (state.step === 'activate' && hasEditableInputs(inputs)));

    useEffect(() => {
        let cancelled = false;

        Promise.all(
            state.requiredComponents.map((componentName) =>
                fetch<ConnectionType[]>(`/api/embedded/v1/components/${componentName}/connections`)
                    .then((connections) => ({componentName, connections: connections ?? []}))
                    .catch((error: unknown) => {
                        console.error(`Failed to load the ${componentName} connections:`, error);

                        return {componentName, connections: undefined};
                    })
            )
        ).then((results) => {
            if (cancelled) {
                return;
            }

            setConnectionsByComponent((previous) => ({
                ...previous,
                ...Object.fromEntries(
                    results
                        .filter((result) => result.connections !== undefined)
                        .map((result) => [result.componentName, result.connections])
                ),
            }));
            setConnectionLoadFailures((previous) => ({
                ...previous,
                ...Object.fromEntries(
                    results.map((result) => [result.componentName, result.connections === undefined])
                ),
            }));
        });

        return () => {
            cancelled = true;
        };
    }, [fetch, state.requiredComponents]);

    const providedRows = providedComponents.map((component) => (
        <div className={styles.componentRow} data-testid={`provided-row-${component.name}`} key={component.name}>
            <div className={styles.componentTitle}>
                {component.icon && (
                    <span className={styles.componentIcon} dangerouslySetInnerHTML={{__html: component.icon}} />
                )}

                <span>{component.title || component.name}</span>
            </div>

            <span className={styles.notice}>{PROVIDED_BY_ADMINISTRATOR_LABEL}</span>
        </div>
    ));

    return (
        <>
            <main className={dialogStyles.dialogContent}>
                {state.error && (
                    <div className={styles.alert} role="alert">
                        {state.error}
                    </div>
                )}

                {aiAgentsError && (
                    <div className={styles.alert} role="alert">
                        {aiAgentsError}
                    </div>
                )}

                {aiAgentsWorkflow?.mcpEnabled != null && (
                    <label className={styles.switchRow}>
                        <span>{AI_AGENTS_SWITCH_LABEL}</span>

                        <input
                            checked={aiAgentsWorkflow.mcpEnabled === true}
                            className={styles.switch}
                            disabled={busy}
                            onChange={(event) => void toggleAiAgents(event.target.checked)}
                            role="switch"
                            type="checkbox"
                        />
                    </label>
                )}

                {aiAgentsWorkflow?.staticWebhookUrl && (
                    <WebhookUrl subject="automation" webhookUrl={aiAgentsWorkflow.staticWebhookUrl} />
                )}

                {state.step === 'connect' && (
                    <div className={styles.steps}>
                        <p className={styles.stepHint}>
                            {existingReference
                                ? 'Choose the accounts this automation uses.'
                                : 'Connect the accounts this automation needs.'}
                        </p>

                        {requiredComponents.map((component) => (
                            <ConnectRow
                                component={component}
                                connectionCreationAllowed={connectionCreationAllowed}
                                connections={connectionsByComponent[component.name] ?? []}
                                highlighted={state.highlightedComponent === component.name}
                                key={component.name}
                                loadFailed={connectionLoadFailures[component.name] === true}
                                onConnect={() => {
                                    connectOpenRef.current = true;

                                    setConnectingComponent(component);
                                }}
                                onRetry={() => void loadConnections(component.name)}
                                onSelect={(connectionId) =>
                                    dispatch({componentName: component.name, connectionId, type: 'SELECT_CONNECTION'})
                                }
                                selectedConnectionId={state.selections[component.name]}
                            />
                        ))}

                        {providedRows}
                    </div>
                )}

                {state.step === 'configure' && (
                    <div className={styles.steps}>
                        <p className={styles.stepHint}>
                            {existingReference
                                ? 'Change the details this automation uses.'
                                : 'This automation needs a few details before it can run.'}
                        </p>

                        {inputs.map((input) =>
                            input.componentReference ? (
                                <ComponentInputField
                                    componentReference={input.componentReference}
                                    componentTitle={componentTitle(input.componentReference.componentName)}
                                    connectionId={state.selections[input.componentReference.componentName]}
                                    fetch={fetch}
                                    key={input.name}
                                    label={input.label || input.name}
                                    name={input.name}
                                    onChange={(value) => dispatch({name: input.name, type: 'SET_INPUT_VALUE', value})}
                                    required={input.required}
                                    value={toInputValueText(state.inputValues[input.name])}
                                />
                            ) : (
                                <div className={styles.field} key={input.name}>
                                    <label className={styles.label} htmlFor={`activation-input-${input.name}`}>
                                        {input.label || input.name}

                                        {input.required && <span className={styles.required}>*</span>}
                                    </label>

                                    <input
                                        className={styles.input}
                                        id={`activation-input-${input.name}`}
                                        onChange={(event) =>
                                            dispatch({
                                                name: input.name,
                                                type: 'SET_INPUT_VALUE',
                                                value: event.target.value,
                                            })
                                        }
                                        required={input.required}
                                        type={INPUT_TYPE_ATTRIBUTES[input.type ?? ''] ?? 'text'}
                                        value={toInputValueText(state.inputValues[input.name])}
                                    />
                                </div>
                            )
                        )}
                    </div>
                )}

                {state.step === 'activate' && (
                    <div className={styles.steps}>
                        {providedRows}

                        <p className={styles.notice}>
                            {nothingToEdit
                                ? 'This automation has nothing to set up.'
                                : existingReference
                                  ? 'Save to keep your changes. Saving does not switch this automation on or off.'
                                  : toolOnly
                                    ? 'Everything is ready. Activate to save your settings.'
                                    : 'Everything is ready. Activate to switch this automation on.'}
                        </p>
                    </div>
                )}
            </main>

            <footer className={dialogStyles.dialogFooter}>
                <DialogPoweredBy />

                <div className={styles.footerActions}>
                    {state.step !== 'done' && (
                        <>
                            {existingReference && (
                                <button
                                    className={dialogStyles.buttonDestructive}
                                    disabled={busy}
                                    onClick={() => void deactivate()}
                                    type="button"
                                >
                                    {confirmingDeactivate ? 'Confirm deactivate' : 'Deactivate'}
                                </button>
                            )}

                            {onEditWorkflow && editable && (
                                <button
                                    className={dialogStyles.buttonSecondary}
                                    disabled={busy}
                                    onClick={() => void editWorkflow()}
                                    type="button"
                                >
                                    Edit workflow
                                </button>
                            )}

                            {backShown && (
                                <button
                                    className={dialogStyles.buttonSecondary}
                                    disabled={busy}
                                    onClick={() => dispatch({type: 'BACK'})}
                                    type="button"
                                >
                                    Back
                                </button>
                            )}

                            {state.step === 'activate' && nothingToEdit ? (
                                <button className={dialogStyles.buttonPrimary} onClick={onClose} type="button">
                                    Done
                                </button>
                            ) : state.step === 'activate' ? (
                                <button
                                    className={dialogStyles.buttonPrimary}
                                    disabled={busy}
                                    onClick={() => void (existingReference ? save() : activate())}
                                    type="button"
                                >
                                    {existingReference ? 'Save' : 'Activate'}
                                </button>
                            ) : (
                                <button
                                    className={dialogStyles.buttonPrimary}
                                    disabled={busy || !canProceed(state)}
                                    onClick={() => dispatch({type: 'NEXT'})}
                                    type="button"
                                >
                                    Next
                                </button>
                            )}
                        </>
                    )}
                </div>
            </footer>

            {connectingComponent && connectingComponent.integrationId != null && (
                <ConnectDialogOpener
                    baseUrl={baseUrl}
                    environment={environment}
                    integrationId={connectingComponent.integrationId}
                    jwtToken={jwtToken}
                    key={connectingComponent.name}
                    mode={mode}
                    onConnected={(connectionId) => handleConnected(connectingComponent.name, connectionId)}
                    onDone={() => {
                        setTimeout(() => {
                            connectOpenRef.current = false;
                        }, 0);

                        setConnectingComponent(undefined);
                    }}
                />
            )}
        </>
    );
};

const FOCUSABLE_SELECTOR = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]';

const ActivationDialog = ({
    baseUrl = 'https://app.bytechef.io',
    connectionCreationAllowed = true,
    environment = 'PRODUCTION',
    jwtToken,
    mode = 'light',
    onActivated,
    onClose,
    onDeactivated,
    onEditWorkflow,
    onSaved,
    workflowTemplateId,
}: ActivationDialogProps) => {
    const [loadResult, setLoadResult] = useState<LoadResultType>();

    const connectOpenRef = useRef(false);
    const containerRef = useRef<HTMLDivElement>(null);
    const onCloseRef = useRef(onClose);

    const {fetch} = useMemo(() => createApiClient(baseUrl, environment, jwtToken), [baseUrl, environment, jwtToken]);

    const darkClass = mode === 'dark' ? ` ${dialogStyles.dark}` : '';

    const loading = loadResult?.key !== workflowTemplateId;
    const template = loadResult?.template;
    const existingReference = loadResult?.existingReference;
    const loadError = loadResult?.error;

    useEffect(() => {
        let cancelled = false;

        const workflowsPromise = fetch<ConnectedUserWorkflowType[]>('/api/embedded/v1/automation/workflows').catch(
            (error: unknown) => {
                console.error('Failed to check whether the automation is already active:', error);

                return undefined;
            }
        );

        fetch<AutomationTemplateType>(
            `/api/embedded/v1/automation/workflow-templates/${encodeURIComponent(workflowTemplateId)}`
        )
            .then(async (loadedTemplate): Promise<LoadResultType> => {
                if (!loadedTemplate?.id) {
                    return {error: TEMPLATE_NOT_FOUND_MESSAGE, key: workflowTemplateId};
                }

                const workflows = await workflowsPromise;

                return {
                    aiAgentsWorkflow: findAiAgentsWorkflow(workflows, loadedTemplate.id),
                    existingReference: findExistingReference(workflows, loadedTemplate.id),
                    key: workflowTemplateId,
                    template: loadedTemplate,
                };
            })
            .catch((error: unknown) => {
                console.error('Failed to load the automation template:', error);

                return {error: describeError(error), key: workflowTemplateId};
            })
            .then((result: LoadResultType) => {
                if (!cancelled) {
                    setLoadResult(result);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [fetch, workflowTemplateId]);

    useEffect(() => {
        onCloseRef.current = onClose;
    }, [onClose]);

    useEffect(() => {
        containerRef.current?.focus();
    }, []);

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (connectOpenRef.current) {
                return;
            }

            if (event.key === 'Escape') {
                onCloseRef.current();

                return;
            }

            const container = containerRef.current;

            if (event.key !== 'Tab' || !container) {
                return;
            }

            const focusableElements = Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
                (element) => element.tabIndex >= 0
            );

            if (focusableElements.length === 0) {
                event.preventDefault();

                return;
            }

            const firstElement = focusableElements[0];
            const lastElement = focusableElements[focusableElements.length - 1];
            const focusInside = container.contains(document.activeElement);

            if (event.shiftKey && (!focusInside || document.activeElement === firstElement)) {
                event.preventDefault();

                lastElement.focus();
            } else if (!event.shiftKey && (!focusInside || document.activeElement === lastElement)) {
                event.preventDefault();

                firstElement.focus();
            }
        };

        window.addEventListener('keydown', handleKeyDown);

        return () => window.removeEventListener('keydown', handleKeyDown);
    }, []);

    return (
        <div
            className={`${dialogStyles.dialogOverlay}${darkClass}`}
            data-testid="activation-dialog-overlay"
            onClick={onClose}
        >
            <div
                aria-labelledby="bytechef-activation-dialog-title"
                aria-modal="true"
                className={`${dialogStyles.dialogContainer}${darkClass}`}
                onClick={(event) => event.stopPropagation()}
                ref={containerRef}
                role="dialog"
                tabIndex={-1}
            >
                <header className={dialogStyles.dialogHeader}>
                    <div>
                        <h2 id="bytechef-activation-dialog-title">
                            {template?.label ?? (existingReference ? 'Edit automation' : 'Activate automation')}
                        </h2>
                    </div>

                    <button
                        aria-label="Close Dialog"
                        className={dialogStyles.closeButton}
                        onClick={onClose}
                        type="button"
                    >
                        <img src={XIcon} alt="" />
                    </button>
                </header>

                {loading ? (
                    <main className={dialogStyles.dialogContentFallback}>
                        <p>Loading...</p>
                    </main>
                ) : template ? (
                    <ActivationBody
                        connectOpenRef={connectOpenRef}
                        baseUrl={baseUrl}
                        connectionCreationAllowed={connectionCreationAllowed}
                        editable={template.kind !== 'REFERENCE'}
                        environment={environment}
                        existingReference={existingReference}
                        fetch={fetch}
                        initialAiAgentsWorkflow={loadResult?.aiAgentsWorkflow}
                        jwtToken={jwtToken}
                        mode={mode}
                        onActivated={onActivated}
                        onClose={onClose}
                        onDeactivated={onDeactivated}
                        onEditWorkflow={onEditWorkflow}
                        onSaved={onSaved}
                        key={template.id}
                        template={template}
                    />
                ) : (
                    <main className={dialogStyles.dialogContentFallback}>
                        <p>{loadError ?? TEMPLATE_NOT_FOUND_MESSAGE}</p>

                        <button className={dialogStyles.buttonSecondary} onClick={onClose} type="button">
                            Close
                        </button>
                    </main>
                )}
            </div>
        </div>
    );
};

export default ActivationDialog;
