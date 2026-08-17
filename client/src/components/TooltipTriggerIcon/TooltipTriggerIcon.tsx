import {TooltipTrigger} from '@/components/ui/tooltip';
import {ReactNode} from 'react';
import {twMerge} from 'tailwind-merge';

interface TooltipTriggerIconPropsI {
    /** The icon element, e.g. `<InfoIcon className="size-3.5 text-muted-foreground" />`. */
    children: ReactNode;
    className?: string;
    /**
     * Accessible name for the trigger. Name what the tooltip is ABOUT, not the icon — "Name" or "Scheduled",
     * never "info" — since a screen reader announces this in place of the graphic. A page of identically-named
     * triggers is a listing of "more information, more information, …" with nothing to distinguish them.
     */
    label: string;
}

/**
 * Focusable, labelled trigger for an icon whose only content is its tooltip.
 *
 * The house idiom was a bare `<svg>` used directly as `TooltipTrigger asChild`. Two defects came with it: an svg
 * is not focusable, so the tooltip was mouse-only and keyboard users could never read it; and most sites passed
 * no `aria-label` at all, so the icon — and therefore the only copy of that explanation — was invisible to
 * screen readers entirely.
 *
 * A real `<button>` rather than a `<span tabIndex={0}>`: revealing content on activation IS an interaction, and
 * the button earns focus, Enter/Space, and Radix's own Escape-to-dismiss handling without further work.
 * `type="button"` keeps it from submitting the forms most of these sit in.
 *
 * Sizing stays on the caller's icon; the wrapper is `inline-flex shrink-0` so it occupies exactly the space the
 * bare svg did inside the flex rows these live in.
 *
 * NOT usable for an icon already inside a clickable element — the chat-row status icons in `AiHubChatsSidebar`
 * sit inside the row's own `<button>`, where a nested button is invalid HTML and would swallow the row click.
 * Those keep a bare svg with `aria-label` + `role="img"`: their meaning is already in the label, and turning
 * each into its own tab stop inside a clickable row would cost more than the tooltip is worth.
 */
const TooltipTriggerIcon = ({children, className, label}: TooltipTriggerIconPropsI) => (
    <TooltipTrigger asChild>
        <button
            aria-label={label}
            className={twMerge(
                'inline-flex shrink-0 cursor-help items-center justify-center rounded-sm text-inherit',
                'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring',
                className
            )}
            type="button"
        >
            {children}
        </button>
    </TooltipTrigger>
);

export default TooltipTriggerIcon;
