def call(Map params = [:]) {
    def status = params.get('status', 'SUCCESS')
    def email = params.get('email', '').trim()
    def buildUrl = env.BUILD_URL
    def buildNumber = env.BUILD_NUMBER
    def jobName = env.JOB_NAME

    if (!email) {
        echo "No notification email specified. Skipping notification."
        return
    }

    // Email format validation
    def emailPattern = ~/^([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+)\.([a-zA-Z]{2,})$/
    if (!(email =~ emailPattern)) {
        echo "WARNING: Notification email '${email}' is not in a valid format. Skipping email dispatch."
        return
    }

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

    try {
        echo "Sending ${status} notification email to ${email}..."
        mail to: email,
             subject: subject,
             body: body
        echo "Notification email sent successfully."
    } catch (Exception e) {
        echo "WARNING: Failed to send email notification to ${email}. Error: ${e.getMessage()}"
        echo "Continuing pipeline execution gracefully..."
    }
}
