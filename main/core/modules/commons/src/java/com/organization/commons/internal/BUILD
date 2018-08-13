
# This package contains internal commons classes, classes that do not depend on other commons libraries to prevent
# cyclical dependencies.
#
# This package is also not meant to be used outside the commons library. It has a "provides" attribute only to allow the
# packages that depend on it to be built.

java_library(name = 'internal',
  provides = artifact(
    org = 'com.fencedin.commons',
    name = 'internal',
    repo = public,
  ),
  sources = globs('*.java'),
)
