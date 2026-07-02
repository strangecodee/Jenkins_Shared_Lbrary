def call() {
    echo "Checking if S3 state backend bucket exists..."
    def bucketExists = sh(script: "aws s3api head-bucket --bucket monitoring-stack-dev-state 2>/dev/null", returnStatus: true)
    if (bucketExists != 0) {
        echo "S3 state backend bucket does not exist. Bootstrapping state and lock resources..."
        dir('terraform-bootstrap') {
            sh 'terraform init'
            sh 'terraform apply -auto-approve'
        }
        echo "State and lock resources successfully bootstrapped!"
    } else {
        echo "S3 state backend bucket exists. Proceeding..."
    }
}
