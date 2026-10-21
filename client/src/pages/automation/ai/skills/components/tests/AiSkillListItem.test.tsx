import {AiSkill} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AiSkillListItem from '../AiSkillListItem';

const {navigateMock} = vi.hoisted(() => ({navigateMock: vi.fn()}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillDeleteAlertDialog', () => ({
    default: () => <div data-testid="delete-dialog" />,
}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillEditDialog', () => ({
    default: () => <div data-testid="edit-dialog" />,
}));

vi.mock('@/pages/automation/ai/skills/components/AiSkillListItemTagList', () => ({
    default: () => <div data-testid="tag-list" />,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

const skill = {description: 'Runbook for billing', id: '7', name: 'billing-runbook', tags: []} as unknown as AiSkill;

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
});
