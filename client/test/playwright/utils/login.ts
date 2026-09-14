import {Page, expect} from '@playwright/test';

export async function login(page: Page, email: string, password: string, rememberMe = false): Promise<void> {
    await page.goto('/login');

    const emailInput = page.getByLabel('Email', {exact: true});
    const passwordInput = page.getByLabel('Password', {exact: true});

    await expect(emailInput).toBeVisible();

    await expect(passwordInput).toBeVisible();

    // The login form can remount while the app finishes its startup checks, which wipes values that were
    // already typed (the submit then fails client-side validation), so refill and resubmit until it navigates.
    await expect(async () => {
        if (new URL(page.url()).pathname !== '/login') {
            return;
        }

        await emailInput.fill(email);

        await passwordInput.fill(password);

        if (rememberMe) {
            await page.getByLabel('Stay logged in').check();
        }

        await expect(emailInput).toHaveValue(email, {timeout: 1000});
        await expect(passwordInput).toHaveValue(password, {timeout: 1000});

        await page.getByRole('button', {name: /log in/i}).click();

        await page.waitForURL((url) => url.pathname !== '/login', {timeout: 5000});
    }).toPass({timeout: 30000});

    await expect(page).not.toHaveURL(/\/login/);
}

export async function logout(page: Page): Promise<void> {
    await page.goto('/account/settings');

    const logoutButton = page.getByRole('button', {name: /log out|sign out/i});

    if (await logoutButton.isVisible().catch(() => false)) {
        await logoutButton.click();

        await page.waitForURL(/\/login/, {timeout: 5000});
    }
}

export async function saveAuthState(
    page: Page,
    email: string,
    password: string,
    storageStatePath: string
): Promise<void> {
    await login(page, email, password);

    await page.context().storageState({path: storageStatePath});
}

export async function isAuthenticated(page: Page): Promise<boolean> {
    try {
        await page.goto('/');

        await page.waitForTimeout(1000);

        return !page.url().includes('/login');
    } catch {
        return false;
    }
}
