import {RefObject, useCallback, useEffect, useMemo, useReducer, useRef, useState} from 'react';
import {ApiResponseError, createApiClient} from '../shared/apiClient';
import dialogStyles from '../shared/dialog.module.css';
import XIcon from '../connect-dialog/assets/x.svg';
import {activationReducer, canProceed, initialActivationState, toInputValueText} from './activationReducer';
import ConnectDialogOpener from './ConnectDialogOpener';
import styles from './ActivationDialog.module.css';
import {
    AutomationProjectType,
    AutomationTemplateType,
    ConnectedUserWorkflowType,
    ConnectionType,
    MissingConnectionErrorType,
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
    onEditWorkflow?: (workflowUuid: string) => void;
    workflowTemplateId: string;
}

const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.';
const TEMPLATE_NOT_FOUND_MESSAGE = 'This automation could not be found.';
const SESSION_EXPIRED_MESSAGE = 'Your session has expired. Reload the page and try again.';
const FORBIDDEN_MESSAGE = "You don't have permission to do this.";
const STATUS_UNKNOWN_MESSAGE = 'Could not check your automations. Please try again.';
const ROLLBACK_FAILED_MESSAGE =
    'Activation failed and could not be fully undone. Check your automations before trying again.';
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

const findTemplate = (
    projects: AutomationProjectType[],
    workflowTemplateId: string
): {kind?: AutomationProjectType['kind']; template: AutomationTemplateType} | undefined => {
    for (const project of projects) {
        const template = project.workflowTemplates?.find(
            (workflowTemplate) => workflowTemplate.id === workflowTemplateId
        );

        if (template) {
            return {kind: project.kind, template};
        }
    }

    return undefined;
};

const templateComponents = (template: AutomationTemplateType): TemplateComponentType[] => [
    ...(template.triggers ?? []),
    ...(template.components ?? []),
];

const templateInputs = (template: AutomationTemplateType): TemplateInputType[] =>
    (template.inputs ?? []).filter((input) => !!input.name);

interface LoadResultType {
    error?: string;
    key: string;
    kind?: AutomationProjectType['kind'];
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
                        <button className={dialogStyles.buttonSecondary} onClick={onConnect} type="button">
                            Connect
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
    fetch: ReturnType<typeof createApiClient>['fetch'];
    jwtToken: string;
    mode?: 'dark' | 'light';
    onActivated?: (workflowUuid: string) => void;
    onClose: () => void;
    onEditWorkflow?: (workflowUuid: string) => void;
    template: AutomationTemplateType;
}

const ActivationBody = ({
    connectOpenRef,
    baseUrl,
    connectionCreationAllowed,
    editable,
    environment,
    fetch,
    jwtToken,
    mode,
    onActivated,
    onClose,
    onEditWorkflow,
    template,
}: ActivationBodyProps) => {
    const [busy, setBusy] = useState(false);
    const [connectingComponent, setConnectingComponent] = useState<TemplateComponentType>();
    const [connectionLoadFailures, setConnectionLoadFailures] = useState<Record<string, boolean>>({});
    const [connectionsByComponent, setConnectionsByComponent] = useState<Record<string, ConnectionType[]>>({});

    const [state, dispatch] = useReducer(activationReducer, template, (initialTemplate) =>
        initialActivationState(templateComponents(initialTemplate), templateInputs(initialTemplate))
    );

    const components = useMemo(() => templateComponents(template), [template]);
    const inputs = useMemo(() => templateInputs(template), [template]);

    const requiredComponents = useMemo(
        () => state.requiredComponents.map((name) => components.find((component) => component.name === name) ?? {name}),
        [components, state.requiredComponents]
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

    const activate = useCallback(async () => {
        setBusy(true);

        const connections = Object.fromEntries(
            Object.entries(state.selections).filter((selection): selection is [string, number] => selection[1] != null)
        );

        let activatedBefore: boolean;

        try {
            const workflows = await fetch<ConnectedUserWorkflowType[]>('/api/embedded/v1/automation/workflows');

            activatedBefore = (workflows ?? []).some(
                (workflow) => workflow.kind === 'REFERENCE' && workflow.automationWorkflowUuid === template.id
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
                body: {connections, inputs: inputs.length > 0 ? state.inputValues : undefined},
                method: 'POST',
            });

            provisioned = true;

            await fetch(`/api/embedded/v1/automation/workflows/${template.id}/enable`, {method: 'POST'});
        } catch (error) {
            console.error(`Failed to activate template ${template.id}:`, error);

            const rolledBack = activatedBefore || (await rollBackProvision());

            const missingConnectionComponentName = readMissingConnectionComponentName(error);

            if (!rolledBack) {
                dispatch({error: ROLLBACK_FAILED_MESSAGE, type: 'FAILED'});
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

        if (onActivated) {
            runHostCallback('onActivated', () => onActivated(template.id));
        }
    }, [fetch, inputs.length, onActivated, rollBackProvision, state.inputValues, state.selections, template.id]);

    const editWorkflow = useCallback(async () => {
        setBusy(true);

        let copiedWorkflowUuid: string;

        try {
            copiedWorkflowUuid = await fetch(`/api/embedded/v1/automation/workflow-templates/${template.id}/copy`, {
                method: 'POST',
            });
        } catch (error) {
            console.error('Failed to copy the workflow template:', error);

            dispatch({error: describeError(error), type: 'FAILED'});
            setBusy(false);

            return;
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

    const backShown =
        state.step !== 'connect' &&
        state.step !== 'done' &&
        (state.requiredComponents.length > 0 || (state.step === 'activate' && inputs.length > 0));

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

    return (
        <>
            <main className={dialogStyles.dialogContent}>
                {state.error && (
                    <div className={styles.alert} role="alert">
                        {state.error}
                    </div>
                )}

                {state.step === 'connect' && (
                    <div className={styles.steps}>
                        <p className={styles.stepHint}>Connect the accounts this automation needs.</p>

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
                    </div>
                )}

                {state.step === 'configure' && (
                    <div className={styles.steps}>
                        <p className={styles.stepHint}>This automation needs a few details before it can run.</p>

                        {inputs.map((input) => (
                            <div className={styles.field} key={input.name}>
                                <label className={styles.label} htmlFor={`activation-input-${input.name}`}>
                                    {input.label || input.name}

                                    {input.required && <span className={styles.required}>*</span>}
                                </label>

                                <input
                                    className={styles.input}
                                    id={`activation-input-${input.name}`}
                                    onChange={(event) =>
                                        dispatch({name: input.name, type: 'SET_INPUT_VALUE', value: event.target.value})
                                    }
                                    required={input.required}
                                    type={INPUT_TYPE_ATTRIBUTES[input.type ?? ''] ?? 'text'}
                                    value={toInputValueText(state.inputValues[input.name])}
                                />
                            </div>
                        ))}
                    </div>
                )}

                {state.step === 'activate' && (
                    <p className={styles.notice}>Everything is ready. Activate to switch this automation on.</p>
                )}

                {state.step === 'done' && <p className={styles.notice}>{template.label || 'This automation'} is on.</p>}
            </main>

            <footer className={dialogStyles.dialogFooter}>
                <div className={styles.footerActions}>
                    {state.step === 'done' ? (
                        <button className={dialogStyles.buttonPrimary} onClick={onClose} type="button">
                            Done
                        </button>
                    ) : (
                        <>
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

                            {state.step === 'activate' ? (
                                <button
                                    className={dialogStyles.buttonPrimary}
                                    disabled={busy}
                                    onClick={() => void activate()}
                                    type="button"
                                >
                                    Activate
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
    onEditWorkflow,
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
    const loadError = loadResult?.error;

    useEffect(() => {
        let cancelled = false;

        fetch<AutomationProjectType[]>('/api/embedded/v1/automation/projects')
            .then((projects) => {
                const found = findTemplate(projects, workflowTemplateId);

                return found
                    ? {key: workflowTemplateId, kind: found.kind, template: found.template}
                    : {error: TEMPLATE_NOT_FOUND_MESSAGE, key: workflowTemplateId};
            })
            .catch((error: unknown) => {
                console.error('Failed to load the automation templates:', error);

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
                        <h2 id="bytechef-activation-dialog-title">{template?.label ?? 'Activate automation'}</h2>
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
                        editable={loadResult?.kind !== 'REFERENCE'}
                        environment={environment}
                        fetch={fetch}
                        jwtToken={jwtToken}
                        mode={mode}
                        onActivated={onActivated}
                        onClose={onClose}
                        onEditWorkflow={onEditWorkflow}
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
