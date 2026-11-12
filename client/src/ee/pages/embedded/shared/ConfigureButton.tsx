import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Settings2Icon} from 'lucide-react';

interface ConfigureButtonProps {
    disabledReason?: string;
    onClick: () => void;
}

const ConfigureButton = ({disabledReason, onClick}: ConfigureButtonProps) => {
    if (disabledReason) {
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="inline-flex">
                        <Button
                            className="rounded-none border-l-0"
                            disabled
                            icon={<Settings2Icon />}
                            label="Configure"
                            variant="outline"
                        />
                    </span>
                </TooltipTrigger>

                <TooltipContent>{disabledReason}</TooltipContent>
            </Tooltip>
        );
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <Button icon={<Settings2Icon />} label="Configure" onClick={onClick} variant="outline" />
            </TooltipTrigger>

            <TooltipContent>Create a configuration or change the version of one</TooltipContent>
        </Tooltip>
    );
};

export default ConfigureButton;
