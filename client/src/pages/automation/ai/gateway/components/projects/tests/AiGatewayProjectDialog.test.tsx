import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import AiGatewayProjectDialog from '../AiGatewayProjectDialog';

import type {AiGatewayProjectType} from '../../../types';

// The settings query result must keep a stable object identity across renders — otherwise the
// section's `useEffect(() => {...}, [data])` (which loads settings into state) fires on every
// render, since a fresh literal would be a new reference each call, causing a render loop.
const {guardrailsSettingsQueryData} = vi.hoisted(() => ({
    guardrailsSettingsQueryData: {
        aiGatewayProjectSettings: {
            blockedTerms: '',
            injectionDetectionEnabled: false,
            moderationEnabled: false,
            projectId: '1',
            redactPii: false,
            redactSecrets: false,
            scanResponses: false,
        },
    },
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiGatewayProjectSettingsQuery: () => ({data: guardrailsSettingsQueryData}),
    useCreateAiGatewayProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAiGatewayProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAiGatewayProjectSettingsMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const project: AiGatewayProjectType = {
    cacheTtlMinutes: null,
    cachingEnabled: false,
    compressionEnabled: false,
    createdDate: '2024-01-01T00:00:00Z',
    description: null,
    id: '1',
    lastModifiedDate: '2024-01-01T00:00:00Z',
    logRetentionDays: null,
    name: 'My Project',
    retryMaxAttempts: null,
    routingPolicyId: null,
    slug: 'my-project',
    timeoutSeconds: null,
    version: 1,
};

const renderDialog = (onClose = vi.fn()) => {
    render(<AiGatewayProjectDialog onClose={onClose} workspaceId="1" />);

    return onClose;
};

const renderDialogInEditMode = (onClose = vi.fn()) => {
    render(<AiGatewayProjectDialog onClose={onClose} project={project} workspaceId="1" />);

    return onClose;
};

const makeGuardrailsDirty = () => {
    fireEvent.click(screen.getByLabelText('Redact PII (emails, SSNs, cards, phones, IPs)'));
};

describe('AiGatewayProjectDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Add Project'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Slug')).toBeInTheDocument();
        expect(screen.getByLabelText('Description (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Routing Policy ID (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Compression Enabled')).toBeInTheDocument();
        expect(screen.getByLabelText('Retry Max Attempts (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Timeout Seconds (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Caching Enabled')).toBeInTheDocument();
        expect(screen.getByLabelText('Cache TTL Minutes (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Log Retention Days (optional)')).toBeInTheDocument();
    });
});

describe('AiGatewayProjectDialog guardrails dismissal guard', () => {
    it('does not show the unsaved guardrails affordance before any edit', () => {
        renderDialogInEditMode();

        expect(screen.queryByText('You have unsaved guardrail changes.')).not.toBeInTheDocument();
    });

    it('shows the unsaved guardrails affordance once a guardrail control is toggled', () => {
        renderDialogInEditMode();

        makeGuardrailsDirty();

        expect(screen.getByText('You have unsaved guardrail changes.')).toBeInTheDocument();
    });

    it('closes on Escape when the guardrails section is untouched', () => {
        const onClose = renderDialogInEditMode();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('does not close on Escape while the guardrails section has unsaved edits', () => {
        const onClose = renderDialogInEditMode();

        makeGuardrailsDirty();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).not.toHaveBeenCalled();
    });

    it('closes on Cancel even while the guardrails section has unsaved edits', () => {
        const onClose = renderDialogInEditMode();

        makeGuardrailsDirty();

        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
