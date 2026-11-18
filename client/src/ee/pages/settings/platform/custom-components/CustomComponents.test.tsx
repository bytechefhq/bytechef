import {render, resetAll, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, test, vi} from 'vitest';

import CustomComponents from './CustomComponents';

const hoisted = vi.hoisted(() => ({
    mockUseCustomComponentsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useCreateCustomComponentMutation: () => ({isPending: false, mutate: vi.fn()}),
        useCustomComponentsQuery: hoisted.mockUseCustomComponentsQuery,
    };
});

const renderCustomComponents = () =>
    render(
        <MemoryRouter>
            <CustomComponents />
        </MemoryRouter>
    );

beforeEach(() => {
    hoisted.mockUseCustomComponentsQuery.mockReturnValue({
        data: {customComponents: []},
        error: null,
        isLoading: false,
    });
});

afterEach(() => {
    resetAll();
});

describe('CustomComponents', () => {
    test('offers creating and importing a component from one New Component menu', async () => {
        const user = userEvent.setup();

        renderCustomComponents();

        expect(screen.getAllByRole('button', {name: /new component/i})).toHaveLength(1);
        expect(screen.queryByRole('button', {name: /import component/i})).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: /new component/i}));

        expect(screen.getByRole('menuitem', {name: /new component/i})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: /import component/i})).toBeInTheDocument();
    });

    test('opens the create dialog from the New Component menu item', async () => {
        const user = userEvent.setup();

        renderCustomComponents();

        await user.click(screen.getByRole('button', {name: /new component/i}));
        await user.click(screen.getByRole('menuitem', {name: /new component/i}));

        expect(await screen.findByRole('dialog', {name: /new custom component/i})).toBeInTheDocument();
    });

    test('opens the import dialog from the Import Component menu item', async () => {
        const user = userEvent.setup();

        renderCustomComponents();

        await user.click(screen.getByRole('button', {name: /new component/i}));
        await user.click(screen.getByRole('menuitem', {name: /import component/i}));

        expect(await screen.findByRole('dialog', {name: /import custom component/i})).toBeInTheDocument();
    });
});
