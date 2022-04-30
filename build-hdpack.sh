# This script should be run from the directory it is in

# Build
javac -d ./build ./z2-hdpack-builder/src/org/lilosan/tiled/*.java ./z2-hdpack-builder/src/org/lilosan/tiled/*.java
cd ./build
jar cvf z2-hdpack-builder.jar org/lilosan/tiled/*

# Run
java -cp z2-hdpack-builder.jar org.lilosan.tiled.Zelda2HiResGenerator

# Clean
cd ..
rm -rf build