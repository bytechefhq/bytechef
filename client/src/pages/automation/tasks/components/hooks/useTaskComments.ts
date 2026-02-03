import {useCallback, useState} from 'react';

import {useTasksStore} from '../../stores/useTasksStore';
import {getCurrentTimestamp} from '../../utils/task-utils';

import type {TaskCommentI} from '../../types/types';

export interface UseTaskCommentsReturnI {
    canSubmit: boolean;
    handleCommentChange: (value: string) => void;
    handleSubmitComment: () => void;
    newComment: string;
}

export function useTaskComments(): UseTaskCommentsReturnI {
    const tasks = useTasksStore((state) => state.tasks);
    const selectedTaskId = useTasksStore((state) => state.selectedTaskId);
    const storeAddComment = useTasksStore((state) => state.addComment);

    const [newComment, setNewComment] = useState('');

    const generateCommentId = useCallback(() => {
        const allComments = tasks.flatMap((task) => task.comments);
        const maxId = Math.max(...allComments.map((comment) => Number.parseInt(comment.id.replace('c', ''))), 0);

        return `c${maxId + 1}`;
    }, [tasks]);

    const handleCommentChange = useCallback((value: string) => {
        setNewComment(value);
    }, []);

    const handleSubmitComment = useCallback(() => {
        if (!newComment.trim() || !selectedTaskId) {
            return;
        }

        const comment: TaskCommentI = {
            author: 'Current User',
            content: newComment.trim(),
            id: generateCommentId(),
            timestamp: getCurrentTimestamp(),
        };

        storeAddComment(selectedTaskId, comment);

        setNewComment('');
    }, [newComment, selectedTaskId, generateCommentId, storeAddComment]);

    return {
        canSubmit: newComment.trim().length > 0,
        handleCommentChange,
        handleSubmitComment,
        newComment,
    };
}
