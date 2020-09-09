#!/user/bin/env groovy


withCredentials([string(credentialsId: 'GENERIC_WEBHOOK_TOKEN', variable: 'GENERIC_WEBHOOK_TOKEN')]) {
                
    properties([
        pipelineTriggers([
            [$class: 'GenericTrigger',
                genericVariables: [
                    [key: 'ref', value: '$.ref'],
                    [key: 'before', value: '$.before'],
                    [key: 'clone_url', value: '$.repository.clone_url'],
                    [key: 'pull_request', value: '$.pull_request']
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

def checkout() {
  stage('Checkout') {
        checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            extensions: scm.extensions + [[$class: 'LocalBranch'], [$class: 'WipeWorkspace']],
            userRemoteConfigs: [[url: "$clone_url"]],
            doGenerateSubmoduleConfigurations: false
        ])
    sh "npm i ."
  }
}

def build() {
  stage('Build Artifacts') {
    sh "npm run-script build"
  }
}

def test() {
  stage('Unit Tests') {
    sh "npm test"
  }
}

def notifyBuild(currentBuild = 'SUCCESS') {

}

def runSecretsScanner() {
  stage('Secrets Scan') {
    sh returnStdout: true, script: 'git secrets --scan -r ./src'
  }
}

node {
  def error = null
  try {
    checkout()
    test()
    runSecretsScanner()
    if(env.BRANCH_NAME=="master" || env.BRANCH_NAME=="develop") {
        build()
        // runSonarScanner()

    }
  } catch(caughtError) {
    currentBuild.result = 'FAILURE'
    error = caughtError
  } finally {
    notifyBuild(currentBuild.result)
    if (error) {
      throw error
    }
    cleanWs()
  }
}