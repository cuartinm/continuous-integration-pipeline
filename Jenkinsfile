#!/user/bin/env groovy

properties([
  pipelineTriggers([
    githubPush(),
  ])
])

def checkout(repo, credentials) {
  stage('Checkout') {
    checkout scm
    sh "npm i ."
  }
}

def build(cmd) {
  stage('Build Artifacts') {
    sh "npm run-script build"
  }
}

def test(cmd) {
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

//Main Node Code

node {
  def error = null
  try {
    checkout(params.sourceRepo, params.CredentialsId)
    test(params.gradleTestCmd)
    runSecretsScanner()
    if(env.BRANCH_NAME=="master" || env.BRANCH_NAME=="develop") {
        build(params.gradleBuildCmd)
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
