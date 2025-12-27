import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {fireEvent, render, screen, waitFor} from '../../../shared/util/test-utils';
import TriggerForm from './TriggerForm';

const mockWorkflowExecutionId = 'test-id';
const mockEnvironment = 'test';

vi.mock('react-router-dom', () => ({
    useParams: () => ({
        environment: mockEnvironment,
        workflowExecutionId: mockWorkflowExecutionId,
    }),
}));

vi.mock('./util/triggerForm-utils', () => ({
    FieldType: {
        CHECKBOX: 1,
        CUSTOM_HTML: 12,
        DATE_PICKER: 2,
        DATETIME_PICKER: 3,
        EMAIL_INPUT: 8,
        FILE_INPUT: 4,
        HIDDEN_FIELD: 13,
        INPUT: 6,
        NUMBER_INPUT: 9,
        PASSWORD_INPUT: 10,
        RADIO: 11,
        SELECT: 7,
        TEXTAREA: 5,
    },
    fetchTriggerFormDefinition: vi.fn(),
}));

import {FieldType, TriggerFormType, fetchTriggerFormDefinition} from './util/triggerForm-utils';

describe('TriggerForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        global.fetch = vi.fn();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    const mockDefinition: TriggerFormType = {
        appendAttribution: true,
        buttonLabel: 'Submit Test',
        formDescription: 'Test Description',
        formTitle: 'Test Form',
        ignoreBots: true,
        inputs: [
            {
                fieldLabel: 'Text Label',
                fieldName: 'textField',
                fieldType: FieldType.INPUT,
                required: true,
            },
            {
                defaultValue: 'true',
                fieldLabel: 'Checkbox Label',
                fieldName: 'checkboxField',
                fieldType: FieldType.CHECKBOX,
                required: false,
            },
        ],
        useWorkflowTimezone: true,
    };

    it('should show loading state and then render the form', async () => {
        vi.mocked(fetchTriggerFormDefinition).mockResolvedValue(mockDefinition);

        render(<TriggerForm />);

        expect(screen.getByText('Loading form…')).toBeInTheDocument();

        await waitFor(() => {
            expect(screen.getByText('Test Form')).toBeInTheDocument();
            expect(screen.getByText('Test Description')).toBeInTheDocument();
            expect(screen.getByText('Text Label')).toBeInTheDocument();
            expect(screen.getByText('Checkbox Label')).toBeInTheDocument();
            expect(screen.getByText('Submit Test')).toBeInTheDocument();
        });
    });

    it('should show error message when fetching definition fails', async () => {
        vi.mocked(fetchTriggerFormDefinition).mockRejectedValueOnce(new Error('Failed to fetch'));

        render(<TriggerForm />);

        expect(await screen.findByText('Failed to fetch', {}, {timeout: 10000})).toBeInTheDocument();
    });

    it('should handle form submission successfully', async () => {
        vi.mocked(fetchTriggerFormDefinition).mockResolvedValue(mockDefinition);
        vi.spyOn(global, 'fetch').mockResolvedValue({
            ok: true,
        } as Response);

        render(<TriggerForm />);

        await waitFor(() => expect(screen.getByText('Submit Test')).toBeInTheDocument(), {timeout: 3000});

        fireEvent.change(screen.getByLabelText('Text Label'), {target: {value: 'test value'}});
        fireEvent.click(screen.getByText('Submit Test'));

        await waitFor(() => {
            expect(screen.getByText('Thank you!')).toBeInTheDocument();
            expect(screen.getByText('Your response has been submitted.')).toBeInTheDocument();
        });

        expect(global.fetch).toHaveBeenCalledWith(
            `/webhooks/${mockWorkflowExecutionId}`,
            expect.objectContaining({
                body: expect.stringContaining('"textField":"test value"'),
                method: 'POST',
            })
        );
    });

    it('should show error message when submission fails', async () => {
        vi.mocked(fetchTriggerFormDefinition).mockResolvedValue(mockDefinition);
        vi.spyOn(global, 'fetch').mockResolvedValue({
            ok: false,
            statusText: 'Server Error',
        } as Response);

        render(<TriggerForm />);

        await waitFor(() => expect(screen.getByText('Submit Test')).toBeInTheDocument(), {timeout: 3000});

        fireEvent.change(screen.getByLabelText('Text Label'), {target: {value: 'test value'}});
        fireEvent.click(screen.getByText('Submit Test'));

        await waitFor(() => {
            expect(screen.getByText('Submission failed: Server Error')).toBeInTheDocument();
        });
    });

    it('should handle file submission', async () => {
        const fileMockDefinition: TriggerFormType = {
            ...mockDefinition,
            inputs: [
                {
                    fieldLabel: 'File Label',
                    fieldName: 'fileField',
                    fieldType: FieldType.FILE_INPUT,
                    required: true,
                },
            ],
        };
        vi.mocked(fetchTriggerFormDefinition).mockResolvedValue(fileMockDefinition);
        vi.spyOn(global, 'fetch').mockResolvedValue({
            ok: true,
        } as Response);

        render(<TriggerForm />);

        const fileLabel = await screen.findByText('File Label', {}, {timeout: 5000});
        expect(fileLabel).toBeInTheDocument();

        const file = new File(['hello'], 'hello.txt', {type: 'text/plain'});
        const input = document.querySelector('input[type="file"]') as HTMLInputElement;

        fireEvent.change(input, {target: {files: [file]}});

        fireEvent.click(screen.getByText('Submit Test'));

        expect(await screen.findByText('Thank you!', {}, {timeout: 5000})).toBeInTheDocument();

        expect(global.fetch).toHaveBeenCalledWith(
            `/webhooks/${mockWorkflowExecutionId}`,
            expect.objectContaining({
                body: expect.any(FormData),
                method: 'POST',
            })
        );
    });
});
