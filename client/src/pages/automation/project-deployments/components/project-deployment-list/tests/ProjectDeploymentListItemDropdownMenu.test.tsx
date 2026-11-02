import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentListItemDropdownMenu from '../ProjectDeploymentListItemDropdownMenu';

const hoistedScope = vi.hoisted(() => ({grantedScopes: ['DEPLOYMENT_CREATE', 'DEPLOYMENT_DELETE'] as string[]}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

const defaultProps = {
    onChangeProjectVersionClick: vi.fn(),
    onDeleteClick: vi.fn(),
    onEditClick: vi.fn(),
};

beforeEach(() => {
    hoistedScope.grantedScopes = ['DEPLOYMENT_CREATE', 'DEPLOYMENT_DELETE'];

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ProjectDeploymentListItemDropdownMenu', () => {
    it('shows every item when all scopes are granted', async () => {
        render(<ProjectDeploymentListItemDropdownMenu {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'More Deployment Actions'}));

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Change Project Version')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('hides Edit and Change Project Version without DEPLOYMENT_CREATE', async () => {
        hoistedScope.grantedScopes = ['DEPLOYMENT_DELETE'];

        render(<ProjectDeploymentListItemDropdownMenu {...defaultProps} />);

        await userEvent.click(screen.getByRole('button', {name: 'More Deployment Actions'}));

        expect(screen.queryByText('Edit')).not.toBeInTheDocument();
        expect(screen.queryByText('Change Project Version')).not.toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('hides the trigger when no item would be shown', () => {
        hoistedScope.grantedScopes = [];

        render(<ProjectDeploymentListItemDropdownMenu {...defaultProps} />);

        expect(screen.queryByRole('button', {name: 'More Deployment Actions'})).not.toBeInTheDocument();
    });
});
