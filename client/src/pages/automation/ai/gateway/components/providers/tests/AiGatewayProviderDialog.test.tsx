import {AiGatewayProviderType} from '@/shared/middleware/graphql';
import {fireEvent, render, screen, stubMutation} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiGatewayProviderDialog from '../AiGatewayProviderDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    AiGatewayProviderType: {
        Anthropic: 'ANTHROPIC',
        AzureOpenai: 'AZURE_OPENAI',
        Cohere: 'COHERE',
        Deepseek: 'DEEPSEEK',
        GoogleGemini: 'GOOGLE_GEMINI',
        Groq: 'GROQ',
        Mistral: 'MISTRAL',
        Openai: 'OPENAI',
    },
    useCreateWorkspaceAiGatewayProviderMutation: vi.fn(),
    useTestWorkspaceAiGatewayProviderConnectionMutation: vi.fn(),
    useUpdateWorkspaceAiGatewayProviderMutation: vi.fn(),
}));

const {
    useCreateWorkspaceAiGatewayProviderMutation,
    useTestWorkspaceAiGatewayProviderConnectionMutation,
    useUpdateWorkspaceAiGatewayProviderMutation,
} = await import('@/shared/middleware/graphql');

const createProviderMutation = stubMutation(vi.mocked(useCreateWorkspaceAiGatewayProviderMutation));
const testProviderConnectionMutation = stubMutation(vi.mocked(useTestWorkspaceAiGatewayProviderConnectionMutation));
const updateProviderMutation = stubMutation(vi.mocked(useUpdateWorkspaceAiGatewayProviderMutation));

beforeEach(() => {
    createProviderMutation.mutate.mockClear();
    testProviderConnectionMutation.mutate.mockClear();
    updateProviderMutation.mutate.mockClear();
});

const renderDialog = (onClose = vi.fn()) => {
    render(<AiGatewayProviderDialog isAdmin onClose={onClose} workspaceId="1" />);

    return onClose;
};

const existingProvider = {
    baseUrl: '',
    config: null,
    createdBy: 'admin',
    createdDate: '2026-01-01T00:00:00Z',
    enabled: true,
    id: '1',
    lastModifiedBy: 'admin',
    lastModifiedDate: '2026-01-01T00:00:00Z',
    name: 'My OpenAI Provider',
    // The generated enum member, not the bare literal: the dialog's provider prop is typed
    // AiGatewayProviderType, which a widened string does not satisfy. Resolves to the mocked module's
    // matching value at run time.
    type: AiGatewayProviderType.Openai,
    version: 1,
};

describe('AiGatewayProviderDialog', () => {
    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Add Provider'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Type')).toBeInTheDocument();
        expect(screen.getByLabelText('API Key')).toBeInTheDocument();
        expect(screen.getByLabelText('Base URL (optional)')).toBeInTheDocument();
    });

    it('renders the type control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Type')).toHaveAttribute('data-slot', 'select-trigger');
    });

    it('shows the Test Connection control in edit mode for admins', () => {
        render(<AiGatewayProviderDialog isAdmin onClose={vi.fn()} provider={existingProvider} workspaceId="1" />);

        expect(screen.getByRole('button', {name: 'Test Connection'})).toBeInTheDocument();
    });

    it('hides the Test Connection control in edit mode for non-admins', () => {
        render(
            <AiGatewayProviderDialog isAdmin={false} onClose={vi.fn()} provider={existingProvider} workspaceId="1" />
        );

        expect(screen.queryByRole('button', {name: 'Test Connection'})).not.toBeInTheDocument();
    });
});
