
# BUILD to provide Commons in a single artifact; used only if needed.
# It is named BUILD-ALL to prevent it from being included as a normal BUILD file during regular compilation.
# If used, it should be named BUILD.

java_library(name = 'commons',
  provides = artifact(
    org = 'com.fencedin.commons',
    name = 'commons-all',
    repo = public,
  ),
  dependencies = [
    '3rdparty/org.slf4j:slf4j',
    '3rdparty/org.json:json',
    '3rdparty/com.vividsolutions:jts',
    '3rdparty/com.spatial4j:spatial4j',
    '3rdparty/net.sf.geographiclib:GeographicLib-Java',
    '3rdparty/org.apache.httpcomponents:httpclient',
    '3rdparty/de.undercouch:bson4jackson',
    '3rdparty/org.apache.commons:commons-lang3',
    '3rdparty/org.apache.commons:commons-validator',
  ],
  sources = rglobs('*.java'),
)

