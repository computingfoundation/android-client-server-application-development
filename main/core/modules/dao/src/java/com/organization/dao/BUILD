
# JVM binary of the dao generator
# --------------------------------------------------------------------------

jvm_binary(name = 'dao',
  basename = 'dao-bin',
  dependencies = [
    ':dao-lib',
    ':dao-runtime-lib',
  ],
  main = 'com.fencedin.dao.DaoGenerator'
)


java_library(name = 'dao-lib',
  dependencies = [
    '3rdparty/de.greenrobot:greendao-generator',
  ],
  sources = globs('*.java'),
)




# Runtime libs for the binary
java_library(name = 'dao-runtime-lib',
  dependencies = [
    '3rdparty/org.freemarker:freemarker',
  ]
)
