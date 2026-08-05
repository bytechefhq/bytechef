import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import {render, resetAll, screen, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import NewIntegrationCodeWorkflowDialog from './NewIntegrationCodeWorkflowDialog';

const hoisted = vi.hoisted(() => ({
    mockGetIntegration: vi.fn(),
    mockMutate: vi.fn(),
    mockNavigate: vi.fn(),
    mockOnClose: vi.fn(),
    mockUseCreateIntegrationCodeWorkflowMutation: vi.fn(),
    mockUseGetComponentDefinitionsQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: hoisted.mockUseGetComponentDefinitionsQuery,
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
        useCreateIntegrationCodeWorkflowMutation: hoisted.mockUseCreateIntegrationCodeWorkflowMutation,
    };
});

vi.mock('@/ee/shared/middleware/embedded/configuration', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/ee/shared/middleware/embedded/configuration');

    class MockIntegrationApi {
        getIntegration = hoisted.mockGetIntegration;
    }

    return {
        ...actual,
        IntegrationApi: MockIntegrationApi,
    };
});

const renderDialog = () =>
    render(
        <MemoryRouter initialEntries={['/embedded/integrations']}>
            <Routes>
                <Route element={<NewIntegrationCodeWorkflowDialog onClose={hoisted.mockOnClose} />} path="*" />
            </Routes>
        </MemoryRouter>
    );

beforeEach(() => {
    // resetAll deletes it after each test, and the component ComboBox (cmdk) needs it to open.
    windowResizeObserver();

    hoisted.mockGetIntegration.mockResolvedValue({id: 42, integrationWorkflowIds: [4200]});

    hoisted.mockUseGetComponentDefinitionsQuery.mockReturnValue({
        data: [
            {name: 'my-component', title: 'My Component', version: 1},
            {name: 'other-component', title: 'Other Component', version: 1},
        ],
    });

    hoisted.mockUseCreateIntegrationCodeWorkflowMutation.mockImplementation(
        (options?: {onSuccess?: (data: {createIntegrationCodeWorkflow: string}) => void}) => ({
            isPending: false,
            mutate: (variables: {componentName: string; language: CodeWorkflowLanguage}) => {
                hoisted.mockMutate(variables);

                options?.onSuccess?.({createIntegrationCodeWorkflow: '42'});
            },
        })
    );
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

/**
 * The component is chosen from the catalog rather than typed, so the integration is bound to a component that exists.
 */
const selectComponent = async (user: ReturnType<typeof userEvent.setup>, title: string) => {
    // Language leads the form, so the component picker is the second combobox.
    await user.click(screen.getAllByRole('combobox')[1]);

    await user.click(await screen.findByText(title));
};

describe('NewIntegrationCodeWorkflowDialog', () => {
    it('creates a JavaScript code workflow and navigates to the new integration on success', async () => {
        const user = userEvent.setup();

        renderDialog();

        await selectComponent(user, 'My Component');

        await user.click(screen.getByRole('button', {name: /create/i}));

        expect(hoisted.mockMutate).toHaveBeenCalledWith({
            categoryId: undefined,
            componentName: 'my-component',
            description: undefined,
            language: CodeWorkflowLanguage.Javascript,
            name: 'My Component',
            permissionExpression: undefined,
            tags: [],
        });

        await waitFor(() => {
            expect(hoisted.mockNavigate).toHaveBeenCalledWith('/embedded/integrations/42/integration-workflows/4200');
        });

        expect(hoisted.mockOnClose).toHaveBeenCalled();
    });

    it('keeps the dialog open when the mutation does not call onSuccess (e.g. a server-side error)', async () => {
        hoisted.mockUseCreateIntegrationCodeWorkflowMutation.mockImplementation(() => ({
            isPending: false,
            mutate: (variables: {componentName: string; language: CodeWorkflowLanguage}) => {
                hoisted.mockMutate(variables);
            },
        }));

        const user = userEvent.setup();

        renderDialog();

        await selectComponent(user, 'Other Component');

        await user.click(screen.getByRole('button', {name: /create/i}));

        expect(hoisted.mockMutate).toHaveBeenCalledWith({
            categoryId: undefined,
            componentName: 'other-component',
            description: undefined,
            language: CodeWorkflowLanguage.Javascript,
            name: 'Other Component',
            permissionExpression: undefined,
            tags: [],
        });

        expect(hoisted.mockNavigate).not.toHaveBeenCalled();
        expect(hoisted.mockOnClose).not.toHaveBeenCalled();
        expect(screen.getByText('Component')).toBeInTheDocument();
    });

    it('disables the Cancel button while the mutation is pending', () => {
        hoisted.mockUseCreateIntegrationCodeWorkflowMutation.mockImplementation(() => ({
            isPending: true,
            mutate: hoisted.mockMutate,
        }));

        renderDialog();

        expect(screen.getByRole('button', {name: /cancel/i})).toBeDisabled();
    });
});
