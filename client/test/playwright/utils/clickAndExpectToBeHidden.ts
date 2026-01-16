import {Locator, expect} from '@playwright/test';

export async function clickAndExpectToBeHidden({
    target,
    timeout = 100,
    trigger,
}: {
    target: Locator;
    timeout?: number;
    trigger: Locator;
}) {
    await expect(async () => {
        if (await trigger.isVisible()) {
            await trigger.click();
        }

        await expect(target).toBeHidden({timeout});
    }).toPass();
}
