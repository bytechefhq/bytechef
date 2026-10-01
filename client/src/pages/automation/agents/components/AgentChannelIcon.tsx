import {ComponentIcon, MessageCircleIcon, WorkflowIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

interface AgentChannelIconProps {
    channelType?: string | null;
    className?: string;
    icon?: string | null;
}

const AgentChannelIcon = ({channelType, className, icon}: AgentChannelIconProps) => {
    const iconClassName = twMerge('size-3.5 shrink-0', className);

    if (icon) {
        return (
            <InlineSVG
                className={iconClassName}
                loader={<ComponentIcon className={iconClassName} />}
                src={icon}
                title={null}
            />
        );
    }

    if (channelType === 'chat') {
        return <MessageCircleIcon className={iconClassName} />;
    }

    if (channelType === 'workflowCall') {
        return <WorkflowIcon className={iconClassName} />;
    }

    return <ComponentIcon className={iconClassName} />;
};

export default AgentChannelIcon;
