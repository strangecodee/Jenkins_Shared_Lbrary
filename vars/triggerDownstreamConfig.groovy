def call(Map config = [:]) {
    def environment = config.get('environment', 'dev')
    def notificationEmail = config.get('notificationEmail', '')
    def tfDir = env.TF_DIR ?: 'terraform'
    
    dir(tfDir) {
        def bastionIp = sh(script: 'terraform output -raw bastion_public_ip', returnStdout: true).trim()
        def efsId = sh(script: 'terraform output -raw efs_id', returnStdout: true).trim()
        def albDnsName = sh(script: 'terraform output -raw alb_dns_name', returnStdout: true).trim()

        echo "Infrastructure provisioned successfully!"
        echo "Bastion IP: ${bastionIp}"
        echo "EFS ID: ${efsId}"
        echo "ALB DNS Name: ${albDnsName}"
        echo "Triggering downstream Ansible configuration job..."

        build job: 'Service-Configuration', wait: true, parameters: [
            string(name: 'BASTION_IP', value: bastionIp),
            string(name: 'EFS_ID', value: efsId),
            string(name: 'ENVIRONMENT', value: environment),
            string(name: 'NOTIFICATION_EMAIL', value: notificationEmail),
            string(name: 'ALB_DNS_NAME', value: albDnsName)
        ]
    }
}
