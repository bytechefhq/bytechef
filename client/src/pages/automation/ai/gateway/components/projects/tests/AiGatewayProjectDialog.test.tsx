import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiGatewayProjectDialog from '../AiGatewayProjectDialog';

import type {AiGatewayProjectType} from '../../../types';

// The settings query result must keep a stable object identity across renders — otherwise the
// section's `useEffect(() => {...}, [data])` (which loads settings into state) fires on every
// render, since a fresh literal would be a new reference each call, causing a render loop.
const {
    createProjectMutateMock,
    guardrailsSettingsQueryData,
    updateProjectMutateAsyncMock,
    updateProjectSettingsMutateAsyncMock,
} = vi.hoisted(() => ({
    createProjectMutateMock: vi.fn(),
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
    updateProjectMutateAsyncMock: vi.fn(),
    updateProjectSettingsMutateAsyncMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiGatewayProjectSettingsQuery: () => ({data: guardrailsSettingsQueryData}),
    useCreateAiGatewayProjectMutation: () => ({isPending: false, mutate: createProjectMutateMock}),
    useUpdateAiGatewayProjectMutation: () => ({isPending: false, mutateAsync: updateProjectMutateAsyncMock}),
    useUpdateAiGatewayProjectSettingsMutation: () => ({
        isPending: false,
        mutateAsync: updateProjectSettingsMutateAsyncMock,
    }),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {toast} = await import('sonner');

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

const clickSave = () => {
    fireEvent.click(screen.getByRole('button', {name: 'Save'}));
};

beforeEach(() => {
    vi.mocked(toast.error).mockClear();
    vi.mocked(toast.success).mockClear();

    createProjectMutateMock.mockClear();

    updateProjectMutateAsyncMock.mockReset();
    updateProjectMutateAsyncMock.mockResolvedValue({});

    updateProjectSettingsMutateAsyncMock.mockReset();
    updateProjectSettingsMutateAsyncMock.mockResolvedValue({});
});

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

// Folding the guardrails save into the single footer Save (see AiGatewayProjectDialog.tsx) removed
// the earlier Escape/overlay dismissal guard: since one Save now persists both halves, there is no
// longer a scenario where the guardrails section holds edits the footer Save would silently skip,
// so blocking dismissal only for that section (and not, say, an edited project name) would be an
// inconsistent half-guard. These tests pin the new, uniform behavior.
describe('AiGatewayProjectDialog dismissal after the guardrails Save button was removed', () => {
    it('closes on Escape when the guardrails section is untouched', () => {
        const onClose = renderDialogInEditMode();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('also closes on Escape while the guardrails section has unsaved edits', () => {
        const onClose = renderDialogInEditMode();

        makeGuardrailsDirty();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('closes on Cancel while the guardrails section has unsaved edits', () => {
        const onClose = renderDialogInEditMode();

        makeGuardrailsDirty();

        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onClose).toHaveBeenCalledTimes(1);
    });
});

describe('AiGatewayProjectDialog footer Save persists both resources', () => {
    it('sends the project update and the guardrail settings, project first, when Save is clicked', async () => {
        const callOrder: string[] = [];

        updateProjectMutateAsyncMock.mockImplementation(async () => {
            callOrder.push('project');

            return {};
        });

        updateProjectSettingsMutateAsyncMock.mockImplementation(async () => {
            callOrder.push('guardrails');

            return {};
        });

        const onClose = renderDialogInEditMode();

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'Renamed Project'}});

        makeGuardrailsDirty();

        clickSave();

        await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

        expect(updateProjectMutateAsyncMock).toHaveBeenCalledWith({
            id: '1',
            input: expect.objectContaining({name: 'Renamed Project'}),
        });

        expect(updateProjectSettingsMutateAsyncMock).toHaveBeenCalledWith({
            input: expect.objectContaining({projectId: '1', redactPii: true}),
        });

        // Discriminates against an implementation that fires both requests concurrently (e.g.
        // Promise.all) instead of the deliberate project-then-guardrails sequencing.
        expect(callOrder).toEqual(['project', 'guardrails']);
    });

    it('does not call the guardrails mutation when the guardrails section has no changes', async () => {
        const onClose = renderDialogInEditMode();

        fireEvent.change(screen.getByLabelText('Name'), {target: {value: 'Renamed Project'}});

        clickSave();

        await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));

        expect(updateProjectMutateAsyncMock).toHaveBeenCalled();
        expect(updateProjectSettingsMutateAsyncMock).not.toHaveBeenCalled();
    });
});

describe('AiGatewayProjectDialog partial-failure handling', () => {
    it('saves the project, keeps the dialog open, and reports a partial failure when the guardrails save fails', async () => {
        updateProjectSettingsMutateAsyncMock.mockRejectedValueOnce(new Error('guardrails endpoint down'));

        const onClose = renderDialogInEditMode();

        makeGuardrailsDirty();

        clickSave();

        await waitFor(() => expect(updateProjectSettingsMutateAsyncMock).toHaveBeenCalled());

        // The project half genuinely persisted — this is not a generic "save failed" state.
        expect(updateProjectMutateAsyncMock).toHaveBeenCalled();

        // Closing here would discard the still-unsaved guardrail edits, so the dialog must stay
        // open.
        expect(onClose).not.toHaveBeenCalled();

        expect(toast.error).toHaveBeenCalledWith(expect.stringContaining('guardrail'));
    });

    it('does not attempt the guardrails save when the project update itself fails', async () => {
        updateProjectMutateAsyncMock.mockRejectedValueOnce(new Error('project endpoint down'));

        const onClose = renderDialogInEditMode();

        makeGuardrailsDirty();

        clickSave();

        await waitFor(() => expect(updateProjectMutateAsyncMock).toHaveBeenCalled());

        // Nothing has been sent to the guardrails endpoint yet, so there is no partial state to
        // reconcile — retrying Save is safe.
        expect(updateProjectSettingsMutateAsyncMock).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
    });
});
