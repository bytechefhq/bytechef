import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import VariablesContent from '../VariablesContent';
import {VariablesProvider} from '../providers/variablesProvider';
import {useVariablesStore} from '../stores/useVariablesStore';

vi.mock('@/shared/components/EnvironmentSelect', () => ({default: () => <div>env-select</div>}));

const variables = [{environmentId: '0', id: '1', name: 'API_URL', value: 'https://api'}];

beforeEach(() => {
    useVariablesStore.setState({currentVariable: undefined, showDeleteDialog: false, showEditDialog: false});
});

const renderContent = (canManage = true, data = variables) =>
    render(
        // main.tsx mounts a single TooltipProvider at the app root; VariableTable's truncated
        // Value column renders a Radix Tooltip, which throws without a provider ancestor.
        <TooltipProvider>
            <VariablesProvider
                value={{
                    canManage,
                    useCreateVariableMutation: () => ({mutate: vi.fn()}),
                    useDeleteVariableMutation: () => ({mutate: vi.fn()}),
                    useUpdateVariableMutation: () => ({mutate: vi.fn()}),
                    useVariablesQuery: () => ({data, error: null, isLoading: false}),
                }}
            >
                <VariablesContent description="d" title="Variables" />
            </VariablesProvider>
        </TooltipProvider>
    );

describe('VariablesContent', () => {
    it('lists variables with their reference expression', () => {
        renderContent();

        expect(screen.getByText('API_URL')).toBeInTheDocument();
        expect(screen.getByText('${vars.API_URL}')).toBeInTheDocument();
    });

    it('shows the empty state with a create button', () => {
        renderContent(true, []);

        expect(screen.getByText('No Variables')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'New Variable'})).toBeInTheDocument();
    });

    it('hides create and row actions without manage permission', () => {
        renderContent(false);

        expect(screen.queryByRole('button', {name: 'New Variable'})).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Variable actions')).not.toBeInTheDocument();
    });

    it('validates the name in the dialog', async () => {
        renderContent();

        await userEvent.click(screen.getByRole('button', {name: 'New Variable'}));
        await userEvent.type(screen.getByLabelText('Name'), '1bad');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText(/must start with a letter or underscore/i)).toBeInTheDocument();
    });

    it('validates the value length in the dialog', async () => {
        renderContent();

        await userEvent.click(screen.getByRole('button', {name: 'New Variable'}));
        await userEvent.type(screen.getByLabelText('Name'), 'GOOD_NAME');
        fireEvent.change(screen.getByLabelText('Value'), {target: {value: 'a'.repeat(4097)}});
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText(/cannot be longer than 4096 characters/i)).toBeInTheDocument();
    });

    it('truncates a long value and reveals the full value in a tooltip on hover', async () => {
        const longValue = 'x'.repeat(80);

        renderContent(true, [{environmentId: '0', id: '1', name: 'LONG_VALUE', value: longValue}]);

        const truncated = `${longValue.slice(0, 60)}…`;

        expect(screen.getByText(truncated)).toBeInTheDocument();
        expect(screen.queryByText(longValue)).not.toBeInTheDocument();

        await userEvent.hover(screen.getByText(truncated));

        expect(await screen.findByText(longValue)).toBeInTheDocument();
    });
});
