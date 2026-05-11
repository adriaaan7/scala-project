FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.11_2.12.21

WORKDIR /app

COPY . .

EXPOSE 8080

CMD ["sbt", "run"]