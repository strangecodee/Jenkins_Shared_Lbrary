def call(Map params = [:]) {
    def status = params.get('status', 'SUCCESS')
    def email = params.get('email', '')
    def buildUrl = env.BUILD_URL
    def buildNumber = env.BUILD_NUMBER
    def jobName = env.JOB_NAME
    
    if (!email) {
        echo "No notification email specified. Skipping notification."
        return
    }
    
    def subject = params.get('subject', "${status}: Jenkins Job '${jobName}' [Build #${buildNumber}]")
    def body = params.get('body', "The build finished with status: ${status}.\n\nView details: ${buildUrl}")
    
    echo "Sending ${status} notification to ${email}..."
    mail to: email,
         subject: subject,
         body: body
}
