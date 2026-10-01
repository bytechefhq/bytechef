import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentModelDialog from './AgentModelDialog';

beforeAll(() => {
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {getClusterElementDefinitionQuery, getRootComponentClusterElementDefinitions} = vi.hoisted(() => ({
    getClusterElementDefinitionQuery: vi.fn(),
    getRootComponentClusterElementDefinitions: vi.fn(),
}));

vi.mock('@/shared/components/component-config/ComponentConfigDialog', () => ({
    default: ({
        excludedPropertyNames,
        onSubmit,
        picker,
        target,
    }: {
        excludedPropertyNames?: string[];
        onSubmit: (values: {connectionId: string | null; parameters: Record<string, unknown>}) => void;
        picker?: ReactNode;
        target: {componentName: string} | null;
    }) => (
        <div>
            <span data-testid="target">{target ? target.componentName : 'none'}</span>

            <span data-testid="excluded">{(excludedPropertyNames ?? []).join(',')}</span>

            {picker}

            <button onClick={() => onSubmit({connectionId: 'conn-9', parameters: {temperature: 0.5}})} type="button">
                save
            </button>
        </div>
    ),
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    useGetClusterElementDefinitionQuery: getClusterElementDefinitionQuery,
    useGetRootComponentClusterElementDefinitions: getRootComponentClusterElementDefinitions,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiProviderCatalogQuery: () => CATALOG_RESULT,
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 123})),
}));

const {CATALOG_RESULT, DEFINITION_BY_COMPONENT, NO_DEFINITION, PROVIDERS_RESULT} = vi.hoisted(() => ({
    CATALOG_RESULT: {data: {aiProviderCatalog: [{key: 'openAi', models: [{label: 'GPT-4o', name: 'gpt-4o'}]}]}},
    DEFINITION_BY_COMPONENT: {
        anthropic: {data: {properties: [{name: 'model', options: [{label: 'Opus', value: 'claude-opus-4-1'}]}]}},
        ollama: {data: {properties: [{name: 'model', optionsDataSource: {optionsLookupDependsOn: []}}]}},
        openAi: {
            data: {
                properties: [
                    {
                        name: 'model',
                        options: [
                            {label: 'Gpt 4o', value: 'gpt-4o'},
                            {label: 'Gpt 4o mini', value: 'gpt-4o-mini'},
                        ],
                    },
                ],
            },
        },
    } as Record<string, {data: {properties: {name: string; options?: unknown[]; optionsDataSource?: unknown}[]}}>,
    NO_DEFINITION: {data: undefined},
    PROVIDERS_RESULT: {
        data: [
            {componentName: 'anthropic', componentVersion: 1, name: 'model', title: 'Antropic Model'},
            {componentName: 'ollama', componentVersion: 1, name: 'model', title: 'Ollama Model'},
            {componentName: 'openAi', componentVersion: 1, name: 'model', title: 'OpenAI Model'},
        ],
    },
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

beforeEach(() => {
    getRootComponentClusterElementDefinitions.mockReset().mockReturnValue(PROVIDERS_RESULT);

    getClusterElementDefinitionQuery
        .mockReset()
        .mockImplementation(
            ({componentName}: {componentName: string}) => DEFINITION_BY_COMPONENT[componentName] ?? NO_DEFINITION
        );
});

describe('AgentModelDialog', () => {
    it('names a model as the catalog does, falling back to the component option label', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(
            <AgentModelDialog
                connectionId={null}
                model=""
                onClose={vi.fn()}
                onSubmit={vi.fn()}
                parameters={{}}
                provider=""
                workspaceId={7}
            />
        );

        await user.click(screen.getByRole('combobox', {name: /provider/i}));
        await user.click(await screen.findByText('OpenAI'));

        await user.click(screen.getByRole('combobox', {name: /model/i}));

        expect(await screen.findByText('GPT-4o')).toBeInTheDocument();
        expect(screen.getByText('Gpt 4o mini')).toBeInTheDocument();
    });

    it('reports the provider, model, connection and properties together on save', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});
        const onSubmit = vi.fn();

        wrap(
            <AgentModelDialog
                connectionId={null}
                model=""
                onClose={vi.fn()}
                onSubmit={onSubmit}
                parameters={{}}
                provider=""
                workspaceId={7}
            />
        );

        await user.click(screen.getByRole('combobox', {name: /provider/i}));
        await user.click(await screen.findByText('OpenAI'));

        await user.click(screen.getByRole('combobox', {name: /model/i}));
        await user.click(await screen.findByText('GPT-4o'));

        await user.click(screen.getByRole('button', {name: 'save'}));

        expect(onSubmit).toHaveBeenCalledWith({
            connectionId: 'conn-9',
            model: 'gpt-4o',
            parameters: {temperature: 0.5},
            provider: 'openAi',
        });
    });

    it('lets a model ID be entered for a provider whose models are loaded from its server', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});
        const onSubmit = vi.fn();

        wrap(
            <AgentModelDialog
                connectionId={null}
                model=""
                onClose={vi.fn()}
                onSubmit={onSubmit}
                parameters={{}}
                provider=""
                workspaceId={7}
            />
        );

        await user.click(screen.getByRole('combobox', {name: /provider/i}));
        await user.click(await screen.findByText('Ollama'));

        await user.type(screen.getByRole('textbox', {name: /model/i}), 'llama3.1');

        expect(screen.getByTestId('target')).toHaveTextContent('ollama');

        await user.click(screen.getByRole('button', {name: 'save'}));

        expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({model: 'llama3.1', provider: 'ollama'}));
    });

    it('drops the chosen model when the provider changes', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(
            <AgentModelDialog
                connectionId="conn-1"
                model="gpt-4o"
                onClose={vi.fn()}
                onSubmit={vi.fn()}
                parameters={{}}
                provider="openAi"
                workspaceId={7}
            />
        );

        expect(screen.getByTestId('target')).toHaveTextContent('openAi');

        await user.click(screen.getByRole('combobox', {name: /provider/i}));
        await user.click(await screen.findByText('Antropic'));

        expect(screen.getByTestId('target')).toHaveTextContent('none');
    });
    it('keeps the model property out of the Properties tab, since the combobox owns it', () => {
        wrap(
            <AgentModelDialog
                connectionId={null}
                model="gpt-4o"
                onClose={vi.fn()}
                onSubmit={vi.fn()}
                parameters={{}}
                provider="openAi"
                workspaceId={7}
            />
        );

        expect(screen.getByTestId('excluded')).toHaveTextContent('model');
    });
});
