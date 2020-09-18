#!/user/bin/env groovy

library 'jenkins-library@master'

withCredentials([string(credentialsId: 'CI_GENERIC_WEBHOOK_TOKEN', variable: 'CI_GENERIC_WEBHOOK_TOKEN')]) {       
  properties([
    pipelineTriggers([
      [$class: 'GenericTrigger',
        genericVariables: [
          [key: 'clone_url', value: '$.repository.clone_url'],
          [key: 'action', value: '$.action'],
          [key: 'head_branch', value: '$.pull_request.head.ref'],
          [key: 'statuses_url', value: '$.pull_request.statuses_url'],
          [key: 'base_branch', value: '$.pull_request.base.ref'],
          [key: 'merged', value: '$.pull_request.merged']
        ],
        token: "$CI_GENERIC_WEBHOOK_TOKEN",
        causeString: "Triggered on $action pull request",
        printPostContent: false,
        printContributedVariables: false,
      ]
    ])
  ])
}

node {
  def error = null
  def target_branch = ""
  def pull_request = true

  switch("$action") {
    case "opened":
      target_branch = "$head_branch"
      break
    case "closed":
      target_branch = "$base_branch"
      break
    default:
      pull_request = false
      break
  }
  
  try {
    if (pull_request) {
      checkout("$clone_url", target_branch)
      test()
      runSecretsScanner()
      // runSonarScanner()
      if("$merged".toBoolean()) {
        build()
      }
      withCredentials([string(credentialsId: 'GITHUB_ACCESS_TOKEN', variable: 'GITHUB_ACCESS_TOKEN')]) {       
        def code = gitHubStatus(
                    context: "continuous-integration",
                    state: "success",
                    description: "your Job was successful. You can check your logs in the following link ->",
                    target_url: "${env.RUN_DISPLAY_URL}",
                    github_access_token: "$GITHUB_ACCESS_TOKEN",
                    statuses_url: "$statuses_url"
                    )
      }
    }
  } catch(Exception e) {
    currentBuild.result = 'FAILURE'
    echo "Exception: ${e}"
    withCredentials([string(credentialsId: 'GITHUB_ACCESS_TOKEN', variable: 'GITHUB_ACCESS_TOKEN')]) {       
      def code = gitHubStatus(
                  context: "continuous-integration",
                  state: "failure",
                  description: "Your job failed. Please check your logs in the following link ->",
                  target_url: "${env.RUN_DISPLAY_URL}",
                  github_access_token: "$GITHUB_ACCESS_TOKEN",
                  statuses_url: "$statuses_url"
                  )
    }
  } finally {
    notifyBuild(currentBuild.result)
    cleanWs()
  }
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
    try {
      def build_command = sh(script: "npm run-script build", returnStatus: true)
    } catch(Exception e) {
      echo "Exception: ${e}"
    }
  }
}

def test() {
  stage('Unit Tests') {
    try {
      def tests_command = sh(script: "npm test", returnStatus: true)
    } catch(Exception e) {
      echo "Exception: ${e}"
    }
  }
}

def notifyBuild(currentBuild = 'SUCCESS') {

}

def runSecretsScanner() {
  stage('Secrets Scan') {
    try {
      def secrets_command = sh(script: "git secrets --scan -r ./src", returnStatus: true)
    } catch(Exception e) {
      echo "Exception: ${e}"
    }
  }
}