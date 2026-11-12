import IntegrationVersionHistorySheet from '@/ee/pages/embedded/integration/components/IntegrationVersionHistorySheet';
import {IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {
    useGetIntegrationVersionsQuery,
    useIntegrationVersionWorkflowChangeQuery,
    useIntegrationVersionWorkflowChangesQuery,
} = vi.hoisted(() => ({
    useGetIntegrationVersionsQuery: vi.fn(),
    useIntegrationVersionWorkflowChangeQuery: vi.fn(),
    useIntegrationVersionWorkflowChangesQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/integrationVersions.queries', () => ({
    useGetIntegrationVersionsQuery,
}));

const integrationVersions = [
    {status: IntegrationStatus.Draft, version: 3},
    {
        description: 'Second release',
        publishedDate: new Date('2026-10-10T10:00:00Z'),
        status: IntegrationStatus.Published,
        version: 2,
    },
    {publishedDate: new Date('2026-10-09T10:00:00Z'), status: IntegrationStatus.Published, version: 1},
];

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<Record<string, unknown>>()),
    useIntegrationVersionWorkflowChangeQuery,
    useIntegrationVersionWorkflowChangesQuery,
}));

vi.mock('@/shared/components/version-workflow-changes/WorkflowVersionChangesDialog', () => ({
    default: ({baseVersionLabel, versionLabel}: {baseVersionLabel?: string; versionLabel: string}) => (
        <div data-testid="mock-changes-dialog">{`${baseVersionLabel ?? ''}|${versionLabel}`}</div>
    ),
}));

describe('IntegrationVersionHistorySheet', () => {
    const onClose = vi.fn();
    const onRestoreIntegrationVersion = vi.fn();

    const renderSheet = (restoreIntegrationVersionPending?: boolean) =>
        render(
            <IntegrationVersionHistorySheet
                integrationId={7}
                onClose={onClose}
                onRestoreIntegrationVersion={onRestoreIntegrationVersion}
                restoreIntegrationVersionPending={restoreIntegrationVersionPending}
            />
        );

    beforeEach(() => {
        onClose.mockReset();
        onRestoreIntegrationVersion.mockReset();
        useGetIntegrationVersionsQuery.mockReset();
        useGetIntegrationVersionsQuery.mockReturnValue({data: integrationVersions});
        useIntegrationVersionWorkflowChangesQuery.mockReset();
        useIntegrationVersionWorkflowChangeQuery.mockReset();

        useIntegrationVersionWorkflowChangesQuery.mockImplementation(
            (_variables: unknown, options?: {enabled?: boolean}) => ({
                data:
                    options?.enabled === false
                        ? undefined
                        : {
                              integrationVersionWorkflowChanges: {
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
        useIntegrationVersionWorkflowChangeQuery.mockImplementation((variables: {integrationVersion: number}) => ({
            data: {
                integrationVersionWorkflowChange: {
                    baseVersion: variables.integrationVersion === 3 ? 2 : 1,
                    changeType: 'MODIFIED',
                    changes: [],
                    label: 'Sync orders',
                    version: variables.integrationVersion,
                    workflowUuid: 'uuid-1',
                },
            },
            isLoading: false,
        }));
    });

    it('lists the versions in the order the server returns them, collapsed', () => {
        renderSheet();

        expect(screen.getByRole('heading', {name: /Version History$/})).toBeInTheDocument();
        expect(screen.getByRole('listbox', {name: 'Integration versions'})).toBeInTheDocument();
        expect(screen.getAllByText(/^V\d+$/).map((element) => element.textContent)).toEqual(['V3', 'V2', 'V1']);
        expect(screen.getByText('Draft')).toBeInTheDocument();
        expect(screen.getAllByText('Published')).toHaveLength(2);
        expect(screen.getByText('Current working version.')).toBeInTheDocument();
        expect(screen.getByText('Second release')).toBeInTheDocument();
        expect(screen.getByText('No description.')).toBeInTheDocument();
        expect(screen.queryByText('Sync orders')).not.toBeInTheDocument();
    });

    it('keeps Apply disabled until a published version is selected', () => {
        renderSheet();

        const applyButton = screen.getByRole('button', {name: 'Apply selected version'});

        expect(applyButton).toBeDisabled();

        fireEvent.click(screen.getByRole('option', {name: 'Version 3'}));

        expect(applyButton).toBeDisabled();
        expect(screen.getByRole('option', {name: 'Version 3'})).toHaveAttribute('aria-disabled', 'true');

        fireEvent.click(screen.getByRole('option', {name: 'Version 2'}));

        expect(applyButton).toBeEnabled();
        expect(screen.getByRole('option', {name: 'Version 2'})).toHaveAttribute('aria-selected', 'true');
    });

    it('restores the selected version after confirmation and closes the sheet', () => {
        onRestoreIntegrationVersion.mockImplementation((_integrationVersion: number, onSuccess: () => void) =>
            onSuccess()
        );

        renderSheet();

        fireEvent.click(screen.getByRole('option', {name: 'Version 1'}));
        fireEvent.click(screen.getByRole('button', {name: 'Apply selected version'}));

        expect(screen.getByText('Apply version V1?')).toBeInTheDocument();
        expect(onRestoreIntegrationVersion).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', {name: 'Apply'}));

        expect(onRestoreIntegrationVersion).toHaveBeenCalledWith(1, expect.any(Function));
        expect(onClose).toHaveBeenCalled();
    });

    it('does not restore when the confirmation is cancelled', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('option', {name: 'Version 2'}));
        fireEvent.click(screen.getByRole('button', {name: 'Apply selected version'}));
        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(screen.queryByText('Apply version V2?')).not.toBeInTheDocument();
        expect(onRestoreIntegrationVersion).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
    });

    it('disables Apply while a restore is pending', () => {
        renderSheet(true);

        fireEvent.click(screen.getByRole('option', {name: 'Version 2'}));

        expect(screen.getByRole('button', {name: 'Apply selected version'})).toBeDisabled();
    });

    it('does not select the version when a changed workflow is clicked', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));
        fireEvent.click(screen.getByText('Sync orders'));

        expect(screen.getByRole('option', {name: 'Version 2'})).toHaveAttribute('aria-selected', 'false');
    });

    it('expands the draft version and loads its changed workflows', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 3'}));

        expect(screen.getByRole('button', {name: 'Toggle changes of version 3'})).toHaveAttribute(
            'aria-expanded',
            'true'
        );
        expect(useIntegrationVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {integrationId: '7', integrationVersion: 3},
            expect.objectContaining({enabled: true})
        );
        expect(screen.getByText('Sync orders')).toBeInTheDocument();
    });

    it('loads changed workflows only for the expanded published version', () => {
        renderSheet();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle changes of version 2'}));

        expect(useIntegrationVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {integrationId: '7', integrationVersion: 2},
            expect.objectContaining({enabled: true})
        );
        expect(useIntegrationVersionWorkflowChangesQuery).not.toHaveBeenCalledWith(
            {integrationId: '7', integrationVersion: 1},
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
