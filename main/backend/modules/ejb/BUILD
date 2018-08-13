
# JVM binary that builds the libs used to create the EJB jar.
# --------------------------------------------------------------------------

jvm_binary(name = 'ejb',
  basename = 'ear-ejb',
  dependencies = [
    'modules/ejb/src/java/com/organization/backend/database',
    'modules/ejb/src/java/com/organization/backend/model',
    'modules/ejb/src/java/com/organization/backend/util',
    ':ejb-runtime-lib',
  ],
  resources = [
    'modules/ejb/src/resources/META-INF',
  ],
)

# Runtime libs for the binary
java_library(name = 'ejb-runtime-lib',
  dependencies = [
    '3rdparty/main/postgresql:postgresql',
    '3rdparty/main/org.json:json',
  ]
)

