import {AiSkill} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AiSkillListItem from '../AiSkillListItem';

const {canManageMock, navigateMock} = vi.hoisted(() => ({
    canManageMock: vi.fn<(createdBy?: string | null) => boolean>(() => true),
    navigateMock: vi.fn(),
}));

vi.mock('@/pages/automation/ai/skills/hooks/useCanManageAiSkill', () => ({
    default: (createdBy?: string | null) => canManageMock(createdBy),
}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillDeleteAlertDialog', () => ({
    default: () => <div data-testid="delete-dialog" />,
}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillEditDialog', () => ({
    default: () => <div data-testid="edit-dialog" />,
}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillListItemTagList', () => ({
    default: ({readOnly}: {readOnly?: boolean}) => <div data-read-only={String(!!readOnly)} data-testid="tag-list" />,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

const skill = {
    createdBy: 'owner@localhost.com',
    description: 'Runbook for billing',
    id: '7',
    name: 'billing-runbook',
    tags: [],
} as unknown as AiSkill;

const renderListItem = () => {
    const deleteSkill = vi.fn().mockResolvedValue(undefined);
    const onDownload = vi.fn();
    const onUpdate = vi.fn();

    render(
        <MemoryRouter initialEntries={['/automation/settings/ai/skills']}>
            <AiSkillListItem deleteSkill={deleteSkill} onDownload={onDownload} onUpdate={onUpdate} skill={skill} />
        </MemoryRouter>
    );

    return {deleteSkill, onDownload, onUpdate};
};

const openRowMenu = async () => {
    await userEvent.click(screen.getByRole('button'));
};

describe('AiSkillListItem', () => {
    beforeEach(() => {
        windowResizeObserver();

        canManageMock.mockReturnValue(true);
    });

    afterEach(() => {
        resetAll();
    });

    it('renders the skill name and description', () => {
        renderListItem();

        expect(screen.getByText('billing-runbook')).toBeInTheDocument();
        expect(screen.getByText('Runbook for billing')).toBeInTheDocument();
    });

    it('renders with the design-system DropdownMenu', async () => {
        renderListItem();

        await openRowMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Download'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('downloads the skill when Download is clicked', async () => {
        const {onDownload} = renderListItem();

        await openRowMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Download'}));

        expect(onDownload).toHaveBeenCalledWith('7', 'billing-runbook');
    });

    it('opens the edit dialog when Edit is clicked', async () => {
        renderListItem();

        expect(screen.queryByTestId('edit-dialog')).not.toBeInTheDocument();

        await openRowMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('edit-dialog')).toBeInTheDocument();
    });

    it('opens the delete dialog when Delete is clicked', async () => {
        renderListItem();

        expect(screen.queryByTestId('delete-dialog')).not.toBeInTheDocument();

        await openRowMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
    });

    it('does not open the skill detail when a menu row is clicked', async () => {
        renderListItem();

        await openRowMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Download'}));

        expect(navigateMock).not.toHaveBeenCalled();
    });

    it('checks whether the viewer may manage the skill by its creator', () => {
        renderListItem();

        expect(canManageMock).toHaveBeenCalledWith('owner@localhost.com');
        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'false');
    });

    it('offers only Download and read-only tags to a member who may not manage the skill', async () => {
        canManageMock.mockReturnValue(false);

        renderListItem();

        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'true');

        await openRowMenu();

        expect(screen.getByRole('menuitem', {name: 'Download'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
    });
});
