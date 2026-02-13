import {useCallback, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import {getCurrentTimestamp} from '../../utils/approval-task-utils';

import type {ApprovalTaskCommentI} from '../../types/types';

export interface UseApprovalTaskCommentsReturnI {
    canSubmit: boolean;
    handleCommentChange: (value: string) => void;
    handleSubmitComment: () => void;
    newComment: string;
}

export function useApprovalTaskComments(): UseApprovalTaskCommentsReturnI {
    const [newComment, setNewComment] = useState('');

    const {addComment, approvalTasks, selectedApprovalTaskId} = useApprovalTasksStore(
        useShallow((state) => ({
            addComment: state.addComment,
            approvalTasks: state.approvalTasks,
            selectedApprovalTaskId: state.selectedApprovalTaskId,
        }))
    );

    const generateCommentId = useCallback(() => {
        const allComments = approvalTasks.flatMap((approvalTask) => approvalTask.comments);
        const maxId = Math.max(...allComments.map((comment) => Number.parseInt(comment.id.replace('c', ''))), 0);

        return `c${maxId + 1}`;
    }, [approvalTasks]);

    const handleCommentChange = useCallback((value: string) => {
        setNewComment(value);
    }, []);

    const handleSubmitComment = useCallback(() => {
        if (!newComment.trim() || !selectedApprovalTaskId) {
            return;
        }

        const comment: ApprovalTaskCommentI = {
            author: 'Current User',
            content: newComment.trim(),
            id: generateCommentId(),
            timestamp: getCurrentTimestamp(),
        };

        addComment(selectedApprovalTaskId, comment);

        setNewComment('');
    }, [newComment, selectedApprovalTaskId, generateCommentId, addComment]);

    return {
        canSubmit: newComment.trim().length > 0,
        handleCommentChange,
        handleSubmitComment,
        newComment,
    };
}
