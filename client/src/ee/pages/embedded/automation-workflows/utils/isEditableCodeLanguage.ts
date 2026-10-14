import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';

export default function isEditableCodeLanguage(language?: CodeWorkflowLanguage | null): boolean {
    return language === CodeWorkflowLanguage.Javascript || language === CodeWorkflowLanguage.Python;
}
