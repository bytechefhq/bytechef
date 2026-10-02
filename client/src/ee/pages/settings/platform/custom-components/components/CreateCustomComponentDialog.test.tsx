import {CustomComponentLanguage} from '@/shared/middleware/graphql';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import CreateCustomComponentDialog from './CreateCustomComponentDialog';

const hoisted = vi.hoisted(() => ({
    mockMutate: vi.fn(),
    mockNavigate: vi.fn(),
    mockUseCreateCustomComponentMutation: vi.fn(),
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
        useCreateCustomComponentMutation: hoisted.mockUseCreateCustomComponentMutation,
    };
});

const renderDialog = () =>
    render(
        <MemoryRouter initialEntries={['/custom-components']}>
            <Routes>
                <Route element={<CreateCustomComponentDialog />} path="*" />
            </Routes>
        </MemoryRouter>
    );

beforeEach(() => {
    hoisted.mockUseCreateCustomComponentMutation.mockImplementation(
        (options?: {onSuccess?: (data: {createCustomComponent: {id: string}}) => void}) => ({
            isPending: false,
            mutate: (variables: {language: CustomComponentLanguage; name: string}) => {
                hoisted.mockMutate(variables);

                options?.onSuccess?.({createCustomComponent: {id: '42'}});
            },
        })
    );
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('CreateCustomComponentDialog', () => {
    it('creates a JavaScript custom component and navigates to its detail route on success', async () => {
        const user = userEvent.setup();

        renderDialog();

        await user.click(screen.getByRole('button', {name: /new component/i}));

        await user.type(screen.getByLabelText('Name'), 'my-component');

        await user.click(screen.getByRole('button', {name: /create/i}));

        expect(hoisted.mockMutate).toHaveBeenCalledWith({
            language: CustomComponentLanguage.Javascript,
            name: 'my-component',
        });

        expect(hoisted.mockNavigate).toHaveBeenCalledWith('42');
    });

    it('keeps the dialog open when the mutation does not call onSuccess (e.g. a server-side error)', async () => {
        hoisted.mockUseCreateCustomComponentMutation.mockImplementation(() => ({
            isPending: false,
            mutate: (variables: {language: CustomComponentLanguage; name: string}) => {
                hoisted.mockMutate(variables);
            },
        }));

        const user = userEvent.setup();

        renderDialog();

        await user.click(screen.getByRole('button', {name: /new component/i}));

        await user.type(screen.getByLabelText('Name'), 'duplicate-name');

        await user.click(screen.getByRole('button', {name: /create/i}));

        expect(hoisted.mockMutate).toHaveBeenCalledWith({
            language: CustomComponentLanguage.Javascript,
            name: 'duplicate-name',
        });

        expect(hoisted.mockNavigate).not.toHaveBeenCalled();
        expect(screen.getByLabelText('Name')).toBeInTheDocument();
    });
});
