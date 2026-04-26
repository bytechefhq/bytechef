import {FormLabel} from '@/components/ui/form';
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from '@/components/ui/tooltip';
import {InfoIcon} from 'lucide-react';

import type {ReactNode} from 'react';

interface FormLabelWithDescriptionPropsI {
    description?: string | null;
    label: ReactNode;
}

export const FormLabelWithDescription = ({description, label}: FormLabelWithDescriptionPropsI) => {
    if (!description) {
        return <FormLabel>{label}</FormLabel>;
    }

    return (
        <FormLabel className="flex items-center gap-1.5">
            <span>{label}</span>

            <TooltipProvider delayDuration={150}>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <button
                            aria-label="Field description"
                            className="inline-flex items-center text-muted-foreground hover:text-foreground"
                            tabIndex={-1}
                            type="button"
                        >
                            <InfoIcon className="size-3.5" />
                        </button>
                    </TooltipTrigger>

                    <TooltipContent className="max-w-xs whitespace-pre-line">{description}</TooltipContent>
                </Tooltip>
            </TooltipProvider>
        </FormLabel>
    );
};
