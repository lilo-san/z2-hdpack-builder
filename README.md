# Zelda 2 : HD Pack Builder
- Repository structure and usage

## original-assets
- Don't modify anything on this directory, used it as a reference. Maps missing from custom-assets will by taken from here.

## custom-assets
- Consider this directory the workspace. Copy from original-assets the maps to modify here, keep the same folder names and file names.

## hdpack-assets
- Don't modify anything on this directory, content is autogenerated by the tool.

## build-hdpack.sh and z2-hdpack-builder
- Requires Java 11 or higher, please download at: https://adoptopenjdk.net .
- build-hdpack.sh is a convenience method to use z2-hdpack-builder. Builds and executes z2-hdpack-builder to produce the output.