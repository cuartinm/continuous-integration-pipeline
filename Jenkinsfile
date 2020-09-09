#!/user/bin/env groovy


withCredentials([string(credentialsId: 'GENERIC_WEBHOOK_TOKEN', variable: 'GENERIC_WEBHOOK_TOKEN')]) {       
  properties([
      pipelineTriggers([
          [$class: 'GenericTrigger',
              genericVariables: [
                  [key: 'clone_url', value: '$.repository.clone_url'],
                  [key: 'action', value: '$.action'],
                  [key: 'head_branch', value: '$.pull_request.head.ref'],
                  [key: 'base_branch', value: '$$.pull_request.base.ref']
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

def checkout(repo, branch) {
  stage('Checkout') {
    checkout([
      $class: 'GitSCM',
      branches: [[name: branch]],
      doGenerateSubmoduleConfigurations: false,
      extensions: [[
        $class: 'CloneOption',
        noTags: false,
        reference: '',
        shallow: false
      ]],
      submoduleCfg: [],
      userRemoteConfigs: [[
        url: repo
      ]]
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
  def action = "$action"
  try {
    if (action=="opened" || action=="closed") {
      checkout("$clone_url", "$head_branch")
      test()
      runSecretsScanner()
      if(env.BRANCH_NAME=="master" || env.BRANCH_NAME=="develop") {
        build()
        // runSonarScanner()
      }
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