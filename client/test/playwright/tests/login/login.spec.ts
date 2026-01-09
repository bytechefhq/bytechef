import {expect, test} from '@playwright/test';

import {LoginPage} from '../../pages/loginPage';

test.describe('Login Page', () => {
    let loginPage: LoginPage;

    test.beforeEach(async ({page}) => {
        loginPage = new LoginPage(page);

        await loginPage.goto();
    });

    test('should display login form elements', async () => {
        await expect(loginPage.form.emailInput).toBeVisible();

        await expect(loginPage.form.passwordInput).toBeVisible();

        await expect(loginPage.form.loginButton).toBeVisible();
    });

    test.describe('Form Validation', () => {
        test('should show validation errors on empty login form submission', async () => {
            await loginPage.form.loginButton.click();

            await expect(loginPage.validationErrors.emailRequired).toBeVisible();

            await expect(loginPage.validationErrors.passwordRequired).toBeVisible();
        });

        test('should prevent form submission with invalid email format', async ({page}) => {
            await loginPage.form.emailInput.fill('invalidemail');

            await loginPage.form.passwordInput.fill('password123');

            await loginPage.form.loginButton.click();

            await expect(page).toHaveURL(/\/login/);

            await expect(loginPage.form.emailInput).toHaveValue('invalidemail');
        });

        test('should show validation error for password shorter than 4 characters', async () => {
            await loginPage.form.emailInput.fill('test@example.com');
            await loginPage.form.passwordInput.fill('123');
            await loginPage.form.loginButton.click();

            await expect(loginPage.validationErrors.passwordRequired).toBeVisible();
        });

        test('should accept valid email and password lengths', async () => {
            await loginPage.form.emailInput.fill('test@example.com');
            await loginPage.form.passwordInput.fill('password123');

            await expect(loginPage.validationErrors.emailRequired).not.toBeVisible();
            await expect(loginPage.validationErrors.passwordRequired).not.toBeVisible();
        });
    });

    test.describe('User Interactions', () => {
        test('should toggle password visibility when clicking eye icon', async () => {
            await test.step('assert password input field and toggle button are visible', async () => {
                await loginPage.form.passwordInput.fill('mypassword123');

                await expect(loginPage.passwordToggle.showButton).toBeVisible();

                await expect(loginPage.form.passwordInput).toHaveAttribute('type', 'password');

                await loginPage.passwordToggle.showButton.click();
            });

            await test.step('click the toggle button', async () => {
                await expect(loginPage.form.passwordInput).toHaveAttribute('type', 'text');
                await expect(loginPage.passwordToggle.hideButton).toBeVisible();

                await loginPage.passwordToggle.hideButton.click();
            });

            await expect(loginPage.form.passwordInput).toHaveAttribute('type', 'password');

            await expect(loginPage.passwordToggle.showButton).toBeVisible();
        });

        test('should not show password toggle when password field is empty', async () => {
            await expect(loginPage.passwordToggle.toggleButton).not.toBeVisible();

            await loginPage.form.passwordInput.fill('test');

            await expect(loginPage.passwordToggle.showButton).toBeVisible();

            await loginPage.form.passwordInput.clear();

            await expect(loginPage.passwordToggle.toggleButton).not.toBeVisible();
        });

        test('should toggle remember me checkbox', async () => {
            await expect(loginPage.form.rememberMeCheckbox).not.toBeChecked();

            await loginPage.form.rememberMeCheckbox.check();

            await expect(loginPage.form.rememberMeCheckbox).toBeChecked();

            await loginPage.form.rememberMeCheckbox.uncheck();

            await expect(loginPage.form.rememberMeCheckbox).not.toBeChecked();
        });
    });

    test.describe('Navigation', () => {
        test('should navigate to password reset page when clicking forgot password link', async ({page}) => {
            await loginPage.links.forgotPassword.click();

            await expect(page).toHaveURL(/\/password-reset\/init/);
        });

        test('should navigate to register page when clicking create account link', async ({page}) => {
            await loginPage.links.createAccount.click();

            await expect(page).toHaveURL(/\/register/);
        });
    });

    test.describe('Authentication', () => {
        test('should successfully login and navigate to projects page', async ({page}) => {
            await loginPage.form.emailInput.fill('admin@localhost.com');
            await loginPage.form.passwordInput.fill('admin');
            await loginPage.form.loginButton.click();

            await page.waitForURL(/\/automation\/projects/, {timeout: 10000});

            await expect(page).toHaveURL(/\/automation\/projects/);

            await expect(page.locator('div').filter({hasText: /^projects$/i})).toBeVisible();
        });
    });
});
