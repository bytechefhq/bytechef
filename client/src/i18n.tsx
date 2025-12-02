import {i18n} from '@lingui/core';
import {I18nProvider} from '@lingui/react';
import {ReactNode, useEffect, useState} from 'react';

const defaultLocale = 'en';

async function loadLocale(locale: string) {
    const {messages} = await import(`./locales/${locale}/messages.ts`);

    i18n.load(locale, messages);
    i18n.activate(locale);
}

loadLocale(defaultLocale);

function I18n({children}: {children: ReactNode}) {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const [locale, setLocale] = useState(defaultLocale);

    useEffect(() => {
        loadLocale(locale);
    }, [locale]);

    return <I18nProvider i18n={i18n}>{children}</I18nProvider>;
}

export default I18n;
