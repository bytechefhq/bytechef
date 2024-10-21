import {MessageType} from '@/pages/platform/copilot/CopilotPanel';
import {BotMessageSquareIcon} from 'lucide-react';
import React from 'react';

const Message = ({message}: {message: MessageType}) => {
    return (
        <div className="flex items-start gap-x-2">
            {message.sender === 'ai' && (
                <div className="flex size-8 items-center justify-center rounded-full border-2 border-primary">
                    <BotMessageSquareIcon className="size-5" />
                </div>
            )}

            {message.sender === 'ai' ? (
                <div className="flex-1">
                    <p className="text-sm">{message.text}</p>
                </div>
            ) : (
                <div className="ml-8 flex-1 rounded-lg bg-card p-3 text-card-foreground">
                    <p className="text-sm">{message.text}</p>
                </div>
            )}
        </div>
    );
};

export default Message;
