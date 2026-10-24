import DataSyncMappingStep from '@/pages/automation/data-syncs/components/wizard/DataSyncMappingStep';
import {DataSyncElementKind} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import {createElement} from 'react';
import {describe, expect, it, vi} from 'vitest';

const {getOptionsMock, setElementMock, updateElementMock} = vi.hoisted(() => ({
    getOptionsMock: vi.fn(),
    setElementMock: vi.fn(),
    updateElementMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useSetDataSyncElementMutation: () => ({isPending: false, mutate: setElementMock}),
        useUpdateDataSyncElementMutation: () => ({isPending: false, mutate: updateElementMock}),
    };
});

vi.mock('@/shared/middleware/platform/configuration', () => ({
    WorkflowNodeOptionApi: class {
        getClusterElementNodeOptions = getOptionsMock;
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

const renderStep = (elements: unknown[]) =>
    render(
        createElement(
            QueryClientProvider,
            {client: new QueryClient()},
            createElement(DataSyncMappingStep, {
                dataSync: {draftWorkflowId: 'wf', elements, id: '10'} as never,
            })
        )
    );

const source = {
    componentName: 'csvFile',
    componentVersion: 1,
    id: '1',
    kind: DataSyncElementKind.Source,
    operationName: 'read',
};
const destination = {
    componentName: 'pg',
    componentVersion: 1,
    id: '2',
    kind: DataSyncElementKind.Destination,
    operationName: 'insert',
};
const processor = {
    componentName: 'dataStreamProcessor',
    componentVersion: 1,
    id: '3',
    kind: DataSyncElementKind.Processor,
    operationName: 'fieldMapper',
    parameters: {mappings: []},
};

describe('DataSyncMappingStep', () => {
    it('shows a loading message before the field lists resolve', () => {
        getOptionsMock.mockImplementation(() => new Promise(() => {}));

        renderStep([source, destination, processor]);

        expect(screen.getByText('Loading available fields…')).toBeInTheDocument();
        expect(screen.queryByLabelText('Source field 1')).not.toBeInTheDocument();
    });

    it('explains that mapping is unsupported when neither side exposes any fields', async () => {
        getOptionsMock.mockResolvedValue([]);

        renderStep([source, destination, processor]);

        await waitFor(() => {
            expect(
                screen.getByText(
                    "This source and destination don't expose their fields automatically, so mapping them isn't supported here yet."
                )
            ).toBeInTheDocument();
        });

        expect(screen.queryByRole('button', {name: 'Auto-map matching fields'})).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Source field 1')).not.toBeInTheDocument();
    });

    it('reports a transient failure instead of a permanent capability limit when the fetch is rejected', async () => {
        getOptionsMock.mockRejectedValue(new Error('network error'));

        renderStep([source, destination, processor]);

        await waitFor(() => {
            expect(
                screen.getByText("Couldn't load the source and destination fields. Try again in a moment.")
            ).toBeInTheDocument();
        });

        expect(
            screen.queryByText(
                "This source and destination don't expose their fields automatically, so mapping them isn't supported here yet."
            )
        ).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Auto-map matching fields'})).not.toBeInTheDocument();
    });

    it('renders the mapping pickers once fields are actually available', async () => {
        getOptionsMock.mockImplementation(({propertyName}: {propertyName: string}) =>
            Promise.resolve(propertyName.endsWith('sourceField') ? [{value: 'email'}] : [{value: 'email_address'}])
        );

        renderStep([source, destination, processor]);

        await waitFor(() => {
            expect(screen.getByRole('button', {name: 'Auto-map matching fields'})).toBeInTheDocument();
        });

        expect(
            screen.queryByText(
                "This source and destination don't expose their fields automatically, so mapping them isn't supported here yet."
            )
        ).not.toBeInTheDocument();
    });
});
