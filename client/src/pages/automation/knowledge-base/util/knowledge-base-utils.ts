export const STATUS_UPLOADED = 0;
export const STATUS_PROCESSING = 1;
export const STATUS_READY = 2;
export const STATUS_ERROR = 3;

export const getStatusLabel = (status: number): string => {
    switch (status) {
        case STATUS_UPLOADED:
            return 'Uploaded';
        case STATUS_PROCESSING:
            return 'Processing';
        case STATUS_READY:
            return 'Ready';
        case STATUS_ERROR:
            return 'Error';
        default:
            return 'Unknown';
    }
};
