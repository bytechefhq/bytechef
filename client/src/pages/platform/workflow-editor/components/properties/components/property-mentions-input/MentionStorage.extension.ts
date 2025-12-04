import {Extension} from '@tiptap/core';

import type {DataPillType} from '@/shared/types';

declare module '@tiptap/core' {
    // eslint-disable-next-line  @typescript-eslint/naming-convention
    interface Storage {
        MentionStorage: {
            dataPills: DataPillType[];
        };
    }
}

export const MentionStorage = Extension.create({
    addStorage() {
        return {
            dataPills: [],
        };
    },
    name: 'MentionStorage',
});
