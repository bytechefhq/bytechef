import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiGatewayRoutingPolicyDialog from '../AiGatewayRoutingPolicyDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    AiGatewayRoutingStrategyType: {
        CostOptimized: 'COST_OPTIMIZED',
        IntelligentBalanced: 'INTELLIGENT_BALANCED',
        IntelligentCost: 'INTELLIGENT_COST',
        IntelligentQuality: 'INTELLIGENT_QUALITY',
        LatencyOptimized: 'LATENCY_OPTIMIZED',
        PriorityFallback: 'PRIORITY_FALLBACK',
        Simple: 'SIMPLE',
        TagBased: 'TAG_BASED',
        WeightedRandom: 'WEIGHTED_RANDOM',
    },
    useCreateWorkspaceAiGatewayRoutingPolicyMutation: () => ({
        isPending: false,
        mutate: hoisted.createMutate,
    }),
    useUpdateWorkspaceAiGatewayRoutingPolicyMutation: () => ({
        isPending: false,
        mutate: hoisted.updateMutate,
    }),
}));

const renderDialog = (onClose = vi.fn()) => {
    render(<AiGatewayRoutingPolicyDialog onClose={onClose} workspaceId="1" />);

    return onClose;
};

describe('AiGatewayRoutingPolicyDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
    });

    it('renders with the dialog role', () => {
        renderDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its title', () => {
        renderDialog();

        expect(screen.getByRole('dialog', {name: 'Add Routing Policy'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        const onClose = renderDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('associates every label with its control', () => {
        renderDialog();

        expect(screen.getByLabelText('Name')).toBeInTheDocument();
        expect(screen.getByLabelText('Strategy')).toBeInTheDocument();
        expect(screen.getByLabelText('Fallback Model (optional)')).toBeInTheDocument();
    });

    it('renders the strategy control as a Radix select trigger', () => {
        renderDialog();

        expect(screen.getByLabelText('Strategy')).toHaveAttribute('data-slot', 'select-trigger');
    });

    it('enables the Create button once name is entered and submits through the create mutation', () => {
        renderDialog();

        const nameInput = screen.getByPlaceholderText('My Routing Policy');

        fireEvent.change(nameInput, {target: {value: 'Production traffic'}});

        const createButton = screen.getByRole('button', {name: 'Create'});

        expect(createButton).not.toBeDisabled();

        fireEvent.click(createButton);

        expect(hoisted.createMutate).toHaveBeenCalledTimes(1);
        expect(hoisted.createMutate).toHaveBeenCalledWith({
            input: {
                fallbackModel: undefined,
                name: 'Production traffic',
                strategy: 'SIMPLE',
                workspaceId: '1',
            },
        });
        expect(hoisted.updateMutate).not.toHaveBeenCalled();
    });

    it('renders the edit form and calls update on Save', () => {
        const existingPolicy = {
            fallbackModel: 'openai/gpt-4o',
            id: '99',
            name: 'High-stakes routes',
            strategy: 'PRIORITY_FALLBACK',
        } as never;

        render(<AiGatewayRoutingPolicyDialog onClose={vi.fn()} routingPolicy={existingPolicy} workspaceId="1" />);

        expect(screen.getByRole('dialog', {name: 'Edit Routing Policy'})).toBeInTheDocument();

        const saveButton = screen.getByRole('button', {name: 'Save'});

        expect(saveButton).not.toBeDisabled();

        fireEvent.click(saveButton);

        expect(hoisted.updateMutate).toHaveBeenCalledTimes(1);
        expect(hoisted.createMutate).not.toHaveBeenCalled();
    });
});
