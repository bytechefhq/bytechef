import {ThreadMessageLike} from '@assistant-ui/react';
import {BotMessageSquareIcon} from 'lucide-react';
import React from 'react';
import {twMerge} from 'tailwind-merge';

const Message = ({message}: {message: ThreadMessageLike}) => {
    return (
        <div className="flex items-start gap-x-2">
            {message.role === 'assistant' && (
                <div className="flex size-8 items-center justify-center rounded-full border-2 border-primary">
                    <BotMessageSquareIcon className="size-5" />
                </div>
            )}

            <div
                className={twMerge(
                    'flex-1',
                    message.role === 'user' && 'ml-8 rounded-lg bg-card p-3 text-card-foreground'
                )}
            >
                <p className="text-sm">{message.content.toString()}</p>
            </div>
        </div>
    );
};

export default Message;
