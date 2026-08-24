import {createCommandSource} from '@/shared/command-bar/sources/createCommandSource';
import {resourceCommandSource} from '@/shared/command-bar/sources/resourceCommandSource';
import {registerCommandSource} from '@/shared/command-bar/useCommandSourceRegistry';

let bootstrapped = false;

/**
 * Registers the always-available command sources. Idempotent because React strict mode mounts App twice in
 * development, and a second registration would duplicate every resource command.
 */
export function bootstrapCommandBar(): void {
    if (bootstrapped) {
        return;
    }

    bootstrapped = true;

    registerCommandSource(resourceCommandSource);
    registerCommandSource(createCommandSource);
}
