
# Package containing classes used to validate data

java_library(name = 'validation',
  provides = artifact(
    org = 'com.fencedin.commons',
    name = 'validation',
    repo = public,
  ),
  dependencies = [
    'modules/commons/src/java/com/fencedin/commons/configuration',
    'modules/commons/src/java/com/fencedin/commons/model',
    'modules/commons/src/java/com/fencedin/commons/gis',
    'modules/commons/src/java/com/fencedin/commons/base',
    '3rdparty/org.apache.commons:commons-lang3',
    '3rdparty/org.apache.commons:commons-validator',
    '3rdparty/com.vividsolutions:jts',
  ],
  sources = globs('*.java'),
)
