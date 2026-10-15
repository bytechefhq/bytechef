import AutomationWorkflowProjectVersionHistorySheet from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowProjectVersionHistorySheet';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationWorkflowProjectVersionsQuery: () => ({
        data: {
            automationWorkflowProjectVersions: [
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
    }),
}));

describe('AutomationWorkflowProjectVersionHistorySheet', () => {
    it('lists the versions in the order the server returns them and expands the latest published one', () => {
        render(<AutomationWorkflowProjectVersionHistorySheet onClose={vi.fn()} open projectId="project-1" />);

        expect(screen.getAllByText(/^V\d+$/).map((element) => element.textContent)).toEqual(['V3', 'V2', 'V1']);
        expect(screen.getByText('Adds greeting')).toBeInTheDocument();
        expect(screen.getByText('Fixes typo')).toBeInTheDocument();
    });
});
