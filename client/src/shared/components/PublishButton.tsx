import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {SendIcon} from 'lucide-react';

interface PublishButtonProps {
    disabledReason?: string;
    onClick: () => void;
}

const PublishButton = ({disabledReason, onClick}: PublishButtonProps) => (
    <Tooltip>
        <TooltipTrigger asChild>
            <span className="inline-block">
                <Button disabled={!!disabledReason} icon={<SendIcon />} label="Publish" onClick={onClick} />
            </span>
        </TooltipTrigger>

        {disabledReason && <TooltipContent>{disabledReason}</TooltipContent>}
    </Tooltip>
);

export default PublishButton;
