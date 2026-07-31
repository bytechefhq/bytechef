import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiGuardrails from './AiGuardrails';

// ---------------------------------------------------------------------------
// Hoisted mocks
// ---------------------------------------------------------------------------

const {invalidateQueriesMock, mutateMock, queryMock} = vi.hoisted(() => ({
    invalidateQueriesMock: vi.fn(),
    mutateMock: vi.fn(),
    queryMock: vi.fn(),
}));

const settings = {
    blockedTerms: 'foo,bar',
    blockingMode: 'BLOCK',
    injectionDetectionEnabled: false,
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

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 123),
}));

describe('AiGuardrails', () => {
    beforeEach(() => {
        mutateMock.mockClear();
        invalidateQueriesMock.mockClear();

        queryMock.mockReturnValue({
            data: {aiGuardrailsWorkspaceSettings: settings},
            error: null,
            isLoading: false,
        });
    });

    it('renders all six toggles, the blocked terms editor, and the blocking mode radio from query data', () => {
        render(<AiGuardrails />);

        expect(screen.getByLabelText('Redact PII')).toBeChecked();
        expect(screen.getByLabelText('Redact secrets')).not.toBeChecked();
        expect(screen.getByLabelText('Scan responses')).not.toBeChecked();
        expect(screen.getByLabelText('Model-based moderation')).toBeChecked();
        expect(screen.getByLabelText('Prompt-injection detection')).not.toBeChecked();

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
        expect(screen.getByLabelText('Model-based moderation')).not.toBeChecked();
        expect(screen.getByLabelText('Prompt-injection detection')).not.toBeChecked();

        expect(screen.getByLabelText('Blocked terms')).toHaveValue('');

        expect(screen.getByLabelText(/Block -- reject the request/)).toBeChecked();
        expect(screen.getByLabelText(/Redact and continue/)).not.toBeChecked();
    });

    it('saves changed values through the update mutation', () => {
        render(<AiGuardrails />);

        fireEvent.click(screen.getByLabelText('Prompt-injection detection'));

        fireEvent.change(screen.getByLabelText('Blocked terms'), {
            target: {value: 'foo,bar,baz'},
        });

        fireEvent.click(screen.getByLabelText(/Redact and continue/));

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith({
            input: {
                blockedTerms: 'foo,bar,baz',
                blockingMode: 'REDACT_AND_CONTINUE',
                injectionDetectionEnabled: true,
                moderationEnabled: true,
                redactPii: true,
                redactSecrets: false,
                scanResponses: false,
                workspaceId: '123',
            },
        });
    });
});
