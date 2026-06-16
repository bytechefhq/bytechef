import {type CSSProperties, type ReactNode, Suspense, lazy, useMemo} from 'react';

const LARGE_PAYLOAD_CHARS = 100_000;

const ReactJson = lazy(async () => {
    const module = await import('react-json-view');

    const component = (module.default as unknown as Record<string, unknown>)?.default || module.default;

    return {default: component} as typeof module;
});

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
    sortKeys,
    src,
    style,
}: JsonViewProps) => {
    const resolvedCollapsed = useMemo(
        () => (collapsed === undefined ? getJsonViewCollapsed(src) : collapsed),
        [collapsed, src]
    );

    return (
        <Suspense fallback={fallback}>
            <ReactJson
                collapseStringsAfterLength={collapseStringsAfterLength}
                collapsed={resolvedCollapsed}
                enableClipboard={enableClipboard}
                name={name}
                sortKeys={sortKeys}
                src={src}
                style={style}
            />
        </Suspense>
    );
};

export default JsonView;
