git clone https://github.com/couchbase/couchbase-lite-ios.git
pushd couchbase-lite-ios
git checkout CBL-7695
git submodule update --init --recursive
popd

git clone https://github.com/couchbaselabs/couchbase-lite-ios-ee.git
pushd couchbase-lite-ios-ee
git checkout CBL-7695
git submodule update --init --recursive
./Scripts/prepare_project.sh --notest
popd
