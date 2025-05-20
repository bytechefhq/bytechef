import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {twMerge} from 'tailwind-merge';

const CredentialsStatus = ({enabled}: {enabled?: boolean}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <svg
                    aria-hidden="true"
                    className={twMerge(
                        'h-2.5 w-2.5',
                        enabled === undefined ? 'fill-secondary' : enabled ? 'fill-success' : 'fill-destructive'
                    )}
                    viewBox="0 0 6 6"
                >
                    <circle cx={3} cy={3} r={3} />
                </svg>
            </TooltipTrigger>

            <TooltipContent>
                {enabled === undefined
                    ? 'The connected user does not yet have any used integrations'
                    : enabled
                      ? 'All used connection credentials are valid'
                      : 'Some used connection credentials are invalid'}
            </TooltipContent>
        </Tooltip>
    );
};

export default CredentialsStatus;
