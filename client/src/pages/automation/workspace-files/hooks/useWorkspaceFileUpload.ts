import {useState} from 'react';

interface UploadResultI {
    id: number;
    mimeType: string;
    name: string;
    sizeBytes: number;
}

export const useWorkspaceFileUpload = () => {
    const [progress, setProgress] = useState<number>(0);

    const upload = async (workspaceId: number, file: File): Promise<UploadResultI> => {
        const formData = new FormData();

        formData.append('workspaceId', String(workspaceId));
        formData.append('file', file);

        setProgress(0);

        const response = await fetch('/api/automation/internal/workspace-files/upload', {
            body: formData,
            method: 'POST',
        });

        if (!response.ok) {
            throw new Error(`Upload failed: ${response.status} ${response.statusText}`);
        }

        setProgress(100);

        return response.json();
    };

    return {progress, upload};
};
