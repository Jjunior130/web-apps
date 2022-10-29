FROM openjdk:8-alpine

COPY target/uberjar/web-apps.jar /web-apps/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/web-apps/app.jar"]
