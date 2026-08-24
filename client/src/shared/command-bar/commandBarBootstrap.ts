import {createCommandSource} from '@/shared/command-bar/sources/createCommandSource';
import {resourceCommandSource} from '@/shared/command-bar/sources/resourceCommandSource';
import {registerCommandSource} from '@/shared/command-bar/useCommandSourceRegistry';

let bootstrapped = false;

export function bootstrapCommandBar(): void {
    if (bootstrapped) {
        return;
    }

    bootstrapped = true;

    registerCommandSource(resourceCommandSource);
    registerCommandSource(createCommandSource);
}
