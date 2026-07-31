import {DataMessagePartProps} from '@assistant-ui/react';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

import KnowledgeBaseCitationsMessage, {KnowledgeBaseCitationsDataI} from '../KnowledgeBaseCitationsMessage';

const toProps = (data: KnowledgeBaseCitationsDataI) =>
    ({data}) as unknown as DataMessagePartProps<KnowledgeBaseCitationsDataI>;

const DATA: KnowledgeBaseCitationsDataI = {
    hits: [
        {
            docId: '7',
            docTitle: 'Onboarding Guide',
            excerpt: 'Spring AI supports vector stores.',
            knowledgeBaseId: '42',
            knowledgeBaseName: 'Company Docs',
            score: 0.92,
        },
        {
            docId: '7',
            docTitle: 'Onboarding Guide',
            excerpt: 'Another chunk of the same document.',
            knowledgeBaseId: '42',
            knowledgeBaseName: 'Company Docs',
            score: 0.87,
        },
        {
            docId: '9',
            docTitle: 'Security Policy',
            excerpt: 'All access is least-privilege.',
            knowledgeBaseId: '42',
            knowledgeBaseName: 'Company Docs',
            score: 0.81,
        },
    ],
    kind: 'knowledge-base-citations',
};

describe('KnowledgeBaseCitationsMessage', () => {
    it('renders one chip per distinct document, linked to the knowledge base page', () => {
        render(
            <MemoryRouter>
                <KnowledgeBaseCitationsMessage {...toProps(DATA)} />
            </MemoryRouter>
        );

        expect(screen.getByText('Sources')).toBeInTheDocument();

        // Two chunks of doc 7 collapse into one chip.
        expect(screen.getAllByRole('link')).toHaveLength(2);
        expect(screen.getByRole('link', {name: /Onboarding Guide/})).toHaveAttribute(
            'href',
            '/automation/knowledge-bases/42'
        );
        expect(screen.getByRole('link', {name: /Security Policy/})).toBeInTheDocument();
    });

    it('falls back to the document id and a plain chip when title or knowledge base id are missing', () => {
        const data: KnowledgeBaseCitationsDataI = {
            hits: [{docId: '13', excerpt: 'Orphan excerpt.'}],
            kind: 'knowledge-base-citations',
        };

        render(
            <MemoryRouter>
                <KnowledgeBaseCitationsMessage {...toProps(data)} />
            </MemoryRouter>
        );

        expect(screen.getByText('Document 13')).toBeInTheDocument();
        expect(screen.queryByRole('link')).not.toBeInTheDocument();
    });

    it('renders nothing when no hit identifies a document', () => {
        const data: KnowledgeBaseCitationsDataI = {
            hits: [{excerpt: 'No identity at all.'}],
            kind: 'knowledge-base-citations',
        };

        const {container} = render(
            <MemoryRouter>
                <KnowledgeBaseCitationsMessage {...toProps(data)} />
            </MemoryRouter>
        );

        expect(container).toBeEmptyDOMElement();
    });
});
