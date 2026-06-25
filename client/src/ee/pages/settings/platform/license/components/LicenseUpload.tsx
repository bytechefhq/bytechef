import Button from '@/components/Button/Button';
import {Label} from '@/components/ui/label';
import useUploadLicense from '@/ee/pages/settings/platform/license/components/hooks/useUploadLicense';
import {Loader2Icon, UploadIcon, XIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

const LicenseUpload = () => {
    const {handleFileChange, handleUpload, removeFile, selectedFile, uploading} = useUploadLicense();

    return (
        <div className="space-y-6">
            <div className="space-y-2">
                <Label>License File</Label>

                <div
                    className={twMerge(
                        'flex flex-col items-center justify-center rounded-lg border-2 border-dashed border-gray-300 p-10',
                        uploading ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-gray-50'
                    )}
                    onClick={() => !uploading && document.getElementById('license-file-upload')?.click()}
                >
                    <UploadIcon className="mb-2 size-8 text-gray-400" />

                    <p className="text-sm text-gray-600">Drop your license file here or click to browse</p>

                    <p className="mt-1 text-xs text-gray-400">.lic files only</p>

                    <input
                        accept=".lic"
                        className="hidden"
                        disabled={uploading}
                        id="license-file-upload"
                        onChange={handleFileChange}
                        type="file"
                    />
                </div>
            </div>

            {selectedFile && (
                <div className="space-y-2">
                    <Label>Selected File</Label>

                    <div className="flex items-center justify-between rounded-md border border-gray-200 bg-gray-50 p-2 px-3">
                        <span className="truncate text-sm font-medium">{selectedFile.name}</span>

                        {!uploading && (
                            <button
                                className="ml-2 rounded-full p-1 hover:bg-gray-200"
                                onClick={(event) => {
                                    event.stopPropagation();
                                    removeFile();
                                }}
                                type="button"
                            >
                                <XIcon className="size-4 text-content-neutral-secondary" />
                            </button>
                        )}
                    </div>
                </div>
            )}

            <div className="flex justify-end">
                <Button disabled={!selectedFile || uploading} onClick={handleUpload}>
                    {uploading ? (
                        <>
                            <Loader2Icon className="mr-2 size-4 animate-spin" />
                            Uploading...
                        </>
                    ) : (
                        <>
                            <UploadIcon className="mr-2 size-4" />
                            Upload License
                        </>
                    )}
                </Button>
            </div>
        </div>
    );
};

export default LicenseUpload;
