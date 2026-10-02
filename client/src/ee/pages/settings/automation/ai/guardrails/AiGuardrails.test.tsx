import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiGuardrails from './AiGuardrails';

const {invalidateQueriesMock, mutateMock, queryMock} = vi.hoisted(() => ({
    invalidateQueriesMock: vi.fn(),
    mutateMock: vi.fn(),
    queryMock: vi.fn(),
}));

const settings = {
    blockedTerms: 'foo,bar',
    blockingMode: 'BLOCK',
    injectionDetectionEnabled: false,
    minConfidence: 0.75,
    moderationEnabled: true,
    redactPii: true,
    redactSecrets: false,
    scanResponses: false,
    workspaceId: '123',
};

vi.mock('@/shared/middleware/graphql', () => ({
    AiGuardrailsBlockingMode: {
        Block: 'BLOCK',
        RedactAndContinue: 'REDACT_AND_CONTINUE',
    },
    useAiGuardrailsWorkspaceSettingsQuery: (...args: unknown[]) => queryMock(...args),
    useUpdateAiGuardrailsWorkspaceSettingsMutation: () => ({isPending: false, mutate: mutateMock}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: invalidateQueriesMock}),
}));

describe('AiGuardrails', () => {
    beforeEach(() => {
        mutateMock.mockClear();
        invalidateQueriesMock.mockClear();

        useWorkspaceStore.setState({currentWorkspaceId: 123});

        queryMock.mockReturnValue({
            data: {aiGuardrailsWorkspaceSettings: settings},
            error: null,
            isLoading: false,
        });
    });

    it('renders the four toggles, the blocked terms editor, and the blocking mode radio from query data', () => {
        render(<AiGuardrails />);

        expect(screen.getByLabelText('Redact PII')).toBeChecked();
        expect(screen.getByLabelText('Redact secrets')).not.toBeChecked();
        expect(screen.getByLabelText('Scan responses')).not.toBeChecked();
        expect(screen.getByLabelText('Restore PII in workflow output')).not.toBeChecked();

        expect(screen.getByLabelText('Blocked terms')).toHaveValue('foo,bar');

        expect(screen.getByLabelText(/Block -- reject the request/)).toBeChecked();
        expect(screen.getByLabelText(/Redact and continue/)).not.toBeChecked();
    });

    it('synthesizes all-off defaults when the query returns null (no settings row yet)', () => {
        queryMock.mockReturnValue({
            data: {aiGuardrailsWorkspaceSettings: null},
            error: null,
            isLoading: false,
        });

        render(<AiGuardrails />);

        expect(screen.getByLabelText('Redact PII')).not.toBeChecked();
        expect(screen.getByLabelText('Redact secrets')).not.toBeChecked();
        expect(screen.getByLabelText('Scan responses')).not.toBeChecked();
        expect(screen.getByLabelText('Restore PII in workflow output')).not.toBeChecked();

        expect(screen.getByLabelText('Blocked terms')).toHaveValue('');

        expect(screen.getByLabelText(/Block -- reject the request/)).toBeChecked();
        expect(screen.getByLabelText(/Redact and continue/)).not.toBeChecked();
    });

    it('saves changed values through the update mutation', () => {
        render(<AiGuardrails />);

        fireEvent.click(screen.getByLabelText('Scan responses'));

        fireEvent.change(screen.getByLabelText('Blocked terms'), {
            target: {value: 'foo,bar,baz'},
        });

        fireEvent.click(screen.getByLabelText(/Redact and continue/));

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith({
            input: {
                blockedTerms: 'foo,bar,baz',
                blockingMode: 'REDACT_AND_CONTINUE',
                injectionDetectionEnabled: false,
                minConfidence: 0.75,
                moderationEnabled: true,
                redactMcpResults: false,
                redactPii: true,
                redactSecrets: false,
                restoreIntoWorkflowOutput: false,
                scanResponses: true,
                workspaceId: '123',
            },
        });
    });

    it('renders the restore-PII toggle off by default and saves true when switched on', () => {
        render(<AiGuardrails />);

        const toggle = screen.getByLabelText('Restore PII in workflow output');

        expect(toggle).not.toBeChecked();

        fireEvent.click(toggle);

        expect(toggle).toBeChecked();

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({restoreIntoWorkflowOutput: true}),
            })
        );
    });

    it('reflects a fetched restoreIntoWorkflowOutput value of true, and saves true unchanged', () => {
        queryMock.mockReturnValue({
            data: {aiGuardrailsWorkspaceSettings: {...settings, restoreIntoWorkflowOutput: true}},
            error: null,
            isLoading: false,
        });

        render(<AiGuardrails />);

        const toggle = screen.getByLabelText('Restore PII in workflow output');

        expect(toggle).toBeChecked();

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({restoreIntoWorkflowOutput: true}),
            })
        );
    });

    it('preserves an API-set minConfidence across a save, since the page has no control for it', () => {
        render(<AiGuardrails />);

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({minConfidence: 0.75}),
            })
        );
    });
});
