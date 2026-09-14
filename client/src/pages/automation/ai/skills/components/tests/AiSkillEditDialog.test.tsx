import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import AiSkillEditDialog from '../AiSkillEditDialog';

const onClose = vi.fn();
const onSave = vi.fn();

const renderDialog = (currentDescription: string | null = 'Summarizes emails') =>
    render(
        <AiSkillEditDialog
            currentDescription={currentDescription}
            currentName="skill12"
            onClose={onClose}
            onSave={onSave}
        />
    );

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AiSkillEditDialog', () => {
    it('should render the current name and description with Save disabled until something changes', () => {
        renderDialog();

        expect(screen.getByText('Edit Skill')).toBeInTheDocument();
        expect(screen.getByLabelText('Name')).toHaveValue('skill12');
        expect(screen.getByLabelText('Description')).toHaveValue('Summarizes emails');
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('should save the trimmed name and a null description when the description is cleared', async () => {
        const user = userEvent.setup();

        renderDialog();

        await user.clear(screen.getByLabelText('Name'));
        await user.type(screen.getByLabelText('Name'), '  renamed  ');
        await user.clear(screen.getByLabelText('Description'));
        await user.click(screen.getByRole('button', {name: 'Save'}));

        expect(onSave).toHaveBeenCalledWith('renamed', null);
    });

    it('should keep Save disabled when the name is blank', async () => {
        const user = userEvent.setup();

        renderDialog(null);

        await user.clear(screen.getByLabelText('Name'));
        await user.type(screen.getByLabelText('Name'), '   ');

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('should close when Cancel is clicked', async () => {
        const user = userEvent.setup();

        renderDialog();

        await user.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onClose).toHaveBeenCalled();
    });

    it('should close when the close button is clicked', async () => {
        const user = userEvent.setup();

        renderDialog();

        await user.click(screen.getByRole('button', {name: 'Close'}));

        expect(onClose).toHaveBeenCalled();
    });
});
