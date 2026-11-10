import AutomationProjectVersionHistorySheet from '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationProjectVersionHistorySheet';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
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

    it('lists the versions in the order the server returns them and expands the latest published one', () => {
        render(<AutomationProjectVersionHistorySheet onClose={vi.fn()} open projectId="project-1" />);

        expect(screen.getAllByText(/^V\d+$/).map((element) => element.textContent)).toEqual(['V3', 'V2', 'V1']);
        expect(screen.getByText('Adds greeting')).toBeInTheDocument();
        expect(screen.getByText('Fixes typo')).toBeInTheDocument();
    });

    it('expands the draft version and loads its changed workflows', () => {
        render(<AutomationProjectVersionHistorySheet onClose={vi.fn()} open projectId="project-1" />);

        fireEvent.click(screen.getByText('V3'));

        expect(useAutomationProjectVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {id: 'project-1', projectVersion: 3},
            expect.objectContaining({enabled: true})
        );
        expect(screen.getAllByText('Sync orders')).toHaveLength(2);
    });

    it('loads changed workflows when a published version is expanded', () => {
        render(<AutomationProjectVersionHistorySheet onClose={vi.fn()} open projectId="project-1" />);

        expect(useAutomationProjectVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {id: 'project-1', projectVersion: 2},
            expect.objectContaining({enabled: true})
        );
        expect(useAutomationProjectVersionWorkflowChangesQuery).not.toHaveBeenCalledWith(
            {id: 'project-1', projectVersion: 1},
            expect.objectContaining({enabled: true})
        );
    });

    it('opens the changes dialog with the version range for the clicked workflow', () => {
        render(<AutomationProjectVersionHistorySheet onClose={vi.fn()} open projectId="project-1" />);

        fireEvent.click(screen.getByText('Sync orders'));

        expect(screen.getByTestId('mock-changes-dialog')).toHaveTextContent('V1|V2');

        fireEvent.click(screen.getByText('V3'));
        fireEvent.click(screen.getAllByText('Sync orders')[0]!);

        expect(screen.getAllByTestId('mock-changes-dialog')[0]).toHaveTextContent('V2|Draft');
    });

    it('expands the latest published version once the version data loads', () => {
        useAutomationProjectVersionsQuery.mockReturnValue({data: undefined});

        const {rerender} = render(
            <AutomationProjectVersionHistorySheet onClose={vi.fn()} open projectId="project-1" />
        );

        useAutomationProjectVersionsQuery.mockReturnValue(versionsResult);

        rerender(<AutomationProjectVersionHistorySheet onClose={vi.fn()} open projectId="project-1" />);

        expect(useAutomationProjectVersionWorkflowChangesQuery).toHaveBeenCalledWith(
            {id: 'project-1', projectVersion: 2},
            expect.objectContaining({enabled: true})
        );
        expect(screen.getByText('Fixes typo')).toBeInTheDocument();
    });
});
