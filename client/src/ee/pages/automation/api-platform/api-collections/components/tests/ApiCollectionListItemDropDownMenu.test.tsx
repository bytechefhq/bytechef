import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiCollectionListItemDropDownMenu from '../ApiCollectionListItemDropDownMenu';

const ALL_SCOPES = ['API_PLATFORM_EDIT', 'API_PLATFORM_DELETE', 'DEPLOYMENT_CREATE'];

const hoistedScope = vi.hoisted(() => ({grantedScopes: [] as string[]}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

const defaultProps = {
    apiCollectionId: 1,
    onChangeProjectVersionClick: vi.fn(),
    onDeleteClick: vi.fn(),
    onEditClick: vi.fn(),
    onNewEndpoint: vi.fn(),
};

const openMenu = async () => {
    const user = userEvent.setup();

    render(<ApiCollectionListItemDropDownMenu {...defaultProps} />);

    await user.click(screen.getByRole('button', {name: 'API Collection actions'}));
};

beforeEach(() => {
    hoistedScope.grantedScopes = [...ALL_SCOPES];

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('ApiCollectionListItemDropDownMenu', () => {
    it('should show every item when all scopes are granted', async () => {
        await openMenu();

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Change Project Version'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'New Endpoint'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Download OpenAPI Spec'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should hide Edit and New Endpoint without API_PLATFORM_EDIT', async () => {
        hoistedScope.grantedScopes = ['API_PLATFORM_DELETE', 'DEPLOYMENT_CREATE'];

        await openMenu();

        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'New Endpoint'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Change Project Version'})).toBeInTheDocument();
    });

    it('should hide Change Project Version without DEPLOYMENT_CREATE', async () => {
        hoistedScope.grantedScopes = ['API_PLATFORM_EDIT', 'API_PLATFORM_DELETE'];

        await openMenu();

        expect(screen.queryByRole('menuitem', {name: 'Change Project Version'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
    });

    it('should keep only Download OpenAPI Spec for a member without any scope', async () => {
        hoistedScope.grantedScopes = [];

        await openMenu();

        expect(screen.getAllByRole('menuitem')).toHaveLength(1);
        expect(screen.getByRole('menuitem', {name: 'Download OpenAPI Spec'})).toBeInTheDocument();
    });
});
