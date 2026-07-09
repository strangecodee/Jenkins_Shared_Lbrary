def call(Map params = [:]) {
    def status = params.get('status', 'SUCCESS')
    def email = params.get('email', '').trim()
    def buildUrl = env.BUILD_URL
    def buildNumber = env.BUILD_NUMBER
    def jobName = env.JOB_NAME

    def subject = params.get('subject', "${status}: Jenkins Job '${jobName}' [Build #${buildNumber}]")
    def body = params.get('body', '')

    // Extract dynamic variables from the pipeline body text
    def envName = "N/A"
    def envMatcher = body =~ /(?i)Environment:\s*([^\s\n]+)/
    if (envMatcher.find()) { envName = envMatcher.group(1).trim() }

    def actionName = "N/A"
    def actionMatcher = body =~ /(?i)Action:\s*([^\s\n]+)/
    if (actionMatcher.find()) { actionName = actionMatcher.group(1).trim() }

    def bastionIp = "N/A"
    def bastionMatcher = body =~ /(?i)Bastion (Host IP|IP):\s*([^\s\n]+)/
    if (bastionMatcher.find()) { bastionIp = bastionMatcher.group(2).trim() }

    def efsId = "N/A"
    def efsMatcher = body =~ /(?i)EFS (File System ID|ID):\s*([^\s\n]+)/
    if (efsMatcher.find()) { efsId = efsMatcher.group(2).trim() }

    def grafanaUrl = ""
    def grafanaMatcher = body =~ /(?i)Grafana Portal:\s*(http[s]?:\/\/[^\s\n]+)/
    if (grafanaMatcher.find()) { grafanaUrl = grafanaMatcher.group(1).trim() }

    // Setup Colors
    def statusClass = (status == 'SUCCESS') ? 'success' : 'failure'
    def headerClass = (status == 'SUCCESS') ? 'success' : 'failure'
    def slackColor = (status == 'SUCCESS') ? '#10b981' : '#ef4444'
    def slackHeaderEmoji = (status == 'SUCCESS') ? '✅' : '❌'
    def subjectTitle = (status == 'SUCCESS') ? 'Deployment Completed Successfully' : 'Deployment Failed'

    // Build Grafana button if URL exists
    def grafanaBtnHtml = ""
    if (grafanaUrl) {
        grafanaBtnHtml = """<a href="${grafanaUrl}" class="btn btn-secondary">Access Grafana Portal</a>"""
    }

    // Build modern HTML email body
    def htmlBody = """<!DOCTYPE html>
<html>
<head>
  <style>
    body {
      font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
      background-color: #f7f9fc;
      margin: 0;
      padding: 0;
    }
    .wrapper {
      padding: 40px 20px;
    }
    .container {
      max-width: 600px;
      margin: 0 auto;
      background-color: #ffffff;
      border-radius: 12px;
      overflow: hidden;
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.05);
      border: 1px solid #eef2f6;
    }
    .header {
      padding: 32px 24px;
      color: #ffffff;
      text-align: center;
    }
    .header.success {
      background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    }
    .header.failure {
      background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    }
    .header h2 {
      margin: 0 0 8px 0;
      font-size: 22px;
      font-weight: 700;
      letter-spacing: -0.5px;
    }
    .header p {
      margin: 0;
      font-size: 14px;
      opacity: 0.9;
    }
    .content {
      padding: 40px 32px;
    }
    .status-badge {
      display: inline-block;
      padding: 6px 16px;
      border-radius: 30px;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      margin-bottom: 24px;
    }
    .status-badge.success {
      background-color: #ecfdf5;
      color: #059669;
    }
    .status-badge.failure {
      background-color: #fef2f2;
      color: #dc2626;
    }
    .intro {
      font-size: 15px;
      color: #4a5568;
      margin-bottom: 30px;
      line-height: 1.6;
    }
    .section-title {
      font-size: 12px;
      font-weight: 700;
      color: #94a3b8;
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 12px;
    }
    .meta-grid {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      overflow: hidden;
      margin-bottom: 30px;
    }
    .meta-row {
      display: flex;
      border-bottom: 1px solid #e2e8f0;
    }
    .meta-row:last-child {
      border-bottom: none;
    }
    .meta-label {
      width: 160px;
      background-color: #f8fafc;
      padding: 12px 16px;
      font-size: 13px;
      font-weight: 600;
      color: #64748b;
      border-right: 1px solid #e2e8f0;
    }
    .meta-value {
      flex: 1;
      padding: 12px 16px;
      font-size: 13px;
      color: #1e293b;
      font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
    }
    .action-group {
      text-align: center;
      margin-top: 35px;
    }
    .btn {
      display: inline-block;
      padding: 12px 24px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      text-decoration: none;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      margin: 8px;
    }
    .btn-primary {
      background-color: #2563eb;
      color: #ffffff !important;
    }
    .btn-secondary {
      background-color: #4f46e5;
      color: #ffffff !important;
    }
    .footer {
      padding: 24px;
      background-color: #f8fafc;
      border-top: 1px solid #f1f5f9;
      text-align: center;
      font-size: 12px;
      color: #94a3b8;
      line-height: 1.5;
    }
    .footer a {
      color: #64748b;
      text-decoration: underline;
    }
  </style>
</head>
<body>
  <div class="wrapper">
    <div class="container">
      <div class="header ${headerClass}">
        <h2>${subjectTitle}</h2>
        <p>Jenkins Observability Pipeline</p>
      </div>
      <div class="content">
        <div class="status-badge ${statusClass}">${status}</div>
        <div class="intro">
          The observability monitoring infrastructure pipeline execution completed with a status of <strong>${status}</strong>. Please find the deployment summary and control actions below.
        </div>
        
        <div class="section-title">Deployment Configuration</div>
        <div class="meta-grid">
          <div class="meta-row">
            <div class="meta-label">Pipeline Job</div>
            <div class="meta-value">${jobName}</div>
          </div>
          <div class="meta-row">
            <div class="meta-label">Build Execution</div>
            <div class="meta-value">#${buildNumber}</div>
          </div>
          <div class="meta-row">
            <div class="meta-label">Environment</div>
            <div class="meta-value">${envName}</div>
          </div>
          <div class="meta-row">
            <div class="meta-label">Action Performed</div>
            <div class="meta-value">${actionName}</div>
          </div>
          <div class="meta-row">
            <div class="meta-label">Bastion Host IP</div>
            <div class="meta-value">${bastionIp}</div>
          </div>
          <div class="meta-row">
            <div class="meta-label">EFS File System</div>
            <div class="meta-value">${efsId}</div>
          </div>
        </div>
        
        <div class="action-group">
          <a href="${buildUrl}" class="btn btn-primary">View Build Logs</a>
          ${grafanaBtnHtml}
        </div>
      </div>
      <div class="footer">
        This is an automated notification. If you have questions about this build, please refer to the <a href="${buildUrl}">Jenkins execution output</a>.
      </div>
    </div>
  </div>
</body>
</html>
"""

    // Email Dispatch Block
    if (email) {
        def emailPattern = ~/^([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+)\.([a-zA-Z]{2,})$/
        if (email =~ emailPattern) {
            try {
                echo "Sending ${status} notification email to ${email}..."
                mail to: email,
                     subject: subject,
                     mimeType: 'text/html',
                     body: htmlBody
                echo "Notification email sent successfully."
            } catch (Exception e) {
                echo "WARNING: Failed to send email notification to ${email}. Error: ${e.getMessage()}"
            }
        } else {
            echo "WARNING: Notification email '${email}' is not in a valid format. Skipping email dispatch."
        }
    }

    // Slack Dispatch Block
    try {
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
                    
                    // Escape special JSON characters
                    def escapedSubject = subject.replace('\\', '\\\\').replace('"', '\\"')
                    
                    def slackGrafanaButtonJson = ""
                    if (grafanaUrl) {
                        slackGrafanaButtonJson = """,
                        {
                            "type": "button",
                            "text": {
                                "type": "plain_text",
                                "text": "Access Grafana",
                                "emoji": true
                            },
                            "url": "${grafanaUrl}",
                            "style": "primary",
                            "action_id": "btn_grafana"
                        }"""
                    }

                    def payload = """{
                        "attachments": [
                            {
                                "color": "${slackColor}",
                                "fallback": "${escapedSubject}",
                                "blocks": [
                                    {
                                        "type": "header",
                                        "text": {
                                            "type": "plain_text",
                                            "text": "${slackHeaderEmoji} ${escapedSubject}",
                                            "emoji": true
                                        }
                                    },
                                    {
                                        "type": "section",
                                        "text": {
                                            "type": "mrkdwn",
                                            "text": "*Observability Stack Execution Run*\\nPipeline run details for Job *${jobName}*."
                                        }
                                    },
                                    {
                                        "type": "divider"
                                    },
                                    {
                                        "type": "section",
                                        "fields": [
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Environment:*\\n${envName}"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Action:*\\n${actionName}"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*Bastion IP:*\\n`${bastionIp}`"
                                            },
                                            {
                                                "type": "mrkdwn",
                                                "text": "*EFS Volume:*\\n`${efsId}`"
                                            }
                                        ]
                                    },
                                    {
                                        "type": "actions",
                                        "elements": [
                                            {
                                                "type": "button",
                                                "text": {
                                                    "type": "plain_text",
                                                    "text": "View Logs",
                                                    "emoji": true
                                                },
                                                "url": "${buildUrl}",
                                                "action_id": "btn_build"
                                            }
                                            ${slackGrafanaButtonJson}
                                        ]
                                    }
                                ]
                            }
                        ]
                    }"""
                    
                    withEnv(["SLACK_PAYLOAD=${payload}", "SLACK_URL=${SLACK_WEBHOOK_URL}"]) {
                        sh(script: 'printf \'%s\' "$SLACK_PAYLOAD" | curl -s -X POST -H \'Content-type: application/json\' --data-binary @- "$SLACK_URL"')
                    }
                    echo "Slack notification sent successfully."
                }
            }
        }
    } catch (Exception e) {
        echo "WARNING: Failed to send Slack notification. Error: ${e.getMessage()}"
    }
}
