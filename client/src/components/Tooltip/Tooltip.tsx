import {
    Tooltip as ShadcnTooltip,
    TooltipContent as ShadcnTooltipContent,
    TooltipPortal as ShadcnTooltipPortal,
    TooltipProvider as ShadcnTooltipProvider,
    TooltipTrigger as ShadcnTooltipTrigger,
} from '@/components/ui/tooltip';
import {type ComponentProps} from 'react';

export const TOOLTIP_DELAY_DURATION = 500;

type TooltipPropsType = ComponentProps<typeof ShadcnTooltip>;

type TooltipProviderPropsType = ComponentProps<typeof ShadcnTooltipProvider>;

function Tooltip({delayDuration = TOOLTIP_DELAY_DURATION, ...props}: TooltipPropsType) {
    return <ShadcnTooltip delayDuration={delayDuration} {...props} />;
}

function TooltipProvider({delayDuration = TOOLTIP_DELAY_DURATION, ...props}: TooltipProviderPropsType) {
    return <ShadcnTooltipProvider delayDuration={delayDuration} {...props} />;
}

const TooltipContent = ShadcnTooltipContent;

const TooltipPortal = ShadcnTooltipPortal;

const TooltipTrigger = ShadcnTooltipTrigger;

export {Tooltip, TooltipContent, TooltipPortal, TooltipProvider, TooltipTrigger};
