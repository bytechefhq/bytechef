import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import Message from '@/pages/platform/copilot/components/Message';
import MessageLoading from '@/pages/platform/copilot/components/MessageLoading';
import {useCopilotStore} from '@/pages/platform/copilot/stores/useCopilotStore';
import {Cross2Icon} from '@radix-ui/react-icons';
import {BotMessageSquareIcon, MessageSquareOffIcon} from 'lucide-react';
import React, {ChangeEvent, KeyboardEvent, useEffect, useRef, useState} from 'react';
import {IconRight} from 'react-day-picker';
import TextareaAutosize from 'react-textarea-autosize';

export type MessageType = {text: string; sender: 'ai' | 'user'};

const CopilotPanel = () => {
    const {setShowCopilot} = useCopilotStore();

    const [messages, setMessages] = useState<MessageType[]>([]);
    const [input, setInput] = useState('');
    const [altDown, setAltDown] = useState<boolean>(false);
    const [loading, setLoading] = useState(false);

    const messagesEndRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({behavior: 'smooth'});
        }
    }, [messages]);

    const handleSend = async () => {
        if (input.trim() === '') {
            return;
        }

        const newMessage: MessageType = {sender: 'user', text: input};

        setMessages([...messages, newMessage]);
        setInput('');
        setLoading(true);

        try {
            const response = await fetch('/api/platform/internal/ai/chat?message=' + input, {
                method: 'GET',
            }).then((res) => res.text());

            const aiMessage: MessageType = {sender: 'ai', text: response};

            setMessages([...messages, newMessage, aiMessage]);
        } catch (error) {
            console.error('Error fetching AI response', error);
        } finally {
            setLoading(false);
        }
    };

    const handleTextAreaChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        setInput(event.target.value);
    };

    const handleKeyDown = (event: KeyboardEvent) => {
        if (event.altKey) {
            setAltDown(true);
        }

        if (event.key === 'Enter') {
            if (altDown) {
                setInput(input + ' \r\n');
            } else {
                return handleSend();
            }
        }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
        if (e.key === 'Alt') {
            setAltDown(false);
        }
    };

    return (
        <div className="relative flex h-full min-h-[50vh] w-[450px] flex-col bg-muted/50 p-3 lg:col-span-2">
            <div className="mb-4 flex items-center justify-between">
                <div className="flex items-center space-x-1">
                    <BotMessageSquareIcon className="size-6" /> <h4>AI Copilot</h4>
                </div>

                <div className="flex">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button onClick={() => setMessages([])} size="icon" variant="ghost">
                                <MessageSquareOffIcon className="size-4" />
                            </Button>
                        </TooltipTrigger>

                        <TooltipContent>Clean messages</TooltipContent>
                    </Tooltip>

                    <Button onClick={() => setShowCopilot(false)} size="icon" variant="ghost">
                        <Cross2Icon />
                    </Button>
                </div>
            </div>

            <div className="flex-1 space-y-5 overflow-auto pb-0.5">
                {messages.map((message, index) => (
                    <Message key={index} message={message} />
                ))}

                {loading && <MessageLoading />}

                <div ref={messagesEndRef} />
            </div>

            <div className="relative mt-4">
                <TextareaAutosize
                    autoFocus
                    className="flex min-h-11 w-full rounded-md border-none bg-background p-3 text-sm shadow-sm ring-2 ring-input placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-foreground/50 disabled:cursor-not-allowed disabled:opacity-50"
                    disabled={loading}
                    onChange={handleTextAreaChange}
                    onKeyDown={handleKeyDown}
                    onKeyUp={handleKeyUp}
                    placeholder="Ask a question..."
                    value={input}
                />

                <div className="absolute bottom-[5px] right-1">
                    <Button className="ml-auto gap-1.5" onClick={handleSend} size="icon" type="submit" variant="ghost">
                        <IconRight className="size-3.5" />
                    </Button>
                </div>
            </div>
        </div>
    );
};

export default CopilotPanel;
