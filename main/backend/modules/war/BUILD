
# JVM binary that builds the libs used to create the WAR jar.
# --------------------------------------------------------------------------

jvm_binary(name = 'war',
  basename = 'ear-war',
  dependencies = [
    'modules/war/src/java/com/organization/backend',
    'modules/war/src/java/com/organization/backend/pages',
    'modules/war/src/java/com/organization/backend/rest',
    ':war-runtime-libs',
  ],
  resources = [
    'modules/war/src/webapp/WEB-INF',
  ],
)


# Runtime libs required to run the the webapp in the WildFly server

java_library(name = 'war-runtime-libs',
  dependencies = [
    '3rdparty/war/org.jboss.seam.conversation:seam-conversation-spi',
  ]
)
