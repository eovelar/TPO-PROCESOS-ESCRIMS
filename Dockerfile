
FROM eclipse-temurin:27-jdk AS build

WORKDIR /app

COPY src ./src

RUN find src -name "*.java" | sort > sources.txt \
    && mkdir -p out \
    && javac --enable-preview --release 27 \
       -encoding UTF-8 -d out @sources.txt

FROM eclipse-temurin:27-jre

WORKDIR /app

COPY --from=build /app/out ./out
COPY --from=build /app/src/web ./src/web

EXPOSE 10000

CMD ["java", "--enable-preview", "-cp", "out", "controller.ServidorDemo"]
