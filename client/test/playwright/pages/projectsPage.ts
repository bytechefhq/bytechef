import {Locator, Page} from '@playwright/test';

export class ProjectsPage {
    readonly buttons: {
        createProject: Locator;
        createWorkflow: Locator;
        save: Locator;
        moreProjectActions: Locator;
        deleteProject: Locator;
        confirmProjectDeletion: Locator;
    };

    readonly form: {
        projectNameInput: Locator;
        workflowLabelInput: Locator;
    };

    readonly dialogs: {
        deleteConfirmationHeading: Locator;
    };

    readonly triggerNode: Locator;

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;

        this.buttons = {
            confirmProjectDeletion: page.getByLabel('Confirm Project Deletion'),
            createProject: page.getByRole('button', {name: 'Create Project'}),
            createWorkflow: page.getByRole('button', {name: 'Create Workflow'}),
            deleteProject: page.getByLabel('Delete Project'),
            moreProjectActions: page.getByLabel('More Project Actions').first(),
            save: page.getByRole('button', {name: 'Save'}),
        };

        this.form = {
            projectNameInput: page.getByRole('textbox', {name: 'Name'}),
            workflowLabelInput: page.getByRole('textbox', {name: 'Label'}),
        };

        this.dialogs = {
            deleteConfirmationHeading: page.getByRole('heading', {name: 'Are you absolutely sure?'}),
        };

        this.triggerNode = page.locator('[data-nodetype="trigger"]').filter({hasText: 'Manual'});
    }

    async goto(): Promise<void> {
        await this.page.goto('/automation/projects');
    }

    async waitForPageLoad(): Promise<void> {
        await this.page.waitForURL(/\/automation\/projects/, {timeout: 10000});

        await this.page.waitForLoadState('networkidle');

        await this.buttons.createProject.waitFor({state: 'visible', timeout: 10000});
    }

    async createProject(projectName: string): Promise<void> {
        await this.buttons.createProject.click();

        await this.form.projectNameInput.fill(projectName);

        await this.buttons.save.click();
    }

    async createWorkflow(workflowLabel: string): Promise<void> {
        await this.buttons.createWorkflow.click();

        await this.form.workflowLabelInput.fill(workflowLabel);

        await this.buttons.save.click();
    }

    getProjectByName(projectName: string): Locator {
        return this.page.getByText(projectName).first();
    }

    getWorkflowByName(workflowName: string): Locator {
        return this.page.getByText(workflowName);
    }

    async waitForNetworkIdle(): Promise<void> {
        await this.page.waitForLoadState('networkidle');
    }

    async deleteProject(projectName: string): Promise<void> {
        const testProjectText = this.getProjectByName(projectName);

        if (await testProjectText.isVisible().catch(() => false)) {
            if (await this.buttons.moreProjectActions.isVisible().catch(() => false)) {
                await this.buttons.moreProjectActions.click();

                await this.buttons.deleteProject.waitFor({state: 'visible', timeout: 5000});

                await this.buttons.deleteProject.click();

                await this.dialogs.deleteConfirmationHeading.waitFor({state: 'visible', timeout: 5000});

                await this.buttons.confirmProjectDeletion.click();

                await testProjectText.waitFor({state: 'hidden', timeout: 10000}).catch(() => {});
            }
        }
    }
}
