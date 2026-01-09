import {Locator, Page} from '@playwright/test';

export class LoginPage {
    readonly form: {
        emailInput: Locator;
        loginButton: Locator;
        passwordInput: Locator;
        rememberMeCheckbox: Locator;
    };

    readonly passwordToggle: {
        hideButton: Locator;
        showButton: Locator;
        toggleButton: Locator;
    };

    readonly links: {
        createAccount: Locator;
        forgotPassword: Locator;
    };

    readonly validationErrors: {
        emailRequired: Locator;
        passwordRequired: Locator;
    };

    private readonly page: Page;

    constructor(page: Page) {
        this.page = page;

        this.form = {
            emailInput: page.getByLabel('Email'),
            loginButton: page.getByRole('button', {name: /log in/i}),
            passwordInput: page.getByRole('textbox', {name: 'Password'}),
            rememberMeCheckbox: page.getByLabel('Stay logged in'),
        };

        this.passwordToggle = {
            hideButton: page.getByRole('button', {name: /hide password/i}),
            showButton: page.getByRole('button', {name: /show password/i}),
            toggleButton: page.getByRole('button', {name: /show password|hide password/i}),
        };

        this.links = {
            createAccount: page.getByRole('button', {name: /create account/i}),
            forgotPassword: page.getByText(/forgot your password/i),
        };

        this.validationErrors = {
            emailRequired: page.getByText(/email is required/i),
            passwordRequired: page.getByText(/password is required/i),
        };
    }

    async goto(): Promise<void> {
        await this.page.goto('/login');
    }
}
