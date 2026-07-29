import {CustomComponentLanguage} from '@/shared/middleware/graphql';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CustomComponentDetail from './CustomComponentDetail';

const hoisted = vi.hoisted(() => ({
    mockUseCustomComponentDefinitionQuery: vi.fn(),
    mockUseCustomComponentQuery: vi.fn(),
    mockUseCustomComponentSourceQuery: vi.fn(),
    mockUseUpdateCustomComponentSourceMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useCustomComponentDefinitionQuery: hoisted.mockUseCustomComponentDefinitionQuery,
        useCustomComponentQuery: hoisted.mockUseCustomComponentQuery,
        useCustomComponentSourceQuery: hoisted.mockUseCustomComponentSourceQuery,
        useUpdateCustomComponentSourceMutation: hoisted.mockUseUpdateCustomComponentSourceMutation,
    };
});

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({defaultLanguage, value}: {defaultLanguage: string; value: string}) => (
        <div data-language={defaultLanguage} data-testid="monaco-editor-mock">
            {value}
        </div>
    ),
}));

const renderDetail = (id: string) =>
    render(
        <MemoryRouter initialEntries={[`/custom-components/${id}`]}>
            <Routes>
                <Route element={<CustomComponentDetail />} path="/custom-components/:id" />
            </Routes>
        </MemoryRouter>
    );

const renderEmbeddedDetail = (customComponentId: string) =>
    render(
        <MemoryRouter initialEntries={['/']}>
            <Routes>
                <Route element={<CustomComponentDetail customComponentId={customComponentId} />} path="/" />
            </Routes>
        </MemoryRouter>
    );

beforeEach(() => {
    hoisted.mockUseCustomComponentDefinitionQuery.mockReturnValue({data: undefined, error: null, isLoading: false});
    hoisted.mockUseCustomComponentSourceQuery.mockReturnValue({data: undefined, error: null, isLoading: false});
    hoisted.mockUseUpdateCustomComponentSourceMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CustomComponentDetail', () => {
    it('renders an editable Monaco editor for a non-Java custom component', async () => {
        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 1,
                    description: null,
                    enabled: true,
                    id: '1',
                    language: CustomComponentLanguage.Javascript,
                    name: 'my-component',
                    title: 'My Component',
                },
            },
            error: null,
            isLoading: false,
        });
        hoisted.mockUseCustomComponentSourceQuery.mockReturnValue({
            data: {customComponentSource: 'console.log("hi");'},
            error: null,
            isLoading: false,
        });

        renderDetail('1');

        const editor = await screen.findByTestId('monaco-editor-mock');

        expect(editor).toHaveAttribute('data-language', 'javascript');
        expect(editor).toHaveTextContent('console.log("hi");');
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('renders a read-only definition view for a Java custom component without an editor', async () => {
        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 2,
                    description: 'A Java component',
                    enabled: true,
                    id: '2',
                    language: CustomComponentLanguage.Java,
                    name: 'my-java-component',
                    title: 'My Java Component',
                },
            },
            error: null,
            isLoading: false,
        });
        hoisted.mockUseCustomComponentDefinitionQuery.mockReturnValue({
            data: {
                customComponentDefinition: {
                    actions: [{description: null, name: 'doSomething', title: 'Do Something'}],
                    triggers: [],
                },
            },
            error: null,
            isLoading: false,
        });

        renderDetail('2');

        expect(await screen.findByText('Do Something')).toBeInTheDocument();
        expect(screen.queryByTestId('monaco-editor-mock')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument();
    });

    it('fetches and renders by the customComponentId prop without a route param', async () => {
        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 1,
                    description: null,
                    enabled: true,
                    id: '7',
                    language: CustomComponentLanguage.Javascript,
                    name: 'my-embedded-component',
                    title: 'My Embedded Component',
                },
            },
            error: null,
            isLoading: false,
        });
        hoisted.mockUseCustomComponentSourceQuery.mockReturnValue({
            data: {customComponentSource: 'console.log("embedded");'},
            error: null,
            isLoading: false,
        });

        renderEmbeddedDetail('7');

        const editor = await screen.findByTestId('monaco-editor-mock');

        expect(editor).toHaveTextContent('console.log("embedded");');
        expect(hoisted.mockUseCustomComponentQuery).toHaveBeenCalledWith({id: '7'}, {enabled: true});
        expect(screen.queryByRole('button', {name: 'Back'})).not.toBeInTheDocument();
    });
});
