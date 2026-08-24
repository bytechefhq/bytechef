import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
    // Every property test logs in, creates a project, imports a workflow and then waits for the
    // workflow editor to mount its canvas and node definitions. Fixture setup counts against the test
    // timeout, and the default 30s runs out mid-setup once several workers share one dev server.
    expect: {
        timeout: 10000,
    },
    forbidOnly: !!process.env.CI,
    fullyParallel: true,
    projects: [
        {
            name: 'chromium',
            use: {...devices['Desktop Chrome']},
        },
    ],
    reporter: 'html',
    retries: process.env.CI ? 2 : 0,
    testDir: './test/playwright',
    testIgnore: ['**/utils/**', '**/fixtures/**'],
    testMatch: /.*\.spec\.ts/,
    timeout: 90000,
    use: {
        actionTimeout: 30000,
        baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173',
        navigationTimeout: 30000,
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
    // Prevent over-subscription of test workers due to workflow editor; the default (~50% of cores)
    workers: process.env.CI ? 1 : 3,
});
