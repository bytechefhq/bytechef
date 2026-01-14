import {Locator, expect} from '@playwright/test';

export async function clickAndExpectToBeVisible({
    autoClick = false,
    target,
    timeout = 100,
    trigger,
}: {
    autoClick?: boolean;
    target: Locator;
    timeout?: number;
    trigger: Locator;
}) {
    await expect(async () => {
        if (!(await target.isVisible()) && (await trigger.isVisible())) {
            await trigger.click();
        }

        await expect(target).toBeVisible({timeout});

        if (autoClick) {
            await target.click();
        }
    }).toPass();
}
