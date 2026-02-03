import Button from '@/components/Button/Button';
import {Avatar, AvatarFallback} from '@/components/ui/avatar';
import {Card, CardContent} from '@/components/ui/card';
import {Textarea} from '@/components/ui/textarea';
import {MessageSquareIcon, SendIcon} from 'lucide-react';

import {formatTimestamp, getInitials} from '../utils/task-utils';
import {useTaskComments} from './hooks/useTaskComments';

import type {TaskCommentI} from '../types/types';

interface TaskCommentsProps {
    comments: TaskCommentI[];
}

export default function TaskComments({comments}: TaskCommentsProps) {
    const {canSubmit, handleCommentChange, handleSubmitComment, newComment} = useTaskComments();

    return (
        <>
            <h3 className="mb-4 text-lg font-medium text-foreground">Comments ({comments.length})</h3>

            <div className="space-y-4">
                {comments.length > 0 ? (
                    comments.map((comment) => {
                        const authorInitials = getInitials(comment.author);
                        const formattedTimestamp = formatTimestamp(comment.timestamp);

                        return (
                            <Card key={comment.id}>
                                <CardContent className="flex items-start gap-3 p-4">
                                    <Avatar className="size-8">
                                        <AvatarFallback className="text-xs">{authorInitials}</AvatarFallback>
                                    </Avatar>

                                    <div className="flex-1">
                                        <div className="mb-1 flex items-center gap-2">
                                            <span className="text-sm font-medium">{comment.author}</span>

                                            <span className="text-xs text-muted-foreground">{formattedTimestamp}</span>
                                        </div>

                                        <p className="text-sm text-muted-foreground">{comment.content}</p>
                                    </div>
                                </CardContent>
                            </Card>
                        );
                    })
                ) : (
                    <div className="py-8 text-center">
                        <MessageSquareIcon className="mx-auto mb-2 size-8 text-muted-foreground" />

                        <p className="text-sm font-medium text-foreground">No comments yet</p>

                        <p className="text-xs text-muted-foreground">Be the first to add a comment</p>
                    </div>
                )}

                <Card>
                    <CardContent className="flex items-start gap-3 p-4">
                        <Avatar className="size-8">
                            <AvatarFallback className="text-xs">CU</AvatarFallback>
                        </Avatar>

                        <div className="flex-1 space-y-2">
                            <Textarea
                                className="min-h-[80px]"
                                onChange={(event) => handleCommentChange(event.target.value)}
                                placeholder="Add a comment..."
                                value={newComment}
                            />

                            <div className="flex justify-end">
                                <Button
                                    className="flex items-center gap-2"
                                    disabled={!canSubmit}
                                    onClick={handleSubmitComment}
                                    size="sm"
                                >
                                    <SendIcon className="size-3" />
                                    Add Comment
                                </Button>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </>
    );
}
