#!/bin/bash
#export Boost_DIR=/opt/boost-1.63.0-gcc4.9/ OpenCV_DIR=/opt/opencv2.4-gcc4.9/ PKG_CONFIG_PATH=/opt/libzip-1.2.0-gcc4.9/lib/pkgconfig PATH=/opt/openexr-2.2.0-bin/bin:/opt/protobuf-3.2.0-gcc4.9/bin/:/opt/python2.7-gcc4.9/bin:$PATH PYTHONPATH=/opt/dlib-19.3-gcc4.9/:/opt/opencv2.4-gcc4.9/lib:/opt/opencv2.4-gcc4.9/lib/python2.7/site-packages/ LD_LIBRARY_PATH=/opt/openexr-2.2.0-bin/lib:/opt/python2.7-gcc4.9/lib/:/opt/boost-1.63.0-gcc4.9/lib/:/opt/opencv2.4-gcc4.9/lib/:/opt/protobuf-3.2.0-gcc4.9/lib:/opt/glog-gcc4.9-bin/lib:/opt/gflags-gcc4.9-bin/lib:/opt/snappy-gcc4.9-bin/lib/:/opt/cuda/lib64/:/opt/libzip-1.2.0-gcc4.9/lib/ CC=/usr/bin/gcc-4.9 CXX=/usr/bin/g++-4.9
source /etc/profile.d/hadoop.sh

exec $@ 

