import {Page, expect} from '@playwright/test';

export async function login(page: Page, email: string, password: string, rememberMe = false): Promise<void> {
    await page.goto('/login');

    await expect(page.getByLabel('Email')).toBeVisible();

    await expect(page.getByLabel('Password')).toBeVisible();

    await page.getByLabel('Email').fill(email);

    await page.getByLabel('Password').fill(password);

    if (rememberMe) {
        await page.getByLabel('Stay logged in').check();
    }

    console.log('Logging in...');

    await page.getByRole('button', {name: /log in/i}).click();

    await page.waitForURL((url) => url.pathname !== '/login', {timeout: 10000});

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
