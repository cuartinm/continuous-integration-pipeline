#!/user/bin/env groovy

node {
  
  try {
    checkout("$clone_url", target_branch)
    test()
    build()
    // runSonarScanner()
  } catch(Exception e) {
    currentBuild.result = 'FAILURE'
    echo "Exception: ${e}"
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