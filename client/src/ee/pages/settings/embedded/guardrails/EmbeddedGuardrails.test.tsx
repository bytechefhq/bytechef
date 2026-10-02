import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import EmbeddedGuardrails from './EmbeddedGuardrails';

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
    scope: 'EMBEDDED',
    workspaceId: null,
};

vi.mock('@/shared/middleware/graphql', () => ({
    AiGuardrailsBlockingMode: {
        Block: 'BLOCK',
        RedactAndContinue: 'REDACT_AND_CONTINUE',
    },
    AiGuardrailsSettingsScope: {
        Embedded: 'EMBEDDED',
        Platform: 'PLATFORM',
        Workspace: 'WORKSPACE',
    },
    useAiGuardrailsWorkspaceSettingsQuery: (...args: unknown[]) => queryMock(...args),
    useUpdateAiGuardrailsWorkspaceSettingsMutation: () => ({isPending: false, mutate: mutateMock}),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: invalidateQueriesMock}),
}));

describe('EmbeddedGuardrails', () => {
    beforeEach(() => {
        mutateMock.mockClear();
        invalidateQueriesMock.mockClear();
        queryMock.mockClear();

        queryMock.mockReturnValue({
            data: {aiGuardrailsWorkspaceSettings: settings},
            error: null,
            isLoading: false,
        });
    });

    it('reads with scope EMBEDDED and no workspaceId', () => {
        render(<EmbeddedGuardrails />);

        expect(queryMock).toHaveBeenCalledWith({scope: 'EMBEDDED'});
    });

    it('renders the four toggles, the blocked terms editor, and the blocking mode radio from query data', () => {
        render(<EmbeddedGuardrails />);

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

        render(<EmbeddedGuardrails />);

        expect(screen.getByLabelText('Redact PII')).not.toBeChecked();
        expect(screen.getByLabelText('Redact secrets')).not.toBeChecked();
        expect(screen.getByLabelText('Scan responses')).not.toBeChecked();
        expect(screen.getByLabelText('Restore PII in workflow output')).not.toBeChecked();

        expect(screen.getByLabelText('Blocked terms')).toHaveValue('');

        expect(screen.getByLabelText(/Block -- reject the request/)).toBeChecked();
        expect(screen.getByLabelText(/Redact and continue/)).not.toBeChecked();
    });

    it('saves changed values through the update mutation, scoped to embedded with no workspaceId', () => {
        render(<EmbeddedGuardrails />);

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
                scope: 'EMBEDDED',
            },
        });
    });

    it('renders the restore-PII toggle off by default and saves true when switched on', () => {
        render(<EmbeddedGuardrails />);

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

        render(<EmbeddedGuardrails />);

        const toggle = screen.getByLabelText('Restore PII in workflow output');

        expect(toggle).toBeChecked();

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({restoreIntoWorkflowOutput: true}),
            })
        );
    });

    it('loads and saves the embedded-scoped guardrails settings', async () => {
        render(<EmbeddedGuardrails />);

        const toggle = await screen.findByLabelText(/redact secrets/i);

        expect(toggle).not.toBeChecked();

        fireEvent.click(toggle);

        expect(toggle).toBeChecked();
    });

    it('preserves an API-set minConfidence across a save, since the page has no control for it', () => {
        render(<EmbeddedGuardrails />);

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith(
            expect.objectContaining({
                input: expect.objectContaining({minConfidence: 0.75}),
            })
        );
    });
});
