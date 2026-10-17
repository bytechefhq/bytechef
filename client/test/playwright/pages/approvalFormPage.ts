import {type Locator, type Page, expect} from '@playwright/test';

/**
 * The hosted approval form at `/resume/<token>` (also opened with `?approved=true|false` from an approval mail).
 */
export class ApprovalFormPage {
    readonly approveButton: Locator;
    readonly approvedHeading: Locator;
    readonly commentInput: Locator;
    readonly confirmApprovalButton: Locator;
    readonly confirmApproveText: Locator;
    readonly confirmDiscardButton: Locator;
    readonly confirmDiscardText: Locator;
    readonly discardButton: Locator;
    readonly discardedHeading: Locator;
    readonly fieldRequiredMessage: Locator;
    readonly noLongerAvailableHeading: Locator;
    readonly submittedApprovalText: Locator;
    readonly submittedDiscardText: Locator;

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;
        this.approveButton = page.getByRole('button', {exact: true, name: 'Approve'});
        this.approvedHeading = page.getByRole('heading', {exact: true, name: 'Approved'});
        this.commentInput = page.getByLabel('Comment (optional)');
        this.confirmApprovalButton = page.getByRole('button', {name: 'Confirm approval'});
        this.confirmApproveText = page.getByText('Confirm to approve this request.');
        this.confirmDiscardButton = page.getByRole('button', {name: 'Confirm discard'});
        this.confirmDiscardText = page.getByText('Confirm to discard this request.');
        this.discardButton = page.getByRole('button', {exact: true, name: 'Discard'});
        this.discardedHeading = page.getByRole('heading', {exact: true, name: 'Discarded'});
        this.fieldRequiredMessage = page.getByText('This field is required');
        this.noLongerAvailableHeading = page.getByRole('heading', {name: 'Form no longer available'});
        this.submittedApprovalText = page.getByText('Your approval has been submitted.');
        this.submittedDiscardText = page.getByText('The request has been discarded.');
    }

    async goto(token: string, approved?: boolean): Promise<void> {
        const query = approved === undefined ? '' : `?approved=${approved}`;

        await this.page.goto(`/resume/${token}${query}`);
    }

    formTitleHeading(formTitle: string): Locator {
        return this.page.getByRole('heading', {name: formTitle});
    }

    inputByLabel(fieldLabel: string): Locator {
        return this.page.getByLabel(fieldLabel, {exact: false}).first();
    }

    async expectApproved(): Promise<void> {
        await expect(this.approvedHeading).toBeVisible();

        await expect(this.submittedApprovalText).toBeVisible();
    }

    async expectDiscarded(): Promise<void> {
        await expect(this.discardedHeading).toBeVisible();

        await expect(this.submittedDiscardText).toBeVisible();
    }
}
