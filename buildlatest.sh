echo "Bygger spinnsyn-arkivering latest"

./gradlew bootJar

docker build . -t spinnsyn-arkivering:latest
