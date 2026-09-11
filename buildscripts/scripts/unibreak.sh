#!/bin/bash -e
set -eo pipefail  # also when run as "bash script.sh", which ignores the flags in the shebang

. ../../include/path.sh

build=_build$ndk_suffix

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	rm -rf $build
	exit 0
else
	exit 255
fi

mkdir -p $build
cd $build

../configure \
	--host=$ndk_triple --with-pic \
	--enable-static --disable-shared

make -j$cores
make DESTDIR="$prefix_dir" install
