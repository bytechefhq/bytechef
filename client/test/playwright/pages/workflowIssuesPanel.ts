import {type Locator, type Page, expect} from '@playwright/test';

import {TIMEOUTS} from '../utils/constants';

/**
 * The workflow editor's design-time issues: the badge on a node, the "<n> issues in this workflow" note and the
 * Workflow Issues sidebar it opens.
 */
export class WorkflowIssuesPanel {
    readonly sidebar: Locator;
    readonly sidebarHeading: Locator;

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        this.sidebar = page.getByRole('complementary', {name: 'Workflow issues'});
        this.sidebarHeading = this.sidebar.getByRole('heading', {level: 2});
    }

    async gotoWorkflowEditor(projectId: string, projectWorkflowId: number): Promise<void> {
        await this.page.goto(`/automation/projects/${projectId}/project-workflows/${projectWorkflowId}`);

        await expect(this.page.getByRole('button', {name: /\snode$/}).first()).toBeVisible({
            timeout: TIMEOUTS.EDITOR_CANVAS_READY,
        });
    }

    nodeIssueBadge(issueCountLabel = '1 issue'): Locator {
        return this.page.getByRole('img', {exact: true, name: issueCountLabel});
    }

    issuesNote(summary?: string): Locator {
        return summary ? this.page.getByText(`${summary} in this workflow`) : this.page.getByText(/ in this workflow$/);
    }

    async openSidebarFromNote(summary?: string): Promise<void> {
        const issuesNote = this.issuesNote(summary);

        await expect(issuesNote).toBeVisible({timeout: TIMEOUTS.EDITOR_CANVAS_READY});

        await this.page.locator('div').filter({has: issuesNote}).getByRole('button', {name: 'View'}).last().click();

        await expect(this.sidebarHeading).toBeVisible();
    }

    nodeIssues(nodeName: string): Locator {
        return this.sidebar
            .locator('section')
            .filter({has: this.page.getByRole('heading', {level: 3, name: nodeName})});
    }
}
