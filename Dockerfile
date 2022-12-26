FROM ibm-semeru-runtimes:open-18-jre-focal AS TEMP

ENV APP_HOME=/app

WORKDIR $APP_HOME

# Copy gradle-related stuff into image
COPY .gradle .gradle
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./

# Build will fail but should download stuff
RUN ./gradlew build || true

# NOW copy the code in
COPY src ./src

# And then run a successful gradle build
RUN ./gradlew build

# Next stage of build
FROM ibm-semeru-runtimes:open-18-jre-focal

ENV ARTIFACT_NAME=ClippyTunes-0.0.1.jar
ENV APP_HOME=/app

WORKDIR $APP_HOME

COPY --from=TEMP $APP_HOME/build/libs/$ARTIFACT_NAME .

CMD ["sh", "-c", "java -jar $ARTIFACT_NAME"]