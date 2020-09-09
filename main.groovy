#!/user/bin/env groovy

node {
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

    printContributedVariables: true,
    printPostContent: true,

    regexpFilterText: '$ref',
    regexpFilterExpression: 'refs/heads/' + BRANCH_NAME
   ]
  ])
 ])

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