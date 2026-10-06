import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {CredentialStatus} from '@/ee/shared/middleware/embedded/connected-user';
import {twMerge} from 'tailwind-merge';

const ConnectedUserCredentialStatus = ({
    componentTitle,
    credentialStatus,
}: {
    componentTitle: string;
    credentialStatus?: CredentialStatus;
}) => {
    const credentialsValid = credentialStatus === 'VALID';

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <div className="flex shrink-0 items-center space-x-1 text-xs text-muted-foreground">
                    <svg
                        aria-hidden="true"
                        className={twMerge('h-2.5 w-2.5', credentialsValid ? 'fill-success' : 'fill-destructive')}
                        viewBox="0 0 6 6"
                    >
                        <circle cx={3} cy={3} r={3} />
                    </svg>

                    <span>{`Account ${credentialsValid ? 'Connected' : 'Errors'}`}</span>
                </div>
            </TooltipTrigger>

            <TooltipContent>
                {credentialsValid
                    ? `The user's ${componentTitle} credentials are valid`
                    : `The user's ${componentTitle} credentials are invalid; they need to reconnect`}
            </TooltipContent>
        </Tooltip>
    );
};

export default ConnectedUserCredentialStatus;
