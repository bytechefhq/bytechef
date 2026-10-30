import {describe, expect, it} from 'vitest';

import {paintMonochromeSvgWithCurrentColor} from '../svg-utils';

describe('paintMonochromeSvgWithCurrentColor', () => {
    it('repaints a black stroke on the root element', () => {
        const svg =
            '<svg fill="none" stroke="#000" viewBox="0 0 24 24"><line x1="4.5" x2="11" y1="9.5" y2="9.5"/></svg>';

        expect(paintMonochromeSvgWithCurrentColor(svg)).toBe(
            '<svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><line x1="4.5" x2="11" y1="9.5" y2="9.5"/></svg>'
        );
    });

    it('repaints every black spelling in attributes and inline styles', () => {
        const svg =
            '<svg viewBox="0 0 24 24"><path fill="black" d="M0"/><path fill="#000000" d="M1"/>' +
            '<path style="fill: rgb(0, 0, 0); stroke:#000" d="M2"/></svg>';

        expect(paintMonochromeSvgWithCurrentColor(svg)).toBe(
            '<svg viewBox="0 0 24 24"><path fill="currentColor" d="M0"/><path fill="currentColor" d="M1"/>' +
                '<path style="fill: currentColor; stroke:currentColor" d="M2"/></svg>'
        );
    });

    it('leaves an icon with any non-black paint untouched', () => {
        const svg = '<svg viewBox="0 0 24 24"><path fill="#000" d="M0"/><path fill="#E01E5A" d="M1"/></svg>';

        expect(paintMonochromeSvgWithCurrentColor(svg)).toBe(svg);
    });

    it('leaves an icon with a gradient paint untouched', () => {
        const svg = '<svg viewBox="0 0 24 24"><path fill="#000" d="M0"/><path fill="url(#gradient)" d="M1"/></svg>';

        expect(paintMonochromeSvgWithCurrentColor(svg)).toBe(svg);
    });

    it('leaves an icon with a style element untouched', () => {
        const svg =
            '<svg viewBox="0 0 24 24"><style>.a{fill:#4285f4}</style><path class="a" fill="#000" d="M0"/></svg>';

        expect(paintMonochromeSvgWithCurrentColor(svg)).toBe(svg);
    });

    it('does not touch colors that only start like black', () => {
        const svg = '<svg viewBox="0 0 24 24"><path fill="#0000ff" d="M0"/></svg>';

        expect(paintMonochromeSvgWithCurrentColor(svg)).toBe(svg);
    });
});
