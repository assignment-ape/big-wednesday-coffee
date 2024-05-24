FROM openjdk:17-alpine

WORKDIR /app

COPY wavedays-1.0.0.jar /app/wavedays-1.0.0.jar

CMD ["java", "-jar", "wavedays-1.0.0.jar"]
