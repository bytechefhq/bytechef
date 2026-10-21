import {ProjectStatus, ProjectVersion} from '@/shared/middleware/automation/configuration';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectVersionHistorySheet from '../ProjectVersionHistorySheet';

const projectVersions: ProjectVersion[] = [
    {status: ProjectStatus.Draft, version: 3},
    {
        description: 'Second release',
        publishedDate: new Date('2026-10-10T10:00:00Z'),
        status: ProjectStatus.Published,
        version: 2,
    },
    {publishedDate: new Date('2026-10-09T10:00:00Z'), status: ProjectStatus.Published, version: 1},
];

describe('ProjectVersionHistorySheet', () => {
    const onDeleteProjectVersion = vi.fn();
    const onRestoreProjectVersion = vi.fn();
    const onSheetOpenChange = vi.fn();

    beforeEach(() => {
        onDeleteProjectVersion.mockReset();
        onRestoreProjectVersion.mockReset();
        onSheetOpenChange.mockReset();
    });

    const renderSheet = () =>
        render(
            <ProjectVersionHistorySheet
                onDeleteProjectVersion={onDeleteProjectVersion}
                onRestoreProjectVersion={onRestoreProjectVersion}
                onSheetOpenChange={onSheetOpenChange}
                projectVersions={projectVersions}
                sheetOpen
            />
        );

    it('renders every version with its status and description', () => {
        renderSheet();

        expect(screen.getByText('V3')).toBeInTheDocument();
        expect(screen.getByText('V2')).toBeInTheDocument();
        expect(screen.getByText('V1')).toBeInTheDocument();
        expect(screen.getByText('Draft')).toBeInTheDocument();
        expect(screen.getAllByText('Published')).toHaveLength(2);
        expect(screen.getByText('Second release')).toBeInTheDocument();
        expect(screen.getByText('No description.')).toBeInTheDocument();
    });

    it('keeps Apply disabled until a published version is selected', () => {
        renderSheet();

        const applyButton = screen.getByRole('button', {name: 'Apply selected version'});

        expect(applyButton).toBeDisabled();

        fireEvent.click(screen.getByRole('option', {name: 'Version 3'}));

        expect(applyButton).toBeDisabled();

        fireEvent.click(screen.getByRole('option', {name: 'Version 2'}));

        expect(applyButton).toBeEnabled();
        expect(screen.getByRole('option', {name: 'Version 2'})).toHaveAttribute('aria-selected', 'true');
    });

    it('restores the selected version after confirmation and closes the sheet', () => {
        onRestoreProjectVersion.mockImplementation((_projectVersion: number, onSuccess: () => void) => onSuccess());

        renderSheet();

        fireEvent.click(screen.getByRole('option', {name: 'Version 1'}));
        fireEvent.click(screen.getByRole('button', {name: 'Apply selected version'}));

        expect(screen.getByText('Apply version V1?')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Apply'}));

        expect(onRestoreProjectVersion).toHaveBeenCalledWith(1, expect.any(Function));
        expect(onSheetOpenChange).toHaveBeenCalledWith(false);
    });

    it('offers delete only for published versions and deletes after confirmation', () => {
        renderSheet();

        expect(screen.queryByRole('button', {name: 'Delete version 3'})).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Delete version 2'}));

        expect(screen.getByText('Delete version V2?')).toBeInTheDocument();
        expect(screen.getByRole('button', {hidden: true, name: 'Apply selected version'})).toBeDisabled();

        fireEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(onDeleteProjectVersion).toHaveBeenCalledWith(2, expect.any(Function));
    });
});
