import {type CSSProperties, type ReactNode, Suspense, lazy, useMemo} from 'react';

const ReactJson = lazy(async () => {
    const module = await import('react-json-view');

    const component = (module.default as unknown as Record<string, unknown>)?.default || module.default;

    return {default: component} as typeof module;
});

const LARGE_PAYLOAD_CHARS = 100_000;

export const getJsonViewCollapsed = (src: object): boolean | number => {
    try {
        return JSON.stringify(src).length > LARGE_PAYLOAD_CHARS ? 1 : false;
    } catch {
        // Circular or otherwise non-serializable structures: collapse to stay responsive.
        return 1;
    }
};

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
    fallback = <div className="p-4 text-sm text-muted-foreground">Loading...</div>,
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
