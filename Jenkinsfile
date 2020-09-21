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
  
  try {
    if (pull_request) {
      checkout("$clone_url", target_branch)
      test()
      runSecretsScanner()
      sonarqube()
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
    cleanWs()
  }
}

def checkout(repo, branch) {
  stage('Checkout') {
    checkout([
      $class: 'GitSCM',
      branches: [[name: "master"]],
      doGenerateSubmoduleConfigurations: false,
      extensions: [[
        $class: 'CloneOption',
        noTags: false,
        reference: '',
        shallow: false
      ]],
      submoduleCfg: [],
      userRemoteConfigs: [[
        url: "https://github.com/cuartinm/angular-demo-app.git"
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

def sonarqube() {
  stage('SonarQube') {
    withSonarQubeEnv('SonarQube Server') {
      def scannerHome = tool 'sonar-scanner'
      sh "${scannerHome}/bin/sonar-scanner"
    }
    timeout(time: 5, unit: 'MINUTES') {
       def qg = waitForQualityGate()
        if (qg.status != 'OK') {
          error "Pipeline aborted due to quality gate failure: ${qg.status}"
        }
    }
  }
}