import AutomationProjectVersionHistorySheet from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationProjectVersionHistorySheet';
import {act, fireEvent, render, screen, within} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {
    useAutomationProjectVersionWorkflowChangeQuery,
    useAutomationProjectVersionWorkflowChangesQuery,
    useAutomationProjectVersionsQuery,
} = vi.hoisted(() => ({
    useAutomationProjectVersionWorkflowChangeQuery: vi.fn(),
    useAutomationProjectVersionWorkflowChangesQuery: vi.fn(),
    useAutomationProjectVersionsQuery: vi.fn(),
}));

vi.mock('@/shared/components/version-workflow-changes/WorkflowVersionChangesDialog', () => ({
    default: ({baseVersionLabel, versionLabel}: {baseVersionLabel?: string; versionLabel: string}) => (
        <div data-testid="mock-changes-dialog">{`${baseVersionLabel ?? ''}|${versionLabel}`}</div>
    ),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<Record<string, unknown>>()),
    useAutomationProjectVersionWorkflowChangeQuery,
    useAutomationProjectVersionWorkflowChangesQuery,
    useAutomationProjectVersionsQuery,
}));

const versionsResult = {
    data: {
        automationProjectVersions: [
            {description: null, publishedDate: null, status: 'DRAFT', version: 3},
            {
                description: 'Adds greeting\nFixes typo',
                publishedDate: '2026-10-07T14:25:20Z',
                status: 'PUBLISHED',
                version: 2,
            },
            {description: null, publishedDate: '2026-10-07T14:11:03Z', status: 'PUBLISHED', version: 1},
        ],
    },
};

const renderSheet = ({
    onClose = vi.fn(),
    onRestoreProjectVersion = vi.fn(),
    restoreProjectVersionPending = false,
}: {
    onClose?: () => void;
    onRestoreProjectVersion?: (projectVersion: number, onSuccess: () => void) => void;
    restoreProjectVersionPending?: boolean;
} = {}) =>
    render(
        <AutomationProjectVersionHistorySheet
            onClose={onClose}
            onRestoreProjectVersion={onRestoreProjectVersion}
            open
            projectId="project-1"
            restoreProjectVersionPending={restoreProjectVersionPending}
        />
    );

describe('AutomationProjectVersionHistorySheet', () => {
    beforeEach(() => {
        useAutomationProjectVersionsQuery.mockReset();
        useAutomationProjectVersionsQuery.mockReturnValue(versionsResult);
        useAutomationProjectVersionWorkflowChangesQuery.mockReset();
        useAutomationProjectVersionWorkflowChangeQuery.mockReset();

        useAutomationProjectVersionWorkflowChangesQuery.mockImplementation(
            (_variables: unknown, options?: {enabled?: boolean}) => ({
                data:
                    options?.enabled === false
                        ? undefined
                        : {
                              automationProjectVersionWorkflowChanges: {
                                  baseVersion: 1,
                                  version: 2,
                                  workflows: [
                                      {
                                          changeCount: 2,
                                          changeType: 'MODIFIED',
                                          label: 'Sync orders',
                                          workflowUuid: 'uuid-1',
                                      },
                                  ],
                              },
                          },
                isLoading: false,
            })
        );
        useAutomationProjectVersionWorkflowChangeQuery.mockImplementation((variables: {projectVersion: number}) => ({
            data: {
                automationProjectVersionWorkflowChange: {
                    baseVersion: variables.projectVersion === 3 ? 2 : 1,
                    changeType: 'MODIFIED',
                    changes: [],
                    label: 'Sync orders',
                    version: variables.projectVersion,
                    workflowUuid: 'uuid-1',
                },
            },
            isLoading: false,
        }));
    });

    it('lists the versions in the order the server returns them, collapsed', () => {
        renderSheet();

        expect(screen.getByRole('heading', {name: /Version History$/})).toBeInTheDocument();
        expect(screen.getAllByText(/^V\d+$/).map((element) => element.textContent)).toEqual(['V3', 'V2', 'V1']);
        expect(screen.getByText('Draft')).toBeInTheDocument();
        expect(screen.getAllByText('Published')).toHaveLength(2);
        expect(screen.getByText('Current working version.')).toBeInTheDocument();
        expect(screen.getByText(/^Adds greeting\s+Fixes typo$/)).toBeInTheDocument();
        expect(screen.getByText('No description.')).toBeInTheDocument();
        expect(screen.queryByText('Sync orders')).not.toBeInTheDocument();
    });

    it('offers only the published versions for selection and applies nothing until one is selected', () => {
        renderSheet();

        expect(screen.getByRole('listbox', {name: 'Project versions'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Version 3'})).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('option', {name: 'Version 2'})).toHaveAttribute('aria-disabled', 'false');
        expect(screen.getByRole('button', {name: 'Apply selected version'})).toBeDisabled();

        fireEvent.click(screen.getByRole('option', {name: 'Version 3'}));

        expect(screen.getByRole('option', {name: 'Version 3'})).toHaveAttribute('aria-selected', 'false');
        expect(screen.getByRole('button', {name: 'Apply selected version'})).toBeDisabled();
    });

    it('restores the selected published version after confirmation and closes once it is applied', () => {
        const onClose = vi.fn();
        const onRestoreProjectVersion = vi.fn();

        renderSheet({onClose, onRestoreProjectVersion});

        fireEvent.click(screen.getByRole('option', {name: 'Version 2'}));

        expect(screen.getByRole('option', {name: 'Version 2'})).toHaveAttribute('aria-selected', 'true');

        fireEvent.click(screen.getByRole('button', {name: 'Apply selected version'}));

        const confirmationDialog = screen.getByRole('alertdialog');

        expect(within(confirmationDialog).getByText('Apply version V2?')).toBeInTheDocument();
        expect(onRestoreProjectVersion).not.toHaveBeenCalled();

        fireEvent.click(within(confirmationDialog).getByRole('button', {name: 'Apply'}));

        expect(onRestoreProjectVersion).toHaveBeenCalledWith(2, expect.any(Function));
        expect(onClose).not.toHaveBeenCalled();

        const [, onSuccess] = onRestoreProjectVersion.mock.calls[0];

        act(() => {
            onSuccess();
        });

        expect(onClose).toHaveBeenCalledTimes(1);
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('keeps the draft when the confirmation is cancelled', () => {
        const onRestoreProjectVersion = vi.fn();

        renderSheet({onRestoreProjectVersion});

        fireEvent.click(screen.getByRole('option', {name: 'Version 1'}));
        fireEvent.click(screen.getByRole('button', {name: 'Apply selected version'}));
        fireEvent.click(within(screen.getByRole('alertdialog')).getByRole('button', {name: 'Cancel'}));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(onRestoreProjectVersion).not.toHaveBeenCalled();
    });

    it('disables applying while a restore is pending', () => {
        renderSheet({restoreProjectVersionPending: true});

        fireEvent.click(screen.getByRole('option', {name: 'Version 2'}));

        expect(screen.getByRole('button', {name: 'Apply selected version'})).toBeDisabled();
    });

    it('closes from the Cancel button', () => {
        const onClose = vi.fn();

        renderSheet({onClose});

        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('expands the draft version and loads its changed workflows', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 3'}));

        expect(screen.getByRole('button', {name: 'Toggle changes of version 3'})).toHaveAttribute(
            'aria-expanded',
            'true'
        );
        expect(useAutomationProjectVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {id: 'project-1', projectVersion: 3},
            expect.objectContaining({enabled: true})
        );
        expect(screen.getByText('Sync orders')).toBeInTheDocument();
    });

    it('loads changed workflows only for the expanded published version', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));

        expect(useAutomationProjectVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {id: 'project-1', projectVersion: 2},
            expect.objectContaining({enabled: true})
        );
        expect(useAutomationProjectVersionWorkflowChangesQuery).not.toHaveBeenCalledWith(
            {id: 'project-1', projectVersion: 1},
            expect.objectContaining({enabled: true})
        );
    });

    it('collapses an expanded version again', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));
        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));

        expect(screen.queryByText('Sync orders')).not.toBeInTheDocument();
    });

    it('opens the changes dialog with the version range for the clicked workflow', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));
        fireEvent.click(screen.getByText('Sync orders'));

        expect(screen.getByTestId('mock-changes-dialog')).toHaveTextContent('V1|V2');

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 3'}));
        fireEvent.click(screen.getAllByText('Sync orders')[0]!);

        expect(screen.getAllByTestId('mock-changes-dialog')[0]).toHaveTextContent('V2|Draft');
    });
});
