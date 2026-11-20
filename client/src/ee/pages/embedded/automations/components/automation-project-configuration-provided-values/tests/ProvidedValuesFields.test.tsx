import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, describe, expect, it, vi} from 'vitest';

import ProvidedValuesFields from '../ProvidedValuesFields';
import {ProvidableValuesType} from '../providedValues-utils';

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: vi.fn(() => ({data: undefined, isLoading: false})),
}));

const QueryWrapper = ({children}: {children: ReactNode}) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
};

const createProvidableValues = (input: {
    internalOnly: boolean;
    required: boolean;
    type: string;
}): ProvidableValuesType =>
    ({
        connectionSlots: [],
        inputs: [
            {
                groupName: null,
                internalOnly: input.internalOnly,
                label: 'API Key',
                name: 'apiKey',
                optionConnections: [],
                required: input.required,
                type: input.type,
            },
        ],
        providedConnections: {},
        providedInputs: {},
    }) as unknown as ProvidableValuesType;

const renderFields = (
    input: {internalOnly: boolean; required: boolean; type: string},
    providedInputs: Record<string, string> = {}
) =>
    render(
        <QueryWrapper>
            <ProvidedValuesFields
                configurationId="1"
                enabled
                onProvidedConnectionsChange={vi.fn()}
                onProvidedInputsChange={vi.fn()}
                providableValues={createProvidableValues(input)}
                providedConnections={{}}
                providedInputs={providedInputs}
                workflowUuid="wf-uuid"
            />
        </QueryWrapper>
    );

const openInputsTab = async () => {
    await userEvent.click(screen.getByRole('tab', {name: /Inputs/}));
};

describe('ProvidedValuesFields', () => {
    beforeAll(() => {
        Element.prototype.hasPointerCapture = () => false;
        Element.prototype.scrollIntoView = () => undefined;
    });

    it('does not offer Let each user fill it for an internal-only input', async () => {
        renderFields({internalOnly: true, required: false, type: 'BOOLEAN'});

        await openInputsTab();

        expect(screen.queryByText('Let each user fill it')).not.toBeInTheDocument();
        expect(screen.getByText('Set a value')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'API Key'}));

        expect(screen.queryByRole('option', {name: 'Let each user fill it'})).not.toBeInTheDocument();
    });

    it('marks a required internal-only input as needed before enabling', async () => {
        renderFields({internalOnly: true, required: true, type: 'STRING'});

        await openInputsTab();

        expect(screen.getByText('API Key *')).toBeInTheDocument();
        expect(screen.getByText('Required before this workflow can be enabled')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Set a value')).toBeInTheDocument();
    });

    it('marks a required internal-only input holding only whitespace as needed before enabling', async () => {
        renderFields({internalOnly: true, required: true, type: 'STRING'}, {apiKey: '   '});

        await openInputsTab();

        expect(screen.getByText('Required before this workflow can be enabled')).toBeInTheDocument();
    });

    it('does not mark a required internal-only input that has a value', async () => {
        renderFields({internalOnly: true, required: true, type: 'STRING'}, {apiKey: 'secret'});

        await openInputsTab();

        expect(screen.queryByText('Required before this workflow can be enabled')).not.toBeInTheDocument();
    });

    it('does not show an input that is not internal-only', async () => {
        renderFields({internalOnly: false, required: true, type: 'STRING'});

        await openInputsTab();

        expect(screen.queryByLabelText('API Key *')).not.toBeInTheDocument();
        expect(screen.queryByPlaceholderText('Let each user fill it')).not.toBeInTheDocument();
        expect(screen.getByText('This workflow has no internal-only inputs to set.')).toBeInTheDocument();
    });
});
