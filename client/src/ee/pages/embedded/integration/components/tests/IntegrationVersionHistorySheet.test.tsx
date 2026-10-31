import IntegrationVersionHistorySheet from '@/ee/pages/embedded/integration/components/IntegrationVersionHistorySheet';
import {IntegrationStatus} from '@/ee/shared/middleware/embedded/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useIntegrationVersionWorkflowChangeQuery, useIntegrationVersionWorkflowChangesQuery} = vi.hoisted(() => ({
    useIntegrationVersionWorkflowChangeQuery: vi.fn(),
    useIntegrationVersionWorkflowChangesQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/integrationVersions.queries', () => ({
    useGetIntegrationVersionsQuery: () => ({
        data: [
            {status: IntegrationStatus.Draft, version: 3},
            {
                description: 'Second release',
                publishedDate: new Date('2026-10-10T10:00:00Z'),
                status: IntegrationStatus.Published,
                version: 2,
            },
            {publishedDate: new Date('2026-10-09T10:00:00Z'), status: IntegrationStatus.Published, version: 1},
        ],
    }),
}));

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
    beforeEach(() => {
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

    it('expands the draft version and loads its changed workflows', () => {
        render(<IntegrationVersionHistorySheet integrationId={7} onClose={vi.fn()} />);

        fireEvent.click(screen.getByText('V3'));

        expect(useIntegrationVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {integrationId: '7', integrationVersion: 3},
            expect.objectContaining({enabled: true})
        );
        expect(screen.getAllByText('Sync orders')).toHaveLength(2);
    });

    it('loads changed workflows when a published version is expanded', () => {
        render(<IntegrationVersionHistorySheet integrationId={7} onClose={vi.fn()} />);

        expect(useIntegrationVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {integrationId: '7', integrationVersion: 2},
            expect.objectContaining({enabled: true})
        );
        expect(useIntegrationVersionWorkflowChangesQuery).not.toHaveBeenCalledWith(
            {integrationId: '7', integrationVersion: 1},
            expect.objectContaining({enabled: true})
        );
    });

    it('opens the changes dialog with the version range for the clicked workflow', () => {
        render(<IntegrationVersionHistorySheet integrationId={7} onClose={vi.fn()} />);

        fireEvent.click(screen.getByText('Sync orders'));

        expect(screen.getByTestId('mock-changes-dialog')).toHaveTextContent('V1|V2');

        fireEvent.click(screen.getByText('V3'));
        fireEvent.click(screen.getAllByText('Sync orders')[0]!);

        expect(screen.getAllByTestId('mock-changes-dialog')[0]).toHaveTextContent('V2|Draft');
    });
});
