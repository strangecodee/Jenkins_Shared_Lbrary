def call() {
    echo "Starting full AWS infrastructure teardown..."
    sh 'bash destroy.sh'
}
