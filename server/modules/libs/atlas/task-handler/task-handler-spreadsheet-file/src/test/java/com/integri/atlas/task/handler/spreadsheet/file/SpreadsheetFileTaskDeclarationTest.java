/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.task.handler.spreadsheet.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

/**
 * @author Ivica Cardic
 */
public class SpreadsheetFileTaskDeclarationTest {

    private final ObjectMapper objectMapper = new ObjectMapper() {
        {
            setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    };

    @Test
    public void testSpreadsheetFileTaskSpecification() throws JsonProcessingException {
        JSONAssert.assertEquals(
            """
            {
                "description":"Reads and writes data from a spreadsheet file.",
                "displayName":"Spreadsheet File",
                "name":"spreadsheetFile",
                "properties":[
                    {
                        "defaultValue":"READ",
                        "description":"The operation to perform.",
                        "displayName":"Operation",
                        "name":"operation",
                        "required":true,
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"Read from file",
                                "value":"READ",
                                "description": "Reads data from a spreadsheet file."
                            },
                            {
                                "name":"Write to file",
                                "value":"WRITE",
                                 "description": "Writes the data to a spreadsheet file."
                            }
                        ]
                    },
                    {
                        "description":"The object property which contains a reference to the spreadsheet file to read from.",
                        "displayName":"File",
                        "displayOption":{
                            "show":{
                                "operation":["READ"]
                            }
                        },
                        "name":"fileEntry",
                        "required":true,
                        "type":"JSON"
                    },
                    {
                        "defaultValue":"CSV",
                        "description":"The format of the file to save the data.",
                        "displayName":"FileFormat",
                        "displayOption":{
                            "show":{
                                "operation":["WRITE"]
                            }
                        },
                        "name":"fileFormat",
                        "type":"SELECT",
                        "options":[
                            {
                                "name":"CSV",
                                "value":"CSV",
                                "description":"Comma-separated value"
                            },
                            {
                                "name":"XLS",
                                "value":"XLS",
                                "description":"Microsoft Excel"
                            },
                            {
                                "name":"XLSX",
                                "value":"XLSX",
                                "description":"Microsoft Excel"
                            }
                        ]
                    },
                    {
                        "description":"Object or array of objects to write to the file.",
                        "displayName":"Input",
                        "displayOption":{
                            "show":{
                                "operation":["WRITE"]
                            }
                        },
                        "name":"input",
                        "required":true,
                        "type":"JSON"
                    },
                    {
                        "displayName":"Options",
                        "name":"options",
                        "type":"COLLECTION",
                        "options":[
                        {
                                "defaultValue":",",
                                "description":"Delimiter to use when reading a csv file.",
                                "displayName":"Delimiter",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"],
                                        "fileFormat":["CSV"]
                                    }
                                },
                                "name":"delimiter",
                                "type":"STRING"
                            },
                            {
                                "defaultValue":"",
                                "description":"File name to set for binary data. By default, \\"spreadsheet.<fileFormat>\\" will be used.",
                                "displayName":"File Name",
                                "displayOption":{
                                    "show":{
                                        "operation":["WRITE"]
                                    }
                                },
                                "name":"fileName",
                                "type":"STRING"
                            },
                            {
                                "defaultValue":true,
                                "description":"The first row of the file contains the header names.",
                                "displayName":"Header Row",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"headerRow",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":false,
                                "description":"When reading from file the empty cells will be filled with an empty string.",
                                "displayName":"Include Empty Cells",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"includeEmptyCells",
                                "type":"BOOLEAN"
                            },
                            {
                                "description":"The amount of child elements to return in a page.",
                                "displayName":"Page Size",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"pageSize",
                                "type":"INTEGER"
                            },
                            {
                                "description":"The page number to get.",
                                "displayName":"Page Number",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"pageNumber",
                                "type":"INTEGER"
                            },
                            {
                                "defaultValue":false,
                                "description":"In some cases and file formats, it is necessary to read data specifically as string, otherwise some special characters are interpreted the wrong way.",
                                "displayName":"Read As String",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"]
                                    }
                                },
                                "name":"readAsString",
                                "type":"BOOLEAN"
                            },
                            {
                                "defaultValue":"Sheet",
                                "description":"The name of the sheet to read from in the spreadsheet. If not set, the first one gets chosen.",
                                "displayName":"Sheet Name",
                                "displayOption":{
                                    "show":{
                                        "operation":["READ"],
                                        "fileFormat":["XLS","XLSX"]
                                    }
                                },
                                "name":"sheetName","type":"STRING"
                            },
                            {
                                "defaultValue":"Sheet",
                                "description":"The name of the sheet to create in the spreadsheet.",
                                "displayName":"Sheet Name",
                                "displayOption":{
                                    "show":{
                                        "operation":["WRITE"],
                                        "fileFormat":["XLS","XLSX"]
                                    }
                                },
                                "name":"sheetName",
                                "type":"STRING"
                            }
                        ],
                        "placeholder":"Add Option"
                    }
                ],
                "version":1.0
            }
            """,
            (JSONObject) JSONParser.parseJSON(
                objectMapper.writeValueAsString(SpreadsheetFileTaskDeclaration.TASK_SPECIFICATION)
            ),
            true
        );
    }
}
