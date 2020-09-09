#!/user/bin/env groovy


withCredentials([string(credentialsId: 'GENERIC_WEBHOOK_TOKEN', variable: 'GENERIC_WEBHOOK_TOKEN')]) {
                
    properties([
        pipelineTriggers([
            [$class: 'GenericTrigger',
                genericVariables: [
                    [key: 'ref', value: '$.ref'],
                    [
                        key: 'before',
                        value: '$.before',
                        expressionType: 'JSONPath', //Optional, defaults to JSONPath
                        regexpFilter: '', //Optional, defaults to empty string
                        defaultValue: '' //Optional, defaults to empty string
                    ]
                ],
                genericRequestVariables: [
                    [key: 'requestWithNumber', regexpFilter: '[^0-9]'],
                    [key: 'requestWithString', regexpFilter: '']
                ],
                genericHeaderVariables: [
                    [key: 'headerWithNumber', regexpFilter: '[^0-9]'],
                    [key: 'headerWithString', regexpFilter: '']
                ],
                causeString: 'Triggered on $ref',
                token: "$GENERIC_WEBHOOK_TOKEN",
                printContributedVariables: true,
                printPostContent: true,
                regexpFilterText: '$ref',
                // regexpFilterExpression: 'refs/heads/' + BRANCH_NAME
            ]
        ])
    ])
}

node {
 stage("build") {
  sh '''
  echo Variables from shell:
  echo ref $ref
  echo before $before
  echo requestWithNumber $requestWithNumber
  echo requestWithString $requestWithString
  echo headerWithNumber $headerWithNumber
  echo headerWithString $headerWithString
  '''
 }
}