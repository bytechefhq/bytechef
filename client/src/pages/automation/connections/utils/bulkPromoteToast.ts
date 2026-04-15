import {BulkPromoteResult} from '@/shared/middleware/graphql';

export interface BulkPromoteToastMessageI {
    message: string;
    type: 'error' | 'info';
}

/**
 * Build the toast message for a bulk-promote-all-private-to-workspace response.
 *
 * Surfacing `skipped` separately from `failed` matters because a benign race (another admin already
 * promoted the same row) must not read as a failure in the admin toast. A summary like "Promoted 12,
 * 2 skipped (already at target)" is reassuring; "failed 2" for the same scenario would not be.
 */
export const buildBulkPromoteToast = (result: BulkPromoteResult): BulkPromoteToastMessageI => {
    const skippedSuffix = result.skipped > 0 ? `, ${result.skipped} skipped (already at target)` : '';

    if (result.failed > 0) {
        const sample = result.failures
            .slice(0, 3)
            .map((failure) => `#${failure.connectionId} (${failure.message})`)
            .join(', ');

        const overflow = result.failures.length > 3 ? ` (+${result.failures.length - 3} more)` : '';

        return {
            message: `Promoted ${result.promoted}${skippedSuffix}, failed ${result.failed}: ${sample}${overflow}`,
            type: 'error',
        };
    }

    return {
        message: `Promoted ${result.promoted} private connection(s) to workspace${skippedSuffix}.`,
        type: 'info',
    };
};
