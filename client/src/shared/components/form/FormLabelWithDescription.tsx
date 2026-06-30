import {FormLabel} from '@/components/ui/form';
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from '@/components/ui/tooltip';
import {InfoIcon} from 'lucide-react';

import type {ReactNode} from 'react';

interface FormLabelWithDescriptionPropsI {
    description?: string | null;
    label: ReactNode;
    required?: boolean;
}

const RequiredMarker = () => (
    <span aria-hidden="true" className="ml-0.5 text-destructive">
        *
    </span>
);

export const FormLabelWithDescription = ({description, label, required}: FormLabelWithDescriptionPropsI) => {
    if (!description) {
        return (
            <FormLabel>
                {label}

                {required && <RequiredMarker />}
            </FormLabel>
        );
    }

    return (
        <FormLabel className="flex items-center gap-1.5">
            <span>
                {label}

                {required && <RequiredMarker />}
            </span>

            <TooltipProvider delayDuration={150}>
                <Tooltip>
                    <TooltipTrigger asChild>
                        <button
                            aria-label="Field description"
                            className="inline-flex items-center text-muted-foreground hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:outline-hidden"
                            onClick={(event) => event.stopPropagation()}
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
