#summary How to build Talc.
#labels Featured

# How to build Talc #

Talc currently still relies on various bits of software.jessies.org infrastructure. In particular, it uses the generic make rules that all software.jessies.org projects use.

I intend to make Talc an independent stand-alone project, but for now you'll need a copy of http://software.jessies.org/salma-hayek/ unpacked in the same directory you "svn co"ed talc into. That is, you should have something like ~/src/talc/ and ~/src/salma-hayek/. You shouldn't need to "make" in salma-hayek, nor bother to "svn update" it, but if you have trouble building, that would be something worth trying.

Hopefully, assuming you have a copy of Talc and a copy of salma-hayek, you should be able to just type "make" in talc/ and then use talc/bin/talc.

Note that the GCJ build (to build a stand-alone binary which allows you to use "#!/usr/bin/talc" to run Talc scripts directly) isn't really supported. GCJ-built binaries were significantly slower than Talc running on Sun's JVM in important areas (particularly arithmetic) even when Talc was an AST-walking interpreter. Now it's a compiler, the situation for GCJ is even worse: where Sun's JVM compiles and optimizes the dynamically-generated code, GCJ is reduced to interpreting it.

The plan going forward is to build a stand-alone binary that links with libjvm.so plus whatever JNI code we need in Talc's libraries.