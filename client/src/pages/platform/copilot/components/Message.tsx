import {MessageType} from '@/pages/platform/copilot/CopilotPanel';
import {BotMessageSquareIcon} from 'lucide-react';
import React from 'react';
import {twMerge} from 'tailwind-merge';

const Message = ({message}: {message: MessageType}) => {
    return (
        <div className="flex items-start gap-x-2">
            {message.sender === 'ai' && (
                <div className="flex size-8 items-center justify-center rounded-full border-2 border-primary">
                    <BotMessageSquareIcon className="size-5" />
                </div>
            )}

            <div
                className={twMerge(
                    'flex-1',
                    message.sender === 'user' && 'ml-8 rounded-lg bg-card p-3 text-card-foreground'
                )}
            >
                <p className="text-sm">{message.text}</p>
            </div>
        </div>
    );
};

export default Message;
