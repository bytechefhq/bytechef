import {type CommandI} from '@/shared/command-bar/types';
import {registerCommandSource} from '@/shared/command-bar/useCommandSourceRegistry';
import {useEffect, useRef} from 'react';

let nextHookSourceId = 0;

/**
 * The React door into the command registry: commands live exactly as long as the component that registered them.
 * `dependencies` is the caller's own dependency array -- pass the values the commands close over.
 */
export function useRegisterCommands(commands: CommandI[], dependencies: unknown[]): void {
    const sourceIdRef = useRef<string | undefined>(undefined);

    if (!sourceIdRef.current) {
        sourceIdRef.current = `hook-${nextHookSourceId++}`;
    }

    useEffect(() => {
        return registerCommandSource({getCommands: () => commands, id: sourceIdRef.current!});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, dependencies);
}
