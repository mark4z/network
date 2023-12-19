FROM openjdk:17
COPY target/nio-0.0.1-SNAPSHOT.jar /app/nio-0.0.1-SNAPSHOT.jar
WORKDIR /app
CMD ["java", "-jar", "nio-0.0.1-SNAPSHOT.jar"]
