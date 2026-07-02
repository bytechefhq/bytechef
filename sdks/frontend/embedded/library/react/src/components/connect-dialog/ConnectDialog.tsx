import {useEffect, useRef, useState} from 'react';
import Logo from './assets/logo.svg';
import XIcon from './assets/x.svg';
import SquareArrowOutUpRightIcon from './assets/square-arrow-out-up-right.svg';
import styles from './styles.module.css';
import {
    ApiFetch,
    BoundFieldMappingIntegrationFieldArgsType,
    BoundFieldMappingObjectListArgsType,
    ComponentPropertyGroupType,
    FieldMappingValueType,
    FormType,
    IntegrationType,
    MapObjectFieldsType,
    MergedMcpToolType,
    MergedWorkflowType,
    OptionType,
    PropertyType,
    RegisterFormSubmitFunction,
    WorkflowInputType,
} from './types';
import type {ExecuteActionFunction} from './useExecuteAction';
import {optionsCacheKey, stableSerialize} from './utils';
import useWorkflowInputOptions from './useWorkflowInputOptions';
import FieldMappingField from './FieldMappingField';

type LoadWorkflowInputOptionsFunction = (
    componentName: string,
    componentVersion: number,
    groupName: string,
    propertyName: string,
    dependencyValues: Record<string, unknown>
) => void;

type HandleWorkflowGroupInputChangeFunction = (
    workflowUuid: string,
    groupName: string,
    memberName: string,
    value: string
) => void;

interface ToggleProps {
    id: string;
    pressed: boolean;
    onPressedChange: (pressed: boolean) => void;
}

const Toggle = ({id, pressed, onPressedChange}: ToggleProps) => (
    <>
        <input
            className={styles.toggleCheckbox}
            id={`react-switch-${id}`}
            type="checkbox"
            checked={pressed}
            onChange={(event) => onPressedChange(event.target.checked)}
        />

        <label
            className={styles.toggleLabel}
            htmlFor={`react-switch-${id}`}
            style={{
                backgroundColor: pressed ? '#1071e5' : '#e2e8f0',
            }}
        >
            <span className={styles.toggleButton} />
        </label>
    </>
);

interface DialogProps {
    apiFetch?: ApiFetch;
    closeDialog: () => void;
    executeAction?: ExecuteActionFunction;
    workflowsView?: boolean;
    form?: FormType;
    handleClick: (event: React.MouseEvent<HTMLButtonElement>) => void;
    handleMcpToolToggle?: (mcpToolId: number, pressed: boolean) => void;
    handleMcpWorkflowToggle?: (workflowUuid: string, pressed: boolean) => void;
    handleMcpWorkflowInputChange?: (workflowUuid: string, inputName: string, value: unknown) => void;
    handleMcpWorkflowGroupInputChange?: HandleWorkflowGroupInputChangeFunction;
    handleWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleWorkflowInputChange: (workflowUuid: string, inputName: string, value: unknown) => void;
    handleWorkflowGroupInputChange?: HandleWorkflowGroupInputChangeFunction;
    integration?: IntegrationType;
    integrationInstanceId?: number;
    isOAuth2?: boolean;
    isOpen: boolean;
    loading?: boolean;
    mapObjectFields?: MapObjectFieldsType;
    mergedMcpTools?: MergedMcpToolType[];
    mergedMcpWorkflows?: MergedWorkflowType[];
    mergedWorkflows: MergedWorkflowType[];
    properties?: PropertyType[];
    registerFormSubmit?: RegisterFormSubmitFunction;
}

const ConnectDialog = ({
    apiFetch,
    closeDialog,
    executeAction,
    workflowsView = false,
    form,
    handleClick,
    handleMcpToolToggle = () => {},
    handleMcpWorkflowToggle = () => {},
    handleMcpWorkflowInputChange = () => {},
    handleMcpWorkflowGroupInputChange = () => {},
    handleWorkflowToggle,
    handleWorkflowInputChange,
    handleWorkflowGroupInputChange = () => {},
    integration,
    integrationInstanceId,
    isOAuth2 = false,
    isOpen,
    loading = false,
    mapObjectFields,
    mergedMcpTools = [],
    mergedMcpWorkflows = [],
    mergedWorkflows,
    properties,
    registerFormSubmit,
}: DialogProps) => {
    // Option reset on integration switch is handled inside useWorkflowInputOptions (keyed on integrationInstanceId),
    // which resets during render — before the new fields load — instead of in an effect that would race the fetch.
    const {loadOptions, optionsByKey} = useWorkflowInputOptions(apiFetch, integrationInstanceId);

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                closeDialog();
            }
        };

        if (isOpen) {
            window.addEventListener('keydown', handleKeyDown);
        }

        return () => {
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [isOpen, closeDialog]);

    if (!isOpen) {
        return null;
    }

    return (
        <div className={styles.dialogOverlay} data-testid="dialog-overlay" onClick={closeDialog}>
            <div className={styles.dialogContainer} onClick={(event) => event.stopPropagation()}>
                <DialogHeader closeDialog={closeDialog} integration={integration} />

                {loading ? (
                    <main className={styles.dialogContentFallback}>
                        <p>Loading...</p>
                    </main>
                ) : integration ? (
                    <DialogContent
                        closeDialog={closeDialog}
                        executeAction={executeAction}
                        workflowsView={workflowsView}
                        form={form}
                        handleMcpToolToggle={handleMcpToolToggle}
                        handleMcpWorkflowToggle={handleMcpWorkflowToggle}
                        handleMcpWorkflowInputChange={handleMcpWorkflowInputChange}
                        handleMcpWorkflowGroupInputChange={handleMcpWorkflowGroupInputChange}
                        handleWorkflowToggle={handleWorkflowToggle}
                        handleWorkflowInputChange={handleWorkflowInputChange}
                        handleWorkflowGroupInputChange={handleWorkflowGroupInputChange}
                        integration={integration}
                        loadWorkflowInputOptions={loadOptions}
                        mapObjectFields={mapObjectFields}
                        mergedMcpTools={mergedMcpTools}
                        mergedMcpWorkflows={mergedMcpWorkflows}
                        mergedWorkflows={mergedWorkflows}
                        properties={properties}
                        registerFormSubmit={registerFormSubmit}
                        workflowInputOptions={optionsByKey}
                    />
                ) : (
                    <main className={styles.dialogContentFallback}>
                        <h2>Unable to Load Integration</h2>

                        <p>We couldn't load the integration data. Please try again later.</p>

                        <button className={styles.buttonSecondary} onClick={closeDialog}>
                            Close
                        </button>
                    </main>
                )}

                {integration && !loading && (
                    <DialogFooter workflowsView={workflowsView} handleClick={handleClick} isOAuth2={isOAuth2} />
                )}

                <DialogPoweredBy />
            </div>
        </div>
    );
};

interface DialogHeaderProps {
    closeDialog: () => void;
    integration?: IntegrationType;
}

const DialogHeader = ({closeDialog, integration}: DialogHeaderProps) => (
    <header className={styles.dialogHeader}>
        <div>
            {integration && integration.icon && (
                <div className={styles.integrationIcon} dangerouslySetInnerHTML={{__html: integration.icon}} />
            )}

            <h2>{integration?.name}</h2>
        </div>

        <button aria-label="Close Dialog" className={styles.closeButton} onClick={closeDialog}>
            <img src={XIcon} />
        </button>
    </header>
);

interface DialogWorkflowsContainerProps {
    executeAction?: ExecuteActionFunction;
    handleWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleWorkflowInputChange: (workflowUuid: string, inputName: string, value: unknown) => void;
    handleWorkflowGroupInputChange: HandleWorkflowGroupInputChangeFunction;
    loadWorkflowInputOptions: LoadWorkflowInputOptionsFunction;
    mapObjectFields?: MapObjectFieldsType;
    mergedWorkflows: MergedWorkflowType[];
    workflowInputOptions: Record<string, OptionType[]>;
}

const DialogWorkflowsContainer = ({
    executeAction,
    handleWorkflowToggle,
    handleWorkflowInputChange,
    handleWorkflowGroupInputChange,
    loadWorkflowInputOptions,
    mapObjectFields,
    mergedWorkflows,
    workflowInputOptions,
}: DialogWorkflowsContainerProps) => {
    return (
        <div data-testid="workflows-container" className={styles.workflowsContainer}>
            {mergedWorkflows?.length > 0 ? (
                <p>Enable, disable and manage your workflows below</p>
            ) : (
                <p>No workflows available for this integration.</p>
            )}

            <ul className={styles.workflowsList}>
                {mergedWorkflows.map((mergedWorkflow) => {
                    const {enabled = false, inputs, workflowUuid, label} = mergedWorkflow;

                    return (
                        <li key={workflowUuid}>
                            <div>
                                <span>{label}</span>

                                <Toggle
                                    id={workflowUuid}
                                    pressed={enabled}
                                    onPressedChange={(pressed) => handleWorkflowToggle(workflowUuid, pressed)}
                                />
                            </div>

                            {enabled && (
                                <div className={styles.workflowInputsContainer}>
                                    <span>INPUTS</span>

                                    {inputs?.length === 0 ? (
                                        <p className={styles.noInputsMessage}>No inputs defined for this workflow.</p>
                                    ) : (
                                        <ul>
                                            {inputs
                                                ?.filter((input: WorkflowInputType) => !input.internalOnly)
                                                .map((input: WorkflowInputType) => (
                                                    <li key={input.name}>
                                                        {renderWorkflowInput({
                                                            executeAction,
                                                            handleInputChange: handleWorkflowInputChange,
                                                            handleWorkflowGroupInputChange,
                                                            input,
                                                            loadWorkflowInputOptions,
                                                            mapObjectFields,
                                                            workflowInputOptions,
                                                            workflowUuid,
                                                        })}
                                                    </li>
                                                ))}
                                        </ul>
                                    )}
                                </div>
                            )}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

interface DialogToolsContainerProps {
    handleMcpToolToggle: (mcpToolId: number, pressed: boolean) => void;
    handleMcpWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleMcpWorkflowInputChange: (workflowUuid: string, inputName: string, value: unknown) => void;
    handleWorkflowGroupInputChange: HandleWorkflowGroupInputChangeFunction;
    loadWorkflowInputOptions: LoadWorkflowInputOptionsFunction;
    mergedMcpTools: MergedMcpToolType[];
    mergedMcpWorkflows: MergedWorkflowType[];
    workflowInputOptions: Record<string, OptionType[]>;
}

const DialogToolsContainer = ({
    handleMcpToolToggle,
    handleMcpWorkflowToggle,
    handleMcpWorkflowInputChange,
    handleWorkflowGroupInputChange,
    loadWorkflowInputOptions,
    mergedMcpTools,
    mergedMcpWorkflows,
    workflowInputOptions,
}: DialogToolsContainerProps) => {
    return (
        <div data-testid="tools-container" className={styles.workflowsContainer}>
            {mergedMcpTools.length === 0 && mergedMcpWorkflows.length === 0 ? (
                <p>No tools available for this integration.</p>
            ) : (
                <p>Enable, disable and manage your tools below</p>
            )}

            {mergedMcpTools.length > 0 && (
                <ul className={styles.workflowsList}>
                    {mergedMcpTools.map((mcpTool) => (
                        <li key={mcpTool.id}>
                            <div>
                                <span>{mcpTool.label || mcpTool.name}</span>

                                <Toggle
                                    id={`mcp-tool-${mcpTool.id}`}
                                    pressed={mcpTool.enabled ?? false}
                                    onPressedChange={(pressed) => handleMcpToolToggle(mcpTool.id, pressed)}
                                />
                            </div>
                        </li>
                    ))}
                </ul>
            )}

            {mergedMcpWorkflows.length > 0 && (
                <ul className={styles.workflowsList}>
                    {mergedMcpWorkflows.map((mergedWorkflow) => {
                        const {enabled = false, inputs, label, workflowUuid} = mergedWorkflow;

                        return (
                            <li key={workflowUuid}>
                                <div>
                                    <span>{label}</span>

                                    <Toggle
                                        id={`mcp-workflow-${workflowUuid}`}
                                        pressed={enabled}
                                        onPressedChange={(pressed) => handleMcpWorkflowToggle(workflowUuid, pressed)}
                                    />
                                </div>

                                {enabled && (
                                    <div className={styles.workflowInputsContainer}>
                                        <span>INPUTS</span>

                                        {inputs?.length === 0 ? (
                                            <p className={styles.noInputsMessage}>
                                                No inputs defined for this workflow.
                                            </p>
                                        ) : (
                                            <ul>
                                                {inputs
                                                    ?.filter((input: WorkflowInputType) => !input.internalOnly)
                                                    .map((input: WorkflowInputType) => (
                                                        <li key={input.name}>
                                                            {renderWorkflowInput({
                                                                handleInputChange: handleMcpWorkflowInputChange,
                                                                handleWorkflowGroupInputChange,
                                                                input,
                                                                loadWorkflowInputOptions,
                                                                workflowInputOptions,
                                                                workflowUuid,
                                                            })}
                                                        </li>
                                                    ))}
                                            </ul>
                                        )}
                                    </div>
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
};

interface DialogContentProps {
    closeDialog: () => void;
    executeAction?: ExecuteActionFunction;
    workflowsView?: boolean;
    form?: FormType;
    handleMcpToolToggle: (mcpToolId: number, pressed: boolean) => void;
    handleMcpWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleMcpWorkflowInputChange: (workflowUuid: string, inputName: string, value: unknown) => void;
    handleMcpWorkflowGroupInputChange?: HandleWorkflowGroupInputChangeFunction;
    handleWorkflowToggle: (workflowUuid: string, pressed: boolean) => void;
    handleWorkflowInputChange: (workflowUuid: string, inputName: string, value: unknown) => void;
    handleWorkflowGroupInputChange?: HandleWorkflowGroupInputChangeFunction;
    integration: IntegrationType;
    loadWorkflowInputOptions?: LoadWorkflowInputOptionsFunction;
    mapObjectFields?: MapObjectFieldsType;
    mergedMcpTools?: MergedMcpToolType[];
    mergedMcpWorkflows?: MergedWorkflowType[];
    mergedWorkflows: MergedWorkflowType[];
    properties?: PropertyType[];
    registerFormSubmit?: RegisterFormSubmitFunction;
    workflowInputOptions?: Record<string, OptionType[]>;
}

type TabType = 'tools' | 'workflows';

const DialogContent = ({
    executeAction,
    workflowsView = false,
    form,
    handleMcpToolToggle,
    handleMcpWorkflowToggle,
    handleMcpWorkflowInputChange,
    handleMcpWorkflowGroupInputChange = () => {},
    handleWorkflowToggle,
    handleWorkflowInputChange,
    handleWorkflowGroupInputChange = () => {},
    integration,
    loadWorkflowInputOptions = () => {},
    mapObjectFields,
    mergedMcpTools = [],
    mergedMcpWorkflows = [],
    mergedWorkflows,
    properties,
    registerFormSubmit,
    workflowInputOptions = {},
}: DialogContentProps) => {
    const [activeTab, setActiveTab] = useState<TabType>('workflows');

    const hasMcpContent = mergedMcpTools.length > 0 || mergedMcpWorkflows.length > 0;
    const showTabs = mergedWorkflows.length > 0 && hasMcpContent;

    // Register the form's submit handler when the component mounts
    useEffect(() => {
        if (registerFormSubmit && form) {
            registerFormSubmit(form.handleSubmit);
        }
    }, [registerFormSubmit, form]);

    return (
        <main className={styles.dialogContent}>
            {!workflowsView && <p>{integration.description}</p>}

            {!workflowsView && form && (
                <form id="form">
                    {properties?.map((property) => {
                        const field = form.register(property.name);

                        const error = form.formState.errors[property.name];

                        return (
                            <DialogInputField
                                key={property.name}
                                label={property.label}
                                name={property.name}
                                options={property.options}
                                placeholder={property.placeholder}
                                required={property.required}
                                field={field}
                                error={error}
                            />
                        );
                    })}
                </form>
            )}

            {workflowsView && showTabs && (
                <div className={styles.tabContainer} role="tablist">
                    <button
                        aria-selected={activeTab === 'workflows'}
                        className={`${styles.tabButton} ${activeTab === 'workflows' ? styles.tabButtonActive : ''}`}
                        onClick={() => setActiveTab('workflows')}
                        role="tab"
                        type="button"
                    >
                        Workflows
                    </button>

                    <button
                        aria-selected={activeTab === 'tools'}
                        className={`${styles.tabButton} ${activeTab === 'tools' ? styles.tabButtonActive : ''}`}
                        onClick={() => setActiveTab('tools')}
                        role="tab"
                        type="button"
                    >
                        Tools
                    </button>
                </div>
            )}

            {workflowsView && (!showTabs || activeTab === 'workflows') && (
                <DialogWorkflowsContainer
                    executeAction={executeAction}
                    handleWorkflowToggle={handleWorkflowToggle}
                    handleWorkflowInputChange={handleWorkflowInputChange}
                    handleWorkflowGroupInputChange={handleWorkflowGroupInputChange}
                    loadWorkflowInputOptions={loadWorkflowInputOptions}
                    mapObjectFields={mapObjectFields}
                    mergedWorkflows={mergedWorkflows}
                    workflowInputOptions={workflowInputOptions}
                />
            )}

            {workflowsView && (!showTabs || activeTab === 'tools') && hasMcpContent && (
                <DialogToolsContainer
                    handleMcpToolToggle={handleMcpToolToggle}
                    handleMcpWorkflowToggle={handleMcpWorkflowToggle}
                    handleMcpWorkflowInputChange={handleMcpWorkflowInputChange}
                    handleWorkflowGroupInputChange={handleMcpWorkflowGroupInputChange}
                    loadWorkflowInputOptions={loadWorkflowInputOptions}
                    mergedMcpTools={mergedMcpTools}
                    mergedMcpWorkflows={mergedMcpWorkflows}
                    workflowInputOptions={workflowInputOptions}
                />
            )}
        </main>
    );
};

interface DialogFooterProps {
    workflowsView?: boolean;
    handleClick: (event: React.MouseEvent<HTMLButtonElement>) => void;
    isOAuth2?: boolean;
}

const DialogFooter = ({workflowsView = false, handleClick, isOAuth2 = false}: DialogFooterProps) => (
    <footer className={styles.dialogFooter}>
        {workflowsView && (
            <button name="disconnectButton" onClick={handleClick} className={styles.buttonDestructive} type="button">
                Disconnect
            </button>
        )}

        {!workflowsView && (
            <button autoFocus onClick={handleClick} className={styles.buttonPrimary} type="button">
                {isOAuth2 ? (
                    <span>
                        Authorize
                        <img data-testid="authorize-icon" src={SquareArrowOutUpRightIcon} />
                    </span>
                ) : (
                    'Connect'
                )}
            </button>
        )}
    </footer>
);

const DialogPoweredBy = () => (
    <div className={styles.poweredByContainer}>
        <img src={Logo} alt="ByteChef Logo" />

        <span>
            Powered by
            <a href="https://bytechef.io" target="_blank" rel="noopener noreferrer">
                ByteChef
            </a>
        </span>
    </div>
);

const hasUnsatisfiedDependencies = (dependencyValues: Record<string, unknown>): boolean =>
    Object.values(dependencyValues).some((value) => value === undefined || value === null || value === '');

interface DialogDynamicSelectFieldProps {
    dependencyValues: Record<string, unknown>;
    label: string;
    loadOptions: (dependencyValues: Record<string, unknown>) => void;
    name: string;
    onChange: (value: string) => void;
    options?: OptionType[];
    required?: boolean;
    value?: string;
}

const DialogDynamicSelectField = ({
    dependencyValues,
    label,
    loadOptions,
    name,
    onChange,
    options,
    required,
    value,
}: DialogDynamicSelectFieldProps) => {
    // `loadOptions` and `dependencyValues` are recreated on every parent render, so depending on them directly would
    // refire the load effect each render. The refs always expose the latest values (synced by the dependency-free
    // effect below, which runs before the load effect) while the serialized key keeps the load effect firing only
    // when the dependency contents actually change.
    const dependencyValuesRef = useRef(dependencyValues);
    const loadOptionsRef = useRef(loadOptions);

    const dependencyValuesKey = stableSerialize(dependencyValues);
    const dependenciesUnsatisfied = hasUnsatisfiedDependencies(dependencyValues);
    const hasOptions = options !== undefined && options.length > 0;

    useEffect(() => {
        dependencyValuesRef.current = dependencyValues;
        loadOptionsRef.current = loadOptions;
    });

    useEffect(() => {
        if (!dependenciesUnsatisfied) {
            loadOptionsRef.current(dependencyValuesRef.current);
        }
    }, [dependencyValuesKey, dependenciesUnsatisfied]);

    return (
        <fieldset className={styles.dialogInputField}>
            <label htmlFor={name}>
                {label}

                {required && <span className={styles.requiredIndicator}>*</span>}
            </label>

            {dependenciesUnsatisfied ? (
                <select id={name} disabled value="">
                    <option value="">Select dependencies first</option>
                </select>
            ) : hasOptions ? (
                <select id={name} value={value ?? ''} onChange={(event) => onChange(event.target.value)}>
                    <option value="">Select {label}</option>

                    {options.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>
            ) : (
                <select id={name} disabled value="">
                    <option value="">No options</option>
                </select>
            )}
        </fieldset>
    );
};

interface DialogGroupFieldProps {
    componentName: string;
    componentVersion: number;
    group: ComponentPropertyGroupType;
    groupName: string;
    handleWorkflowGroupInputChange: HandleWorkflowGroupInputChangeFunction;
    inputName: string;
    loadWorkflowInputOptions: LoadWorkflowInputOptionsFunction;
    memberValues: Record<string, unknown>;
    workflowInputOptions: Record<string, OptionType[]>;
    workflowUuid: string;
}

const DialogGroupField = ({
    componentName,
    componentVersion,
    group,
    groupName,
    handleWorkflowGroupInputChange,
    inputName,
    loadWorkflowInputOptions,
    memberValues,
    workflowInputOptions,
    workflowUuid,
}: DialogGroupFieldProps) => (
    <fieldset className={styles.workflowInputsContainer}>
        {/* A single-property group's title just duplicates its lone member's label (e.g. Slack's "channel" group
            with one "Channel" property), so only show the group title when the group bundles multiple members. */}
        {(group.properties?.length ?? 0) > 1 && <legend>{group.label ?? group.name}</legend>}

        {group.properties?.map((member) => {
            if (member.dynamicOptions) {
                const dependencyValues = collectDependencyValues(member.optionsLookupDependsOn, memberValues);

                return (
                    <DialogDynamicSelectField
                        key={member.name}
                        dependencyValues={dependencyValues}
                        label={member.label ?? member.name}
                        loadOptions={(dependencies) =>
                            loadWorkflowInputOptions(
                                componentName,
                                componentVersion,
                                groupName,
                                member.name,
                                dependencies
                            )
                        }
                        name={`${group.name}.${member.name}`}
                        onChange={(value) =>
                            handleWorkflowGroupInputChange(workflowUuid, inputName, member.name, value)
                        }
                        options={
                            workflowInputOptions[
                                optionsCacheKey(
                                    componentName,
                                    componentVersion,
                                    groupName,
                                    member.name,
                                    dependencyValues
                                )
                            ]
                        }
                        required={member.required}
                        value={memberValues[member.name] as string | undefined}
                    />
                );
            }

            return (
                <DialogInputField
                    key={member.name}
                    onChange={(event) =>
                        handleWorkflowGroupInputChange(workflowUuid, inputName, member.name, event.target.value)
                    }
                    label={member.label ?? member.name}
                    name={`${group.name}.${member.name}`}
                    options={member.options?.map((option) => option.value)}
                    required={member.required}
                    field={{value: memberValues[member.name] as string | undefined}}
                />
            );
        })}
    </fieldset>
);

const collectDependencyValues = (
    dependencyNames: string[] | undefined,
    siblingValues: Record<string, unknown>
): Record<string, unknown> => {
    const dependencyValues: Record<string, unknown> = {};

    dependencyNames?.forEach((dependencyName) => {
        dependencyValues[dependencyName] = siblingValues[dependencyName];
    });

    return dependencyValues;
};

interface RenderWorkflowInputArgs {
    executeAction?: ExecuteActionFunction;
    handleInputChange: (workflowUuid: string, inputName: string, value: unknown) => void;
    handleWorkflowGroupInputChange: HandleWorkflowGroupInputChangeFunction;
    input: WorkflowInputType;
    loadWorkflowInputOptions: LoadWorkflowInputOptionsFunction;
    mapObjectFields?: MapObjectFieldsType;
    workflowInputOptions: Record<string, OptionType[]>;
    workflowUuid: string;
}

const renderWorkflowInput = ({
    executeAction,
    handleInputChange,
    handleWorkflowGroupInputChange,
    input,
    loadWorkflowInputOptions,
    mapObjectFields,
    workflowInputOptions,
    workflowUuid,
}: RenderWorkflowInputArgs) => {
    if (input.type === 'field_mapping') {
        const objectName = input.objectName ?? input.name;
        const rawConfig = mapObjectFields?.[objectName];

        if (!rawConfig || !executeAction) {
            return null;
        }

        const config = {
            ...rawConfig,
            integrationFields: {
                get: (args: BoundFieldMappingIntegrationFieldArgsType) =>
                    rawConfig.integrationFields.get({...args, executeAction}),
            },
            objectTypes: {
                get: (args: BoundFieldMappingObjectListArgsType) => rawConfig.objectTypes.get({...args, executeAction}),
            },
        };

        return (
            <FieldMappingField
                config={config}
                label={input.label}
                onChange={(value: FieldMappingValueType) => handleInputChange(workflowUuid, input.name, value)}
                required={input.required}
                value={input.value as FieldMappingValueType | undefined}
            />
        );
    }

    const componentReference = input.componentReference;
    const group = componentReference?.group;

    if (componentReference && group) {
        const memberValues = (input.value as Record<string, unknown> | undefined) ?? {};

        return (
            <DialogGroupField
                componentName={componentReference.componentName}
                componentVersion={componentReference.componentVersion}
                group={group}
                groupName={componentReference.groupName}
                handleWorkflowGroupInputChange={handleWorkflowGroupInputChange}
                inputName={input.name}
                loadWorkflowInputOptions={loadWorkflowInputOptions}
                memberValues={memberValues}
                workflowInputOptions={workflowInputOptions}
                workflowUuid={workflowUuid}
            />
        );
    }

    return (
        <DialogInputField
            onChange={(event) => handleInputChange(workflowUuid, input.name, event.target.value)}
            label={input.label}
            name={input.name}
            required={input.required}
            field={{value: input.value as string | number | readonly string[] | undefined}}
        />
    );
};

interface DialogInputFieldsetProps {
    label: string;
    name: string;
    placeholder?: string;
    options?: string[];
    onChange?: (event: React.ChangeEvent<HTMLSelectElement | HTMLInputElement>) => void;
    required?: boolean;
    field?: React.InputHTMLAttributes<HTMLInputElement | HTMLSelectElement>;
    error?: {
        message: string;
    };
}

const DialogInputField = ({
    label,
    name,
    options,
    placeholder,
    required,
    field,
    error,
    onChange,
}: DialogInputFieldsetProps) => (
    <fieldset className={styles.dialogInputField}>
        <label htmlFor={name}>
            {label}

            {required && <span className={styles.requiredIndicator}>*</span>}
        </label>

        {options ? (
            <select id={name} {...field}>
                <option value="">Select {label}</option>

                {options.map((option) => (
                    <option key={option} value={option}>
                        {option}
                    </option>
                ))}
            </select>
        ) : (
            <input id={name} {...field} onChange={onChange} placeholder={placeholder} />
        )}

        {error && <span className={styles.inputError}>{error.message}</span>}
    </fieldset>
);

export default ConnectDialog;
