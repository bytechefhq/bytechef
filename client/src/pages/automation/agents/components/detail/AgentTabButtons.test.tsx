import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AgentTabButtons from './AgentTabButtons';

const renderAgentTabButtons = (isDeletePending = false) => {
    const onCloseDropdownMenu = vi.fn();
    const onDeleteClick = vi.fn();
    const onExportClick = vi.fn();
    const onShowEditDialog = vi.fn();
    const onShowVersionHistorySheet = vi.fn();

    render(
        <AgentTabButtons
            isDeletePending={isDeletePending}
            onCloseDropdownMenu={onCloseDropdownMenu}
            onDeleteClick={onDeleteClick}
            onExportClick={onExportClick}
            onShowEditDialog={onShowEditDialog}
            onShowVersionHistorySheet={onShowVersionHistorySheet}
        />
    );

    return {onCloseDropdownMenu, onDeleteClick, onExportClick, onShowEditDialog, onShowVersionHistorySheet};
};

describe('AgentTabButtons', () => {
    it('opens the edit dialog and closes the dropdown when Edit is clicked', () => {
        const {onCloseDropdownMenu, onShowEditDialog} = renderAgentTabButtons();

        fireEvent.click(screen.getByRole('button', {name: 'Edit'}));

        expect(onShowEditDialog).toHaveBeenCalled();
        expect(onCloseDropdownMenu).toHaveBeenCalled();
    });

    it('opens the version history sheet when Agent History is clicked', () => {
        const {onShowVersionHistorySheet} = renderAgentTabButtons();

        fireEvent.click(screen.getByRole('button', {name: 'Agent History'}));

        expect(onShowVersionHistorySheet).toHaveBeenCalled();
    });

    it('triggers the export when Export is clicked', () => {
        const {onExportClick} = renderAgentTabButtons();

        fireEvent.click(screen.getByRole('button', {name: 'Export'}));

        expect(onExportClick).toHaveBeenCalled();
    });

    it('calls the delete mutation when Delete is clicked', () => {
        const {onDeleteClick} = renderAgentTabButtons();

        fireEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(onDeleteClick).toHaveBeenCalled();
    });

    it('disables the Delete button while the delete mutation is pending', () => {
        renderAgentTabButtons(true);

        expect(screen.getByRole('button', {name: 'Delete'})).toBeDisabled();
    });
});
