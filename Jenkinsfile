#!/user/bin/env groovy

withCredentials([string(credentialsId: 'GENERIC_WEBHOOK_TOKEN', variable: 'GENERIC_WEBHOOK_TOKEN')]) {       
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


def setGitHubStatus(context, state){
  withCredentials([string(credentialsId: 'GITHUB_ACCESS_TOKEN', variable: 'GITHUB_ACCESS_TOKEN')]) {

    def builder = new groovy.json.JsonBuilder()
    builder context: "$context", state: "$state"

    def httpConn = new URL("$statuses_url").openConnection();
    httpConn.setRequestMethod("POST");
    httpConn.setRequestProperty("Authorization", "token $GITHUB_ACCESS_TOKEN")
    httpConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
    httpConn.setRequestProperty("Accept", "application/json");
    httpConn.setDoOutput(true);
    httpConn.getOutputStream().write(builder.toString().getBytes("UTF-8"));
    return httpConn.getResponseCode();

    // sh """
    //   curl \
    //   -X POST \
    //   -H "Accept: application/vnd.github.v3+json" \
    //   -H "Authorization: token $GITHUB_ACCESS_TOKEN" \
    //   $statuses_url \
    //   -d '{"context":"$context", "state":"$state"}'
    // """
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
      setGitHubStatus("build", "pending")
      def build_command = sh(script: "npm run-script build", returnStatus: true)
      setGitHubStatus("build", "success")
    } catch(Exception e) {
      setGitHubStatus("build", "failure")
      echo "Exception: ${e}"
    }
  }
}

def test() {
  stage('Unit Tests') {
    try {
      setGitHubStatus("unit-tests", "pending")
      def tests_command = sh(script: "npm test", returnStatus: true)
      setGitHubStatus("unit-tests", "success")
    } catch(Exception e) {
      setGitHubStatus("unit-tests", "failure")
      echo "Exception: ${e}"
    }
  }
}

def notifyBuild(currentBuild = 'SUCCESS') {

}

def runSecretsScanner() {
  stage('Secrets Scan') {
    try {
      setGitHubStatus("git-secrets", "pending")
      def tests_command = sh(script: "git secrets --scan -r ./src", returnStatus: true)
      setGitHubStatus("git-secrets", "success")
    } catch(Exception e) {
      setGitHubStatus("git-secrets", "failure")
      echo "Exception: ${e}"
    }
  }
}