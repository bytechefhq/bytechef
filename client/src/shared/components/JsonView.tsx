import {type CSSProperties, type ReactNode, useEffect, useMemo, useState} from 'react';

import type ReactJsonViewComponent from 'react-json-view';

const LARGE_PAYLOAD_CHARS = 100_000;

let reactJsonComponent: typeof ReactJsonViewComponent | null = null;
let reactJsonComponentPromise: Promise<typeof ReactJsonViewComponent> | null = null;

const loadReactJsonComponent = () => {
    if (!reactJsonComponentPromise) {
        reactJsonComponentPromise = import('react-json-view').then((module) => {
            reactJsonComponent =
                ((module.default as unknown as Record<string, unknown>)?.default as typeof module.default) ||
                module.default;

            return reactJsonComponent;
        });

        reactJsonComponentPromise.catch(() => {
            reactJsonComponentPromise = null;
        });
    }

    return reactJsonComponentPromise;
};

export const useJsonViewLoaded = () => {
    const [loadError, setLoadError] = useState<unknown>(null);
    const [loaded, setLoaded] = useState(() => reactJsonComponent !== null);

    useEffect(() => {
        if (loaded) {
            return;
        }

        let active = true;

        loadReactJsonComponent().then(
            () => {
                if (active) {
                    setLoaded(true);
                }
            },
            (error: unknown) => {
                if (active) {
                    setLoadError(error ?? new Error('Failed to load the JSON viewer'));
                }
            }
        );

        return () => {
            active = false;
        };
    }, [loaded]);

    if (loadError) {
        throw loadError;
    }

    return loaded;
};

const exceedsCharBudget = (src: unknown, limit: number): boolean => {
    let remaining = limit;

    const seen = new WeakSet<object>();

    const visit = (value: unknown): boolean => {
        if (value === null) {
            remaining -= 4;

            return remaining <= 0;
        }

        const valueType = typeof value;

        if (valueType === 'string') {
            remaining -= (value as string).length + 2;

            return remaining <= 0;
        }

        if (valueType === 'number' || valueType === 'boolean') {
            remaining -= String(value).length;

            return remaining <= 0;
        }

        if (valueType !== 'object') {
            return false;
        }

        if (seen.has(value as object)) {
            return true;
        }

        seen.add(value as object);

        if (Array.isArray(value)) {
            for (const item of value) {
                remaining -= 1;

                if (visit(item)) {
                    return true;
                }
            }

            return false;
        }

        for (const [key, entry] of Object.entries(value as Record<string, unknown>)) {
            remaining -= key.length + 4;

            if (remaining <= 0 || visit(entry)) {
                return true;
            }
        }

        return false;
    };

    return visit(src);
};

export const getJsonViewCollapsed = (src: object): boolean | number =>
    exceedsCharBudget(src, LARGE_PAYLOAD_CHARS) ? 1 : false;

interface JsonViewProps {
    collapseStringsAfterLength?: number;
    collapsed?: boolean | number;
    enableClipboard?: boolean;
    fallback?: ReactNode;
    name?: string | false;
    shouldCollapse?: (field: {src: object}) => boolean;
    sortKeys?: boolean;
    src: object;
    style?: CSSProperties;
}

const JsonView = ({
    collapseStringsAfterLength = 10000,
    collapsed,
    enableClipboard = false,
    fallback = <span className="block p-4 text-sm text-muted-foreground">Loading...</span>,
    name,
    shouldCollapse,
    sortKeys,
    src,
    style,
}: JsonViewProps) => {
    const loaded = useJsonViewLoaded();

    const resolvedCollapsed = useMemo(
        () => (collapsed === undefined ? getJsonViewCollapsed(src) : collapsed),
        [collapsed, src]
    );

    const ReactJson = reactJsonComponent;

    if (!loaded || !ReactJson) {
        return fallback;
    }

    return (
        <ReactJson
            collapseStringsAfterLength={collapseStringsAfterLength}
            collapsed={resolvedCollapsed}
            enableClipboard={enableClipboard}
            name={name}
            shouldCollapse={shouldCollapse}
            sortKeys={sortKeys}
            src={src}
            style={style}
        />
    );
};

export default JsonView;
