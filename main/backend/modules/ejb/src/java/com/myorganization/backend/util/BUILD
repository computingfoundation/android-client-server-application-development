
java_library(name = 'util',
  dependencies = [
    #'3rdparty/main/org.mongodb:bson',
    '3rdparty/main/com.fasterxml.jackson.core:jackson-databind',
    '3rdparty/main/de.undercouch:bson4jackson',
    '3rdparty/main/org.mongodb:mongodb-driver',
    '3rdparty/main/org.slf4j:slf4j',
  ],
  sources = globs('*.java'),
)


#
# Note:
#
# The MongoDB driver library is only needed as it contains its own specific
# org.bson packages and classes that do not exist in the standalone org.bson library.
# These packages and classes are needed by the the BsonUtil class.
#
