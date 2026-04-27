import ApprovalForm from '@/shared/components/approval-form/ApprovalForm';
import {useParams} from 'react-router-dom';

export default function ResumeForm() {
    const {id} = useParams<{id: string}>();

    return (
        <div className="flex h-full overflow-auto">
            <div className="mx-auto mt-6 w-full max-w-2xl p-6">
                <ApprovalForm id={id} setDocumentTitle />

                <div className="mt-8 space-x-1 border-t pt-4 text-center text-xs text-muted-foreground">
                    <span>Powered by</span>

                    <a href="https://www.bytechef.io" rel="noopener noreferrer" target="_blank">
                        ByteChef
                    </a>
                </div>
            </div>
        </div>
    );
}
