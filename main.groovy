
def getJsonWebToken(privateKey) {
    try {
        privateCrtKey = getRSAPrivateKey(privateKey)
        time = accessTime()
        def jsonWebToken = Jwts.builder()
        .setSubject("RS256")
        .signWith(RS256, privateCrtKey)
        .setExpiration(time['expirationTime'])
        .setIssuedAt(time['iat'])
        .setIssuer(APP_ID)
        .compact()
        return jsonWebToken
    } catch(Exception e){
        echo "Exception: ${e}"
        error "Failed to create a JWT"
    }
}

def getToken(jsonWebToken) {
    try {
        def httpConn = new URL("https://api.github.com/app/installations/${INSTALLATION_ID}/access_tokens").openConnection();
        httpConn.setRequestProperty( 'Authorization', "Bearer ${jsonWebToken}" )
        httpConn.setRequestProperty( 'Accept', 'application/vnd.github.machine-man-preview+json' )
        httpConn.setRequestMethod("POST");
        def responseText = httpConn.getInputStream().getText()
        def slurper = new JsonSlurper()
        def resultMap = slurper.parseText(responseText)
        def token = resultMap["token"]        
        return token
    } catch(Exception e){
        echo "Exception: ${e}"
        error "Failed to get a token"
    }
}

def getPreviousCheckNameRunID(repository, commitID, token, checkName) {
    try {
        def httpConn = new URL("https://api.github.com/repos/${ORGANIZATION_NAME}/${repository}/commits/${commitID}/check-runs").openConnection();
        httpConn.setDoOutput(true)
        httpConn.setRequestProperty( 'Authorization', "token ${token}" )
        httpConn.setRequestProperty( 'Accept', 'application/vnd.github.antiope-preview+json' )
        checkRuns = httpConn.getInputStream().getText();
        def slurperCheckRun = new JsonSlurper()
        def resultMapCheckRun = slurperCheckRun.parseText(checkRuns)
        def check_run_id = resultMapCheckRun.check_runs
                      .find { it.name == checkName }
                      .id
        return check_run_id
    } catch(Exception e){
        error 'Failed to retrieve the check id'
    }           

    
}

def validateAuth(jsonWebToken) {
    try {
        def httpConn = new URL("https://api.github.com/app").openConnection();
        httpConn.setRequestProperty( 'Authorization', "Bearer ${jsonWebToken}" )
        httpConn.setRequestProperty( 'Accept', 'application/vnd.github.machine-man-preview+json' )
        return httpConn.getResponseCode();
    } catch(Exception e){
        echo "Exception: ${e}"
        error "Authentication request failed"
    }           
}

def setCheckName(repository, checkName, status, previousDay, requestMethod, commitID=null, check_run_id=null) {
    try {
        def jsonCheckRun = new groovy.json.JsonBuilder()
        updateCheckRun = ["name":"${checkName}", "status": "in_progress", "conclusion":"${status}", "completed_at": "${previousDay}"]
        def url = "https://api.github.com/repos/${ORGANIZATION_NAME}/${repository}/check-runs"

        if (requestMethod == "POST") {
            updateCheckRun["head_sha"] = "${commitID}"
        } else {
            url += "/${check_run_id}"
        }

        // Cast map to json
        jsonCheckRun updateCheckRun

        def httpConn = new URL(url).openConnection();
        setRequestMethod(httpConn, requestMethod);
        httpConn.setDoOutput(true)
        httpConn.setRequestProperty( 'Authorization', "token ${token}" )
        httpConn.setRequestProperty( 'Accept', 'application/vnd.github.antiope-preview+json' )
        httpConn.getOutputStream().write(jsonCheckRun.toString().getBytes("UTF-8"));
        return httpConn.getResponseCode();
    } catch(Exception e){
        echo "Exception: ${e}"
        error "Failed to create a check run"
    }   
}

def buildGithubCheck(repository, commitID, privateKey, status, checkName) {
    def currentTime = new Date().format("yyyy-MM-dd'T'HH:mm:ss'Z'")
    def checkName_run_id
  
    jsonWebToken = getJsonWebToken(privateKey)
    getStatusCode = validateAuth(jsonWebToken)
    if (!(getStatusCode in [200,201])) {
        error "Authentication request failed, status code: ${getStatusCode}"
    }
    token = getToken(jsonWebToken)
  
    try {
        checkName_run_id = getPreviousCheckNameRunID(repository, commitID, token, checkName)
    } catch(Exception e) {
        echo "Exception: ${e}"
        echo "Check name does not exist"
    }

    if (checkName_run_id) {
        getStatusCode = setCheckName(repository, checkName, status, currentTime, "PATCH", commitID, checkName_run_id)
    } else {
        getStatusCode = setCheckName(repository, checkName, status, previousDay, "POST", commitID)
    }
    if (!(getStatusCode in [200,201])) {
        error "Failed to create a check run, status code: ${getStatusCode}"
    }
}

def check_runs = new buildGithubCheck()


pipeline {
    agent {
        kubernetes {
            label "GitHub-Check-Runs-${UUID.randomUUID().toString().substring(0,8)}"
            defaultContainer 'jnlp'
        }
    }
    stages {
        stage('Checkout') {
            steps {
                script {
                    git (credentialsId: 'github-token',
                    url: "https://github.com/<organization-name>/" + repoName,
                    branch: "master")
                }
            }
        }
        stage("Build") {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: '<credentialsId>', keyFileVariable: 'privateKey', passphraseVariable: '', usernameVariable: '')]) {
                        try {
                            build_command = sh(script: "mvn clean install", returnStatus: true)
                            check_runs.buildGithubCheck(<REPO_NAME>, <COMMIT_ID>, privateKey, 'success', "build")
                        } catch(Exception e) {
                            check_runs.buildGithubCheck(<REPO_NAME>, <COMMIT_ID>, privateKey, 'failure', "build")
                            echo "Exception: ${e}"
                        }
                    }
                }
            }
        }
        stage("Unit Test") {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: '<credentialsId>', keyFileVariable: 'privateKey', passphraseVariable: '', usernameVariable: '')]) {
                        try {
                            def test = sh(script: "python unitTest.py", returnStdout: true)
                            check_runs.buildGithubCheck(<REPO_NAME>, <COMMIT_ID>, privateKey, 'success', "unit-test")
                        } catch(Exception e) {
                            check_runs.buildGithubCheck(<REPO_NAME>, <COMMIT_ID>, privateKey, 'failure', "unit-test")
                            echo "Exception: ${e}"
                        }
                    }
                }
            }
        }
        stage("Integration Test") {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: '<credentialsId>', keyFileVariable: 'privateKey', passphraseVariable: '', usernameVariable: '')]) {
                        try {
                            def test = sh(script: "python integrationTest.py", returnStdout: true)
                            check_runs.buildGithubCheck(<REPO_NAME>, <COMMIT_ID>, privateKey, 'success', "integration-test")
                        } catch(Exception e) {
                            check_runs.buildGithubCheck(<REPO_NAME>, <COMMIT_ID>, privateKey, 'failure', "integration-test")
                            echo "Exception: ${e}"
                        }
                    }
                }
            }
        }
    }
}