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
        token: "$GENERIC_WEBHOOK_TOKEN",
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


def setGitHubStatus(context, state, description){
  withCredentials([string(credentialsId: 'GITHUB_ACCESS_TOKEN', variable: 'GITHUB_ACCESS_TOKEN')]) {

    def builder = new groovy.json.JsonBuilder()
    builder context: "$context", state: "$state", description: "$description", target_url: "${env.RUN_DISPLAY_URL}""
    try {
      def httpConn = new URL("$statuses_url").openConnection();
      httpConn.setRequestMethod("POST");
      httpConn.setRequestProperty("Authorization", "token $GITHUB_ACCESS_TOKEN")
      httpConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
      httpConn.setRequestProperty("Accept", "application/json");
      httpConn.setDoOutput(true);
      httpConn.getOutputStream().write(builder.toString().getBytes("UTF-8"));
      return httpConn.getResponseCode();
    } catch(Exception e){
      echo "Exception: ${e}"
    }           
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
      setGitHubStatus("build", "success", "build artifact")
    } catch(Exception e) {
      setGitHubStatus("build", "failure", "build artifact")
      echo "Exception: ${e}"
    }
  }
}

def test() {
  stage('Unit Tests') {
    try {
      def tests_command = sh(script: "npm test", returnStatus: true)
      setGitHubStatus("unit-tests", "success", "Run unit tests")
    } catch(Exception e) {
      setGitHubStatus("unit-tests", "failure", "Run unit tests")
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
      setGitHubStatus("git-secrets", "success", "Scan sources to precent adding secrets")
    } catch(Exception e) {
      setGitHubStatus("git-secrets", "failure", "Scan sources to precent adding secrets")
      echo "Exception: ${e}"
    }
  }
}