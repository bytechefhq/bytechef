import {Extension} from '@tiptap/core';

export const MentionStorage = Extension.create({
    addStorage() {
        return {
            dataPills: [],
        };
    },
    name: 'MentionStorage',
});
