import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PlayIcon} from 'lucide-react';

const IntegrationHeaderRunButton = ({onRunClick, runDisabled}: {onRunClick: () => void; runDisabled: boolean}) => {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="hover:bg-background/70 [&_svg]:size-5"
                    disabled={runDisabled}
                    icon={<PlayIcon className="text-success" />}
                    onClick={() => onRunClick()}
                    size="icon"
                    variant="ghost"
                />
            </TooltipTrigger>

            <TooltipContent>
                {runDisabled
                    ? `The workflow cannot be executed. Please set all required workflow input parameters, connections and component properties.`
                    : `Run the current workflow`}
            </TooltipContent>
        </Tooltip>
    );
};

export default IntegrationHeaderRunButton;
