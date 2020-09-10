curl -X POST \
-H "Accept: application/vnd.github.v3+json" \
-H "Authorization: token a66016b754277f91de594e053f2c0fcee9b22268" \
https://api.github.com/repos/cuartinm/angular-demo-app/statuses/8eb9d2e5d7ab69989c03f7460af2573b763ca99e \
-d '{"state":"pending", "context":"bavv-ci"}'