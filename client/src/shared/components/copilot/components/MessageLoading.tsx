import {BotMessageSquareIcon} from 'lucide-react';
import React from 'react';

const MessageLoading = () => {
    return (
        <div className="flex items-center gap-x-2">
            <div className="flex size-8 items-center justify-center rounded-full border-2 border-primary">
                <BotMessageSquareIcon className="size-5" />
            </div>

            <div className="flex animate-pulse space-x-2">
                <div className="size-2 rounded-full bg-gray-500"></div>

                <div className="size-2 rounded-full bg-gray-500"></div>

                <div className="size-2 rounded-full bg-gray-500"></div>
            </div>
        </div>
    );
};

export default MessageLoading;
