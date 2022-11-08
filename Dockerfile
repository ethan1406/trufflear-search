FROM amazoncorretto:18
MAINTAINER trufflear.com
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
EXPOSE 50051
ENTRYPOINT ["java","-jar","/app.jar"]