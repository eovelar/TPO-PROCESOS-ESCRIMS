
FROM eclipse-temurin:27-jdk AS build

WORKDIR /app

COPY src ./src
COPY lib ./lib

RUN mkdir -p out \
    && find src -name "*.java" | sort > sources.txt \
    && javac --enable-preview --release 27 \
       -encoding UTF-8 \
       -cp "lib/*" \
       -d out \
       @sources.txt

FROM eclipse-temurin:27-jre

WORKDIR /app

COPY --from=build /app/out ./out
COPY --from=build /app/lib ./lib
COPY --from=build /app/src/web ./src/web

EXPOSE 10000

CMD ["java", "--enable-preview", "-cp", "out:lib/*", "controller.ServidorWeb"]
