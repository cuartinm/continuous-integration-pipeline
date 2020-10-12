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
        regexpFilterExpression: '^((false (opened|reopened|synchronize))|(true (closed)))? (develop|master)?$',
        regexpFilterText: '$merged $action $base_branch'
      ]
    ])
  ])
}

node {
  def target_branch = "$head_branch"

  if ("$action" == "closed") {
    target_branch = "$base_branch"
  }

  try {

    gitCheckout(
      repository: "$clone_url",
      branch: "$target_branch"
    )

    npmInit()

    npmTest()

    // sonarQubeScan(
    //   projectKey: "angular-demo-project",
    //   src: "./src"
    // )

    if("$merged".toBoolean()) {
      npmBuild()
      def props = readJSON file: 'package.json'
      uploadArtifact(
        username: "macuartin@gmail.com",
        password: "AP8fnUEUEdJ7UVbRVpFVWQxJfyn",
        artifactory_url: "https://macuartin.jfrog.io",
        repository: "generic-local",
        application: "${props.name}",
        version: "${props.version}",
        artifact: "${props.name}-${props.version}.tar.gz",
        target: "dist/${props.name}-${props.version}.tar.gz"
      )
    }

    withCredentials([string(credentialsId: 'GITHUB_ACCESS_TOKEN', variable: 'GITHUB_ACCESS_TOKEN')]) {       
      gitHubStatus(
        context: "continuous-integration",
        state: "success",
        description: "your Job was successful. You can check your logs in the following link ->",
        target_url: "${env.RUN_DISPLAY_URL}",
        github_access_token: "$GITHUB_ACCESS_TOKEN",
        statuses_url: "$statuses_url"
      )
    }

  } catch(Exception e) {
    currentBuild.result = 'FAILURE'
    echo "Exception: ${e}"
    withCredentials([string(credentialsId: 'GITHUB_ACCESS_TOKEN', variable: 'GITHUB_ACCESS_TOKEN')]) {       
      gitHubStatus(
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