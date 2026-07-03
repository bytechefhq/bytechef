import AskUserQuestionMessage, {
    AskUserQuestionDataI,
} from '@/shared/components/ai-chat/messages/AskUserQuestionMessage';
import CreateConnectionMessage, {
    CreateConnectionDataI,
} from '@/shared/components/ai-chat/messages/CreateConnectionMessage';
import RunErrorMessage, {RunErrorDataI} from '@/shared/components/ai-chat/messages/RunErrorMessage';
import SelectConnectionMessage, {
    SelectConnectionDataI,
} from '@/shared/components/ai-chat/messages/SelectConnectionMessage';
import SelectPropertyOptionMessage, {
    SelectPropertyOptionDataI,
} from '@/shared/components/ai-chat/messages/SelectPropertyOptionMessage';
import {DataMessagePartProps} from '@assistant-ui/react';

export const aiChatDataComponents = {
    'ask-user-question': (props: DataMessagePartProps<AskUserQuestionDataI>) => <AskUserQuestionMessage {...props} />,
    'create-connection': (props: DataMessagePartProps<CreateConnectionDataI>) => <CreateConnectionMessage {...props} />,
    'run-error': (props: DataMessagePartProps<RunErrorDataI>) => <RunErrorMessage {...props} />,
    'select-connection': (props: DataMessagePartProps<SelectConnectionDataI>) => <SelectConnectionMessage {...props} />,
    'select-property-option': (props: DataMessagePartProps<SelectPropertyOptionDataI>) => (
        <SelectPropertyOptionMessage {...props} />
    ),
};
