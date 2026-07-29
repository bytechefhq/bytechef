import hljs from 'highlight.js/lib/common';
import 'highlight.js/styles/github.css';
import {useMemo} from 'react';

export interface CodeDataI {
    language?: string;
    name?: string;
    source: string;
}

export default function CodeView({data}: {data: CodeDataI}) {
    const highlighted = useMemo(() => {
        if (data.language && hljs.getLanguage(data.language)) {
            return hljs.highlight(data.source, {language: data.language}).value;
        }

        // highlight.js escapes the source while tokenising, so the result is safe to inject.
        return hljs.highlightAuto(data.source).value;
    }, [data.language, data.source]);

    return (
        <div className="flex h-full flex-col overflow-hidden bg-surface-neutral-primary">
            {data.name ? (
                <div className="border-b border-stroke-neutral-secondary px-4 py-3 text-sm font-medium text-content-neutral-secondary">
                    {data.name}
                </div>
            ) : null}

            <pre className="m-0 flex-1 overflow-auto p-4 text-sm leading-relaxed">
                <code className="hljs bg-transparent p-0" dangerouslySetInnerHTML={{__html: highlighted}} />
            </pre>
        </div>
    );
}
