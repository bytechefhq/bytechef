/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.llm.constant;

/**
 * @author Ivica Cardic
 */
public enum Language {

    AF("af", "Afrikaans"),
    AR("ar", "Arabic"),
    HY("hy", "Armenian"),
    AZ("az", "Azerbaijani"),
    BE("be", "Belarusian"),
    BS("bs", "Bosnian"),
    BG("bg", "Bulgarian"),
    CA("ca", "Catalan"),
    ZH("zh", "Chinese"),
    HR("hr", "Croatian"),
    CS("cs", "Czech"),
    DA("da", "Danish"),
    NL("nl", "Dutch"),
    EL("el", "Greek"),
    ET("et", "Estonian"),
    EN("en", "English"),
    FI("fi", "Finnish"),
    FR("fr", "French"),
    GL("gl", "Galician"),
    DE("de", "German"),
    HE("he", "Hebrew"),
    HI("hi", "Hindi"),
    HU("hu", "Hungarian"),
    IS("is", "Icelandic"),
    ID("id", "Indonesian"),
    IT("it", "Italian"),
    JA("ja", "Japanese"),
    KK("kk", "Kazakh"),
    KN("kn", "Kannada"),
    KO("ko", "Korean"),
    LT("lt", "Lithuanian"),
    LV("lv", "Latvian"),
    MA("ma", "Marathi"),
    MK("mk", "Macedonian"),
    MR("mr", "Marathi"),
    MS("ms", "Malay"),
    NE("ne", "Nepali"),
    NO("no", "Norwegian"),
    FA("fa", "Persian"),
    PL("pl", "Polish"),
    PT("pt", "Portuguese"),
    RO("ro", "Romanian"),
    RU("ru", "Russian"),
    SK("sk", "Slovak"),
    SL("sl", "Slovenian"),
    SR("sr", "Serbian"),
    ES("es", "Spanish"),
    SV("sv", "Swedish"),
    SW("sw", "Swahili"),
    TA("ta", "Tamil"),
    TL("tl", "Tagalog"),
    TH("th", "Thai"),
    TR("tr", "Turkish"),
    UK("uk", "Ukrainian"),
    UR("ur", "Urdu"),
    VI("vi", "Vietnamese"),
    CY("cy", "Welsh");

    private final String code;
    private final String label;

    Language(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
