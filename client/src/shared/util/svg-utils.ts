const BLACK_PAINTS = ['#000', '#000000', 'black', 'rgb(0,0,0)'];

const NEUTRAL_PAINTS = ['currentcolor', 'none', 'transparent'];

const PAINT_PATTERN = /(?<![\w-])(fill|stroke)(?![\w-])(\s*=\s*"|\s*=\s*'|\s*:\s*)([^"';]*)/g;

const normalizePaint = (paint: string) => paint.replace(/\s+/g, '').toLowerCase();

/**
 * Repaints an icon drawn only in black with currentColor, so it takes the theme's text color instead of staying black
 * on a dark background. Icons with any other paint, a gradient or a style element keep their own colors.
 */
export function paintMonochromeSvgWithCurrentColor(svg: string): string {
    if (/<style[\s>]/i.test(svg)) {
        return svg;
    }

    const paints = [...svg.matchAll(PAINT_PATTERN)].map((match) => normalizePaint(match[3]));

    const blackOnly =
        paints.some((paint) => BLACK_PAINTS.includes(paint)) &&
        paints.every((paint) => BLACK_PAINTS.includes(paint) || NEUTRAL_PAINTS.includes(paint));

    if (!blackOnly) {
        return svg;
    }

    return svg.replace(PAINT_PATTERN, (match, property: string, separator: string, paint: string) => {
        if (!BLACK_PAINTS.includes(normalizePaint(paint))) {
            return match;
        }

        return `${property}${separator}${paint.match(/^\s*/)![0]}currentColor`;
    });
}
