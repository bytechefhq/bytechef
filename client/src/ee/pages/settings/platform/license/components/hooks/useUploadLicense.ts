import {useUploadLicenceMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChangeEvent, useCallback, useState} from 'react';

export default function useUploadLicense() {
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [uploading, setUploading] = useState(false);

    const queryClient = useQueryClient();

    const {mutateAsync: uploadLicence} = useUploadLicenceMutation();

    const handleFileChange = useCallback((event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];

        if (file) {
            setSelectedFile(file);
        }
    }, []);

    const handleUpload = useCallback(async () => {
        if (!selectedFile) {
            return;
        }

        setUploading(true);

        try {
            const contents = await selectedFile.text();

            await uploadLicence({contents});

            queryClient.invalidateQueries({queryKey: ['licence']});

            setSelectedFile(null);
        } finally {
            setUploading(false);
        }
    }, [selectedFile, uploadLicence, queryClient]);

    const removeFile = useCallback(() => {
        setSelectedFile(null);
    }, []);

    return {
        handleFileChange,
        handleUpload,
        removeFile,
        selectedFile,
        uploading,
    };
}
