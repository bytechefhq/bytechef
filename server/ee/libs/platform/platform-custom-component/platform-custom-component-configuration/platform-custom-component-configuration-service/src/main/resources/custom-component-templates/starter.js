({
    name: "__NAME__",
    title: "__NAME__",
    version: 1,
    description: "A custom component.",
    // connection: {
    //     baseUri: "https://api.example.com",
    //     authorizations: [
    //         {type: "BEARER_TOKEN"}
    //     ],
    //     properties: [
    //         {name: "accessToken", type: "STRING", label: "Access Token", required: true}
    //     ]
    // },
    actions: [
        {
            name: "myAction",
            title: "My Action",
            description: "An example action.",
            properties: [
                {name: "input", type: "STRING", label: "Input", required: false}
            ],
            perform: function (inputParameters, connectionParameters, context) {
                // context.log("info", "myAction invoked");
                //
                // const response = context.http({
                //     method: "GET",
                //     url: "https://api.example.com/items",
                //     headers: {"Authorization": "Bearer " + connectionParameters.accessToken}
                // });
                //
                // return response.body;

                return inputParameters;
            }
        }
    ]
})
