def call(Map config = [:]) {
    def bastionIp   = config.get('bastionIp', '')
    def efsId       = config.get('efsId', '')
    def environment = config.get('environment', 'dev')
    def directory   = config.get('directory', '.')
    def credentialsId = config.get('credentialsId', 'aws-ssh-key-id')

    echo "Preparing Ansible environment inside: ${directory}"
    dir(directory) {
        echo "Installing Ansible dependencies..."
        sh 'pip install --user boto3 botocore ansible-core --break-system-packages'

        echo "Installing Ansible collections..."
        sh 'ansible-galaxy collection install amazon.aws ansible.posix community.general'

        echo "Checking Ansible playbook syntax..."
        sh 'ansible-playbook --syntax-check site.yml'

        echo "Executing Ansible playbook for environment: ${environment}..."
        withCredentials([file(credentialsId: credentialsId, variable: 'SSH_KEY_FILE')]) {
            sh """
            ansible-playbook -i inventory/aws_ec2.yml site.yml \
                -e bastion_ip=${bastionIp} \
                -e grafana_efs_file_system_id=${efsId} \
                -e ansible_ssh_private_key_file=${SSH_KEY_FILE}
            """
        }
    }
}
