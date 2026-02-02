import {Locator, Page, expect} from '@playwright/test';

import {clickAndExpectToBeHidden} from '../utils/clickAndExpectToBeHidden';
import {clickAndExpectToBeVisible} from '../utils/clickAndExpectToBeVisible';

export class ProjectsPage {
    readonly createProjectButton: Locator;
    readonly createProjectDialog: Locator;
    readonly createWorkflowDialogHeading: Locator;
    readonly deleteConfirmationDialogHeading: Locator;
    readonly deleteProjectConfirmationButton: Locator;
    readonly deleteProjectDropdownButton: Locator;
    readonly projectFormNameInput: Locator;
    readonly saveButton: Locator;
    readonly workflowFormLabelInput: Locator;

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        this.createProjectButton = page.locator('button[aria-label="Create Project"]');
        this.createProjectDialog = page.getByRole('dialog', {name: 'Create Project'});
        this.createWorkflowDialogHeading = page.getByRole('heading', {name: 'Create Workflow'});
        this.deleteConfirmationDialogHeading = page.getByRole('heading', {name: 'Are you absolutely sure?'});
        this.deleteProjectConfirmationButton = page.getByLabel('Confirm Project Deletion');
        this.deleteProjectDropdownButton = page.getByLabel('Delete Project');
        this.projectFormNameInput = page.getByRole('textbox', {name: 'Name'});
        this.saveButton = page.getByRole('button', {name: 'Save'});
        this.workflowFormLabelInput = page.getByRole('textbox', {name: 'Label'});
    }

    async waitForPageLoad(): Promise<void> {
        await this.page.waitForURL(/\/automation\/projects/, {timeout: 10000});

        await this.page.waitForLoadState('domcontentloaded', {timeout: 10000});

        await expect(this.createProjectButton).toBeVisible({timeout: 60000});
    }

    async createProject(projectName: string): Promise<string> {
        await this.createProjectButton.click();

        await expect(this.createProjectDialog).toBeVisible({timeout: 10000});

        await this.projectFormNameInput.fill(projectName);

        await this.saveButton.click();

        await expect(this.createProjectDialog).toBeHidden({timeout: 10000});

        await expect(this.page.getByText(projectName).first()).toBeVisible({timeout: 20000});

        const projectItem = this.page.getByTestId('project-item').filter({hasText: projectName}).first();

        const projectId = await projectItem.getAttribute('aria-label');

        if (!projectId) {
            throw new Error(`Project ID not found for project name: ${projectName}`);
        }

        return projectId;
    }

    async deleteProject(projectId: string): Promise<void> {
        await this.page.goto('/automation/projects');

        await this.waitForPageLoad();

        await this.page.waitForLoadState('domcontentloaded');

        const projectItem = this.page.getByLabel(projectId);

        await expect(projectItem).toBeVisible({timeout: 20000});

        const moreProjectActionsButton = this.page.getByTestId(`${projectId}-moreProjectActionsButton`);

        await expect(moreProjectActionsButton).toBeVisible({timeout: 60000});

        await clickAndExpectToBeVisible({
            target: this.deleteProjectDropdownButton,
            trigger: moreProjectActionsButton,
        });

        await clickAndExpectToBeVisible({
            target: this.deleteConfirmationDialogHeading,
            trigger: this.deleteProjectDropdownButton,
        });

        await clickAndExpectToBeHidden({
            target: moreProjectActionsButton,
            trigger: this.deleteProjectConfirmationButton,
        });
    }
}
