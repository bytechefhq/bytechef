import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
    forbidOnly: !!process.env.CI,
    fullyParallel: true,
    projects: [
        {
            name: 'chromium',
            use: {...devices['Desktop Chrome']},
        },
        {
            name: 'firefox',
            use: {...devices['Desktop Firefox']},
        },
        {
            name: 'webkit',
            use: {...devices['Desktop Safari']},
        },
    ],
    reporter: 'html',
    retries: process.env.CI ? 2 : 0,
    testDir: './test/playwright',
    testIgnore: ['**/utils/**', '**/fixtures/**'],
    testMatch: /.*\.spec\.ts/,
    use: {
        baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173',
        screenshot: 'only-on-failure',
        trace: 'on-first-retry',
        video: 'retain-on-failure',
    },
    webServer: {
        command: 'npm run dev',
        reuseExistingServer: !process.env.CI,
        stderr: 'pipe',
        stdout: 'ignore',
        timeout: 120 * 1000,
        url: 'http://127.0.0.1:5173',
    },
    workers: process.env.CI ? 1 : undefined,
});
