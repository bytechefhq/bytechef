import {getCookie} from '@/shared/util/cookie-utils';
import {Dispatch, SetStateAction} from 'react';

export type UploadFileStatusType = 'completed' | 'error' | 'pending' | 'uploading';

export interface UploadFileI {
    file: File;
    status: string;
    statusMessage?: string;
}

interface UploadSelectedFileProps<T extends UploadFileI> {
    fieldName: string;
    file: File;
    index: number;
    setSelectedFiles: Dispatch<SetStateAction<T[]>>;
    url: string;
}

export function formatFileSize(bytes: number) {
    if (bytes === 0) {
        return '0 Bytes';
    }

    const bytesPerUnit = 1024;
    const sizeUnits = ['Bytes', 'KB', 'MB', 'GB'];
    const unitIndex = Math.floor(Math.log(bytes) / Math.log(bytesPerUnit));
    const sizeInUnit = bytes / Math.pow(bytesPerUnit, unitIndex);

    return parseFloat(sizeInUnit.toFixed(1)) + ' ' + sizeUnits[unitIndex];
}

export async function uploadSelectedFile<T extends UploadFileI>({
    fieldName,
    file,
    index,
    setSelectedFiles,
    url,
}: UploadSelectedFileProps<T>) {
    const updateSelectedFile = (status: UploadFileStatusType, statusMessage?: string) => {
        setSelectedFiles((previousFiles) => {
            const updatedFiles = [...previousFiles];

            updatedFiles[index] = {
                ...updatedFiles[index],
                status,
                ...(statusMessage === undefined ? {} : {statusMessage}),
            };

            return updatedFiles;
        });
    };

    updateSelectedFile('uploading');

    try {
        const formData = new FormData();

        formData.append(fieldName, file);

        const response = await fetch(url, {
            body: formData,
            headers: {
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
            },
            method: 'POST',
        });

        if (!response.ok) {
            throw new Error(`Upload failed: ${response.statusText}`);
        }

        updateSelectedFile('completed', 'Uploaded successfully');
    } catch (error) {
        updateSelectedFile('error', error instanceof Error ? error.message : 'Upload failed');
    }
}
