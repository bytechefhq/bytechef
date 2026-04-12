#!/bin/sh

JAVA_BASE_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=GMT \
  -Djava.io.tmpdir=/opt/bytechef/server/tmp \
  -Dloader.path=/opt/bytechef/external_jars"

JAVA_SERVER_OPTS="-Dserver.tomcat.basedir=/opt/bytechef/server \
  -Dserver.tomcat.accesslog.directory=/opt/bytechef/server/logs"

SPRING_PROFILES="${SPRING_PROFILES_ACTIVE:-}"

case "$1" in
  liquibase)
    shift

    if [ -n "$SPRING_PROFILES" ]; then
      LIQUIBASE_PROFILES="liquibase,${SPRING_PROFILES}"
    else
      LIQUIBASE_PROFILES="liquibase"
    fi

    echo "Running Liquibase database migration..."

    exec java \
      $JAVA_BASE_OPTS \
      -jar server/server-app.jar \
      --spring.profiles.active="${LIQUIBASE_PROFILES}" \
      "$@"
    ;;
  *)
    exec java \
      $JAVA_BASE_OPTS \
      $JAVA_SERVER_OPTS \
      -jar server/server-app.jar \
      "$@"
    ;;
esac
