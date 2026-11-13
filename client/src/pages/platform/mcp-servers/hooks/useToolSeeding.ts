import {useCallback, useEffect, useRef, useState} from 'react';

export type SeededValuesByKeyType = Record<string, Record<string, string>>;

export type SeedToolType = (key: string) => Promise<Record<string, string>>;

interface UseToolSeedingParamsI {
    keys: string[];
    seed: SeedToolType;
    seedKey: string;
}

interface ToolSeedI {
    failed: boolean;
    promise: Promise<Record<string, string>>;
    seedKey: string;
}

const KEY_SEPARATOR = '\u0000';

async function runSeed(seed: SeedToolType, key: string, onFailure: () => void): Promise<Record<string, string>> {
    try {
        return await seed(key);
    } catch {
        onFailure();

        return {};
    }
}

export default function useToolSeeding({keys, seed, seedKey}: UseToolSeedingParamsI) {
    const [seededValuesByKey, setSeededValuesByKey] = useState<SeededValuesByKeyType>({});

    const seedRef = useRef(seed);
    const toolSeedsRef = useRef<Map<string, ToolSeedI>>(new Map());

    seedRef.current = seed;

    const keysKey = keys.join(KEY_SEPARATOR);

    const waitForSeeding = useCallback(async (): Promise<SeededValuesByKeyType> => {
        let toolSeeds = Array.from(toolSeedsRef.current.entries());

        while (true) {
            const seededEntries = await Promise.all(
                toolSeeds.map(async ([key, toolSeed]) => [key, await toolSeed.promise] as const)
            );

            const currentToolSeeds = Array.from(toolSeedsRef.current.entries());

            const isUnchanged =
                currentToolSeeds.length === toolSeeds.length &&
                currentToolSeeds.every(
                    ([key, toolSeed], index) => toolSeeds[index][0] === key && toolSeeds[index][1] === toolSeed
                );

            if (isUnchanged) {
                return Object.fromEntries(seededEntries);
            }

            toolSeeds = currentToolSeeds;
        }
    }, []);

    const getFailedSeedKeys = useCallback(
        (): string[] =>
            Array.from(toolSeedsRef.current.entries())
                .filter(([, toolSeed]) => toolSeed.failed)
                .map(([key]) => key),
        []
    );

    useEffect(() => {
        const currentKeys = keysKey ? keysKey.split(KEY_SEPARATOR) : [];
        const toolSeeds = toolSeedsRef.current;

        const removedKeys = Array.from(toolSeeds.keys()).filter((key) => !currentKeys.includes(key));

        for (const key of removedKeys) {
            toolSeeds.delete(key);
        }

        if (removedKeys.length > 0) {
            setSeededValuesByKey((previousValues) =>
                Object.fromEntries(Object.entries(previousValues).filter(([key]) => !removedKeys.includes(key)))
            );
        }

        const keysToSeed = currentKeys.filter((key) => toolSeeds.get(key)?.seedKey !== seedKey);

        for (const key of keysToSeed) {
            const toolSeed: ToolSeedI = {failed: false, promise: Promise.resolve({}), seedKey};

            toolSeed.promise = runSeed(seedRef.current, key, () => {
                toolSeed.failed = true;
            });

            toolSeeds.set(key, toolSeed);

            void toolSeed.promise.then((seededValues) => {
                if (toolSeedsRef.current.get(key) !== toolSeed) {
                    return;
                }

                setSeededValuesByKey((previousValues) => ({...previousValues, [key]: seededValues}));
            });
        }
    }, [keysKey, seedKey]);

    return {
        getFailedSeedKeys,
        seededValuesByKey,
        waitForSeeding,
    };
}
