import {defineConfig} from '@playwright/test';

// Copied into client/ by the changelog-media workflow; runs the capture specs Claude writes against the running app.
export default defineConfig({
    fullyParallel: false,
    outputDir: process.env.CAPTURE_DIR ? `${process.env.CAPTURE_DIR}/playwright-output` : 'test-results/capture',
    reporter: 'list',
    retries: 0,
    testDir: './test/playwright/changelog-capture',
    timeout: 180_000,
    use: {
        baseURL: 'http://127.0.0.1:5173',
        browserName: 'chromium',
        colorScheme: 'light',
        deviceScaleFactor: 2,
        trace: 'retain-on-failure',
        viewport: {height: 900, width: 1440},
    },
    workers: 1,
});
