import {type CommandI} from '@/shared/command-bar/types';
import {registerCommandSource} from '@/shared/command-bar/useCommandSourceRegistry';
import {useEffect, useRef} from 'react';

let nextHookSourceId = 0;

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
