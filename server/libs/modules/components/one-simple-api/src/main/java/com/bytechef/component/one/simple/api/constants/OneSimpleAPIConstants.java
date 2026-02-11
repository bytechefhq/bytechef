/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.one.simple.api.constants;

import static com.bytechef.component.definition.ComponentDsl.option;

import com.bytechef.component.definition.Option;
import java.util.List;

/**
 * @author Luka Ljubic
 * @author Monika Kušter
 */
public class OneSimpleAPIConstants {

    public static final String CUSTOM_CSS = "custom_css";
    public static final String CUSTOM_SIZE = "custom_size";
    public static final String DESCRIPTION = "description";
    public static final String ELAPSED = "elapsed";
    public static final String FORCE_REFRESH = "force";
    public static final String FROM_CURRENCY = "from_currency";
    public static final String FROM_VALUE = "from_value";
    public static final String FULL_PAGE = "fullpage";
    public static final String HTML = "html";
    public static final String HEIGHT = "height";
    public static final String SCREEN_SIZE = "screen";
    public static final String SOURCE = "source";
    public static final String TITLE = "title";
    public static final String TO_CURRENCY = "to_currency";
    public static final String TOKEN = "token";
    public static final String TRANSPARENT_BACKGROUND = "background";
    public static final String URL = "url";
    public static final String WAIT = "wait";
    public static final String WIDTH = "width";

    public static final List<Option<String>> SCREEN_SIZE_OPTIONS = List.of(
        option("Default (1920x1080)", "default"),
        option("Phone (375x667)", "phone"),
        option("Phone Landscape (667x375)", "landscape-phone"),
        option("Tablet (768x1024)", "tablet"),
        option("Tablet Landscape (1024x768)", "landscape-tablet"),
        option("Retina (2880x1800)", "retina"),
        option("4K (3840x2160)", "4k"),
        option("8K (7680x4320)", "8k"),
        option("Custom Size", CUSTOM_SIZE));

    public static final List<Option<String>> CURRENCY_OPTIONS = List.of(
        option("UAE Dirham", "AED"),
        option("Afghan Afghani", "AFN"),
        option("Albanian Lek", "ALL"),
        option("Armenian Dram", "AMD"),
        option("Netherlands Antillean Guilder", "ANG"),
        option("Angolan Kwanza", "AOA"),
        option("Argentine Peso", "ARS"),
        option("Australian Dollar", "AUD"),
        option("Aruban Florin", "AWG"),
        option("Azerbaijani Manat", "AZN"),
        option("Bosnia and Herzegovina Mark", "BAM"),
        option("Barbados Dollar", "BBD"),
        option("Bangladeshi Taka", "BDT"),
        option("Bulgarian Lev", "BGN"),
        option("Bahraini Dinar", "BHD"),
        option("Burundian Franc", "BIF"),
        option("Bermudian Dollar", "BMD"),
        option("Brunei Dollar", "BND"),
        option("Bolivian Boliviano", "BOB"),
        option("Brazilian Real", "BRL"),
        option("Bahamian Dollar", "BSD"),
        option("Bhutanese Ngultrum", "BTN"),
        option("Botswana Pula", "BWP"),
        option("Belarusian Ruble", "BYN"),
        option("Belize Dollar", "BZD"),
        option("Canadian Dollar", "CAD"),
        option("Congolese Franc", "CDF"),
        option("Swiss Franc", "CHF"),
        option("Chilean Peso", "CLP"),
        option("Chinese Renminbi", "CNY"),
        option("Colombian Peso", "COP"),
        option("Costa Rican Colon", "CRC"),
        option("Cuban Convertible Peso", "CUC"),
        option("Cuban Peso", "CUP"),
        option("Cape Verdean Escudo", "CVE"),
        option("Czech Koruna", "CZK"),
        option("Djiboutian Franc", "DJF"),
        option("Danish Krone", "DKK"),
        option("Dominican Peso", "DOP"),
        option("Algerian Dinar", "DZD"),
        option("Egyptian Pound", "EGP"),
        option("Eritrean Nakfa", "ERN"),
        option("Ethiopian Birr", "ETB"),
        option("Euro", "EUR"),
        option("Fiji Dollar", "FJD"),
        option("Falkland Islands Pound", "FKP"),
        option("Faroese Króna", "FOK"),
        option("Pound Sterling", "GBP"),
        option("Georgian Lari", "GEL"),
        option("Guernsey Pound", "GGP"),
        option("Ghanaian Cedi", "GHS"),
        option("Gibraltar Pound", "GIP"),
        option("Gambian Dalasi", "GMD"),
        option("Guinean Franc", "GNF"),
        option("Guatemalan Quetzal", "GTQ"),
        option("Guyanese Dollar", "GYD"),
        option("Hong Kong Dollar", "HKD"),
        option("Honduran Lempira", "HNL"),
        option("Croatin Kuna", "HRK"),
        option("Haitian Gourde", "HTG"),
        option("Hungarian Forint", "HUF"),
        option("Indonesian Rupiah", "IDR"),
        option("Israeli New Shekel", "ILS"),
        option("Manx Pound", "IMP"),
        option("Indian Rupee", "INR"),
        option("Iraqi Dinar", "IQD"),
        option("Iranian Rial", "IRR"),
        option("Icelandic Króna", "ISK"),
        option("Jamaican Dollar", "JMD"),
        option("Jordanian Dinar", "JOD"),
        option("Japanese Yen", "JPY"),
        option("Kenyan Shilling", "KES"),
        option("Kyrgyzstani Som", "KGS"),
        option("Cambodian Riel", "KHR"),
        option("Kiribati Dollar", "KID"),
        option("Comorian Franc", "KMF"),
        option("South Korean Won", "KRW"),
        option("Kuwaiti Dinar", "KWD"),
        option("Cayman Islands Dollar", "KYD"),
        option("Kazakhstani Tenge", "KZT"),
        option("Lao Kip", "LAK"),
        option("Lebanese Pound", "LBP"),
        option("Sri Lanka Rupee", "LKR"),
        option("Liberian Dollar", "LRD"),
        option("Lesotho Loti", "LSL"),
        option("Libyan Dinar", "LYD"),
        option("Moroccan Dirham", "MAD"),
        option("Moldovan Leu", "MDL"),
        option("Malagasy Ariary", "MGA"),
        option("Macedonian Denar", "MKD"),
        option("Burmese Kyat", "MMK"),
        option("Mongolian Tögrög", "MNT"),
        option("Macanese Pataca", "MOP"),
        option("Mauritanian Ouguiya", "MRU"),
        option("Mauritian Rupee", "MUR"),
        option("Maldivian Rufiyaa", "MVR"),
        option("Malawian Kwacha", "MWK"),
        option("Mexican Peso", "MXN"),
        option("Malaysian Ringgit", "MYR"),
        option("Mozambican Metical", "MZN"),
        option("Namibian Dollar", "NAD"),
        option("Nigerian Naira", "NGN"),
        option("Nicaraguan Córdoba", "NIO"),
        option("Norwegian Krone", "NOK"),
        option("Nepalese Rupee", "NPR"),
        option("New Zealand Dollar", "NZD"),
        option("Omani Rial", "OMR"),
        option("Panamanian Balboa", "PAB"),
        option("Peruvian Sol", "PEN"),
        option("Papua New Guinean Kina", "PGK"),
        option("Philippine Peso", "PHP"),
        option("Pakistani Rupee", "PKR"),
        option("Polish Zloty", "PLN"),
        option("Paraguayan Guaraní", "PYG"),
        option("Qatari Riyal", "QAR"),
        option("Romanian Leu", "RON"),
        option("Serbian Dinar", "RSD"),
        option("Russian Ruble", "RUB"),
        option("Rwandan Franc", "RWF"),
        option("Saudi Riyal", "SAR"),
        option("Solomon Islands Dollar", "SBD"),
        option("Seychellois Rupee", "SCR"),
        option("Sudanese Pound", "SDG"),
        option("Swedish Krona", "SEK"),
        option("Singapore Dollar", "SGD"),
        option("Saint Helena Pound", "SHP"),
        option("Sierra Leonean Leone", "SLL"),
        option("Somali Shilling", "SOS"),
        option("Surinamese Dollar", "SRD"),
        option("South Sudanese Pound", "SSP"),
        option("São Tomé and Príncipe Dobra", "STN"),
        option("Syrian Pound", "SYP"),
        option("Eswatini Lilangeni", "SZL"),
        option("Thai Baht", "THB"),
        option("Tajikistani Somoni", "TJS"),
        option("Turkmenistan Manat", "TMT"),
        option("Tunisian Dinar", "TND"),
        option("Tongan Paʻanga", "TOP"),
        option("Turkish Lira", "TRY"),
        option("Trinidad and Tobago Dollar", "TTD"),
        option("Tuvaluan Dollar", "TVD"),
        option("New Taiwan Dollar", "TWD"),
        option("Tanzanian Shilling", "TZS"),
        option("Ukrainian Hryvnia", "UAH"),
        option("Ugandan Shilling", "UGX"),
        option("United States Dollar", "USD"),
        option("Uruguayan Peso", "UYU"),
        option("Uzbekistani So'm", "UZS"),
        option("Venezuelan Bolívar Soberano", "VES"),
        option("Vietnamese Đồng", "VND"),
        option("Vanuatu Vatu", "VUV"),
        option("Samoan Tālā", "WST"),
        option("Central African CFA Franc", "XAF"),
        option("East Caribbean Dollar", "XCD"),
        option("Special Drawing Rights", "XDR"),
        option("West African CFA Franc", "XOF"),
        option("CFP Franc", "XPF"),
        option("Yemeni Rial", "YER"),
        option("South African Rand", "ZAR"),
        option("Zambian Kwacha", "ZMW"));

    private OneSimpleAPIConstants() {
    }
}
