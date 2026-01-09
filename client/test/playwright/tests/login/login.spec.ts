import {expect, test} from '@playwright/test';

test('should load the home page', async ({page}) => {
    await test.step('Navigate to home page', async () => {
        await page.goto('/');
    });

    await test.step('Wait for page to load', async () => {
        await page.waitForLoadState('networkidle');
    });

    await test.step('Verify page title', async () => {
        await expect(page).toHaveTitle(/ByteChef/i);
    });
});

test('should navigate to login page', async ({page}) => {
    await test.step('Navigate to login page', async () => {
        await page.goto('/login');
    });

    await test.step('Verify login form elements are visible', async () => {
        await expect(page.getByLabel('Email')).toBeVisible();

        await expect(page.getByLabel('Password')).toBeVisible();

        await expect(page.getByRole('button', {name: /log in/i})).toBeVisible();
    });
});

test('should show validation errors on empty login form submission', async ({page}) => {
    await test.step('Navigate to login page', async () => {
        await page.goto('/login');
    });

    await test.step('Submit empty login form', async () => {
        await page.getByRole('button', {name: /log in/i}).click();
    });

    await test.step('Verify validation errors are displayed', async () => {
        await expect(page.getByText(/email is required/i)).toBeVisible();

        await expect(page.getByText(/password is required/i)).toBeVisible();
    });
});
