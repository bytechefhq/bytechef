import type {LinguiConfig} from '@lingui/conf';

const config: LinguiConfig = {
    locales: ['en', 'hr'],
    sourceLocale: 'en',
    catalogs: [
        {
            path: 'src/locales/{locale}/messages',
            include: ['src'],
        },
    ],
};

export default config;
