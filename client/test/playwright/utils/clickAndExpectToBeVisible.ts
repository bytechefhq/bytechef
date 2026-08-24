import {Locator, expect} from '@playwright/test';

import {TIMEOUTS} from './constants';

export async function clickAndExpectToBeVisible({
    autoClick = false,
    clickTimeout = TIMEOUTS.RETRY_CLICK,
    target,
    timeout = TIMEOUTS.CLICK_AND_EXPECT,
    trigger,
    visibilityTimeout = TIMEOUTS.RETRY_VISIBILITY,
}: {
    autoClick?: boolean;
    clickTimeout?: number;
    target: Locator;
    timeout?: number;
    trigger: Locator;
    visibilityTimeout?: number;
}) {
    await expect(async () => {
        if (!(await target.isVisible())) {
            // Bounded so a trigger that is not on the page yet cannot swallow the whole retry budget
            // in a single iteration (the global actionTimeout is far longer than `timeout`).
            await trigger.click({timeout: clickTimeout});
        }

        await expect(target).toBeVisible({timeout: visibilityTimeout});

        if (autoClick) {
            await target.click();
        }
    }).toPass({timeout});
}
