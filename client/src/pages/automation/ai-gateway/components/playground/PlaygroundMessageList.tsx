import Button from '@/components/Button/Button';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {PlusIcon, TrashIcon} from 'lucide-react';
import {useCallback} from 'react';

export interface PlaygroundMessageI {
    content: string;
    role: 'ASSISTANT' | 'SYSTEM' | 'USER';
}

interface PlaygroundMessageListProps {
    messages: PlaygroundMessageI[];
    onMessagesChange: (messages: PlaygroundMessageI[]) => void;
}

const ROLE_OPTIONS: PlaygroundMessageI['role'][] = ['SYSTEM', 'USER', 'ASSISTANT'];

const PlaygroundMessageList = ({messages, onMessagesChange}: PlaygroundMessageListProps) => {
    const handleAddMessage = useCallback(() => {
        onMessagesChange([...messages, {content: '', role: 'USER'}]);
    }, [messages, onMessagesChange]);

    const handleRemoveMessage = useCallback(
        (index: number) => {
            const updatedMessages = messages.filter((_, messageIndex) => messageIndex !== index);

            onMessagesChange(updatedMessages);
        },
        [messages, onMessagesChange]
    );

    const handleContentChange = useCallback(
        (index: number, content: string) => {
            const updatedMessages = messages.map((message, messageIndex) =>
                messageIndex === index ? {...message, content} : message
            );

            onMessagesChange(updatedMessages);
        },
        [messages, onMessagesChange]
    );

    const handleRoleChange = useCallback(
        (index: number, role: PlaygroundMessageI['role']) => {
            const updatedMessages = messages.map((message, messageIndex) =>
                messageIndex === index ? {...message, role} : message
            );

            onMessagesChange(updatedMessages);
        },
        [messages, onMessagesChange]
    );

    return (
        <div className="space-y-3">
            {messages.map((message, index) => (
                <div className="flex gap-2" key={index}>
                    <div className="w-32 shrink-0">
                        <Select
                            onValueChange={(value) => handleRoleChange(index, value as PlaygroundMessageI['role'])}
                            value={message.role}
                        >
                            <SelectTrigger className="h-9 text-xs">
                                <SelectValue />
                            </SelectTrigger>

                            <SelectContent>
                                {ROLE_OPTIONS.map((role) => (
                                    <SelectItem key={role} value={role}>
                                        {role.charAt(0) + role.slice(1).toLowerCase()}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>

                    <Textarea
                        className="min-h-[60px] flex-1 resize-y text-sm"
                        onChange={(event) => handleContentChange(index, event.target.value)}
                        placeholder={`Enter ${message.role.toLowerCase()} message...`}
                        value={message.content}
                    />

                    <button
                        className="shrink-0 self-start p-2 text-destructive hover:text-destructive/80"
                        disabled={messages.length <= 1}
                        onClick={() => handleRemoveMessage(index)}
                    >
                        <TrashIcon className="size-4" />
                    </button>
                </div>
            ))}

            <Button
                className="mt-2"
                icon={<PlusIcon className="size-4" />}
                label="Add Message"
                onClick={handleAddMessage}
                variant="outline"
            />
        </div>
    );
};

export default PlaygroundMessageList;
