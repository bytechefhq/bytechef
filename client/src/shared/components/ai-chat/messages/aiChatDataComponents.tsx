import ApprovalRequestMessage, {
    ApprovalRequestDataI,
} from '@/shared/components/ai-chat/messages/ApprovalRequestMessage';
import AskUserQuestionMessage, {
    AskUserQuestionDataI,
} from '@/shared/components/ai-chat/messages/AskUserQuestionMessage';
import CreateConnectionMessage, {
    CreateConnectionDataI,
} from '@/shared/components/ai-chat/messages/CreateConnectionMessage';
import KnowledgeBaseCitationsMessage, {
    KnowledgeBaseCitationsDataI,
} from '@/shared/components/ai-chat/messages/KnowledgeBaseCitationsMessage';
import RunErrorMessage, {RunErrorDataI} from '@/shared/components/ai-chat/messages/RunErrorMessage';
import SelectConnectionMessage, {
    SelectConnectionDataI,
} from '@/shared/components/ai-chat/messages/SelectConnectionMessage';
import SelectPropertyOptionMessage, {
    SelectPropertyOptionDataI,
} from '@/shared/components/ai-chat/messages/SelectPropertyOptionMessage';
import {DataMessagePartProps} from '@assistant-ui/react';

export const aiChatDataComponents = {
    'approval-request': (props: DataMessagePartProps<ApprovalRequestDataI>) => <ApprovalRequestMessage {...props} />,
    'ask-user-question': (props: DataMessagePartProps<AskUserQuestionDataI>) => <AskUserQuestionMessage {...props} />,
    'create-connection': (props: DataMessagePartProps<CreateConnectionDataI>) => <CreateConnectionMessage {...props} />,
    'knowledge-base-citations': (props: DataMessagePartProps<KnowledgeBaseCitationsDataI>) => (
        <KnowledgeBaseCitationsMessage {...props} />
    ),
    'run-error': (props: DataMessagePartProps<RunErrorDataI>) => <RunErrorMessage {...props} />,
    'select-connection': (props: DataMessagePartProps<SelectConnectionDataI>) => <SelectConnectionMessage {...props} />,
    'select-property-option': (props: DataMessagePartProps<SelectPropertyOptionDataI>) => (
        <SelectPropertyOptionMessage {...props} />
    ),
};
