def call(Map params = [:]) {
    def status = params.get('status', 'SUCCESS')
    def email = params.get('email', '').trim()
    def buildUrl = env.BUILD_URL
    def buildNumber = env.BUILD_NUMBER
    def jobName = env.JOB_NAME

    def subject = params.get('subject', "${status}: Jenkins Job '${jobName}' [Build #${buildNumber}]")
    def defaultBody = """\
==================================================
Jenkins Pipeline Execution Alert
==================================================
Job Name:     ${jobName}
Build Number: #${buildNumber}
Status:       ${status}
Build URL:    ${buildUrl}
==================================================
This is an automated notification. Please do not reply directly.
"""
    def body = params.get('body', defaultBody)

    // Email Dispatch Block
    if (email) {
        def emailPattern = ~/^([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+)\.([a-zA-Z]{2,})$/
        if (email =~ emailPattern) {
            try {
                echo "Sending ${status} notification email to ${email}..."
                mail to: email,
                     subject: subject,
                     body: body
                echo "Notification email sent successfully."
            } catch (Exception e) {
                echo "WARNING: Failed to send email notification to ${email}. Error: ${e.getMessage()}"
            }
        } else {
            echo "WARNING: Notification email '${email}' is not in a valid format. Skipping email dispatch."
        }
    } else {
        echo "No notification email specified. Skipping email dispatch."
    }

    // Slack Dispatch Block
    try {
        // Try to bind slack-webhook-url credential if it exists
        def hasSlackCred = false
        try {
            withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'TEST_VAR')]) {
                hasSlackCred = true
            }
        } catch (Exception credErr) {
            echo "Slack credential 'slack-webhook-url' not found in Jenkins. Skipping Slack dispatch."
        }

        if (hasSlackCred) {
            withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'SLACK_WEBHOOK_URL')]) {
                if (SLACK_WEBHOOK_URL) {
                    echo "Sending ${status} notification to Slack..."
                    def color = (status == 'SUCCESS') ? 'good' : 'danger'
                    
                    // Escape special JSON characters
                    def escapedBody = body.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '')
                    def escapedSubject = subject.replace('\\', '\\\\').replace('"', '\\"')
                    
                    def payload = """{
                        "attachments": [
                            {
                                "color": "${color}",
                                "title": "${escapedSubject}",
                                "title_link": "${buildUrl}",
                                "text": "${escapedBody}",
                                "fallback": "${escapedSubject}",
                                "fields": [
                                    {
                                        "title": "Job Name",
                                        "value": "${jobName}",
                                        "short": true
                                    },
                                    {
                                        "title": "Build Number",
                                        "value": "#${buildNumber}",
                                        "short": true
                                    },
                                    {
                                        "title": "Status",
                                        "value": "${status}",
                                        "short": true
                                    }
                                ]
                            }
                        ]
                    }"""
                    
                    // Send to Slack using curl via environment variable to protect special characters
                    withEnv(["SLACK_PAYLOAD=${payload}", "SLACK_URL=${SLACK_WEBHOOK_URL}"]) {
                        sh(script: 'printf \'%s\' "$SLACK_PAYLOAD" | curl -s -X POST -H \'Content-type: application/json\' --data-binary @- "$SLACK_URL"')
                    }
                    echo "Slack notification sent successfully."
                }
            }
        }
    } catch (Exception e) {
        echo "WARNING: Failed to send Slack notification. Error: ${e.getMessage()}"
        echo "Continuing pipeline execution gracefully..."
    }
}
