import {useCallback, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

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
    const [newComment, setNewComment] = useState('');

    const {addComment, selectedTaskId, tasks} = useTasksStore(
        useShallow((state) => ({
            addComment: state.addComment,
            selectedTaskId: state.selectedTaskId,
            tasks: state.tasks,
        }))
    );

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

        addComment(selectedTaskId, comment);

        setNewComment('');
    }, [newComment, selectedTaskId, generateCommentId, addComment]);

    return {
        canSubmit: newComment.trim().length > 0,
        handleCommentChange,
        handleSubmitComment,
        newComment,
    };
}
