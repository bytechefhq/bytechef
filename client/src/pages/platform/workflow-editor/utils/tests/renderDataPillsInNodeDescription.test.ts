import {describe, expect, it} from 'vitest';

import renderDataPillsInNodeDescription from '../renderDataPillsInNodeDescription';

const getIconSource = (reference: string) => `/icons/${reference.split('.')[0]}.svg`;

describe('renderDataPillsInNodeDescription', () => {
    it('should render a data pill reference as a chip with the node icon', () => {
        const html = renderDataPillsInNodeDescription('<td>${awsS3_1}</td>', getIconSource);

        const container = document.createElement('div');

        container.innerHTML = html;

        const chip = container.querySelector('span');

        expect(chip).toHaveTextContent(/^awsS3_1$/);
        expect(chip?.querySelector('img')).toHaveAttribute('src', '/icons/awsS3_1.svg');
    });

    it('should render every reference in a value', () => {
        const html = renderDataPillsInNodeDescription('<td>${var_1} and ${awsS3_1.key}</td>', getIconSource);

        const container = document.createElement('div');

        container.innerHTML = html;

        expect(Array.from(container.querySelectorAll('span')).map((chip) => chip.textContent)).toEqual([
            'var_1',
            'awsS3_1.key',
        ]);
    });

    it('should leave text without references unchanged', () => {
        expect(renderDataPillsInNodeDescription('<td>OBJECT</td>', getIconSource)).toBe('<td>OBJECT</td>');
    });

    it('should strip unsafe markup from the description', () => {
        const html = renderDataPillsInNodeDescription(
            '<td onclick="alert(1)">${var_1}<script>alert(1)</script></td>',
            getIconSource
        );

        expect(html).not.toContain('script');
        expect(html).not.toContain('onclick');
        expect(html).toContain('var_1</span>');
    });

    it('should escape quotes in the icon source', () => {
        const html = renderDataPillsInNodeDescription('${var_1}', () => 'data:image/svg+xml,<svg a="b"/>');

        expect(html).toContain('src="data:image/svg+xml,<svg a=&quot;b&quot;/>"');
    });
});
