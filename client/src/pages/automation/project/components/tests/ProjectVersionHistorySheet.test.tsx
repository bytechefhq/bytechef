import {ProjectStatus, ProjectVersion} from '@/shared/middleware/automation/configuration';
import {render} from '@/shared/util/test-utils';
import {fireEvent, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectVersionHistorySheet from '../ProjectVersionHistorySheet';

const {useProjectVersionWorkflowChangeQuery, useProjectVersionWorkflowChangesQuery} = vi.hoisted(() => ({
    useProjectVersionWorkflowChangeQuery: vi.fn(),
    useProjectVersionWorkflowChangesQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<Record<string, unknown>>()),
    useProjectVersionWorkflowChangeQuery,
    useProjectVersionWorkflowChangesQuery,
}));

vi.mock('@/shared/components/version-workflow-changes/WorkflowVersionChangesDialog', () => ({
    default: ({baseVersionLabel, versionLabel}: {baseVersionLabel?: string; versionLabel: string}) => (
        <div data-testid="mock-changes-dialog">{`${baseVersionLabel ?? ''}|${versionLabel}`}</div>
    ),
}));

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
        useProjectVersionWorkflowChangesQuery.mockReset();
        useProjectVersionWorkflowChangeQuery.mockReset();

        useProjectVersionWorkflowChangesQuery.mockReturnValue({
            data: {
                projectVersionWorkflowChanges: {
                    baseVersion: 1,
                    version: 2,
                    workflows: [{changeCount: 2, changeType: 'MODIFIED', label: 'Sync orders', workflowUuid: 'uuid-1'}],
                },
            },
            isLoading: false,
        });
        useProjectVersionWorkflowChangeQuery.mockImplementation(
            (variables: {projectVersion: number}, options?: {enabled?: boolean}) => ({
                data:
                    options?.enabled === false
                        ? undefined
                        : {
                              projectVersionWorkflowChange: {
                                  baseVersion: variables.projectVersion === 3 ? 2 : 1,
                                  changes: [],
                                  changeType: 'MODIFIED',
                                  label: 'Sync orders',
                                  version: variables.projectVersion,
                                  workflowUuid: 'uuid-1',
                              },
                          },
                isLoading: false,
            })
        );
        onDeleteProjectVersion.mockReset();
        onRestoreProjectVersion.mockReset();
        onSheetOpenChange.mockReset();

        Object.defineProperty(window, 'location', {value: {href: ''}, writable: true});
    });

    const renderSheet = () =>
        render(
            <ProjectVersionHistorySheet
                onDeleteProjectVersion={onDeleteProjectVersion}
                onRestoreProjectVersion={onRestoreProjectVersion}
                onSheetOpenChange={onSheetOpenChange}
                projectId={7}
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

    it('exports the chosen version from its actions menu', async () => {
        const user = userEvent.setup();

        renderSheet();

        await user.click(screen.getByRole('button', {name: 'Version 3 actions'}));
        await user.click(screen.getByRole('menuitem', {name: 'Export'}));

        expect(window.location.href).toBe('/api/automation/internal/projects/7/export?projectVersion=3');
        expect(screen.getByRole('option', {name: 'Version 3'})).toHaveAttribute('aria-selected', 'false');
    });

    it('offers delete only for published versions and deletes after confirmation', async () => {
        const user = userEvent.setup();

        renderSheet();

        await user.click(screen.getByRole('button', {name: 'Version 3 actions'}));

        expect(screen.getByRole('menuitem', {name: 'Export'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();

        await user.keyboard('{Escape}');

        await user.click(screen.getByRole('button', {name: 'Version 2 actions'}));
        await user.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(screen.getByRole('option', {hidden: true, name: 'Version 2'})).toHaveAttribute('aria-selected', 'false');

        expect(screen.getByText('Delete version V2?')).toBeInTheDocument();
        expect(screen.getByRole('button', {hidden: true, name: 'Apply selected version'})).toBeDisabled();

        fireEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(onDeleteProjectVersion).toHaveBeenCalledWith(2, expect.any(Function));
    });

    it('loads the changed workflows only when a version is expanded', () => {
        renderSheet();

        expect(useProjectVersionWorkflowChangesQuery).not.toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({enabled: true})
        );

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));

        expect(useProjectVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {projectId: '7', projectVersion: 2},
            expect.objectContaining({enabled: true})
        );
    });

    it('shows the changed workflows of a version after expanding it', () => {
        renderSheet();

        expect(screen.queryByText('Sync orders')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));

        expect(screen.getByText('Sync orders')).toBeInTheDocument();
    });

    it('does not select the version when a changed workflow is clicked', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));
        fireEvent.click(screen.getByText('Sync orders'));

        expect(screen.getByRole('option', {name: 'Version 2'})).toHaveAttribute('aria-selected', 'false');
    });

    it('opens the changes dialog with V1 \u2192 V2 labels for a published version', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));
        fireEvent.click(screen.getByText('Sync orders'));

        expect(screen.getByTestId('mock-changes-dialog')).toHaveTextContent('V1|V2');
    });

    it('opens the changes dialog with V2 \u2192 Draft labels for the draft version', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 3'}));
        fireEvent.click(screen.getByText('Sync orders'));

        expect(screen.getByTestId('mock-changes-dialog')).toHaveTextContent('V2|Draft');
    });
});
