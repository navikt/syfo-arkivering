FROM gcr.io/distroless/java17@sha256:901215ab3ae619500f184668461cf901830e7a9707f8f9c016d9c08d8060db5a

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseParallelGC -XX:ActiveProcessorCount=2"

COPY build/libs/app.jar /app/
WORKDIR /app
CMD ["app.jar"]
