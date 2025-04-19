After creating you Snowflake account, you have to create SECURITY INTEGRATION.
https://docs.snowflake.com/en/sql-reference/sql/create-security-integration-oauth-snowflake
    1. Create SQL Worksheet
    2. Change <name> and <callback url> to fit your application.
           CREATE SECURITY INTEGRATION <name>
           TYPE = oauth
           ENABLED = true
           OAUTH_CLIENT = custom
           OAUTH_CLIENT_TYPE = 'CONFIDENTIAL'
           OAUTH_REDIRECT_URI = '<callback url>'
           OAUTH_ISSUE_REFRESH_TOKENS = TRUE
           OAUTH_ALLOW_NON_TLS_REDIRECT_URI = true
           OAUTH_REFRESH_TOKEN_VALIDITY = 86400;
    3. Run the SQL Worksheet.

To get your Client ID and Client Secret:
https://docs.snowflake.com/sql-reference/functions/system_show_oauth_client_secrets
    1. Create SQL Worksheet
    2. Change <name> to name of your security integration:
        select system$show_oauth_client_secrets( '<name>' )
    3. Run the SQL Worksheet
