import {useState} from 'react';

interface UseCreateTestDialogProps {
    onClose: () => void;
    onCreate: (name: string, description?: string) => void;
}

export default function useCreateTestDialog({onClose, onCreate}: UseCreateTestDialogProps) {
    const [description, setDescription] = useState('');
    const [name, setName] = useState('');

    const handleCreate = () => {
        if (!name.trim()) {
            return;
        }

        onCreate(name.trim(), description.trim() || undefined);
        onClose();
    };

    return {
        description,
        handleCreate,
        name,
        setDescription,
        setName,
    };
}
