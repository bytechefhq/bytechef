import {describe, it, expect, vi, beforeEach} from 'vitest';
import {render, screen, fireEvent, within} from '@testing-library/react';
import ConnectDialog from './ConnectDialog';
import {IntegrationType, PropertyType, WorkflowInputType} from './types';

describe('ConnectDialog', () => {
    const createConnectionProps = {
        closeDialog: vi.fn(),
        handleClick: vi.fn(),
        handleWorkflowToggle: vi.fn(),
        handleWorkflowInputChange: vi.fn(),
        integration: undefined,
        isOpen: true,
        selectedWorkflows: [],
        mergedWorkflows: [],
        workflowsView: false,
    };

    const editConnectionProps = {
        ...createConnectionProps,
        workflowsView: true,
        integration: {
            name: 'Test Integration',
            description: 'Integration description',
            workflows: [
                {
                    label: 'Test Workflow',
                    workflowUuid: 'workflow-123',
                    inputs: [],
                },
            ],
        },
        integrationInstance: {
            id: 123,
            enabled: true,
            credentialStatus: 'VALID',
            workflows: [
                {
                    workflowUuid: 'workflow-123',
                    enabled: true,
                    inputs: {
                        input1: 'test value',
                    },
                },
            ],
        },
        mergedWorkflows: [
            {
                label: 'Test Workflow',
                workflowUuid: 'workflow-123',
                enabled: true,
                inputs: [
                    {
                        name: 'input1',
                        label: 'Input 1',
                        type: 'string' as const,
                        value: 'test value',
                    },
                ],
            },
        ],
    };

    const createFormMock = (
        options: {
            errors?: Record<string, {message: string}>;
            isDirty?: boolean;
            isSubmitting?: boolean;
        } = {}
    ) => ({
        register: (name?: string) => ({
            name: name || 'test-field',
            defaultValue: '',
            ref: () => {},
            onInput: () => {},
            onChange: () => {},
            onBlur: () => {},
        }),
        handleSubmit: (callback?: (data: {[key: string]: unknown}) => void) => (e?: React.FormEvent) => {
            e?.preventDefault?.();
            callback?.({});
            return true;
        },
        formState: {
            errors: options.errors || {},
            isDirty: options.isDirty !== undefined ? options.isDirty : false,
            isSubmitting: options.isSubmitting !== undefined ? options.isSubmitting : false,
        },
    });

    describe('Rendering Tests', () => {
        beforeEach(() => {
            vi.clearAllMocks();
        });

        it('renders correctly with minimal props', () => {
            const {container} = render(<ConnectDialog {...createConnectionProps} />);

            expect(screen.getByTestId('dialog-overlay')).toBeInTheDocument();

            expect(container.querySelector('div[class*="dialogContainer"]')).toBeInTheDocument();
            expect(container.querySelector('header[class*="dialogHeader"]')).toBeInTheDocument();

            expect(screen.getByText('Unable to Load Integration')).toBeInTheDocument();

            expect(
                screen.getByText("We couldn't load the integration data. Please try again later.")
            ).toBeInTheDocument();
        });

        it('renders with proper accessibility attributes', () => {
            render(<ConnectDialog {...createConnectionProps} />);

            const closeButton = screen.getByRole('button', {name: /close dialog/i});

            expect(closeButton).toBeInTheDocument();
            expect(closeButton).toHaveAttribute('aria-label', 'Close Dialog');

            expect(closeButton.querySelector('img')).toBeInTheDocument();

            const cancelButton = screen.getByText('Close');

            expect(cancelButton).toBeInTheDocument();

            const overlay = screen.getByTestId('dialog-overlay');

            overlay.click();

            expect(createConnectionProps.closeDialog).toHaveBeenCalled();
        });

        it('displays error state when integration is null', () => {
            render(<ConnectDialog {...createConnectionProps} />);

            expect(screen.getByText('Unable to Load Integration')).toBeInTheDocument();

            expect(
                screen.getByText("We couldn't load the integration data. Please try again later.")
            ).toBeInTheDocument();

            expect(screen.getByText('Close')).toBeInTheDocument();

            expect(screen.queryByText('Continue')).not.toBeInTheDocument();
        });

        it('displays integration content when data is available', () => {
            const propsWithIntegration = {
                ...createConnectionProps,
                integration: {
                    name: 'Test Integration',
                    description: 'This is a test integration',
                    connectionConfig: {
                        authorizationType: '',
                        inputs: [],
                    },
                },
            };

            render(<ConnectDialog {...propsWithIntegration} />);

            expect(screen.getByText('This is a test integration')).toBeInTheDocument();

            const primaryButton = screen.getByRole('button', {name: /connect/i});

            expect(primaryButton).toBeInTheDocument();
            expect(primaryButton).toBeEnabled();
        });

        it('does not render when isOpen is false', () => {
            const closedProps = {
                ...createConnectionProps,
                isOpen: false,
            };

            const {container} = render(<ConnectDialog {...closedProps} />);

            expect(container.firstChild).toBeNull();

            expect(screen.queryByTestId('dialog-overlay')).not.toBeInTheDocument();
        });
    });

    describe('Component Composition Tests', () => {
        it('shows DialogContent when integration is available', () => {
            const props = {
                ...createConnectionProps,
                integration: {name: 'Test Integration', description: 'Description'},
            };
            render(<ConnectDialog {...props} />);

            expect(screen.getByText('Description')).toBeInTheDocument();
            expect(screen.queryByText('Unable to Load Integration')).not.toBeInTheDocument();
        });

        it('shows different content if we are editing instead of creating', () => {
            render(<ConnectDialog {...editConnectionProps} />);

            expect(screen.getByText('Disconnect')).toBeInTheDocument();

            expect(screen.getByTestId('workflows-container')).toBeInTheDocument();
            expect(screen.getByText('Enable, disable and manage your workflows below')).toBeInTheDocument();
        });
    });

    describe('User Interaction Tests', () => {
        it('calls closeDialog when escape key is pressed', () => {
            const closeMock = vi.fn();

            render(<ConnectDialog {...createConnectionProps} closeDialog={closeMock} />);

            fireEvent.keyDown(window, {key: 'Escape'});

            expect(closeMock).toHaveBeenCalled();
        });

        it('prevents event propagation when clicking inside dialog', () => {
            const closeMock = vi.fn();

            render(<ConnectDialog {...createConnectionProps} closeDialog={closeMock} />);

            fireEvent.click(screen.getByText('Unable to Load Integration'));

            expect(closeMock).not.toHaveBeenCalled();
        });
    });

    describe('Workflow Management UI Tests', () => {
        it('toggles workflow when switch is clicked', () => {
            const toggleMock = vi.fn();
            const propsWithToggle = {
                ...editConnectionProps,
                handleWorkflowToggle: toggleMock,
            };

            render(<ConnectDialog {...propsWithToggle} />);

            const workflowItem = screen.getByText('Test Workflow').closest('li');
            const toggleLabel = workflowItem?.querySelector('label[class*="toggleLabel"]');

            expect(toggleLabel).toBeInTheDocument();

            fireEvent.click(toggleLabel as Element);

            expect(toggleMock).toHaveBeenCalledWith('workflow-123', false);
        });

        it('shows input fields when workflow is selected', () => {
            const propsWithEnabledWorkflow = {
                ...editConnectionProps,
                selectedWorkflows: ['workflow-123'],
            };

            render(<ConnectDialog {...propsWithEnabledWorkflow} />);

            expect(screen.getByText('INPUTS')).toBeInTheDocument();
            expect(screen.getByLabelText('Input 1')).toBeInTheDocument();
        });
    });

    describe('Form Validation UI Tests', () => {
        it('displays validation errors when form has errors', () => {
            const props = {
                ...createConnectionProps,
                integration: {
                    name: 'Test Integration',
                    connectionConfig: {
                        authorizationType: '',
                        inputs: [{name: 'apiKey', label: 'API Key', required: true} as WorkflowInputType],
                    },
                } as IntegrationType,
                properties: [{name: 'apiKey', label: 'API Key', required: true}] as PropertyType[],
                form: createFormMock({
                    errors: {
                        apiKey: {message: 'API Key is required'},
                    },
                    isDirty: true,
                }),
            };

            const {container} = render(<ConnectDialog {...props} />);

            const errorElement = container.querySelector(`[class*="inputError"]`);

            expect(errorElement).toBeInTheDocument();
            expect(errorElement).toHaveTextContent('API Key is required');

            const fieldset = screen.getByLabelText('API Key', {exact: false}).closest('fieldset');

            expect(fieldset).toBeInTheDocument();

            const errorWithin = within(fieldset!).getByText('API Key is required');

            expect(errorWithin).toBeInTheDocument();
        });

        it('renders select dropdowns correctly when options are provided', () => {
            const props = {
                ...createConnectionProps,
                integration: {name: 'Test'} as IntegrationType,
                properties: [
                    {
                        name: 'country',
                        label: 'Country',
                        options: ['USA', 'Canada', 'UK'],
                    },
                ] as PropertyType[],
                form: createFormMock(),
            };

            render(<ConnectDialog {...props} />);

            const select = screen.getByRole('combobox', {name: /country/i});

            expect(select).toBeInTheDocument();

            expect(screen.getByText('Select Country')).toBeInTheDocument();
            expect(screen.getByText('USA')).toBeInTheDocument();
            expect(screen.getByText('Canada')).toBeInTheDocument();
        });

        it('displays required field indicators for required fields', () => {
            const props = {
                ...createConnectionProps,
                integration: {name: 'Test'} as IntegrationType,
                properties: [
                    {name: 'required', label: 'Required Field', required: true},
                    {name: 'optional', label: 'Optional Field', required: false},
                ] as PropertyType[],
                form: createFormMock(),
            };

            render(<ConnectDialog {...props} />);

            const requiredFieldset = screen.getByLabelText('Required Field', {exact: false}).closest('fieldset');
            const optionalFieldset = screen.getByLabelText('Optional Field', {exact: false}).closest('fieldset');

            expect(requiredFieldset).toBeInTheDocument();
            expect(optionalFieldset).toBeInTheDocument();

            expect(within(requiredFieldset!).getByText('*')).toBeInTheDocument();
            expect(within(optionalFieldset!).queryByText('*')).not.toBeInTheDocument();
        });
    });

    describe('Button Text and Icon Tests', () => {
        it('displays correct button text based on workflowsView and isOAuth2', () => {
            const oauth2Props = {
                ...createConnectionProps,
                integration: {name: 'Test'} as IntegrationType,
                workflowsView: false,
                isOAuth2: true,
            };

            const {rerender} = render(<ConnectDialog {...oauth2Props} />);

            expect(screen.getByText('Authorize')).toBeInTheDocument();
            expect(screen.getByTestId('authorize-icon')).toBeInTheDocument();

            rerender(
                <ConnectDialog
                    {...{
                        ...oauth2Props,
                        isOAuth2: false,
                        workflowsView: false,
                    }}
                />
            );

            expect(screen.getByText('Connect')).toBeInTheDocument();

            rerender(
                <ConnectDialog
                    {...{
                        ...oauth2Props,
                        isOAuth2: false,
                        workflowsView: true,
                    }}
                />
            );

            expect(screen.getByText('Disconnect')).toBeInTheDocument();
        });
    });

    describe('Form Submission Tests', () => {
        it('calls handleSubmit when form is submitted', () => {
            const handleClickMock = vi.fn();

            const props = {
                ...createConnectionProps,
                integration: {name: 'Test Integration'} as IntegrationType,
                properties: [{name: 'field1', label: 'Field 1', type: 'string'} as PropertyType],
                form: createFormMock(),
                handleClick: handleClickMock,
            };

            render(<ConnectDialog {...props} />);

            const submitButton = screen.getByRole('button', {name: /connect/i});

            fireEvent.click(submitButton);

            expect(handleClickMock).toHaveBeenCalled();
        });
    });
});
