#!/user/bin/env groovy

node {
  
  try {
    checkout()
    test()
    sonarqube()
    build()
  } catch(Exception e) {
    currentBuild.result = 'FAILURE'
    echo "Exception: ${e}"
  } finally {
    cleanWs()
  }
}

def checkout() {
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