import {RefObject, useLayoutEffect, useState} from 'react';

/**
 * Tracks an element's content-box width with a ResizeObserver, so layout can react to the space actually
 * available instead of a fixed breakpoint. Reads the width synchronously on mount (useLayoutEffect) to avoid
 * a flash of the zero-width default before the observer's first callback fires.
 */
const useElementWidth = (ref: RefObject<HTMLElement | null>) => {
    const [width, setWidth] = useState(0);

    useLayoutEffect(() => {
        const element = ref.current;

        if (!element) {
            return;
        }

        setWidth(element.getBoundingClientRect().width);

        const resizeObserver = new ResizeObserver(([entry]) => {
            setWidth(entry.contentRect.width);
        });

        resizeObserver.observe(element);

        return () => resizeObserver.disconnect();
    }, [ref]);

    return width;
};

export default useElementWidth;
