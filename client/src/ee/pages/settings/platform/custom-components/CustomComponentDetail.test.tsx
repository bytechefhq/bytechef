import {CustomComponentLanguage, CustomComponentStatus} from '@/shared/middleware/graphql';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {fireEvent, render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CustomComponentDetail from './CustomComponentDetail';

const hoisted = vi.hoisted(() => ({
    mockNavigate: vi.fn(),
    mockUseCustomComponentDefinitionQuery: vi.fn(),
    mockUseCustomComponentQuery: vi.fn(),
    mockUseCustomComponentSourceQuery: vi.fn(),
    mockUsePublishCustomComponentMutation: vi.fn(),
    mockUseUpdateCustomComponentSourceMutation: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('react-router-dom');

    return {
        ...actual,
        useNavigate: () => hoisted.mockNavigate,
    };
});

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useCustomComponentDefinitionQuery: hoisted.mockUseCustomComponentDefinitionQuery,
        useCustomComponentQuery: hoisted.mockUseCustomComponentQuery,
        useCustomComponentSourceQuery: hoisted.mockUseCustomComponentSourceQuery,
        usePublishCustomComponentMutation: hoisted.mockUsePublishCustomComponentMutation,
        useUpdateCustomComponentSourceMutation: hoisted.mockUseUpdateCustomComponentSourceMutation,
    };
});

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({
        defaultLanguage,
        onChange,
        value,
    }: {
        defaultLanguage: string;
        onChange: (value: string | undefined) => void;
        value: string;
    }) => (
        <div data-language={defaultLanguage} data-testid="monaco-editor-mock">
            <button onClick={() => onChange('edited content')} type="button">
                Edit
            </button>

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
    hoisted.mockUsePublishCustomComponentMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
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

    it('shows a Publish button enabled only when the component is a clean draft', async () => {
        hoisted.mockUseCustomComponentSourceQuery.mockReturnValue({
            data: {customComponentSource: 'console.log("hi");'},
            error: null,
            isLoading: false,
        });

        // DRAFT + not dirty -> enabled
        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 1,
                    description: null,
                    enabled: true,
                    id: '1',
                    language: CustomComponentLanguage.Javascript,
                    name: 'my-component',
                    status: CustomComponentStatus.Draft,
                    title: 'My Component',
                },
            },
            error: null,
            isLoading: false,
        });

        const cleanDraftRender = renderDetail('1');

        expect(await screen.findByRole('button', {name: 'Publish'})).toBeEnabled();

        cleanDraftRender.unmount();

        // DRAFT + dirty -> disabled
        const dirtyDraftRender = renderDetail('1');

        await screen.findByTestId('monaco-editor-mock');

        fireEvent.click(screen.getByRole('button', {name: 'Edit'}));

        expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();

        dirtyDraftRender.unmount();

        // PUBLISHED -> disabled
        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 1,
                    description: null,
                    enabled: true,
                    id: '1',
                    language: CustomComponentLanguage.Javascript,
                    name: 'my-component',
                    status: CustomComponentStatus.Published,
                    title: 'My Component',
                },
            },
            error: null,
            isLoading: false,
        });

        renderDetail('1');

        expect(await screen.findByRole('button', {name: 'Publish'})).toBeDisabled();
    });

    it('navigates to the new draft when saving a published component returns a different id', async () => {
        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 1,
                    description: null,
                    enabled: true,
                    id: '1',
                    language: CustomComponentLanguage.Javascript,
                    name: 'my-component',
                    status: CustomComponentStatus.Published,
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

        let onSuccess: ((result: unknown) => void) | undefined;

        hoisted.mockUseUpdateCustomComponentSourceMutation.mockImplementation(
            (options: {onSuccess?: (result: unknown) => void}) => {
                onSuccess = options?.onSuccess;

                return {isPending: false, mutate: vi.fn()};
            }
        );

        renderDetail('1');

        await screen.findByTestId('monaco-editor-mock');

        onSuccess?.({updateCustomComponentSource: {id: '99'}});

        expect(hoisted.mockNavigate).toHaveBeenCalledWith('/custom-components/99');
    });

    it('does not navigate when saving in prop-driven mode returns a different id', async () => {
        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 1,
                    description: null,
                    enabled: true,
                    id: '7',
                    language: CustomComponentLanguage.Javascript,
                    name: 'my-embedded-component',
                    status: CustomComponentStatus.Published,
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

        let onSuccess: ((result: unknown) => void) | undefined;

        hoisted.mockUseUpdateCustomComponentSourceMutation.mockImplementation(
            (options: {onSuccess?: (result: unknown) => void}) => {
                onSuccess = options?.onSuccess;

                return {isPending: false, mutate: vi.fn()};
            }
        );

        renderEmbeddedDetail('7');

        await screen.findByTestId('monaco-editor-mock');

        onSuccess?.({updateCustomComponentSource: {id: '99'}});

        expect(hoisted.mockNavigate).not.toHaveBeenCalled();
    });

    it('shows the Ask Copilot button when the copilot is enabled and the editor is route-driven', async () => {
        applicationInfoStore.setState((state) => ({...state, ai: {...state.ai, copilot: {enabled: true}}}));

        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 1,
                    description: null,
                    enabled: true,
                    id: '1',
                    language: CustomComponentLanguage.Javascript,
                    name: 'my-component',
                    status: CustomComponentStatus.Draft,
                    title: 'My Component',
                },
            },
            error: null,
            isLoading: false,
        });
        hoisted.mockUseCustomComponentSourceQuery.mockReturnValue({
            data: {customComponentSource: 'source'},
            error: null,
            isLoading: false,
        });

        renderDetail('1');

        expect(await screen.findByRole('button', {name: 'Ask Copilot'})).toBeInTheDocument();

        applicationInfoStore.setState((state) => ({...state, ai: {...state.ai, copilot: {enabled: false}}}));
    });

    it('hides the Ask Copilot button in the prop-driven embedding even when the copilot is enabled', async () => {
        applicationInfoStore.setState((state) => ({...state, ai: {...state.ai, copilot: {enabled: true}}}));

        hoisted.mockUseCustomComponentQuery.mockReturnValue({
            data: {
                customComponent: {
                    componentVersion: 1,
                    description: null,
                    enabled: true,
                    id: '7',
                    language: CustomComponentLanguage.Javascript,
                    name: 'my-component',
                    status: CustomComponentStatus.Draft,
                    title: 'My Component',
                },
            },
            error: null,
            isLoading: false,
        });
        hoisted.mockUseCustomComponentSourceQuery.mockReturnValue({
            data: {customComponentSource: 'source'},
            error: null,
            isLoading: false,
        });

        renderEmbeddedDetail('7');

        await screen.findByTestId('monaco-editor-mock');

        expect(screen.queryByRole('button', {name: 'Ask Copilot'})).not.toBeInTheDocument();

        applicationInfoStore.setState((state) => ({...state, ai: {...state.ai, copilot: {enabled: false}}}));
    });
});
