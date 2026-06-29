def call(Map config = [:]) {
    def action = config.get('action', 'apply')
    def environment = config.get('environment', 'dev')
    def directory = config.get('directory', '.')

    echo "Initializing Terraform inside: ${directory}"
    dir(directory) {
        sh 'terraform init'
        
        if (action == 'apply') {
            echo "Generating Terraform plan for environment: ${environment}"
            sh "terraform plan -var environment=${environment} -out=tfplan"
            
            echo "Applying Terraform plan..."
            sh "terraform apply -auto-approve tfplan"
        } else if (action == 'destroy') {
            echo "Destroying infrastructure for environment: ${environment}..."
            sh "terraform destroy -auto-approve -var environment=${environment}"
        } else {
            error "Unknown Terraform action: ${action}"
        }
    }
}
