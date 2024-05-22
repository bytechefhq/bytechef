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

package com.bytechef.google.commons;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.drive.Drive;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.sheets.v4.Sheets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GoogleServicesTest {

    private static final Parameters mockedParameters = mock(Parameters.class);

    @BeforeAll
    static void beforeAll() {
        when(mockedParameters.getRequiredString(ACCESS_TOKEN))
            .thenReturn("accessToken");
    }

    @Test
    void testGetCalendar() {
        Calendar calendar = GoogleServices.getCalendar(mockedParameters);

        assertEquals("Google Calendar Component", calendar.getApplicationName());
    }

    @Test
    void testGetPeopleService() {
        PeopleService peopleService = GoogleServices.getPeopleService(mockedParameters);

        assertEquals("Google People Component", peopleService.getApplicationName());
    }

    @Test
    void testGetDocs() {
        Docs docs = GoogleServices.getDocs(mockedParameters);

        assertEquals("Google Docs Component", docs.getApplicationName());
    }

    @Test
    void testGetDrive() {
        Drive drive = GoogleServices.getDrive(mockedParameters);

        assertEquals("Google Drive Component", drive.getApplicationName());
    }

    @Test
    void getMail() {
        Gmail gmail = GoogleServices.getMail(mockedParameters);

        assertEquals("Google Mail Component", gmail.getApplicationName());
    }

    @Test
    void getSheets() throws Exception {
        Sheets sheets = GoogleServices.getSheets(mockedParameters);

        assertEquals("Google Sheets Component", sheets.getApplicationName());
    }
}
