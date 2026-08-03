import {fireEvent, render, screen, stubMutation, userEvent, within} from '@/shared/util/test-utils';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiGatewayModelType} from '../../../types';
import AiGatewayModelDialog from '../AiGatewayModelDialog';

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateWorkspaceAiGatewayModelMutation: vi.fn(),
    useUpdateWorkspaceAiGatewayModelMutation: vi.fn(),
    useWorkspaceAiGatewayProvidersQuery: () => ({
        data: {
            workspaceAiGatewayProviders: [
                {
                    baseUrl: null,
                    config: null,
                    createdBy: null,
                    createdDate: '2026-01-01T00:00:00Z',
                    enabled: true,
                    id: '1',
                    lastModifiedBy: null,
                    lastModifiedDate: '2026-01-01T00:00:00Z',
                    name: 'OpenAI',
                    type: 'OPENAI',
                    version: 1,
                },
            ],
        },
    }),
    useWorkspaceAiGatewayRoutingPoliciesQuery: () => ({
        data: {
            workspaceAiGatewayRoutingPolicies: [
                {
                    config: null,
                    createdDate: '2026-01-01T00:00:00Z',
                    deployments: [],
                    enabled: true,
                    fallbackModel: null,
                    id: '1',
                    lastModifiedDate: '2026-01-01T00:00:00Z',
                    name: 'Default Policy',
                    strategy: 'ROUND_ROBIN',
                    version: 1,
                },
            ],
        },
    }),
}));

const {useCreateWorkspaceAiGatewayModelMutation, useUpdateWorkspaceAiGatewayModelMutation} =
    await import('@/shared/middleware/graphql');

const createModelMutation = stubMutation(vi.mocked(useCreateWorkspaceAiGatewayModelMutation));
const updateModelMutation = stubMutation(vi.mocked(useUpdateWorkspaceAiGatewayModelMutation));

beforeEach(() => {
    createModelMutation.mutate.mockClear();
    updateModelMutation.mutate.mockClear();
});

const modelWithRoutingPolicy: AiGatewayModelType = {
    alias: null,
    capabilities: null,
    contextWindow: null,
    createdDate: '2026-01-01T00:00:00Z',
    defaultRoutingPolicyId: '1',
    enabled: true,
    id: '10',
    inputCostPerMTokens: null,
    lastModifiedDate: '2026-01-01T00:00:00Z',
    name: 'gpt-4o',
    outputCostPerMTokens: null,
    providerId: '1',
    version: 1,
};

const renderDialog = (onClose = vi.fn()) => {
    render(<AiGatewayModelDialog onClose={onClose} workspaceId="1" />);

    return onClose;
};

describe('AiGatewayModelDialog', () => {
    beforeAll(() => {
        // Radix Select relies on pointer-capture APIs that jsdom does not implement.
        Element.prototype.hasPointerCapture = vi.fn(() => false);
        Element.prototype.setPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
    });

    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Add Model'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Provider')).toBeInTheDocument();
        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Alias (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Context Window (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Input Cost per 1M Tokens (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Output Cost per 1M Tokens (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Capabilities (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Default Routing Policy (optional)')).toBeInTheDocument();
    });

    it('renders the provider control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Provider')).toHaveAttribute('data-slot', 'select-trigger');
    });

    it('renders the default routing policy control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Default Routing Policy (optional)')).toHaveAttribute(
            'data-slot',
            'select-trigger'
        );
    });

    it('offers the inherit option even when the model already has a routing policy', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        render(<AiGatewayModelDialog model={modelWithRoutingPolicy} onClose={vi.fn()} workspaceId="1" />);

        const trigger = screen.getByLabelText('Default Routing Policy (optional)');

        expect(trigger).toHaveTextContent('Default Policy');

        await user.click(trigger);

        const listbox = await screen.findByRole('listbox');

        const inheritOption = within(listbox).getByRole('option', {name: 'Inherit from workspace/system default'});

        await user.click(inheritOption);

        expect(screen.getByLabelText('Default Routing Policy (optional)')).toHaveTextContent(
            'Inherit from workspace/system default'
        );
    });
});
