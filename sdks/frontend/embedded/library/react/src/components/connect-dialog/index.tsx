import ConnectDialog from './ConnectDialog';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {createRoot} from 'react-dom/client';
import useOAuth2 from './useOAuth2';
import {
    CodePayloadI,
    FormSubmitHandler,
    FormType,
    IntegrationInstanceType,
    IntegrationInstanceWorkflowType,
    IntegrationType,
    MergedWorkflowType,
    PropertyType,
    RegisterFormSubmitFunction,
    TokenPayloadI,
    WorkflowInputType,
} from './types';

const OAUTH2_TYPES = ['OAUTH2_AUTHORIZATION_CODE', 'OAUTH2_AUTHORIZATION_CODE_PKCE'];

interface ValidationRuleType {
    required: boolean;
    requiredMessage?: string;
}

interface ConnectionDialogHookReturnType {
    closeDialog: () => void;
    openDialog: () => void;
}

function createApiClient(baseUrl: string, environment: string, jwtToken: string) {
    const defaultHeaders = {
        Authorization: `Bearer ${jwtToken}`,
        'X-Environment': environment,
        'Content-Type': 'application/json',
    };

    return {
        async fetch<T>(
            endpoint: string,
            options: {
                method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
                body?: object;
                headers?: Record<string, string>;
            } = {}
        ): Promise<T> {
            const {method = 'GET', body, headers = {}} = options;
            const url = `${baseUrl}${endpoint}`;

            try {
                const response = await fetch(url, {
                    method,
                    headers: {...defaultHeaders, ...headers},
                    ...(body && {body: JSON.stringify(body)}),
                });

                if (!response.ok) {
                    throw new Error(`Response status: ${response.status}`);
                }

                // Only try to parse JSON if we expect a response body
                if (method === 'GET' || response.headers.get('content-length') !== '0') {
                    try {
                        return await response.json();
                    } catch (error: unknown) {
                        console.warn('Empty or non-JSON response : ', (error as Error).message);

                        return {} as T;
                    }
                }

                return {} as T;
            } catch (error: unknown) {
                console.error(`API Error (${endpoint}):`, (error as Error).message);

                throw error;
            }
        },
    };
}

function debounce<T extends (...args: unknown[]) => unknown>(fn: T, delay: number): (...args: Parameters<T>) => void {
    let timeoutId: ReturnType<typeof setTimeout> | null = null;

    return function (this: unknown, ...args: Parameters<T>) {
        if (timeoutId) {
            clearTimeout(timeoutId);
        }

        timeoutId = setTimeout(() => {
            fn.apply(this, args);

            timeoutId = null;
        }, delay);
    };
}

interface UseConnectDialogProps {
    baseUrl?: string;
    environment?: string;
    integrationId: string;
    integrationInstanceId?: string;
    jwtToken: string;
}

export default function useConnectDialog({
    baseUrl = 'https://app.bytechef.io',
    environment = 'PRODUCTION',
    integrationId,
    integrationInstanceId,
    jwtToken,
}: UseConnectDialogProps): ConnectionDialogHookReturnType {
    const [integration, setIntegration] = useState<IntegrationType | undefined>(undefined);
    const [isOAuth2, setIsOAuth2] = useState(false);
    const [isOpen, setIsOpen] = useState(false);
    const [formValues, setFormValues] = useState<Record<string, string>>({});
    const [formErrors, setFormErrors] = useState<Record<string, {message: string}>>({});
    const [enabledOverrides, setEnabledOverrides] = useState<Record<string, boolean | undefined>>({});
    const [inputOverrides, setInputOverrides] = useState<Record<string, Record<string, string>>>({});
    const [isLoading, setIsLoading] = useState(false);
    const [workflowsView, setWorkflowsView] = useState(!!integrationInstanceId);
    const [currentIntegrationInstanceId, setCurrentIntegrationInstanceId] = useState<number | undefined>(
        integrationInstanceId ? Number(integrationInstanceId) : undefined
    );

    const inputRefs = useRef<Record<string, HTMLInputElement>>({});
    const portalContainerRef = useRef<HTMLElement | null>(null);
    const rootRef = useRef<ReturnType<typeof createRoot> | null>(null);
    const formSubmitRef = useRef<FormSubmitHandler | null>(null);

    const {fetch} = useMemo(() => createApiClient(baseUrl, environment, jwtToken), [baseUrl, environment, jwtToken]);

    // Merge integration workflows with instance workflows to get complete workflow data, because the backend is incomplete
    const mergedWorkflows: MergedWorkflowType[] = useMemo(() => {
        if (!integration?.workflows) {
            return [];
        }

        const currentInstance = integration.integrationInstances?.find(
            (instance: IntegrationInstanceType) => instance.id === currentIntegrationInstanceId
        );

        return integration.workflows.map((workflow) => {
            const instanceWorkflow = currentInstance?.workflows?.find(
                (currentWorkflow) => currentWorkflow.workflowUuid === workflow.workflowUuid
            );

            const serverEnabled = instanceWorkflow?.enabled ?? workflow.enabled ?? false;

            const effectiveEnabled = enabledOverrides[workflow.workflowUuid] ?? serverEnabled;

            const workflowInputOverrides = inputOverrides[workflow.workflowUuid];

            return {
                ...workflow,
                enabled: effectiveEnabled,
                inputs: Array.isArray(workflow.inputs)
                    ? workflow.inputs.map((input: WorkflowInputType) => ({
                          ...input,
                          value:
                              workflowInputOverrides?.[input.name] ??
                              (instanceWorkflow?.inputs as Record<string, string> | undefined)?.[input.name] ??
                              '',
                      }))
                    : [],
            } as MergedWorkflowType;
        });
    }, [integration, currentIntegrationInstanceId, enabledOverrides, inputOverrides]);

    const registerFormSubmit = useCallback<RegisterFormSubmitFunction>((submitFn) => {
        formSubmitRef.current = submitFn;
    }, []);

    const saveOAuth2Connection = useCallback(
        async (payload: CodePayloadI | TokenPayloadI) => {
            try {
                const newIntegrationInstanceId: number = await fetch(
                    `/api/embedded/v1/integrations/${integrationId}/instances`,
                    {
                        method: 'POST',
                        body: {
                            connection: {
                                parameters: payload,
                            },
                        },
                    }
                );

                const integrationData: IntegrationType = await fetch(
                    `/api/embedded/v1/integrations/${integrationId}`
                );

                const createdInstance = integrationData.integrationInstances?.find(
                    (instance) => instance.id === newIntegrationInstanceId
                ) || integrationData.integrationInstances?.[0];

                if (createdInstance) {
                    setCurrentIntegrationInstanceId(createdInstance.id);
                }

                setIntegration(integrationData);
                setWorkflowsView(true);
            } catch (error) {
                console.error('Failed to save OAuth2 connection:', error);
            }
        },
        [fetch, integrationId]
    );

    const saveNonOAuth2Connection = useCallback(
        async (formData: Record<string, string>) => {
            try {
                const newIntegrationInstanceId: number = await fetch(
                    `/api/embedded/v1/integrations/${integrationId}/instances`,
                    {
                        method: 'POST',
                        body: {
                            connection: {
                                parameters: formData,
                            },
                        },
                    }
                );

                const integrationData: IntegrationType = await fetch(
                    `/api/embedded/v1/integrations/${integrationId}`
                );

                const createdInstance = integrationData.integrationInstances?.find(
                    (instance) => instance.id === newIntegrationInstanceId
                ) || integrationData.integrationInstances?.[0];

                if (createdInstance) {
                    setCurrentIntegrationInstanceId(createdInstance.id);
                }

                setIntegration(integrationData);
                setWorkflowsView(true);
            } catch (error) {
                console.error('Failed to save non-OAuth2 connection:', error);
            }
        },
        [fetch, integrationId]
    );

    const handleSubmit = useCallback(() => {
        if (formSubmitRef.current) {
            const submitFunction = formSubmitRef.current(() => saveNonOAuth2Connection(formValues));

            submitFunction();
        }
    }, [formValues, saveNonOAuth2Connection]);

    const handleWorkflowToggle = useCallback(
        async (workflowUuid: string, pressed: boolean) => {
            if (!currentIntegrationInstanceId || isNaN(currentIntegrationInstanceId)) {
                console.error('Invalid integration instance ID');

                return;
            }

            try {
                setEnabledOverrides((previous) => ({...previous, [workflowUuid]: pressed}));

                const method = pressed ? 'POST' : 'DELETE';

                await fetch(
                    `/api/embedded/v1/integration-instances/${currentIntegrationInstanceId}/workflows/${workflowUuid}/enable`,
                    {
                        method,
                    }
                );
            } catch (error) {
                setEnabledOverrides((previous) => ({
                    ...previous,
                    [workflowUuid]: !pressed,
                }));

                console.error('Failed to toggle workflow:', error);
            }
        },
        [fetch, currentIntegrationInstanceId]
    );

    const isOAuth2AuthorizationType = useMemo(
        () =>
            integration?.connectionConfig?.authorizationType &&
            OAUTH2_TYPES.includes(integration?.connectionConfig?.authorizationType),
        [integration]
    );

    const handleOnCodeSuccess = useCallback(
        (payload: CodePayloadI) => {
            if (payload.code) {
                saveOAuth2Connection(payload);
            }
        },
        [saveOAuth2Connection]
    );

    const handleOnTokenSuccess = useCallback(
        (payload: TokenPayloadI) => {
            if (payload.access_token) {
                saveOAuth2Connection(payload);
            }
        },
        [saveOAuth2Connection]
    );

    const oauth2Scope = useMemo(() => {
        const scopes = integration?.connectionConfig?.oauth2?.scopes;

        if (Array.isArray(scopes)) {
            return scopes.join(' ');
        }

        if (scopes && typeof scopes === 'object') {
            return Object.entries(scopes)
                .filter(([, enabled]) => String(enabled) === 'true')
                .map(([scopeName]) => scopeName)
                .join(' ');
        }

        return typeof scopes === 'string' ? scopes : '';
    }, [integration?.connectionConfig?.oauth2?.scopes]);

    const {getAuth} = useOAuth2({
        ...integration?.connectionConfig?.oauth2,
        authorizationUrl: integration?.connectionConfig?.oauth2?.authorizationUrl || '',
        clientId: integration?.connectionConfig?.oauth2?.clientId || '',
        redirectUri: integration?.connectionConfig?.oauth2?.redirectUri || '',
        onCodeSuccess: handleOnCodeSuccess,
        onError: (error: string) => console.error(error),
        onTokenSuccess: handleOnTokenSuccess,
        responseType: isOAuth2AuthorizationType ? 'code' : 'token',
        scope: oauth2Scope,
    });

    const createValidationRules = useCallback((properties: PropertyType[]): Record<string, ValidationRuleType> => {
        if (!properties || properties.length === 0) {
            return {};
        }

        const rules: Record<string, ValidationRuleType> = {};

        properties.forEach((property) => {
            rules[property.name] = {
                required: !!property.required,
                requiredMessage: property.required ? `${property.label} is required` : undefined,
            };
        });

        return rules;
    }, []);

    const validationRules = useMemo(
        () => createValidationRules(integration?.connectionConfig?.inputs || []),
        [createValidationRules, integration?.connectionConfig?.inputs]
    );

    const validateForm = useCallback((data: Record<string, string>, rules: Record<string, ValidationRuleType>) => {
        const errors: Record<string, {message: string}> = {};
        const validatedData: Record<string, string> = {};

        Object.keys(rules).forEach((fieldName) => {
            const rule = rules[fieldName];
            const value = data[fieldName] || '';

            if (rule.required && value.trim() === '') {
                errors[fieldName] = {message: rule.requiredMessage || 'This field is required'};
            } else {
                validatedData[fieldName] = value;
            }
        });

        return {
            isValid: Object.keys(errors).length === 0,
            errors,
            validatedData,
        };
    }, []);

    const form: FormType = useMemo(() => {
        return {
            register: (name: string) => ({
                name,
                defaultValue: formValues?.[name] || '',
                ref: (element: HTMLInputElement) => {
                    if (element) {
                        inputRefs.current[name] = element;
                    }
                },
                onInput: (event: React.FormEvent<HTMLInputElement>) => {
                    const value = event.currentTarget.value;

                    setFormValues((previous: Record<string, string>) => ({...previous, [name]: value}));
                },
            }),
            handleSubmit: (callback: (data: {[key: string]: unknown}) => void) => (event?: React.FormEvent) => {
                if (event) {
                    event.preventDefault();
                }

                const currentValues = Object.entries(inputRefs.current).reduce(
                    (values, [name, ref]) => {
                        values[name] = ref.value;

                        return values;
                    },
                    {} as Record<string, string>
                );

                const {isValid, errors, validatedData} = validateForm(currentValues, validationRules);

                if (isValid) {
                    setFormErrors({});

                    callback(validatedData);

                    return true;
                } else {
                    setFormErrors(errors);

                    return false;
                }
            },
            formState: {
                errors: formErrors,
            },
        };
    }, [formErrors, formValues, validateForm, validationRules]);

    const openDialog = async () => {
        setIntegration(undefined);
        setEnabledOverrides({});
        setInputOverrides({});
        setIsLoading(true);
        setIsOpen(true);

        try {
            const integrationData: IntegrationType = await fetch(`/api/embedded/v1/integrations/${integrationId}`);

            let targetInstance: IntegrationInstanceType | undefined;

            if (integrationInstanceId && integrationData.integrationInstances) {
                targetInstance = integrationData.integrationInstances.find(
                    (instance) => instance.id === Number(integrationInstanceId)
                );
            }

            if (!targetInstance) {
                targetInstance = integrationData.integrationInstances?.[0];
            }

            if (targetInstance) {
                setCurrentIntegrationInstanceId(targetInstance.id);
                setWorkflowsView(true);
            } else if (integrationInstanceId) {
                setCurrentIntegrationInstanceId(Number(integrationInstanceId));
                setWorkflowsView(true);
            } else {
                setCurrentIntegrationInstanceId(undefined);
                setWorkflowsView(false);
            }

            setIntegration(integrationData);
        } finally {
            setIsLoading(false);
        }
    };

    const closeDialog = () => {
        setWorkflowsView(false);

        setIsOpen(false);
    };

    const handleDisconnect = useCallback(async () => {
        if (!currentIntegrationInstanceId || isNaN(currentIntegrationInstanceId)) {
            console.error('Invalid integration instance ID');

            return;
        }

        await fetch(`/api/embedded/v1/integration-instances/${currentIntegrationInstanceId}`, {
            method: 'DELETE',
        });

        setCurrentIntegrationInstanceId(undefined);
        setFormValues({});
        setInputOverrides({});
        setEnabledOverrides({});
        debouncedFetchesRef.current = {};
        setWorkflowsView(false);
    }, [fetch, currentIntegrationInstanceId]);

    const handleClick = useCallback(
        (event: React.MouseEvent<HTMLButtonElement>) => {
            if ((event.target as HTMLButtonElement).name === 'disconnectButton') {
                handleDisconnect()
                    .then(() => closeDialog())
                    .catch((error) => console.error('Failed to disconnect:', error));

                return;
            }

            if (isOAuth2) {
                getAuth();
            } else {
                handleSubmit();
            }
        },
        [isOAuth2, handleDisconnect, getAuth, handleSubmit]
    );

    const debouncedFetchesRef = useRef<Record<string, (...args: unknown[]) => void>>({});

    const currentIntegrationInstanceIdRef = useRef(currentIntegrationInstanceId);
    const inputOverridesRef = useRef(inputOverrides);
    const integrationRef = useRef(integration);

    currentIntegrationInstanceIdRef.current = currentIntegrationInstanceId;
    inputOverridesRef.current = inputOverrides;
    integrationRef.current = integration;

    const handleWorkflowInputChange = useCallback(
        (workflowUuid: string, inputName: string, value: string) => {
            setInputOverrides((previous) => {
                const updated = {
                    ...previous,
                    [workflowUuid]: {
                        ...previous[workflowUuid],
                        [inputName]: value,
                    },
                };

                inputOverridesRef.current = updated;

                return updated;
            });

            if (!currentIntegrationInstanceIdRef.current || isNaN(currentIntegrationInstanceIdRef.current)) {
                console.error('Invalid integration instance ID');

                return;
            }

            const debouncedFetchKey = workflowUuid;

            if (!debouncedFetchesRef.current[debouncedFetchKey]) {
                debouncedFetchesRef.current[debouncedFetchKey] = debounce(() => {
                    const instanceId = currentIntegrationInstanceIdRef.current;

                    if (!instanceId) {
                        return;
                    }

                    const currentIntegration = integrationRef.current;
                    const currentInstance = currentIntegration?.integrationInstances?.find(
                        (instance: IntegrationInstanceType) => instance.id === instanceId
                    );
                    const serverInputs =
                        (currentInstance?.workflows?.find(
                            (workflow: IntegrationInstanceWorkflowType) =>
                                workflow.workflowUuid === workflowUuid
                        )?.inputs as Record<string, string> | undefined) || {};

                    const mergedInputs = {
                        ...serverInputs,
                        ...inputOverridesRef.current[workflowUuid],
                    };

                    void fetch(
                        `/api/embedded/v1/integration-instances/${instanceId}/workflows/${workflowUuid}`,
                        {
                            body: {
                                inputs: mergedInputs,
                            },
                            method: 'PUT',
                        }
                    ).catch((error) => console.error('Failed to save workflow inputs:', error));
                }, 600);
            }

            debouncedFetchesRef.current[debouncedFetchKey]();
        },
        [fetch]
    );

    // Create portal container only once
    useEffect(() => {
        let container = document.getElementById('connect-dialog-portal');

        if (!container) {
            container = document.createElement('div');

            container.id = 'connect-dialog-portal';

            document.body.appendChild(container);
            portalContainerRef.current = container;
        }

        // Cleanup on unmount
        return () => {
            if (portalContainerRef.current) {
                document.body.removeChild(portalContainerRef.current);
            }
        };
    }, []);

    // Handle creation and updates
    useEffect(() => {
        // Clean up when closed
        if (!isOpen) {
            if (rootRef.current) {
                rootRef.current.unmount();

                rootRef.current = null;
            }

            return;
        }

        // Create root if needed
        if (!rootRef.current && portalContainerRef.current) {
            rootRef.current = createRoot(portalContainerRef.current);
        }

        // Always render with current state
        if (rootRef.current) {
            rootRef.current.render(
                <ConnectDialog
                    closeDialog={closeDialog}
                    form={form}
                    handleWorkflowToggle={handleWorkflowToggle}
                    handleWorkflowInputChange={handleWorkflowInputChange}
                    handleClick={handleClick}
                    integration={integration}
                    isOAuth2={isOAuth2}
                    isOpen={isOpen}
                    loading={isLoading}
                    properties={integration?.connectionConfig?.inputs}
                    registerFormSubmit={registerFormSubmit}
                    workflowsView={workflowsView}
                    mergedWorkflows={mergedWorkflows}
                />
            );
        }
    }, [
        isOpen,
        form,
        formValues,
        handleClick,
        integration,
        isOAuth2,
        registerFormSubmit,
        handleWorkflowToggle,
        handleWorkflowInputChange,
        integrationInstanceId,
        workflowsView,
        mergedWorkflows,
        currentIntegrationInstanceId,
    ]);

    useEffect(() => {
        if (isOAuth2AuthorizationType) {
            setIsOAuth2(true);
        }
    }, [isOAuth2AuthorizationType]);

    return {
        openDialog,
        closeDialog,
    };
}
